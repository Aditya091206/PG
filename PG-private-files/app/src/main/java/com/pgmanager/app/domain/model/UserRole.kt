package com.pgmanager.app.domain.model

enum class UserRole {
    OWNER,
    STUDENT,
    UNASSIGNED;

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.lowercase()) {
                "owner" -> OWNER
                "student" -> STUDENT
                else -> UNASSIGNED
            }
        }
    }
}
