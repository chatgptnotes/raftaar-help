package com.example.sevaapp.models

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("api/mobileLogin") // Replace with the correct endpoint
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/mobileReg") // Replace with the correct endpoint for registration
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("api/submit-booking")
    fun bookAmbulance(@Body bookingDetails: BookingDetails): Call<BookingResponse>

    @GET("/api/profile/{userId}") // Profile endpoint
    fun getProfile(@Path("id") userId: Int, @Header("Authorization") token: String): Call<ProfileResponse>

    @POST("/check-email")
    fun checkEmailExists(@Body email: String): Call<CheckEmailResponse>
}
data class CheckEmailResponse(
    val exists: Boolean
)

data class ProfileResponse(
    val success: Boolean,
    val data: ProfileData
)

data class ProfileData(
    val id: Int,
    val full_name: String,
    val dob: String?,
    val address1: String?,
    val address2: String?,
    val phone1: String?,
    val phone2: String?,
    val image: String? // Nullable in case the image is not available
)

data class LoginRequest(
    val email: String,
    val password: String
)
//data class LoginResponse(val message: String, val access_token: String, val token_type: String,val userId: String)

data class LoginResponse(
    val userId: String,
    val access_token: String,
    val email: String, // Add this field if not already present
)

data class RegisterRequest(val name: String, val phone: String, val email: String, val password: String,
                           val password_confirmation: String)

data class RegisterResponse(val message: String, val access_token: String, val token_type: String)



data class BookingDetails(val phone: String, val latitude: Double, val longitude: Double, val address: String,
                          val city: String, val pin_code: String)

data class BookingResponse(val status: String, val message: String, val data: BookingData)

data class BookingData(val phone: String, val latitude: Double, val longitude: Double, val address: String,
                       val city: String, val pin_code: String)


