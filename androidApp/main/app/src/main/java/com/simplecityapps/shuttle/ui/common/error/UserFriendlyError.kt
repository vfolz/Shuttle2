package com.simplecityapps.shuttle.ui.common.error

class UserFriendlyError(override var message: String) : Error(message) {

    override fun toString(): String {
        return "UserFriendlyError(message: $message)"
    }
}
