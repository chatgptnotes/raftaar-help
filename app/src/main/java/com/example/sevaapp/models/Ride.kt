package com.example.sevaapp.models

data class Ride1(
    val date: String,
    val time: String,
    val status: String,
    val vehicle: String,
    val crm: String,
    val pickupLocation: String,
    val dropLocation: String,
    val profilePicture: String
)

data class Ride(
    val date: String,
    val time: String,
    val status: String,
    val vehicle: String,
    val crm: String,
    val pickupLocation: String,
    val dropLocation: String,
    val profilePicture: String
)