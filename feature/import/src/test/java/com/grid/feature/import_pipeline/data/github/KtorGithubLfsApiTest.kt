package com.grid.feature.import_pipeline.data.github

import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.UploadPayload
import com.painkiller.domain.lfs.LfsObjectAction
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class KtorGithubLfsApiTest {

    @Test
    fun requestUploadAction_postsBatchWithLfsContentType() = runTest {
        val api = KtorGithubLfsApi(
            client = PainkillerHttpClient.create(MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertTrue(request.url.toString().contains("/owner/repo.git/info/lfs/objects/batch"))
                assertEquals("application/vnd.git-lfs+json", request.headers[HttpHeaders.Accept])
                respond(
                    content = ByteReadChannel(
                        """{"transfer":"basic","objects":[{"oid":"abc","size":12,"actions":{"upload":{"href":"https://upload"}}}]}""",
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }),
            tokenProvider = { "token" },
        )

        val response = api.requestUploadAction("owner", "repo", "abc", 12L, "main")
        assertEquals("abc", response.objects.first().oid)
    }

    @Test
    fun uploadObject_opensPayloadStreamAndUploads() = runTest {
        var opened = 0
        val payload = object : UploadPayload {
            override val sizeBytes: Long = 4
            override fun openStream() = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)).also { opened++ }
        }
        val api = KtorGithubLfsApi(
            client = PainkillerHttpClient.create(MockEngine { request ->
                assertEquals(HttpMethod.Put, request.method)
                assertEquals("https://upload", request.url.toString())
                respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
            }),
            tokenProvider = { "token" },
        )

        api.uploadObject(LfsObjectAction("https://upload"), payload)

        assertEquals(1, opened)
    }

    @Test
    fun requestUploadAction_authRequiredWhen401() = runTest {
        val api = KtorGithubLfsApi(
            client = PainkillerHttpClient.create(MockEngine { respondError(HttpStatusCode.Unauthorized) }),
            tokenProvider = { "token" },
        )
        assertThrows(GithubGitDataException.AuthRequired::class.java) {
            runBlocking { api.requestUploadAction("o", "r", "oid", 1L, "main") }
        }
    }
}
