package com.serviall.saputils.globals

object UserSession {
    var username: String = ""
    var wharehouse: String = ""
    fun isLoggedIn(): Boolean = username.trim() != ""
    fun hasBod(): Boolean = wharehouse.trim() != ""
    fun clearUser() {
        username = ""
    }
    fun clearBod() {
        wharehouse = ""
    }
}