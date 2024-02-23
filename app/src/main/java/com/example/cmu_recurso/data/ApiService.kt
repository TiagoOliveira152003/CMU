package com.example.cmu_recurso.data

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("reverse?")
    fun getLocationInfo(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "json",
        @Query("zoom") zoom: Int = 18,
        @Query("addressdetails") addressDetails: Int = 1
    ): Call<JsonObject>
}