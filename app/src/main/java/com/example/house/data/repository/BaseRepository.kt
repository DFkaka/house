
package com.example.house.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.house.data.local.DatabaseHelper

open class BaseRepository(context: Context) {
    protected val db: SQLiteDatabase by lazy {
        DatabaseHelper.getInstance(context).writableDatabase
    }

    protected fun Cursor.getLong(col: String): Long = getLong(getColumnIndexOrThrow(col))
    protected fun Cursor.getInt(col: String): Int = getInt(getColumnIndexOrThrow(col))
    protected fun Cursor.getDouble(col: String): Double = getDouble(getColumnIndexOrThrow(col))
    protected fun Cursor.getString(col: String): String? = getString(getColumnIndexOrThrow(col))
    protected fun Cursor.getDoubleOrNull(col: String): Double? {
        val idx = getColumnIndexOrThrow(col)
        return if (isNull(idx)) null else getDouble(idx)
    }
    protected fun Cursor.getLongOrNull(col: String): Long? {
        val idx = getColumnIndexOrThrow(col)
        return if (isNull(idx)) null else getLong(idx)
    }

    protected fun ContentValues.putIfNotNull(key: String, value: Any?) {
        when (value) {
            null -> putNull(key)
            is String -> put(key, value)
            is Long -> put(key, value)
            is Int -> put(key, value)
            is Double -> put(key, value)
            is Boolean -> put(key, if (value) 1 else 0)
        }
    }
}
