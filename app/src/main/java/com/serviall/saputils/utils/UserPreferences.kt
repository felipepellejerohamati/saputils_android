package com.serviall.saputils.utils

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREF_NAME = "serviall_user_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_REMEMBER = "remember_user"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginData(context: Context, username: String, password: String, remember: Boolean) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        if (remember) {
            editor.putString(KEY_USERNAME, username)
            editor.putString(KEY_PASSWORD, password)
            editor.putBoolean(KEY_REMEMBER, true)
        } else {
            editor.clear()
        }
        editor.apply()
    }

    fun getUsername(context: Context): String {
        return getPrefs(context).getString(KEY_USERNAME, "") ?: ""
    }

    fun getPassword(context: Context): String {
        return getPrefs(context).getString(KEY_PASSWORD, "") ?: ""
    }

    fun isRemembered(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER, false)
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
