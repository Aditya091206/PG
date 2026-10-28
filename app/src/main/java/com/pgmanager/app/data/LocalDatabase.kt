package com.pgmanager.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pgmanager.app.domain.model.User
import com.pgmanager.app.domain.model.UserRole
import java.security.MessageDigest

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id TEXT PRIMARY KEY,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                full_name TEXT NOT NULL,
                phone TEXT NOT NULL,
                role TEXT NOT NULL
            )
            """.trimIndent()
        )
        seedUser(db, "local-owner", "owner@pgmanager.local", "Owner@123", "Demo Owner", "9876543210", UserRole.OWNER)
        seedUser(db, "local-student", "student@pgmanager.local", "Student@123", "Demo Student", "9876543211", UserRole.STUDENT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun authenticate(email: String, password: String): User? {
        readableDatabase.query(
            "users",
            arrayOf("id", "email", "full_name", "phone", "role"),
            "email = ? AND password_hash = ?",
            arrayOf(email.trim().lowercase(), hash(password)),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return User(
                userId = cursor.getString(0),
                email = cursor.getString(1),
                fullName = cursor.getString(2),
                phoneNumber = cursor.getString(3),
                role = UserRole.fromString(cursor.getString(4))
            )
        }
    }

    private fun seedUser(
        db: SQLiteDatabase,
        id: String,
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: UserRole
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("email", email)
            put("password_hash", hash(password))
            put("full_name", fullName)
            put("phone", phone)
            put("role", role.name.lowercase())
        }
        db.insertOrThrow("users", null, values)
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val DATABASE_NAME = "pg_manager.db"
        private const val DATABASE_VERSION = 1
    }
}
