package com.painkiller.data.github

/**
 * Backend-mediated GitHub App token exchange boundary.
 *
 * Android clients must never hold the GitHub App private key. A backend
 * service performs JWT + installation-token minting and returns a short-lived
 * installation token for the selected installation id.
 */
interface GithubAppAuthApi {
    suspend fun exchangeInstallationToken(installationId: String): String
}
