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

class MultiFileCommitOrchestratorTest {

    private val target = RepoTarget(
        owner = "octocat",
        repo = "hello-world",
        branch = BranchTarget(name = "main"),
        targetPath = TargetPath(normalized = "uploads"),
    )

    private val rootTarget = target.copy(targetPath = TargetPath(normalized = ""))

    private fun entry(path: String, content: String = "aGVsbG8=") =
        MultiFileCommitEntry(repoPath = path, contentBase64 = content)

    private fun input(
        target: RepoTarget = this.target,
        entries: List<MultiFileCommitEntry> = listOf(entry("uploads/a.md")),
        commitMessage: String = "Add files",
    ) = MultiFileCommitInput(target = target, entries = entries, commitMessage = commitMessage)

    // ─── success paths ────────────────────────────────────────────────────────

    @Test
    fun success_twoFiles_blobCreatedForEach_oneTreeAndOneCommit() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(
            input(
                entries = listOf(
                    entry("uploads/a.md"),
                    entry("uploads/b.md"),
                )
            )
        )

        assertTrue("expected Success but got $result", result is MultiFileCommitResult.Success)
        result as MultiFileCommitResult.Success
        assertEquals("commit-sha-999", result.commitSha)
        assertEquals(listOf("uploads/a.md", "uploads/b.md"), result.committedPaths)

        assertEquals(
            listOf("getRef", "getCommit", "createBlob", "createBlob", "createTree", "createCommit", "updateRef"),
            api.calls,
        )
        assertEquals(2, api.blobRequests.size)
    }

    @Test
    fun success_entriesSortedDeterministicallyByPath() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        // Provide entries in reverse alphabetical order.
        val result = orchestrator.execute(
            input(
                entries = listOf(
                    entry("src/z.kt"),
                    entry("src/a.kt"),
                    entry("src/m.kt"),
                )
            )
        )

        assertTrue(result is MultiFileCommitResult.Success)
        val treeReq = api.lastCreateTreeRequest
        assertNotNull(treeReq)
        assertEquals(
            listOf("src/a.kt", "src/m.kt", "src/z.kt"),
            treeReq!!.tree.map { it.path },
        )
        assertEquals(
            listOf("src/a.kt", "src/m.kt", "src/z.kt"),
            (result as MultiFileCommitResult.Success).committedPaths,
        )
    }

    @Test
    fun success_folderWithSubfolders_allPathsCommitted() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(
            input(
                entries = listOf(
                    entry("src/main/Foo.kt"),
                    entry("src/main/Bar.kt"),
                    entry("src/test/FooTest.kt"),
                )
            )
        )

        assertTrue(result is MultiFileCommitResult.Success)
        val treeReq = api.lastCreateTreeRequest
        assertNotNull(treeReq)
        assertEquals(3, treeReq!!.tree.size)
        assertEquals("src/main/Bar.kt", treeReq.tree[0].path)
        assertEquals("src/main/Foo.kt", treeReq.tree[1].path)
        assertEquals("src/test/FooTest.kt", treeReq.tree[2].path)
    }

    @Test
    fun success_emptyEntries_withTargetPath_injectsGitkeepAtTargetPath() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(entries = emptyList()))

        assertTrue(result is MultiFileCommitResult.Success)
        result as MultiFileCommitResult.Success
        assertEquals(listOf("uploads/.gitkeep"), result.committedPaths)

        val treeReq = api.lastCreateTreeRequest
        assertNotNull(treeReq)
        assertEquals(1, treeReq!!.tree.size)
        assertEquals("uploads/.gitkeep", treeReq.tree[0].path)

        // .gitkeep is an empty file — blob request must use UTF-8 encoding with empty content.
        assertEquals(1, api.blobRequests.size)
        assertEquals("", api.blobRequests[0].content)
        assertEquals("utf-8", api.blobRequests[0].encoding)
    }

    @Test
    fun success_emptyEntries_emptyTargetPath_gitkeepAtRoot() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(target = rootTarget, entries = emptyList()))

        assertTrue(result is MultiFileCommitResult.Success)
        assertEquals(listOf(".gitkeep"), (result as MultiFileCommitResult.Success).committedPaths)

        val treeReq = api.lastCreateTreeRequest
        assertNotNull(treeReq)
        assertEquals(".gitkeep", treeReq!!.tree[0].path)
    }

    @Test
    fun success_updateRef_usesBaseShaAsExpectedSha_andForceIsFalse() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        orchestrator.execute(input())

        assertEquals("base-ref-sha", api.lastUpdateRefExpectedSha)
        assertEquals(false, api.lastUpdateRefRequest?.force)
        assertEquals("refs/heads/main", api.lastUpdateRefRef)
    }

    @Test
    fun success_createTree_usesBaseTreeSha_andContainsBlobShas() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        orchestrator.execute(input(entries = listOf(entry("uploads/a.md"))))

        val treeReq = api.lastCreateTreeRequest
        assertNotNull(treeReq)
        assertEquals("base-tree-sha", treeReq!!.baseTree)
        assertEquals(1, treeReq.tree.size)
        assertEquals("uploads/a.md", treeReq.tree[0].path)
        assertEquals("blob-sha-1", treeReq.tree[0].sha)
        assertEquals(TreeEntry.MODE_FILE, treeReq.tree[0].mode)
        assertEquals(TreeEntry.TYPE_BLOB, treeReq.tree[0].type)
        assertNull(treeReq.tree[0].content) // sha-mode only, no inline content
    }

    @Test
    fun success_createCommit_singleParentIsOriginalBranchHead() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        orchestrator.execute(input(commitMessage = "My commit"))

        val commitReq = api.lastCreateCommitRequest
        assertNotNull(commitReq)
        assertEquals("My commit", commitReq!!.message)
        assertEquals("new-tree-sha", commitReq.tree)
        assertEquals(listOf("base-ref-sha"), commitReq.parents)
    }

    @Test
    fun success_commitUrlMayBeNull() = runTest {
        val api = MultiFakeApi(commitHtmlUrl = null)
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is MultiFileCommitResult.Success)
        assertNull((result as MultiFileCommitResult.Success).commitUrl)
        assertNotNull(result.commitSha)
    }

    // ─── ZIP-Slip prevention ──────────────────────────────────────────────────

    @Test
    fun zipSlipPath_dotDotTraversal_isInvalidInput_noApiCalls() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(
            input(entries = listOf(entry("../../etc/passwd")))
        )

        assertTrue(result is MultiFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun zipSlipPath_absolutePath_isInvalidInput_noApiCalls() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(
            input(entries = listOf(entry("/etc/passwd")))
        )

        assertTrue(result is MultiFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun zipSlipPath_windowsAbsolutePath_isInvalidInput_noApiCalls() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(
            input(entries = listOf(entry("C:/windows/system32/cmd.exe")))
        )

        assertTrue(result is MultiFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    // ─── input validation ─────────────────────────────────────────────────────

    @Test
    fun blankCommitMessage_isInvalidInput_noApiCalls() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(commitMessage = "   "))

        assertTrue(result is MultiFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    @Test
    fun duplicateRepoPaths_isInvalidInput_noApiCalls() = runTest {
        val api = MultiFakeApi()
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(
            input(
                entries = listOf(
                    entry("uploads/a.md"),
                    entry("uploads/a.md"),
                )
            )
        )

        assertTrue(result is MultiFileCommitResult.InvalidInput)
        assertEquals(emptyList<String>(), api.calls)
    }

    // ─── failure mapping ─────────────────────────────────────────────────────

    @Test
    fun authError_atGetRef_stopsBeforeBlobs() = runTest {
        val api = MultiFakeApi(getRefThrows = GithubGitDataException.AuthRequired())
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(entries = listOf(entry("uploads/a.md"), entry("uploads/b.md"))))

        assertTrue(result is MultiFileCommitResult.AuthError)
        assertEquals(listOf("getRef"), api.calls)
    }

    @Test
    fun branchNotFound_atGetRef_stopsBeforeAnyMutation() = runTest {
        val api = MultiFakeApi(getRefThrows = GithubGitDataException.RefNotFound())
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is MultiFileCommitResult.BranchNotFound)
        assertEquals(listOf("getRef"), api.calls)
    }

    @Test
    fun networkError_atGetRef_stopsBeforeAnyMutation() = runTest {
        val api = MultiFakeApi(getRefThrows = GithubGitDataException.NetworkUnavailable())
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is MultiFileCommitResult.NetworkError)
        assertEquals(listOf("getRef"), api.calls)
    }

    @Test
    fun permissionDenied_atCreateBlob_stopsBeforeRefUpdate() = runTest {
        val api = MultiFakeApi(createBlobThrows = GithubGitDataException.PermissionDenied())
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input(entries = listOf(entry("uploads/a.md"), entry("uploads/b.md"))))

        assertTrue(result is MultiFileCommitResult.PermissionError)
        assertTrue("createBlob" in api.calls)
        assertTrue("updateRef" !in api.calls)
    }

    @Test
    fun shaMismatch_duringUpdateRef_isMapped() = runTest {
        val api = MultiFakeApi(updateRefThrows = GithubGitDataException.ShaMismatch())
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is MultiFileCommitResult.ShaMismatch)
        assertTrue("createCommit" in api.calls)
        assertEquals("updateRef", api.calls.last())
    }

    @Test
    fun protectedBranch_duringUpdateRef_isMapped() = runTest {
        val api = MultiFakeApi(updateRefThrows = GithubGitDataException.ProtectedBranch())
        val orchestrator = MultiFileCommitOrchestrator(api)

        val result = orchestrator.execute(input())

        assertTrue(result is MultiFileCommitResult.ProtectedBranch)
        assertTrue("updateRef" in api.calls)
    }
}

