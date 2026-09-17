package com.audiovol.other

import android.content.Context
import android.content.SharedPreferences

class SharedPref(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    companion object {
        private const val PREF_NAME = "AudioVolPreferences"
    }
}