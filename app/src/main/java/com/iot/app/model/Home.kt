package com.iot.app.model

import java.util.UUID

data class Home(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)
