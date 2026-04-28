package com.painkiller.data.github

import com.painkiller.data.security.SecureTokenStore
import com.painkiller.domain.github.CreateBlobRequest
import com.painkiller.domain.github.CreateBlobResponse
import com.painkiller.domain.github.CreateCommitRequest
import com.painkiller.domain.github.CreateCommitResponse
import com.painkiller.domain.github.CreateTreeRequest
import com.painkiller.domain.github.CreateTreeResponse
import com.painkiller.domain.github.GitCommit
import com.painkiller.domain.github.GitRef
import com.painkiller.domain.github.GitRefObject
import com.painkiller.domain.github.GitTreeRef
import com.painkiller.domain.github.GithubGitDataApi
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.SingleFileCommitResult
import com.painkiller.domain.github.UpdateRefRequest
import com.painkiller.domain.github.UploadPayload
import com.painkiller.domain.lfs.LfsBatchObjectResponse
import com.painkiller.domain.lfs.LfsBatchResponse
import com.painkiller.domain.lfs.LfsObjectAction
import com.painkiller.domain.lfs.LfsObjectActions
import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class GithubLfsRepositoryTest {

    @Test
    fun uploadFailure_preventsPointerCommit() = runTest {
        var committed = false
        val lfsApi = object : KtorGithubLfsApi(PainkillerHttpClient.create(), { "token" }) {
            override suspend fun requestUploadAction(owner: String, repo: String, oid: String, size: Long, refName: String): LfsBatchResponse =
                LfsBatchResponse(objects = listOf(LfsBatchObjectResponse(oid, size, actions = LfsObjectActions(upload = LfsObjectAction("https://upload")))))

            override suspend fun uploadObject(action: LfsObjectAction, payload: UploadPayload) {
                throw GithubGitDataException.NetworkUnavailable()
            }
        }
        val singleRepo = object : SingleFileCommitRepository(fakeGitDataApi(), tokenStore()) {
            override suspend fun commitSingleFile(input: com.painkiller.domain.github.SingleFileCommitInput): SingleFileCommitResult {
                committed = true
                return super.commitSingleFile(input)
            }
        }
        val repo = GithubLfsRepository(lfsApi, singleRepo)

        val result = repo.uploadSingleFileAndCommitPointer(target(), "big.bin", payload("data"), "msg")

        assertTrue(result is GithubLfsUploadResult.Failure)
        assertTrue(!committed)
    }

    @Test
    fun shaMismatch_isMappedToFailure() = runTest {
        val lfsApi = object : KtorGithubLfsApi(PainkillerHttpClient.create(), { "token" }) {
            override suspend fun requestUploadAction(owner: String, repo: String, oid: String, size: Long, refName: String): LfsBatchResponse =
                LfsBatchResponse(objects = listOf(LfsBatchObjectResponse(oid, size, actions = null)))
        }
        val singleRepo = SingleFileCommitRepository(fakeGitDataApi(shaMismatch = true), tokenStore())
        val repo = GithubLfsRepository(lfsApi, singleRepo)

        val result = repo.uploadSingleFileAndCommitPointer(target(), "big.bin", payload("data"), "msg")

        assertTrue(result is GithubLfsUploadResult.Failure)
    }

    @Test
    fun missingUploadHref_returnsFailure_beforeCommit() = runTest {
        var committed = false
        val lfsApi = object : KtorGithubLfsApi(PainkillerHttpClient.create(), { "token" }) {
            override suspend fun requestUploadAction(owner: String, repo: String, oid: String, size: Long, refName: String): LfsBatchResponse =
                LfsBatchResponse(
                    objects = listOf(
                        LfsBatchObjectResponse(
                            oid = oid,
                            size = size,
                            actions = LfsObjectActions(upload = LfsObjectAction(href = "")),
                        ),
                    ),
                )
        }
        val singleRepo = object : SingleFileCommitRepository(fakeGitDataApi(), tokenStore()) {
            override suspend fun commitSingleFile(input: com.painkiller.domain.github.SingleFileCommitInput): SingleFileCommitResult {
                committed = true
                return super.commitSingleFile(input)
            }
        }
        val repo = GithubLfsRepository(lfsApi, singleRepo)

        val result = repo.uploadSingleFileAndCommitPointer(target(), "big.bin", payload("data"), "msg")

        assertTrue(result is GithubLfsUploadResult.Failure)
        assertTrue(!committed)
    }

    private fun payload(value: String): UploadPayload = object : UploadPayload {
        private val bytes = value.toByteArray()
        override val sizeBytes: Long = bytes.size.toLong()
        override fun openStream() = ByteArrayInputStream(bytes)
    }

    private fun target() = RepoTarget("o", "r", BranchTarget("main"), TargetPath(""))

    private fun tokenStore() = object : SecureTokenStore {
        override suspend fun writeGithubToken(token: String) = Unit
        override suspend fun readGithubToken(): String? = "token"
        override suspend fun clearGithubToken() = Unit
    }

    private fun fakeGitDataApi(shaMismatch: Boolean = false) = object : GithubGitDataApi {
        override suspend fun getRef(owner: String, repo: String, ref: String): GitRef =
            GitRef(ref = "refs/heads/main", obj = GitRefObject(sha = "base", type = "commit"))

        override suspend fun getCommit(owner: String, repo: String, commitSha: String): GitCommit =
            GitCommit(sha = "base", message = "m", tree = GitTreeRef("tree"))

        override suspend fun createBlob(owner: String, repo: String, request: CreateBlobRequest): CreateBlobResponse =
            CreateBlobResponse("blob")

        override suspend fun createTree(owner: String, repo: String, request: CreateTreeRequest): CreateTreeResponse =
            CreateTreeResponse("tree")

        override suspend fun createCommit(owner: String, repo: String, request: CreateCommitRequest): CreateCommitResponse =
            CreateCommitResponse(sha = "commit", message = "m", htmlUrl = "url")

        override suspend fun updateRef(owner: String, repo: String, ref: String, request: UpdateRefRequest, expectedSha: String): GitRef {
            if (shaMismatch) throw GithubGitDataException.ShaMismatch()
            return GitRef(ref = "refs/heads/main", obj = GitRefObject(sha = "commit", type = "commit"))
        }
    }
}
