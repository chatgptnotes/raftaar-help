package com.emergency.sevaapp.models

data class BookingDetails(
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val pin_code: String)

data class BookingResponse(val status: String, val message: String, val data: BookingData)

data class BookingData(val phone: String, val latitude: Double, val longitude: Double, val address: String,
                       val city: String, val pin_code: String)

