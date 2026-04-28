package com.painkiller.domain.github

object ReleaseAssetValidation {
    fun validate(request: UploadReleaseAssetRequest): ValidationError? {
        if (request.name.isBlank()) return ValidationError.NameRequired
        if (request.contentType.isBlank()) return ValidationError.ContentTypeRequired
        if (request.data.isEmpty()) return ValidationError.DataRequired
        return null
    }

    sealed interface ValidationError {
        data object NameRequired : ValidationError
        data object ContentTypeRequired : ValidationError
        data object DataRequired : ValidationError
    }
}
