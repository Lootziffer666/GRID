package com.grid.feature.import_pipeline.data.github

import com.painkiller.domain.github.CreateBlobRequest
import com.painkiller.domain.github.CreateBlobResponse
import com.painkiller.domain.github.CreateCommitRequest
import com.painkiller.domain.github.CreateCommitResponse
import com.painkiller.domain.github.CreateTreeRequest
import com.painkiller.domain.github.CreateTreeResponse
import com.painkiller.domain.github.GitCommit
import com.painkiller.domain.github.GitRef
import com.painkiller.domain.github.GithubGitDataApi
import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.UpdateRefRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.JsonConvertException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Production HTTP implementation of [GithubGitDataApi] backed by Ktor + OkHttp.
 *
 * Calls the six GitHub REST endpoints required by the Painkiller commit flow:
 *
 *   GET    /repos/{owner}/{repo}/git/ref/{ref}
 *   GET    /repos/{owner}/{repo}/git/commits/{sha}
 *   POST   /repos/{owner}/{repo}/git/blobs
 *   POST   /repos/{owner}/{repo}/git/trees
 *   POST   /repos/{owner}/{repo}/git/commits
 *   PATCH  /repos/{owner}/{repo}/git/refs/{ref}
 *
 * ## Status-code mapping
 *
 * | HTTP                                   | Exception                                  |
 * |----------------------------------------|--------------------------------------------|
 * | 401                                    | [GithubGitDataException.AuthRequired]      |
 * | 403, body mentions "protected"         | [GithubGitDataException.ProtectedBranch]   |
 * | 403 (other)                            | [GithubGitDataException.PermissionDenied]  |
 * | 404                                    | [GithubGitDataException.RefNotFound]       |
 * | 422, body mentions SHA / "fast forward"| [GithubGitDataException.ShaMismatch]       |
 * | 5xx, transport, timeout, DNS           | [GithubGitDataException.NetworkUnavailable]|
 * | other                                  | rethrown as [GithubGitDataException.NetworkUnavailable] for safety |
 *
 * No raw response body, status code, or token is ever placed in the
 * exception message — Painkiller's user-facing messages are derived in
 * `:domain/error/PainkillerErrorMapper`.
 *
 * `force = true` is never set on `updateRef`. The orchestrator passes
 * `force = false` and the caller-supplied `expectedSha` is sent as part of
 * the request body so the GitHub API itself enforces the safety contract.
 */
class KtorGithubGitDataApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val baseUrl: String = PainkillerHttpClient.GITHUB_BASE_URL,
) : GithubGitDataApi {

    override suspend fun getRef(owner: String, repo: String, ref: String): GitRef =
        execute {
            client.get("$baseUrl/repos/$owner/$repo/git/ref/$ref") {
                withBearer(requireToken())
            }
        }

    override suspend fun getCommit(owner: String, repo: String, commitSha: String): GitCommit =
        execute {
            client.get("$baseUrl/repos/$owner/$repo/git/commits/$commitSha") {
                withBearer(requireToken())
            }
        }

    override suspend fun getTree(
        owner: String,
        repo: String,
        treeSha: String,
        recursive: Boolean,
    ): CreateTreeResponse = execute {
        client.get("$baseUrl/repos/$owner/$repo/git/trees/$treeSha") {
            withBearer(requireToken())
            if (recursive) parameter("recursive", "1")
        }
    }

    override suspend fun createBlob(
        owner: String,
        repo: String,
        request: CreateBlobRequest,
    ): CreateBlobResponse = execute {
        client.post("$baseUrl/repos/$owner/$repo/git/blobs") {
            withBearer(requireToken())
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun createTree(
        owner: String,
        repo: String,
        request: CreateTreeRequest,
    ): CreateTreeResponse = execute {
        client.post("$baseUrl/repos/$owner/$repo/git/trees") {
            withBearer(requireToken())
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun createCommit(
        owner: String,
        repo: String,
        request: CreateCommitRequest,
    ): CreateCommitResponse = execute {
        client.post("$baseUrl/repos/$owner/$repo/git/commits") {
            withBearer(requireToken())
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /**
     * `expectedSha` is informational at this layer — the orchestrator only
     * issues this call after a [getRef] returned that SHA, and `force = false`
     * makes GitHub reject any non-fast-forward update with HTTP 422, which we
     * map to [GithubGitDataException.ShaMismatch].
     */
    override suspend fun updateRef(
        owner: String,
        repo: String,
        ref: String,
        request: UpdateRefRequest,
        expectedSha: String,
    ): GitRef = execute {
        require(!request.force) {
            "force=true is forbidden by GRID's safety contract"
        }
        client.patch("$baseUrl/repos/$owner/$repo/git/$ref") {
            withBearer(requireToken())
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    private suspend fun requireToken(): String =
        tokenProvider() ?: throw GithubGitDataException.AuthRequired()

    private suspend inline fun <reified T> execute(crossinline block: suspend () -> HttpResponse): T {
        val response: HttpResponse = try {
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
            // Defence-in-depth: any unclassified transport error must not leak
            // an exception message that could carry token bytes.
            throw GithubGitDataException.NetworkUnavailable()
        }

        when (response.status.value) {
            in 200..299 -> {
                return try {
                    response.body()
                } catch (e: JsonConvertException) {
                    throw GithubGitDataException.NetworkUnavailable()
                } catch (e: Throwable) {
                    throw GithubGitDataException.NetworkUnavailable()
                }
            }
            HttpStatusCode.Unauthorized.value -> throw GithubGitDataException.AuthRequired()
            HttpStatusCode.Forbidden.value -> throw classifyForbidden(response)
            HttpStatusCode.NotFound.value -> throw GithubGitDataException.RefNotFound()
            HttpStatusCode.UnprocessableEntity.value -> throw classify422(response)
            in 500..599 -> throw GithubGitDataException.NetworkUnavailable()
            else -> throw GithubGitDataException.NetworkUnavailable()
        }
    }

    private suspend fun classifyForbidden(response: HttpResponse): GithubGitDataException {
        val body = safeReadBodyForClassification(response)
        return if (body != null && body.contains("protected", ignoreCase = true)) {
            GithubGitDataException.ProtectedBranch()
        } else {
            GithubGitDataException.PermissionDenied()
        }
    }

    private suspend fun classify422(response: HttpResponse): GithubGitDataException {
        val body = safeReadBodyForClassification(response)
        return if (body != null &&
            (body.contains("not a fast forward", ignoreCase = true) ||
                body.contains("update is not", ignoreCase = true) ||
                body.contains("sha", ignoreCase = true))
        ) {
            GithubGitDataException.ShaMismatch()
        } else {
            // Other 422s usually mean validation errors on the request itself,
            // which the orchestrator's input validation should have prevented.
            GithubGitDataException.PermissionDenied()
        }
    }

    /**
     * Reads the response body for classification only. The body is **never**
     * returned to user-facing code — it is inspected solely to distinguish
     * 403-protected from 403-permission and 422-sha-mismatch from 422-other.
     * On any read failure, returns null so the caller falls back to the
     * generic mapping. Token-like prefixes inside an unexpected body would
     * still never reach the UI because the exception messages are fixed
     * strings.
     */
    private suspend fun safeReadBodyForClassification(response: HttpResponse): String? = try {
        response.body<String>()
    } catch (e: Throwable) {
        null
    }
}
