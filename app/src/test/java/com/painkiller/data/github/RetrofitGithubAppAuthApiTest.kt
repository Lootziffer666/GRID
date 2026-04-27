package com.painkiller.data.github

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class RetrofitGithubAppAuthApiTest {

    @Test
    fun exchangeInstallationToken_postsInstallationId_andReturnsToken() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"token":"ghs_demo","expires_at":"2026-04-27T12:00:00Z"}"""),
            )
            server.start()

            val api = RetrofitGithubAppAuthApi.create(server.url("/").toString())
            val token = api.exchangeInstallationToken("12345")

            assertEquals("ghs_demo", token)
            val request = server.takeRequest()
            assertEquals("/github-app/exchange", request.path)
            assertEquals("POST", request.method)
            assertEquals("""{"installation_id":"12345"}""", request.body.readUtf8())
        } finally {
            server.shutdown()
        }
    }
}
