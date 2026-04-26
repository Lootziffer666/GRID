package com.painkiller.app.domain.model

/**
 * Painkiller error envelope used across the domain layer.
 *
 * Every user-facing error must say:
 * - what happened
 * - whether user data was lost
 * - what Painkiller did or did not do
 * - the next useful step
 *
 * The full mapping from low-level failures to this shape lands in Gate 8.
 * Gate 0 only declares the type so domain models can refer to it.
 */
data class HumanReadableError(
    val title: String,
    val explanation: String,
    val userDataLost: Boolean,
    val nextStep: String,
)
