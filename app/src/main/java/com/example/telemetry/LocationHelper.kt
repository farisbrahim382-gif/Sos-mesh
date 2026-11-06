package com.example.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

data class DeviceLocation(
    val latitude: Double?,
    val longitude: Double?,
    val source: String // "GPS_EXACT", "MANUAL", "ESTIMATED", "UNKNOWN"
)

object LocationHelper {
    @SuppressLint("MissingPermission")
    fun getLastKnownGpsLocation(context: Context): DeviceLocation {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = locationManager?.getProviders(true) ?: emptyList()
            var bestLocation: Location? = null

            for (provider in providers) {
                val l = locationManager?.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            if (bestLocation != null) {
                DeviceLocation(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude,
                    source = "GPS_EXACT"
                )
            } else {
                DeviceLocation(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    source = "ESTIMATED"
                )
            }
        } catch (_: Exception) {
            DeviceLocation(
                latitude = null,
                longitude = null,
                source = "UNKNOWN"
            )
        }
    }
}
