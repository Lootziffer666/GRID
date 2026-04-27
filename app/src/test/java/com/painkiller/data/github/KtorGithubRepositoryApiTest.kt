package com.painkiller.data.github

import com.painkiller.domain.github.GithubGitDataException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class KtorGithubRepositoryApiTest {

    @Test
    fun listRepositories_singlePage_returnsList() = runTest {
        val api = api(MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/user/repos"))
            assertEquals("100", request.url.parameters["per_page"])
            respond(
                content = ByteReadChannel(
                    """[{"id":1,"name":"a","full_name":"o/a","private":false,"default_branch":"main"}]"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        })
        val repos = api.listRepositories()
        assertEquals(1, repos.size)
        assertEquals("a", repos[0].name)
    }

    @Test
    fun listRepositories_authRequired_when401() = runTest {
        val api = api(MockEngine { respondError(HttpStatusCode.Unauthorized) })
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            kotlinx.coroutines.runBlocking { api.listRepositories() }
        }
    }

    @Test
    fun listRepositories_networkUnavailable_onIoException() = runTest {
        val api = api(MockEngine { throw IOException("dropped") })
        assertThrows(GithubGitDataException.NetworkUnavailable::class.java) {
            kotlinx.coroutines.runBlocking { api.listRepositories() }
        }
    }

    @Test
    fun listRepositories_authRequired_whenTokenMissing_noHttpCall() = runTest {
        var called = false
        val api = KtorGithubRepositoryApi(
            client = PainkillerHttpClient.create(MockEngine {
                called = true
                respond(content = ByteReadChannel("[]"), status = HttpStatusCode.OK)
            }),
            tokenProvider = { null },
        )
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            kotlinx.coroutines.runBlocking { api.listRepositories() }
        }
        assertTrue("no HTTP call must be made when token is missing", !called)
    }

    @Test
    fun listBranches_returnsBranches() = runTest {
        val api = api(MockEngine {
            respond(
                content = ByteReadChannel(
                    """[{"name":"main","commit":{"sha":"abc"},"protected":true},
                       {"name":"dev","commit":{"sha":"def"},"protected":false}]"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        })
        val branches = api.listBranches("o", "r")
        assertEquals(2, branches.size)
        assertEquals("main", branches[0].name)
        assertEquals(true, branches[0].protected)
    }

    private fun api(engine: MockEngine, token: String? = "test-token") =
        KtorGithubRepositoryApi(
            client = PainkillerHttpClient.create(engine),
            tokenProvider = { token },
        )
}
