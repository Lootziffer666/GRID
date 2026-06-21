package com.grid.feature.import_pipeline.data.github

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorGithubTokenProbeApiTest {

    @Test
    fun probe_returnsLogin_on200() = runTest {
        val probe = KtorGithubTokenProbeApi(
            client = PainkillerHttpClient.create(MockEngine { request ->
                assertEquals("https://api.github.com/user", request.url.toString())
                assertEquals("Bearer ghp_validToken", request.headers[HttpHeaders.Authorization])
                respond(
                    content = ByteReadChannel("""{"login":"octocat","id":1}"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }),
        )
        val result = probe.probe("ghp_validToken")
        assertEquals("octocat", result.login)
        assertEquals(1L, result.id)
    }

    @Test
    fun probe_throwsAuthRequired_on401() = runTest {
        val probe = KtorGithubTokenProbeApi(
            client = PainkillerHttpClient.create(MockEngine { respondError(HttpStatusCode.Unauthorized) }),
        )
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            kotlinx.coroutines.runBlocking { probe.probe("ghp_invalid") }
        }
    }

    @Test
    fun probe_throwsPermissionDenied_on403() = runTest {
        val probe = KtorGithubTokenProbeApi(
            client = PainkillerHttpClient.create(MockEngine { respondError(HttpStatusCode.Forbidden) }),
        )
        assertThrows(GithubGitDataException.PermissionDenied::class.java) {
            kotlinx.coroutines.runBlocking { probe.probe("ghp_token") }
        }
    }

    @Test
    fun probe_throwsNetworkUnavailable_on5xx() = runTest {
        val probe = KtorGithubTokenProbeApi(
            client = PainkillerHttpClient.create(MockEngine { respondError(HttpStatusCode.BadGateway) }),
        )
        assertThrows(GithubGitDataException.NetworkUnavailable::class.java) {
            kotlinx.coroutines.runBlocking { probe.probe("ghp_token") }
        }
    }

    @Test
    fun probe_blankToken_throwsIllegalArgument() = runTest {
        val probe = KtorGithubTokenProbeApi(client = PainkillerHttpClient.create(MockEngine { respondError(HttpStatusCode.OK) }))
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { probe.probe("   ") }
        }
    }

    // ─── format check ─────────────────────────────────────────────────────────

    @Test
    fun format_classicPat_isValid() {
        // Real-format-shaped fake token: "ghp_" + 36 alphanumeric chars.
        assertTrue(GithubTokenFormat.looksValid("ghp_abcdefghijklmnopqrstuvwxyzABCDEFGHIJ"))
    }

    @Test
    fun format_fineGrainedPat_isValid() {
        val candidate = "github_pat_" + "A".repeat(80)
        assertTrue(GithubTokenFormat.looksValid(candidate))
    }

    @Test
    fun format_random_isInvalid() {
        assertFalse(GithubTokenFormat.looksValid("not-a-token"))
        assertFalse(GithubTokenFormat.looksValid(""))
        assertFalse(GithubTokenFormat.looksValid("ghp_short"))
    }
}
