package com.painkiller.data.github

import com.painkiller.domain.github.GithubGitDataException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.call.body

/**
 * Validates a GitHub Personal Access Token by calling `GET /user`.
 *
 * Painkiller does not implement a full OAuth web flow because the OAuth
 * "authorization code" exchange requires a `client_secret` that cannot be
 * shipped in a public Android app without leaking it. Instead, the user
 * pastes a Personal Access Token (classic `ghp_…` or fine-grained
 * `github_pat_…`) into the auth screen, and this probe verifies it works
 * before [com.painkiller.data.security.SecureTokenStore] persists it.
 *
 * The probe call requires only the `read:user` scope; for actual upload
 * GitHub will check the additional `repo` (or fine-grained "Contents:
 * write") scope at the commit endpoints.
 */
interface GithubTokenProbeApi {

    /**
     * Returns the GitHub login of the user that owns [token] on success.
     * Throws [GithubGitDataException] subtypes on failure so the caller
     * can use the same error pipeline as the commit flow.
     */
    suspend fun probe(token: String): TokenProbeResult
}

/**
 * Successful response from `GET /user`.
 */
@Serializable
data class TokenProbeResult(
    val login: String,
    val id: Long,
)

/**
 * Production HTTP implementation of [GithubTokenProbeApi].
 */
class KtorGithubTokenProbeApi(
    private val client: HttpClient,
    private val baseUrl: String = PainkillerHttpClient.GITHUB_BASE_URL,
) : GithubTokenProbeApi {

    override suspend fun probe(token: String): TokenProbeResult {
        require(token.isNotBlank()) { "token must not be blank" }
        val response: HttpResponse = try {
            client.get("$baseUrl/user") { withBearer(token) }
        } catch (e: HttpRequestTimeoutException) {
            throw GithubGitDataException.NetworkUnavailable()
        } catch (e: SocketTimeoutException) {
            throw GithubGitDataException.NetworkUnavailable()
        } catch (e: UnknownHostException) {
            throw GithubGitDataException.NetworkUnavailable()
        } catch (e: IOException) {
            throw GithubGitDataException.NetworkUnavailable()
        } catch (e: Throwable) {
            throw GithubGitDataException.NetworkUnavailable()
        }

        when (response.status.value) {
            in 200..299 -> return try {
                response.body<TokenProbeResult>()
            } catch (e: Throwable) {
                throw GithubGitDataException.NetworkUnavailable()
            }
            HttpStatusCode.Unauthorized.value -> throw GithubGitDataException.AuthRequired()
            HttpStatusCode.Forbidden.value -> throw GithubGitDataException.PermissionDenied()
            in 500..599 -> throw GithubGitDataException.NetworkUnavailable()
            else -> throw GithubGitDataException.NetworkUnavailable()
        }
    }
}

/**
 * Thin format check for a GitHub PAT. Used by the auth screen for an
 * instant client-side hint before incurring a network round-trip; it does
 * not replace the [GithubTokenProbeApi.probe] call.
 *
 * Accepts:
 *   - classic PAT: `ghp_` + 36 chars
 *   - fine-grained PAT: `github_pat_` + ≥ 60 chars
 *   - GitHub App installation token: `ghs_` + 36 chars
 */
object GithubTokenFormat {
    private val CLASSIC_PAT = Regex("""^ghp_[A-Za-z0-9]{36,}$""")
    private val FINE_GRAINED_PAT = Regex("""^github_pat_[A-Za-z0-9_]{60,}$""")
    private val INSTALLATION = Regex("""^ghs_[A-Za-z0-9]{36,}$""")

    fun looksValid(token: String): Boolean {
        val trimmed = token.trim()
        return CLASSIC_PAT.matches(trimmed) ||
            FINE_GRAINED_PAT.matches(trimmed) ||
            INSTALLATION.matches(trimmed)
    }
}

internal val ProbeResultJson: Json = PainkillerHttpClient.json
