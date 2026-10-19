package com.serviall.saputils.utils

import  android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale

fun MostrarMensaje(context: Context, mensaje: String,
                   tiempoLargo : Boolean = false) {
    var tiempo = Toast.LENGTH_LONG
    if (!tiempoLargo)
        tiempo = Toast.LENGTH_LONG
    val toast = Toast.makeText(context, mensaje, tiempo)
    toast.show()
}

fun EscribirLog(tag: String = "ProyectoA", mensaje: String) {
    Log.d(tag, mensaje)
}

fun FormatearNumeroSeparadorMiles(numero: Double, moneda: String = "€"): String {
    try {
        val formato = NumberFormat.getNumberInstance(Locale("es", "ES"))
        formato.minimumFractionDigits = 2
        formato.maximumFractionDigits = 2

        var numFormateado = formato.format(numero) + moneda
        return  numFormateado
    } catch (e: Exception) {
        return "0,00"
    }
}

fun NumeroTieneDecimales(numero: Double): Boolean {
    try {
        return numero % 1 != 0.0
    } catch (e: Exception)
    {
        return false
    }
}
fun get64BaseString(value:String):String{
    return Base64.encodeToString(value.toByteArray(),Base64.NO_WRAP)
}