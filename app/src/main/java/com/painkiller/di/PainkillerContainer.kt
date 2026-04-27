package com.painkiller.di

import android.content.Context
import com.painkiller.BuildConfig
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.painkiller.data.files.SafFileReader
import com.painkiller.data.files.SafFolderReader
import com.painkiller.data.files.SafZipReader
import com.painkiller.data.github.GithubAuthRepository
import com.painkiller.data.github.GithubPullRequestRepository
import com.painkiller.data.github.GithubReleaseRepository
import com.painkiller.data.github.GithubRepoBranchRepository
import com.painkiller.data.github.GithubTokenProbeApi
import com.painkiller.data.github.KtorGithubGitDataApi
import com.painkiller.data.github.KtorGithubPullRequestApi
import com.painkiller.data.github.KtorGithubReleaseApi
import com.painkiller.data.github.KtorGithubRepositoryApi
import com.painkiller.data.github.KtorGithubTokenProbeApi
import com.painkiller.data.github.MultiFileCommitRepository
import com.painkiller.data.github.PainkillerHttpClient
import com.painkiller.data.github.RetrofitGithubAppAuthApi
import com.painkiller.data.github.SingleFileCommitRepository
import com.painkiller.data.security.EncryptedSecureTokenStore
import com.painkiller.data.security.SecureTokenStore
import com.painkiller.data.settings.DataStoreRepoTargetSettingsStore
import com.painkiller.data.settings.RepoTargetSettingsStore
import com.painkiller.domain.github.GithubGitDataApi
import com.painkiller.domain.github.GithubPullRequestApi
import com.painkiller.domain.github.GithubReleaseApi
import com.painkiller.domain.github.GithubRepositoryApi
import io.ktor.client.HttpClient

private val Context.repoTargetDataStore: DataStore<Preferences> by preferencesDataStore(name = "repo_target_settings")

/**
 * Manual DI container for the production wiring.
 *
 * Held by [com.painkiller.PainkillerApplication] for the lifetime of the
 * process. All singletons are created lazily on first access. No third-party
 * DI framework is used — Painkiller's surface is small enough that an
 * explicit container is clearer than a generated one.
 *
 * The container does **not** hold the GitHub token. Tokens live only in
 * [SecureTokenStore]; every HTTP API reads them on each call so sign-out is
 * effective immediately and the container itself has no secret state.
 */
class PainkillerContainer(appContext: Context) {

    private val app = appContext.applicationContext

    /** Process-wide HTTP client. Stateless except for connection pool. */
    private val httpClient: HttpClient by lazy { PainkillerHttpClient.create() }

    val secureTokenStore: SecureTokenStore by lazy { EncryptedSecureTokenStore(app) }

    val repoTargetSettingsStore: RepoTargetSettingsStore by lazy {
        DataStoreRepoTargetSettingsStore(app.repoTargetDataStore)
    }

    val safFileReader: SafFileReader by lazy { SafFileReader(app) }
    val safFolderReader: SafFolderReader by lazy { SafFolderReader(app) }
    val safZipReader: SafZipReader by lazy { SafZipReader(app) }
    private val githubAppBrokerBaseUrl: String =
        BuildConfig.GITHUB_APP_BROKER_BASE_URL.trim()

    private val tokenProvider: suspend () -> String? = { secureTokenStore.readGithubToken() }

    private val tokenProbeApi: GithubTokenProbeApi by lazy {
        KtorGithubTokenProbeApi(httpClient)
    }

    private val gitDataApi: GithubGitDataApi by lazy {
        KtorGithubGitDataApi(httpClient, tokenProvider)
    }

    private val repositoryApi: GithubRepositoryApi by lazy {
        KtorGithubRepositoryApi(httpClient, tokenProvider)
    }

    private val pullRequestApi: GithubPullRequestApi by lazy {
        KtorGithubPullRequestApi(httpClient, tokenProvider)
    }

    private val releaseApi: GithubReleaseApi by lazy {
        KtorGithubReleaseApi(httpClient, tokenProvider)
    }

    val authRepository: GithubAuthRepository by lazy {
        GithubAuthRepository(
            oauthApi = null, // OAuth web flow not available without server-side client_secret.
            appAuthApi = if (githubAppBrokerBaseUrl.isNotBlank()) {
                RetrofitGithubAppAuthApi.create(githubAppBrokerBaseUrl)
            } else {
                null
            },
            tokenProbeApi = tokenProbeApi,
            secureTokenStore = secureTokenStore,
        )
    }

    val repoBranchRepository: GithubRepoBranchRepository by lazy {
        GithubRepoBranchRepository(repositoryApi, secureTokenStore)
    }

    val pullRequestRepository: GithubPullRequestRepository by lazy {
        GithubPullRequestRepository(pullRequestApi, secureTokenStore)
    }

    val releaseRepository: GithubReleaseRepository by lazy {
        GithubReleaseRepository(releaseApi, secureTokenStore)
    }

    val singleFileCommitRepository: SingleFileCommitRepository by lazy {
        SingleFileCommitRepository(gitDataApi, secureTokenStore)
    }

    val multiFileCommitRepository: MultiFileCommitRepository by lazy {
        MultiFileCommitRepository(gitDataApi, secureTokenStore)
    }
}
