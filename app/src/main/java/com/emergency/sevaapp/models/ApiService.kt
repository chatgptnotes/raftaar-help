package com.emergency.sevaapp.models

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/mobileLogin") // Replace with the correct endpoint
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/mobileReg") // Replace with the correct endpoint for registration
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("api/submit-booking")
    fun bookAmbulance(@Body bookingDetails: BookingDetails): Call<BookingResponse>

    @GET("api/profile/{user_id}")
    fun getProfile(@Path("user_id") userId: String): Call<ProfileResponse>

    @PUT("api/updateProfile/{user_id}")
    fun updateProfile(@Path("user_id") userId: String, @Body profile: Map<String, String>): Call<ProfileResponse>

//    @PUT("updateProfile/{userId}")
//    fun updateProfile(@Path("userId") userId: String, @Body updateRequest: Map<String, String>): Call<Void>

    @GET("api/get-booking/{phone}")
    suspend fun getBooking(@Path("phone") phone: String): Response<ApiResponse>

    @PUT("api/users/delete/{unique_id}")
    fun softDeleteUser(@Path("unique_id") userId: String): Call<Void>

    @POST("api/forget-password")
    fun forgetPassword(@Body request: ForgetPasswordRequest): Call<ForgetPasswordResponse>

    @POST("api/forget-password")
    fun resendOtp(@Body request: ForgetPasswordRequest): Call<ForgetPasswordResponse>



    @POST("api/verify-otp")
    fun verifyOtp(@Body request: OtpVerificationRequest): Call<OtpVerificationResponse>

    @POST("api/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<ResetPasswordResponse>
}