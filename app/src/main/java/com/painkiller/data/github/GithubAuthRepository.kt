package com.painkiller.data.github

import com.painkiller.data.security.SecureTokenStore
import com.painkiller.domain.github.GithubGitDataException

/**
 * GitHub auth boundary used by the auth screen and the upload flow.
 *
 * Painkiller currently supports two auth states:
 *
 * 1. **Personal Access Token** ([signInWithPersonalAccessToken]) — the user
 *    pastes a classic `ghp_…` or fine-grained `github_pat_…` token into the
 *    auth screen. The token is validated against `GET /user` via the
 *    [GithubTokenProbeApi] before being persisted in [SecureTokenStore]
 *    (encrypted via AndroidX Security in production builds).
 *
 * 2. **OAuth authorization code candidate** ([authenticateWithAuthorizationCode]) —
 *    preserved as a disabled-by-default capability check. No production flow
 *    is enabled in this build.
 *
 * No raw token is ever logged, returned in [authState], or surfaced in
 * exception messages.
 */
class GithubAuthRepository(
    private val oauthApi: GithubOAuthApi?,
    private val tokenProbeApi: GithubTokenProbeApi?,
    private val secureTokenStore: SecureTokenStore,
) {
    fun isOAuthExchangeAvailable(): Boolean = oauthApi != null

    suspend fun authState(): GithubAuthState {
        val token = secureTokenStore.readGithubToken() ?: return GithubAuthState.Unauthenticated
        return GithubAuthState.Authenticated(tokenPreview = token.preview())
    }

    suspend fun authenticateWithAuthorizationCode(code: String): GithubAuthResult {
        val api = oauthApi
            ?: return GithubAuthResult.Failure("OAuth web flow is not available in this build.")
        if (code.isBlank()) {
            return GithubAuthResult.Failure("Authorization code is required.")
        }

        return runCatching { api.exchangeAuthorizationCode(code) }.fold(
            onSuccess = { token ->
                if (token.isBlank()) {
                    GithubAuthResult.Failure("Received empty token from GitHub OAuth.")
                } else {
                    secureTokenStore.writeGithubToken(token)
                    GithubAuthResult.Success(
                        state = GithubAuthState.Authenticated(tokenPreview = token.preview()),
                    )
                }
            },
            onFailure = {
                GithubAuthResult.Failure("GitHub authentication failed.")
            },
        )
    }

    /**
     * Validates a Personal Access Token by calling `GET /user`, then persists
     * it via [SecureTokenStore] on success.
     *
     * Returns [GithubAuthResult.Failure] without persisting anything if the
     * token is blank, malformed, rejected by GitHub, or unreachable. The
     * raw [token] is never echoed in the failure reason.
     */
    suspend fun signInWithPersonalAccessToken(token: String): GithubAuthResult {
        val probe = tokenProbeApi
            ?: return GithubAuthResult.Failure("Token validation is not configured.")
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            return GithubAuthResult.Failure("Token is required.")
        }

        return try {
            probe.probe(trimmed)
            secureTokenStore.writeGithubToken(trimmed)
            GithubAuthResult.Success(
                state = GithubAuthState.Authenticated(tokenPreview = trimmed.preview()),
            )
        } catch (e: GithubGitDataException.AuthRequired) {
            GithubAuthResult.Failure("GitHub rejected this token.")
        } catch (e: GithubGitDataException.PermissionDenied) {
            GithubAuthResult.Failure(
                "Token is missing required scopes. Painkiller needs at least repo write access."
            )
        } catch (e: GithubGitDataException.NetworkUnavailable) {
            GithubAuthResult.Failure(
                "Could not reach GitHub. Check your connection and try again."
            )
        } catch (e: Throwable) {
            GithubAuthResult.Failure("GitHub returned an unexpected error.")
        }
    }

    suspend fun logout() {
        secureTokenStore.clearGithubToken()
    }

    private fun String.preview(): String {
        val clean = trim()
        if (clean.length <= 8) return "********"
        return clean.take(4) + "…" + clean.takeLast(4)
    }
}
