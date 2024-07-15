package com.opdroid.weatherapp.models

import java.io.Serializable

data class Wind(
    val speed: Double,
    val deg: Int
) : Serializable // Serializable is used to serialize the object and pass it to another activity