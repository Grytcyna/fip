package com.grytsyna.fixitpro.enum_status

enum class Status {
    NEW,
    IN_PROCESS,
    COMPLETED,
    CANCELED,
    DELETED;

    companion object {
        fun fromName(name: String): Status {
            return Status.entries.find { it.name.equals(name, ignoreCase = true) } ?: DELETED
        }
    }
}
