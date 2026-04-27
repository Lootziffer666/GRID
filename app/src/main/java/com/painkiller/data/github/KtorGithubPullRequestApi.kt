package com.painkiller.data.github

import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.GithubPullRequestApi
import com.painkiller.domain.github.GithubPullRequestSummary
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

class KtorGithubPullRequestApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val baseUrl: String = PainkillerHttpClient.GITHUB_BASE_URL,
) : GithubPullRequestApi {

    override suspend fun listPullRequests(
        owner: String,
        repo: String,
        state: String,
    ): List<GithubPullRequestSummary> {
        val token = tokenProvider() ?: throw GithubGitDataException.AuthRequired()
        val response = transport {
            client.get("$baseUrl/repos/$owner/$repo/pulls") {
                withBearer(token)
                parameter("state", state)
                parameter("per_page", PER_PAGE)
            }
        }
        handleStatus(response)
        return decodeBody(response)
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
    }
}
