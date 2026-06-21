package com.grid.feature.import_pipeline.data.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the Ktor [HttpClient] used by all GitHub HTTP integrations.
 *
 * The client:
 *   - sets `Accept: application/vnd.github+json` per GitHub REST guidance
 *   - sets `X-GitHub-Api-Version: 2022-11-28` to pin a stable API surface
 *   - sets a short `User-Agent` (GitHub requires one for all requests)
 *   - configures generous timeouts so transport errors surface as
 *     [com.painkiller.domain.github.GithubGitDataException.NetworkUnavailable]
 *     and not an indefinite hang
 *   - decodes JSON via [kotlinx.serialization] with `ignoreUnknownKeys = true`
 *     and `encodeDefaults = true` (the latter is mandatory per
 *     `knownbugs.md` BUG-20260426-002)
 *
 * The Authorization header is **not** added here — every API implementation
 * pulls a fresh token from [com.grid.feature.import_pipeline.data.security.SecureTokenStore]
 * on each request and applies it via [withBearer]. This means sign-out
 * takes effect without rebuilding the client, and the raw token never lives
 * in the client object.
 */
internal object PainkillerHttpClient {

    const val USER_AGENT = "GRID/0.1 (+https://github.com/lootziffer666/painkiller)"
    const val API_VERSION = "2022-11-28"
    const val GITHUB_BASE_URL = "https://api.github.com"

    private const val REQUEST_TIMEOUT_MS = 30_000L
    private const val CONNECT_TIMEOUT_MS = 15_000L
    private const val SOCKET_TIMEOUT_MS = 30_000L

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun create(engine: HttpClientEngine? = null): HttpClient {
        val factory: (io.ktor.client.HttpClientConfig<*>.() -> Unit) -> HttpClient = { block ->
            if (engine != null) HttpClient(engine, block) else HttpClient(OkHttp, block)
        }
        return factory {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
            install(DefaultRequest) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header("X-GitHub-Api-Version", API_VERSION)
                header(HttpHeaders.UserAgent, USER_AGENT)
            }
        }
    }
}

/**
 * Adds the Bearer token header to a request. The token is read from the
 * provided suspend lambda so [io.ktor.client.HttpClient] is never coupled
 * to a stored secret.
 */
internal fun HttpRequestBuilder.withBearer(token: String) {
    header(HttpHeaders.Authorization, "Bearer $token")
}
