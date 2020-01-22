package com.example.locationtracker

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface GoogleApiService{
    @GET
    fun getNearbyPlaces(@Url url :String): Call<ResultData>

}