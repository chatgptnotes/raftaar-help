package com.raftaar.emergencyy.models

data class LocationModel(
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val pincode: String
)
