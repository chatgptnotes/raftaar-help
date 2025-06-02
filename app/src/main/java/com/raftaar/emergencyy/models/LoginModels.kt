package com.raftaar.emergencyy.models

data class LoginRequest(
    val user_name: String,
    val password: String
)
data class LoginResponse(
    val message: String,
    val unique_id: String,
    val access_token: String,
    val token_type: String
)