package com.grid.feature.import_pipeline.data.github

import com.painkiller.domain.github.CreateBlobRequest
import com.painkiller.domain.github.CreateCommitRequest
import com.painkiller.domain.github.CreateTreeRequest
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.TreeEntry
import com.painkiller.domain.github.UpdateRefRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class KtorGithubGitDataApiTest {

    private val owner = "octocat"
    private val repo = "hello-world"

    @Test
    fun getRef_success_returnsParsedRef() = runTest {
        val api = api(MockEngine { request ->
            assertEquals("$BASE/repos/$owner/$repo/git/ref/heads/main", request.url.toString())
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            assertEquals("application/vnd.github+json", request.headers[HttpHeaders.Accept])
            assertTrue(
                "User-Agent must be set",
                !request.headers[HttpHeaders.UserAgent].isNullOrBlank(),
            )
            respondJson(
                """{"ref":"refs/heads/main","object":{"sha":"abc","type":"commit"}}"""
            )
        })

        val ref = api.getRef(owner, repo, "heads/main")
        assertEquals("refs/heads/main", ref.ref)
        assertEquals("abc", ref.obj.sha)
    }

    @Test
    fun authRequired_isThrownOn401() = runTest {
        val api = api(MockEngine { respondError(HttpStatusCode.Unauthorized) })
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            kotlinx.coroutines.runBlocking { api.getRef(owner, repo, "heads/main") }
        }
    }

    @Test
    fun missingToken_throwsAuthRequired_beforeAnyHttpCall() = runTest {
        var calls = 0
        val api = KtorGithubGitDataApi(
            client = PainkillerHttpClient.create(MockEngine {
                calls++
                respondOk()
            }),
            tokenProvider = { null },
        )
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            kotlinx.coroutines.runBlocking { api.getRef(owner, repo, "heads/main") }
        }
        assertEquals("no HTTP call must be made when token is missing", 0, calls)
    }

    @Test
    fun forbidden_withProtectedBody_isProtectedBranch() = runTest {
        val api = api(MockEngine {
            respondJson(
                """{"message":"Branch is protected and refused this push."}""",
                status = HttpStatusCode.Forbidden,
            )
        })
        assertThrows(GithubGitDataException.ProtectedBranch::class.java) {
            kotlinx.coroutines.runBlocking {
                api.updateRef(
                    owner, repo, "refs/heads/main",
                    UpdateRefRequest(sha = "x", force = false),
                    expectedSha = "y",
                )
            }
        }
    }

    @Test
    fun forbidden_withoutProtectedHint_isPermissionDenied() = runTest {
        val api = api(MockEngine {
            respondJson(
                """{"message":"Resource not accessible by personal access token"}""",
                status = HttpStatusCode.Forbidden,
            )
        })
        assertThrows(GithubGitDataException.PermissionDenied::class.java) {
            kotlinx.coroutines.runBlocking { api.getRef(owner, repo, "heads/main") }
        }
    }

    @Test
    fun notFound_isRefNotFound() = runTest {
        val api = api(MockEngine { respondError(HttpStatusCode.NotFound) })
        assertThrows(GithubGitDataException.RefNotFound::class.java) {
            kotlinx.coroutines.runBlocking { api.getRef(owner, repo, "heads/missing") }
        }
    }

    @Test
    fun unprocessable_withFastForwardHint_isShaMismatch() = runTest {
        val api = api(MockEngine {
            respondJson(
                """{"message":"Update is not a fast forward"}""",
                status = HttpStatusCode.UnprocessableEntity,
            )
        })
        assertThrows(GithubGitDataException.ShaMismatch::class.java) {
            kotlinx.coroutines.runBlocking {
                api.updateRef(
                    owner, repo, "refs/heads/main",
                    UpdateRefRequest(sha = "x", force = false),
                    expectedSha = "y",
                )
            }
        }
    }

    @Test
    fun fiveHundred_isNetworkUnavailable() = runTest {
        val api = api(MockEngine { respondError(HttpStatusCode.InternalServerError) })
        assertThrows(GithubGitDataException.NetworkUnavailable::class.java) {
            kotlinx.coroutines.runBlocking { api.getRef(owner, repo, "heads/main") }
        }
    }

    @Test
    fun ioException_isMappedToNetworkUnavailable() = runTest {
        val api = api(MockEngine { throw IOException("connection reset") })
        assertThrows(GithubGitDataException.NetworkUnavailable::class.java) {
            kotlinx.coroutines.runBlocking { api.getRef(owner, repo, "heads/main") }
        }
    }

    @Test
    fun unknownThrowable_doesNotLeakMessage_mappedToNetworkUnavailable() = runTest {
        val api = api(MockEngine { error("ghp_FAKEFAKE_must_not_leak_to_user") })
        // Even if the underlying engine throws something with a token-like
        // string, the HTTP layer must classify it as NetworkUnavailable, not
        // surface the raw cause.
        val ex = assertThrows(GithubGitDataException.NetworkUnavailable::class.java) {
            kotlinx.coroutines.runBlocking { api.getRef(owner, repo, "heads/main") }
        }
        assertTrue(
            "raw token-like substring must not appear in mapped exception",
            !(ex.message ?: "").contains("ghp_FAKEFAKE"),
        )
    }

    @Test
    fun updateRef_withForceTrue_throwsIllegalArgument() = runTest {
        val api = api(MockEngine { respondOk() })
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                api.updateRef(
                    owner, repo, "refs/heads/main",
                    UpdateRefRequest(sha = "x", force = true),
                    expectedSha = "y",
                )
            }
        }
    }

    @Test
    fun createBlob_postsBody_andReturnsSha() = runTest {
        val api = api(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.encodedPath.endsWith("/git/blobs"))
            respondJson("""{"sha":"blob-1"}""", status = HttpStatusCode.Created)
        })
        val res = api.createBlob(
            owner, repo,
            CreateBlobRequest(content = "aGVsbG8=", encoding = "base64"),
        )
        assertEquals("blob-1", res.sha)
    }

    @Test
    fun createTree_postsBody_andReturnsSha() = runTest {
        val api = api(MockEngine { respondJson("""{"sha":"tree-1"}""", status = HttpStatusCode.Created) })
        val res = api.createTree(
            owner, repo,
            CreateTreeRequest(
                baseTree = "base",
                tree = listOf(TreeEntry(path = "a.md", sha = "blob-1")),
            ),
        )
        assertEquals("tree-1", res.sha)
    }

    @Test
    fun createCommit_postsBody_andReturnsSha() = runTest {
        val api = api(MockEngine {
            respondJson(
                """{"sha":"commit-1","message":"m","html_url":"https://github.com/o/r/commit/commit-1"}""",
                status = HttpStatusCode.Created,
            )
        })
        val res = api.createCommit(
            owner, repo,
            CreateCommitRequest(message = "m", tree = "tree-1", parents = listOf("p")),
        )
        assertEquals("commit-1", res.sha)
        assertEquals("https://github.com/o/r/commit/commit-1", res.htmlUrl)
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun api(engine: MockEngine, token: String? = "test-token"): KtorGithubGitDataApi =
        KtorGithubGitDataApi(
            client = PainkillerHttpClient.create(engine),
            tokenProvider = { token },
        )

    private companion object {
        const val BASE = "https://api.github.com"
    }
}

private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(
    content = ByteReadChannel(json),
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)
