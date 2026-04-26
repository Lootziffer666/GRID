package com.painkiller.domain.github

import com.painkiller.domain.target.BranchTarget
import com.painkiller.domain.target.RepoTarget
import com.painkiller.domain.target.TargetPath
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleFileCommitOrchestratorTest {

    private val target = RepoTarget(
        owner = "octocat",
        repo = "hello-world",
        branch = BranchTarget(name = "main"),
        targetPath = TargetPath(normalized = "docs"),
    )

    private fun input(
        target: RepoTarget = this.target,
        fileName: String = "notes.md",
        contentBase64: String = "aGVsbG8=",
        commitMessage: String = "Add notes.md",
    ) = SingleFileCommitInput(
        target = target,
        fileName = fileName,
        contentBase64 = contentBase64,
        commitMessage = commitMessage,
    )

    @Test
    fun success_runsAllSixStepsInOrder_andUpdatesRefLastWithExpectedSha() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue("expected Success but got $result", result is SingleFileCommitResult.Success)
        result as SingleFileCommitResult.Success
        assertEquals("commit-sha-789", result.commitSha)
        assertEquals("https://github.com/octocat/hello-world/commit/commit-sha-789", result.commitUrl)
        assertEquals("docs/notes.md", result.committedPath)

        // Step order: getRef, getCommit, createBlob, createTree, createCommit, updateRef.
        assertEquals(
            listOf("getRef", "getCommit", "createBlob", "createTree", "createCommit", "updateRef"),
            api.calls,
        )

        // updateRef must use the original branch SHA so a concurrent push surfaces as ShaMismatch.
        assertEquals("base-ref-sha", api.lastUpdateRefExpectedSha)
        assertEquals("commit-sha-789", api.lastUpdateRefRequest?.sha)
        assertEquals(false, api.lastUpdateRefRequest?.force)
        assertEquals("refs/heads/main", api.lastUpdateRefRef)
    }

    @Test
    fun success_emptyTargetFolder_committedPathIsJustFileName() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        val rootTarget = target.copy(targetPath = TargetPath(normalized = ""))
        val result = orchestrator.execute(input(target = rootTarget, fileName = "README.md"))

        assertTrue(result is SingleFileCommitResult.Success)
        assertEquals("README.md", (result as SingleFileCommitResult.Success).committedPath)
    }

    @Test
    fun shaMismatch_duringUpdateRef_isMappedAndRefHasNotAdvanced() = runTest {
        val api = HappyPathFakeApi(updateRefThrows = GithubGitDataException.ShaMismatch())
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is SingleFileCommitResult.ShaMismatch)
        // updateRef was attempted (last step), but the orchestrator stops there cleanly.
        assertTrue("getRef" in api.calls)
        assertTrue("createCommit" in api.calls)
        assertEquals("updateRef", api.calls.last())
    }

    @Test
    fun protectedBranch_duringUpdateRef_isMapped() = runTest {
        val api = HappyPathFakeApi(updateRefThrows = GithubGitDataException.ProtectedBranch())
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is SingleFileCommitResult.ProtectedBranch)
    }

    @Test
    fun authError_atGetRef_stopsBeforeAnyMutation() = runTest {
        val api = HappyPathFakeApi(getRefThrows = GithubGitDataException.AuthRequired())
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is SingleFileCommitResult.AuthError)
        // Failing at step 1 means no blob, tree, commit, or ref update happened.
        assertEquals(listOf("getRef"), api.calls)
    }

    @Test
    fun branchNotFound_atGetRef_stopsBeforeAnyMutation() = runTest {
        val api = HappyPathFakeApi(getRefThrows = GithubGitDataException.RefNotFound())
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is SingleFileCommitResult.BranchNotFound)
        assertEquals(listOf("getRef"), api.calls)
    }

    @Test
    fun networkError_atGetRef_stopsBeforeAnyMutation() = runTest {
        val api = HappyPathFakeApi(getRefThrows = GithubGitDataException.NetworkUnavailable())
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is SingleFileCommitResult.NetworkError)
        assertEquals(listOf("getRef"), api.calls)
    }

    @Test
    fun permissionDenied_atCreateBlob_stopsBeforeRefUpdate() = runTest {
        val api = HappyPathFakeApi(createBlobThrows = GithubGitDataException.PermissionDenied())
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is SingleFileCommitResult.PermissionError)
        assertTrue("createBlob" in api.calls)
        assertTrue("updateRef" !in api.calls)
    }

    @Test
    fun blankFileName_isInvalidInput_andDoesNotCallApi() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(fileName = "   "))

        assertTrue(result is SingleFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun fileNameWithSlash_isInvalidInput_andDoesNotCallApi() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(fileName = "sub/notes.md"))

        assertTrue(result is SingleFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun fileNameDotDot_isInvalidInput() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(fileName = ".."))

        assertTrue(result is SingleFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun blankCommitMessage_isInvalidInput() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(commitMessage = "   "))

        assertTrue(result is SingleFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun emptyContent_isInvalidInput() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(contentBase64 = ""))

        assertTrue(result is SingleFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun successCommitUrlMayBeNull_whenApiOmitsHtmlUrl() = runTest {
        val api = HappyPathFakeApi(commitHtmlUrl = null)
        val orchestrator = SingleFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is SingleFileCommitResult.Success)
        assertNull((result as SingleFileCommitResult.Success).commitUrl)
        assertNotNull(result.commitSha)
    }

    @Test
    fun createTree_request_includesBaseTreeAndOneBlobEntry() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        orchestrator.execute(input())

        val tree = api.lastCreateTreeRequest
        assertNotNull(tree)
        assertEquals("base-tree-sha", tree!!.baseTree)
        assertEquals(1, tree.tree.size)
        val entry = tree.tree.single()
        assertEquals("docs/notes.md", entry.path)
        assertEquals(TreeEntry.MODE_FILE, entry.mode)
        assertEquals(TreeEntry.TYPE_BLOB, entry.type)
        assertEquals("blob-sha-456", entry.sha)
        assertNull(entry.content) // we use sha-mode (blob already created), not inline content
    }

    @Test
    fun createCommit_request_singleParentIsTheOriginalBranchHead() = runTest {
        val api = HappyPathFakeApi()
        val orchestrator = SingleFileCommitOrchestrator(api)

        orchestrator.execute(input(commitMessage = "Custom message"))

        val commit = api.lastCreateCommitRequest
        assertNotNull(commit)
        assertEquals("Custom message", commit!!.message)
        assertEquals("new-tree-sha", commit.tree)
        assertEquals(listOf("base-ref-sha"), commit.parents)
    }
}

