package com.raftaar.emergencyy.apiclient

import com.raftaar.emergencyy.models.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://admin.emergencyseva.in/" // Replace with your base URL
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}

//https://demo.bachao.co/api/mobileReg                post
//https://demo.bachao.co/api/mobileLogin              post
//https://demo.bachao.co/api/updateProfile/rseva-11   update
//https://demo.bachao.co/api/profile/rseva-11         get
//https://demo.bachao.co/api/profile/rseva-11         get
//https://demo.bachao.co/api/get-booking/{phone}      get
