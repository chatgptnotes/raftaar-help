package com.raftaar.emergencyy.models

data class RegisterRequest(
    val full_name: String,
    val phone1: String,
    val pinCode: String, // Add this field
    val user_name: String,
    val password: String,
    val password_confirmation: String
)

data class RegisterResponse(
    val message: String,
    val unique_id: String,
    val user: User
)
