package com.painkiller.data.github

import com.painkiller.domain.github.GithubGitDataException
import com.painkiller.domain.github.UploadPayload
import com.painkiller.domain.lfs.LfsBatchObjectRequest
import com.painkiller.domain.lfs.LfsBatchRef
import com.painkiller.domain.lfs.LfsBatchRequest
import com.painkiller.domain.lfs.LfsBatchResponse
import com.painkiller.domain.lfs.LfsObjectAction
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Base64

open class KtorGithubLfsApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
) {
    open suspend fun requestUploadAction(
        owner: String,
        repo: String,
        oid: String,
        size: Long,
        refName: String,
    ): LfsBatchResponse {
        val token = tokenProvider() ?: throw GithubGitDataException.AuthRequired()
        val batchUrl = "https://github.com/$owner/$repo.git/info/lfs/objects/batch"
        val request = LfsBatchRequest(
            operation = "upload",
            objects = listOf(LfsBatchObjectRequest(oid = oid, size = size)),
            ref = LfsBatchRef(name = "refs/heads/$refName"),
        )
        val response = transport {
            client.post(batchUrl) {
                header(HttpHeaders.Accept, LFS_CONTENT_TYPE)
                contentType(ContentType.parse(LFS_CONTENT_TYPE))
                header(HttpHeaders.Authorization, basicAuth(token))
                setBody(request)
            }
        }
        return when (response.status.value) {
            in 200..299 -> decodeBody(response)
            HttpStatusCode.Unauthorized.value -> throw GithubGitDataException.AuthRequired()
            HttpStatusCode.Forbidden.value -> throw GithubGitDataException.PermissionDenied()
            HttpStatusCode.NotFound.value -> throw GithubGitDataException.RefNotFound()
            in 500..599 -> throw GithubGitDataException.NetworkUnavailable()
            else -> throw GithubGitDataException.NetworkUnavailable()
        }
    }

    open suspend fun uploadObject(action: LfsObjectAction, payload: UploadPayload) {
        val response = transport {
            client.put(action.href) {
                action.header.forEach { (key, value) -> header(key, value) }
                header(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                setBody(
                    StreamingPayloadContent(
                        payload = payload,
                        payloadContentType = ContentType.Application.OctetStream,
                    ),
                )
            }
        }
        if (response.status.value !in 200..299) {
            throw GithubGitDataException.NetworkUnavailable()
        }
    }

    open suspend fun verifyObject(action: LfsObjectAction, oid: String, size: Long) {
        val response = transport {
            client.post(action.href) {
                action.header.forEach { (key, value) -> header(key, value) }
                header(HttpHeaders.Accept, LFS_CONTENT_TYPE)
                contentType(ContentType.parse(LFS_CONTENT_TYPE))
                setBody(mapOf("oid" to oid, "size" to size))
            }
        }
        if (response.status.value !in 200..299) {
            throw GithubGitDataException.NetworkUnavailable()
        }
    }

    private fun basicAuth(token: String): String {
        val payload = Base64.getEncoder().encodeToString("git:$token".toByteArray())
        return "Basic $payload"
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

    private suspend inline fun <reified T> decodeBody(response: HttpResponse): T = try {
        response.body()
    } catch (e: Throwable) {
        throw GithubGitDataException.NetworkUnavailable()
    }

    private companion object {
        const val LFS_CONTENT_TYPE = "application/vnd.git-lfs+json"
    }
}

private class StreamingPayloadContent(
    private val payload: UploadPayload,
    private val payloadContentType: ContentType,
) : OutgoingContent.WriteChannelContent() {
    override val contentLength: Long = payload.sizeBytes
    override val contentType: ContentType = payloadContentType

    override suspend fun writeTo(channel: ByteWriteChannel) {
        withContext(Dispatchers.IO) {
            payload.openStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    channel.writeFully(buffer, 0, read)
                }
            }
        }
    }
}
