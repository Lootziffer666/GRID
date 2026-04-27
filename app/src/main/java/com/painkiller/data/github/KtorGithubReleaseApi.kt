package com.painkiller.data.github

import com.painkiller.domain.github.CreateReleaseRequest
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.GithubReleaseAssetSummary
import com.painkiller.domain.github.GithubReleaseApi
import com.painkiller.domain.github.GithubReleaseSummary
import com.painkiller.domain.github.UploadReleaseAssetRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.content.ByteArrayContent
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class KtorGithubReleaseApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val baseUrl: String = PainkillerHttpClient.GITHUB_BASE_URL,
) : GithubReleaseApi {

    override suspend fun listReleases(owner: String, repo: String): List<GithubReleaseSummary> {
        val token = tokenProvider() ?: throw GithubGitDataException.AuthRequired()
        val response = transport {
            client.get("$baseUrl/repos/$owner/$repo/releases") {
                withBearer(token)
            }
        }
        handleStatus(response)
        return decodeBody(response)
    }

    override suspend fun createRelease(
        owner: String,
        repo: String,
        request: CreateReleaseRequest,
    ): GithubReleaseSummary {
        val token = tokenProvider() ?: throw GithubGitDataException.AuthRequired()
        val response = transport {
            client.post("$baseUrl/repos/$owner/$repo/releases") {
                withBearer(token)
                setBody(request)
            }
        }
        handleStatus(response)
        return decodeBody(response)
    }

    override suspend fun uploadReleaseAsset(
        owner: String,
        repo: String,
        releaseId: Long,
        request: UploadReleaseAssetRequest,
    ): GithubReleaseAssetSummary {
        val token = tokenProvider() ?: throw GithubGitDataException.AuthRequired()
        val uploadUrl = "https://uploads.github.com/repos/$owner/$repo/releases/$releaseId/assets"
        val response = transport {
            client.post {
                url(
                    URLBuilder(uploadUrl).apply {
                        parameters.append("name", request.name)
                    }.build(),
                )
                withBearer(token)
                header(HttpHeaders.ContentType, request.contentType)
                setBody(
                    ByteArrayContent(
                        bytes = request.data,
                        contentType = ContentType.parse(request.contentType),
                    ),
                )
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
}
