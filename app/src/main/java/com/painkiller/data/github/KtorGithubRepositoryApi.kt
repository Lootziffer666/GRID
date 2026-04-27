package com.painkiller.data.github

import com.painkiller.domain.github.GithubBranchSummary
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.GithubRepositoryApi
import com.painkiller.domain.github.GithubRepositorySummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Production HTTP implementation of [GithubRepositoryApi].
 *
 * Lists the authenticated user's repositories and a repository's branches.
 * Pagination is honoured up to a sane upper bound — Painkiller is an
 * upload tool, not a search tool, so 200 repos / 200 branches is enough for
 * the picker UI; users with larger orgs will see a "type to filter" prompt.
 *
 * Throws [GithubGitDataException] on every error so the upper layers can
 * use the same `:domain/error/PainkillerErrorMapper` mapping that the
 * commit flow uses.
 */
class KtorGithubRepositoryApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val baseUrl: String = PainkillerHttpClient.GITHUB_BASE_URL,
) : GithubRepositoryApi {

    override suspend fun listRepositories(): List<GithubRepositorySummary> {
        val token = tokenProvider() ?: throw GithubGitDataException.AuthRequired()
        val all = mutableListOf<GithubRepositorySummary>()
        var page = 1
        while (page <= MAX_PAGES) {
            val response = transport {
                client.get("$baseUrl/user/repos") {
                    withBearer(token)
                    parameter("per_page", PER_PAGE)
                    parameter("page", page)
                    parameter("sort", "updated")
                    parameter("affiliation", "owner,collaborator")
                }
            }
            handleStatus(response)
            val pageItems: List<GithubRepositorySummary> = decodeBody(response)
            all += pageItems
            if (pageItems.size < PER_PAGE) break
            page++
        }
        return all
    }

    override suspend fun listBranches(owner: String, repo: String): List<GithubBranchSummary> {
        val token = tokenProvider() ?: throw GithubGitDataException.AuthRequired()
        val all = mutableListOf<GithubBranchSummary>()
        var page = 1
        while (page <= MAX_PAGES) {
            val response = transport {
                client.get("$baseUrl/repos/$owner/$repo/branches") {
                    withBearer(token)
                    parameter("per_page", PER_PAGE)
                    parameter("page", page)
                }
            }
            handleStatus(response)
            val pageItems: List<GithubBranchSummary> = decodeBody(response)
            all += pageItems
            if (pageItems.size < PER_PAGE) break
            page++
        }
        return all
    }

    private suspend fun transport(block: suspend () -> HttpResponse): HttpResponse = try {
        block()
    } catch (e: GithubGitDataException) {
        throw e
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

    private fun handleStatus(response: HttpResponse) {
        when (response.status.value) {
            in 200..299 -> Unit
            HttpStatusCode.Unauthorized.value -> throw GithubGitDataException.AuthRequired()
            HttpStatusCode.Forbidden.value -> throw GithubGitDataException.PermissionDenied()
            HttpStatusCode.NotFound.value -> throw GithubGitDataException.RefNotFound()
            in 500..599 -> throw GithubGitDataException.NetworkUnavailable()
            else -> throw GithubGitDataException.NetworkUnavailable()
        }
    }

    private suspend inline fun <reified T> decodeBody(response: HttpResponse): T = try {
        response.body()
    } catch (e: Throwable) {
        throw GithubGitDataException.NetworkUnavailable()
    }

    private companion object {
        const val PER_PAGE = 100
        const val MAX_PAGES = 5 // 500 items max — ample for the picker UI
    }
}