/**
 * Recording fake of [GithubGitDataApi] for multi-file orchestrator tests.
 *
 * Returns deterministic happy-path values unless a `*Throws` parameter is set.
 * [calls] records every method name reached, enabling assertion that mutations
 * did not occur after early failures. [blobRequests] records every blob request
 * in order, enabling inspection of encoding and content. Blob SHAs are issued
 * sequentially (`blob-sha-1`, `blob-sha-2`, …) so the tree entry assertions can
 * verify which file maps to which SHA.
 */
private class MultiFakeApi(
    private val getRefThrows: GithubGitDataException? = null,
    private val getCommitThrows: GithubGitDataException? = null,
    private val createBlobThrows: GithubGitDataException? = null,
    private val createTreeThrows: GithubGitDataException? = null,
    private val createCommitThrows: GithubGitDataException? = null,
    private val updateRefThrows: GithubGitDataException? = null,
    private val commitHtmlUrl: String? = "https://github.com/octocat/hello-world/commit/commit-sha-999",
) : GithubGitDataApi {

    val calls: MutableList<String> = mutableListOf()
    val blobRequests: MutableList<CreateBlobRequest> = mutableListOf()

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

    private var blobCount = 0

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
        )
    }

    override suspend fun createBlob(
        owner: String,
        repo: String,
        request: CreateBlobRequest,
    ): CreateBlobResponse {
        calls += "createBlob"
        blobRequests += request
        createBlobThrows?.let { throw it }
        blobCount++
        return CreateBlobResponse(sha = "blob-sha-$blobCount")
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
            sha = "commit-sha-999",
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
