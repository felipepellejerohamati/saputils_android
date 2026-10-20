package com.serviall.saputils.apicalls

import android.net.Uri
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.serviall.saputils.utils.EscribirLog
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ServicioAPI {
    @GET("login")
    suspend fun login(@Header("Authorization") token: String,
                      @Query("uid") uid: String,
                      @Query("pwd")pass: String): String
    @GET("getwhs")
    suspend fun getswhs(@Header("Authorization") token: String,
                        @Query(value="uid") uid:String): List<String>

    companion object {
        private var servicioAPI: ServicioAPI? = null
        private var url: String = "https://alliotapi.serviall.cl/faena/"

        fun getInstance(): ServicioAPI {
            if (servicioAPI == null) {
                try {
                    servicioAPI = Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(MoshiConverterFactory.create())

                        .build()
                        .create(ServicioAPI::class.java)
                } catch (e: Exception)
                {
                    EscribirLog(mensaje = "Error al ejecutar servicioAPI: " + e.message)
                }
            }
            return servicioAPI!!
        }
    }
}