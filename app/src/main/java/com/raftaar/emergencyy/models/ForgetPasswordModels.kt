package com.raftaar.emergencyy.models

data class ForgetPasswordResponse(val message: String, val phone: String, val otp_expiry: String)
data class ForgetPasswordRequest(val phone1: String)

data class OtpVerificationRequest(val phone1: String, val otp: String)
data class OtpVerificationResponse(val message: String, val phone: String)

data class ResetPasswordRequest(val phone1: String, val password: String, val password_confirmation: String)
data class ResetPasswordResponse(val message: String)