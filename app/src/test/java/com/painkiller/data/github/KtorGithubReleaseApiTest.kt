package com.painkiller.data.github

import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.UploadPayload
import com.painkiller.domain.github.UploadReleaseAssetRequest
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
import java.io.ByteArrayInputStream

class KtorGithubReleaseApiTest {

    @Test
    fun uploadReleaseAsset_postsToUploadsHost_withNameQueryAndContentType() = runTest {
        var opened = 0
        val api = api(MockEngine { request ->
            assertEquals("uploads.github.com", request.url.host)
            assertTrue(request.url.encodedPath.endsWith("/repos/o/r/releases/22/assets"))
            assertEquals("video.mp4", request.url.parameters["name"])
            assertEquals("video/mp4", request.headers[HttpHeaders.ContentType])
            respond(
                content = ByteReadChannel("""{"id":99,"name":"video.mp4","size":5}"""),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        })

        val result = api.uploadReleaseAsset(
            owner = "o",
            repo = "r",
            releaseId = 22L,
            request = UploadReleaseAssetRequest(
                name = "video.mp4",
                contentType = "video/mp4",
                payload = payload(5) { opened++ },
            ),
        )

        assertEquals("video.mp4", result.name)
        assertEquals(99L, result.id)
        assertEquals(1, opened)
    }

    @Test
    fun uploadReleaseAsset_authRequired_when401() = runTest {
        val api = api(MockEngine { respondError(HttpStatusCode.Unauthorized) })
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            kotlinx.coroutines.runBlocking {
                api.uploadReleaseAsset(
                    owner = "o",
                    repo = "r",
                    releaseId = 3L,
                    request = UploadReleaseAssetRequest("a.bin", "application/octet-stream", payload(1)),
                )
            }
        }
    }

    @Test
    fun uploadReleaseAsset_authRequired_whenTokenMissing_noHttpCall() = runTest {
        var called = false
        val api = KtorGithubReleaseApi(
            client = PainkillerHttpClient.create(MockEngine {
                called = true
                respond(content = ByteReadChannel("{}"), status = HttpStatusCode.OK)
            }),
            tokenProvider = { null },
        )
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            kotlinx.coroutines.runBlocking {
                api.uploadReleaseAsset(
                    owner = "o",
                    repo = "r",
                    releaseId = 3L,
                    request = UploadReleaseAssetRequest("a.bin", "application/octet-stream", payload(1)),
                )
            }
        }
        assertTrue(!called)
    }

    private fun api(engine: MockEngine, token: String? = "token") = KtorGithubReleaseApi(
        client = PainkillerHttpClient.create(engine),
        tokenProvider = { token },
    )

    private fun payload(size: Int, onOpen: () -> Unit = {}): UploadPayload = object : UploadPayload {
        override val sizeBytes: Long = size.toLong()
        override fun openStream() = ByteArrayInputStream(ByteArray(size) { 1 }).also { onOpen() }
    }
}
