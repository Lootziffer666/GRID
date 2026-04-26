package com.painkiller.data.github

import com.painkiller.data.security.SecureTokenStore

class GithubAuthRepository(
    private val oauthApi: GithubOAuthApi,
    private val secureTokenStore: SecureTokenStore
) {

    suspend fun authState(): GithubAuthState {
        val token = secureTokenStore.readGithubToken() ?: return GithubAuthState.Unauthenticated
        return GithubAuthState.Authenticated(tokenPreview = token.preview())
    }

    suspend fun authenticateWithAuthorizationCode(code: String): GithubAuthResult {
        if (code.isBlank()) {
            return GithubAuthResult.Failure("Authorization code is required.")
        }

        return runCatching {
            oauthApi.exchangeAuthorizationCode(code)
        }.fold(
            onSuccess = { token ->
                if (token.isBlank()) {
                    GithubAuthResult.Failure("Received empty token from GitHub OAuth.")
                } else {
                    secureTokenStore.writeGithubToken(token)
                    GithubAuthResult.Success(
                        state = GithubAuthState.Authenticated(tokenPreview = token.preview())
                    )
                }
            },
            onFailure = {
                GithubAuthResult.Failure("GitHub authentication failed.")
            }
        )
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