/**
 * Recording fake of [GithubGitDataApi] tuned for orchestrator tests.
 *
 * Returns a deterministic happy path unless one of the `*Throws` parameters
 * is set, in which case the corresponding step throws that exception
 * instead of returning. The recorded `calls` list documents which API
 * methods the orchestrator actually reached, which is the easiest way to
 * assert that mutations did not happen after a failure.
 */
private class HappyPathFakeApi(
    private val getRefThrows: GithubGitDataException? = null,
    private val getCommitThrows: GithubGitDataException? = null,
    private val createBlobThrows: GithubGitDataException? = null,
    private val createTreeThrows: GithubGitDataException? = null,
    private val createCommitThrows: GithubGitDataException? = null,
    private val updateRefThrows: GithubGitDataException? = null,
    private val commitHtmlUrl: String? = "https://github.com/octocat/hello-world/commit/commit-sha-789",
) : GithubGitDataApi {

    val calls: MutableList<String> = mutableListOf()

    var lastCreateTreeRequest: CreateTreeRequest? = null
        private set
    var lastCreateCommitRequest: CreateCommitRequest? = null
        private set
    var lastUpdateRefExpectedSha: String? = null
        private set
    var lastUpdateRefRequest: UpdateRefRequest? = null
        private set
    var lastUpdateRefRef: String? = null
        private set

    override suspend fun getRef(owner: String, repo: String, ref: String): GitRef {
        calls += "getRef"
        getRefThrows?.let { throw it }
        return GitRef(
            ref = "refs/$ref",
            obj = GitRefObject(sha = "base-ref-sha", type = "commit"),
        )
    }

    override suspend fun getCommit(owner: String, repo: String, commitSha: String): GitCommit {
        calls += "getCommit"
        getCommitThrows?.let { throw it }
        return GitCommit(
            sha = commitSha,
            message = "base commit",
            tree = GitTreeRef(sha = "base-tree-sha"),
            parents = emptyList(),
        )
    }

    override suspend fun createBlob(
        owner: String,
        repo: String,
        request: CreateBlobRequest,
    ): CreateBlobResponse {
        calls += "createBlob"
        createBlobThrows?.let { throw it }
        return CreateBlobResponse(sha = "blob-sha-456")
    }

    override suspend fun createTree(
        owner: String,
        repo: String,
        request: CreateTreeRequest,
    ): CreateTreeResponse {
        calls += "createTree"
        lastCreateTreeRequest = request
        createTreeThrows?.let { throw it }
        return CreateTreeResponse(sha = "new-tree-sha", tree = request.tree)
    }

    override suspend fun createCommit(
        owner: String,
        repo: String,
        request: CreateCommitRequest,
    ): CreateCommitResponse {
        calls += "createCommit"
        lastCreateCommitRequest = request
        createCommitThrows?.let { throw it }
        return CreateCommitResponse(
            sha = "commit-sha-789",
            message = request.message,
            htmlUrl = commitHtmlUrl,
        )
    }

    override suspend fun updateRef(
        owner: String,
        repo: String,
        ref: String,
        request: UpdateRefRequest,
        expectedSha: String,
    ): GitRef {
        calls += "updateRef"
        lastUpdateRefRef = ref
        lastUpdateRefRequest = request
        lastUpdateRefExpectedSha = expectedSha
        updateRefThrows?.let { throw it }
        return GitRef(
            ref = ref,
            obj = GitRefObject(sha = request.sha, type = "commit"),
        )
    }
}
