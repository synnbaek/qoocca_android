package com.qoocca.parentapp.data.model

data class LoginResponse(
    val parentId: Long,
    val accessToken: String,
    val parentName: String
)
