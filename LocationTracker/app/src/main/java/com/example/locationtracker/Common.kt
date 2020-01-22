package com.example.locationtracker

object Common{

    private val GOOGLE_API_URL = "https://maps.googleapis.com/"

    val googleApiService:GoogleApiService
        get() = RetrofitClient.getClient(GOOGLE_API_URL).create(GoogleApiService::class.java)

}