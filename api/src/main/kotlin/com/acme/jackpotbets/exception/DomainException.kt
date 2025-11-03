package com.acme.jackpotbets.exception

open class DomainException(
    val statusCode: Int,
    override val message: String?,
    override val cause: Throwable?,
) : Exception(message, cause)

class BadArgumentException(
    override val message: String?,
    override val cause: Throwable? = null
) : DomainException(400, message, cause)
