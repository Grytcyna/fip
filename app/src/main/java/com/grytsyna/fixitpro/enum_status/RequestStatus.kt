package com.grytsyna.fixitpro.enum_status

enum class RequestStatus(val description: String) {
    REQUEST_NEW_ORDER("New Order"),
    REQUEST_ORDER("Order"),
    REQUEST_CHANGED_ORDER("Changed Order"),
    REQUEST_NEW_RECALL("New Recall"),
    REQUEST_CANCELED_ORDER("Canceled Order"),
    UNKNOWN("unknown");

    companion object {
        fun fromDescription(description: String): RequestStatus {
            return entries.find { it.description.equals(description, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
