package com.painkiller.data.files

import com.painkiller.domain.files.SelectedSource

/**
 * Android-facing SAF intake boundary for Gate 1.
 *
 * Implementations stay in `:app` because they depend on Android URI/SAF APIs.
 * Domain logic consumes [SelectedSource] only.
 */
interface SafSourceIntake {
    suspend fun readSingleFile(): SelectedSource
    suspend fun readMultipleFiles(): SelectedSource
    suspend fun readFolderTree(): SelectedSource
    suspend fun readZipFile(): SelectedSource
}
