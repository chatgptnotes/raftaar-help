package com.emergency.sevaapp.models

data class ApiResponse(
    val success: Boolean,
    val data: List<Ride>
)
data class Ride(
    val id: Int,
    val phone: String,
    val assigned_staff_id: Int,
    val latitude: String,
    val longitude: String,
    val status: Int,
    val tariff: String,
    val distance: String,
    val remark: String?,
    val patient_id: String,
    val telecallers: String,
    val clicked: String,
    val created_at: String,
    val updated_at: String,
    val address: String,
    val city: String,
    val pin_code: String,
    val hospital: Hospital?, // Hospital object to hold hospital details
    val driver: Driver?      // Driver object to hold driver details
)

data class Hospital(
//    val id: Int?,
    val hospital_name: String?,
//    val phone: String?,
//    val latitude: String?,
//    val longitude: String?,
//    val address: String?,
//    val city: String?,
//    val pin_code: String?
)

data class Driver(
//    val id: Int?,
    val first_name: String?,
    val last_name: String?,
    val service_type: String?,
    val phone: String?,
//    val latitude: String?,
//    val longitude: String?,
//    val profile_image: String?,
//    val vehicle_proof: String?,
//    val vehical_model: String?,
//    val vehicle_number: String?,
//    val address: String?,
//    val city: String?,
//    val pincode: String?
)
