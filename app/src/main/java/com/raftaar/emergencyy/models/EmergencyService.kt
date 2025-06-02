package com.raftaar.emergencyy.models

data class EmergencyService(
    val name: String,
    val phoneNumber: String,
    val description: String,
    val location: String,
    val isAvailable24x7: Boolean
) 