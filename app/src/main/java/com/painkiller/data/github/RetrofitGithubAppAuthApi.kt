package com.painkiller.data.github

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class ExchangeRequest(
    val installation_id: String,
)

data class ExchangeResponse(
    val token: String,
    val expires_at: String? = null,
)

/**
 * Retrofit-backed implementation for backend-mediated GitHub App installation
 * token exchange.
 *
 * Base URL defaults to the Android emulator loopback host.
 */
class RetrofitGithubAppAuthApi(
    private val service: GithubAppAuthRetrofitService,
) : GithubAppAuthApi {

    override suspend fun exchangeInstallationToken(installationId: String): String {
        val response = service.exchange(
            ExchangeRequest(installation_id = installationId.trim()),
        )
        return response.token
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"

        fun create(baseUrl: String = DEFAULT_BASE_URL): RetrofitGithubAppAuthApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service = retrofit.create(GithubAppAuthRetrofitService::class.java)
            return RetrofitGithubAppAuthApi(service)
        }
    }
}

interface GithubAppAuthRetrofitService {
    @POST("github-app/exchange")
    suspend fun exchange(@Body request: ExchangeRequest): ExchangeResponse
}
