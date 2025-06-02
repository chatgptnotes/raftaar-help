package com.raftaar.emergencyy.models

data class ProfileResponse(
    val message: String,
    val user: User,
    val data: User
)

data class User(
    val id: Int,
    val unique_id: String,
    val full_name: String,
    val dob: String,
    val address1: String? = null,
    val address2: String? = null,
    val address3: String? = null,
    val latitude1: Double? = null,
    val longitude1: Double? = null,
    val latitude2: Double? = null,
    val longitude2: Double? = null,
    val latitude3: Double? = null,
    val longitude3: Double? = null,
    val phone1: String,
    val phone2: String,
    val pinCode: String,
    val user_name: String,
    val image: String? // Updated image field
)
