package com.painkiller.domain.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseAssetValidationTest {

    @Test
    fun validate_nameBlank_returnsNameRequired() {
        val error = ReleaseAssetValidation.validate(
            UploadReleaseAssetRequest(
                name = "   ",
                contentType = "application/octet-stream",
                data = byteArrayOf(1),
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
                data = byteArrayOf(1),
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
                data = byteArrayOf(),
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
                data = byteArrayOf(1, 2, 3),
            ),
        )

        assertNull(error)
    }
}
