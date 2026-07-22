package com.senspark.game.exception

import com.senspark.game.declare.ErrorCode

/**
 * ⚠️ DANGEROUS: `message` is sent VERBATIM to the client (BaseEncryptRequestHandler.sendExceptionError →
 * sendError(code, message)). ONLY ever pass user-safe literals — NEVER raw exception text, DB/SQL errors,
 * or any string that originated outside this server (e.g. an ap-* backend errorMessage). Leaking those
 * exposes uids, table + SQL-function names (see feedback: "never return technical errors to the client").
 * Prefer not to use this at all unless you specifically need a curated user-facing message; for anything
 * unexpected, throw a plain Exception (→ generic "Handler server error") or model a safe result instead.
 */
class CustomException(message: String, val code: Int, val willTraceLog: Boolean = false) : Exception(message) {
    constructor(message: String, code: Int) : this(message, code, false)
    constructor(message: String) : this(message, ErrorCode.SERVER_ERROR, false)
}