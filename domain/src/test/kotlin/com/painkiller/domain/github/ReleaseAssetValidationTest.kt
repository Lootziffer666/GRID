package com.painkiller.domain.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class ReleaseAssetValidationTest {

    @Test
    fun validate_nameBlank_returnsNameRequired() {
        val error = ReleaseAssetValidation.validate(
            UploadReleaseAssetRequest(
                name = "   ",
                contentType = "application/octet-stream",
                payload = testPayload(size = 1L),
            ),
        )

        assertEquals(ReleaseAssetValidation.ValidationError.NameRequired, error)
    }

    @Test
    fun validate_contentTypeBlank_returnsContentTypeRequired() {
        val error = ReleaseAssetValidation.validate(
            UploadReleaseAssetRequest(
                name = "archive.zip",
                contentType = "",
                payload = testPayload(size = 1L),
            ),
        )

        assertEquals(ReleaseAssetValidation.ValidationError.ContentTypeRequired, error)
    }

    @Test
    fun validate_dataEmpty_returnsDataRequired() {
        val error = ReleaseAssetValidation.validate(
            UploadReleaseAssetRequest(
                name = "archive.zip",
                contentType = "application/zip",
                payload = testPayload(size = 0L),
            ),
        )

        assertEquals(ReleaseAssetValidation.ValidationError.DataRequired, error)
    }

    @Test
    fun validate_validRequest_returnsNull() {
        val error = ReleaseAssetValidation.validate(
            UploadReleaseAssetRequest(
                name = "archive.zip",
                contentType = "application/zip",
                payload = testPayload(size = 3L),
            ),
        )

        assertNull(error)
    }
    private fun testPayload(size: Long): UploadPayload = object : UploadPayload {
        override val sizeBytes: Long = size
        override fun openStream() = ByteArrayInputStream(ByteArray(size.coerceAtLeast(0L).toInt()))
    }
}
