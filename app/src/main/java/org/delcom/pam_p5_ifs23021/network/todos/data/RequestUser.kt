package org.delcom.pam_p5_ifs23021.network.todos.data

import kotlinx.serialization.Serializable

@Serializable
data class RequestUserChange (
    val name: String,
    val username: String,
    val about: String = ""
)

@Serializable
data class RequestUserChangePassword (
    val newPassword: String,
    val password: String
)