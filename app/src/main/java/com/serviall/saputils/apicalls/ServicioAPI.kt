package com.serviall.saputils.apicalls

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.serviall.saputils.data.ServialItem
import com.serviall.saputils.utils.EscribirLog
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

class StringTypeAdapter : TypeAdapter<String>() {
    override fun write(out: JsonWriter, value: String?) {
        out.value(value ?: "")
    }

    override fun read(reader: JsonReader): String {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return ""
        }
        return reader.nextString() ?: ""
    }
}

interface ServicioAPI {
    @GET("login")
    suspend fun login(@Header("Authorization") token: String,
                      @Query("uid") uid: String,
                      @Query("pwd")pass: String): String
    @GET("getwhs")
    suspend fun getswhs(@Header("Authorization") token: String,
                        @Query(value="uid") uid:String): List<String>
    @GET("orders")
    suspend fun orders(@Header("Authorization") token: String,
                        @Query(value="whscode") whs:String): List<ServialItem>
    @GET("reservasabiertas")
    suspend fun reservasabiertas(@Header("Authorization") token: String,
                       @Query(value="whscode") whs:String): List<ServialItem>
    @GET("OpenDocuments")
    suspend fun OpenDocuments(@Header("Authorization") token: String,
                                 @Query(value="whscode") whs:String): List<ServialItem>
    @POST("delivery")
    suspend fun delivery(@Header("Authorization") token: String,
                         @Body ServialItem: ServialItem) : String
    @POST("postEntrega")
    suspend fun postEntrega(@Header("Authorization") token: String,
                            @Query(value="whscode") whs:String,
                         @Body ServialItem: ServialItem) : String
    @POST("postReserva")
    suspend fun postReserva(@Header("Authorization") token: String,
                            @Query(value="whscode") whs:String,
                         @Body ServialItem: ServialItem) : String

    @POST("dataissues")
    suspend fun tesseractocr(
        @Header("Authorization") token: String,
        @Query("whscode") whscode: String,
        @Body imageBody: RequestBody
    ): String
    @POST("dataissuesobj")
    suspend fun dataissuesobj(
        @Header("Authorization") token: String,
        @Query("whscode") whscode: String,
        @Body imageBody: RequestBody
    ): ServialItem

    companion object {
        private var servicioAPI: ServicioAPI? = null
        private var url: String = "https://alliotapi.serviall.cl/faena/"

        fun getInstance(): ServicioAPI {
            if (servicioAPI == null) {
                try {
                    val gson = GsonBuilder()
                        .registerTypeAdapter(String::class.java, StringTypeAdapter())
                        .registerTypeAdapter(com.serviall.saputils.data.ItemStatus::class.java, com.serviall.saputils.data.ItemStatusAdapter())
                        .registerTypeAdapter(com.serviall.saputils.data.ProductItem::class.java, com.serviall.saputils.data.ProductItemDeserializer())
                        .registerTypeAdapter(ServialItem::class.java, com.serviall.saputils.data.ServialItemDeserializer())
                        .create()

                    val okHttpClient = OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()

                    servicioAPI = Retrofit.Builder()
                        .baseUrl(url)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create(gson))
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
