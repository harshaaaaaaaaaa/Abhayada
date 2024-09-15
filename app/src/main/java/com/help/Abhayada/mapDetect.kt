package com.help.Abhayada

import android.location.Location

fun isUserInMarkedArea(userLatitude: Double, userLongitude: Double, markedArea: MarkedArea): Boolean {
    val userLocation = Location("").apply {
        latitude = userLatitude
        longitude = userLongitude
    }
    val areaLocation = Location("").apply {
        latitude = markedArea.latitude
        longitude = markedArea.longitude
    }
    val distance = userLocation.distanceTo(areaLocation)
    return distance <= markedArea.radius
}