package com.example.youtubesharebridge

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        private const val DB_NAME = "chat.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "messages"
        private const val COL_ID = "id"
        private const val COL_TARGET = "target_computer_id"
        private const val COL_MESSAGE = "message"
        private const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TARGET TEXT,
                $COL_MESSAGE TEXT,
                $COL_TIMESTAMP INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertMessage(targetId: String, message: String) {
        val values = ContentValues().apply {
            put(COL_TARGET, targetId)
            put(COL_MESSAGE, message)
            put(COL_TIMESTAMP, System.currentTimeMillis())
        }
        writableDatabase.insert(TABLE_NAME, null, values)
    }

    fun getMessages(targetId: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            arrayOf(COL_MESSAGE),
            "$COL_TARGET=?",
            arrayOf(targetId),
            null, null, "$COL_TIMESTAMP ASC"
        )
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0))
        }
        cursor.close()
        return list
    }
} 