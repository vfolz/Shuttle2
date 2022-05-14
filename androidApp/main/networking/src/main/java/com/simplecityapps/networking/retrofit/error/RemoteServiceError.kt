package com.simplecityapps.networking.retrofit.error

/**
 * An error response from the server.
 */
open class RemoteServiceError : Error() {

    override fun toString(): String {
        return "RemoteServiceError(message: $message)"
    }
}
