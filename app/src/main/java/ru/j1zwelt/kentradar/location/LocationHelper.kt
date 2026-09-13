package ru.j1zwelt.kentradar.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.annotation.RequiresPermission

class LocationHelper(val context: Context) {
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun addLocationUpdateListener(onLocationChanged: (Location) -> Unit) {
        if (locationManager == null) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        val lastLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (lastLocation != null) {
            onLocationChanged(lastLocation)
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.i("Attempt", "Устройство зафиксировало движение! Свежие координаты: $location")
                onLocationChanged(location)
            }

            override fun onProviderDisabled(provider: String) {
                Log.e("Attempt", "GPS выключен в шторке!")
            }

            override fun onProviderEnabled(provider: String) {}
        }

        if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 3000L, 1f, locationListener!!
            )
        } else {
            locationManager!!.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 3000L, 2f, locationListener!!
            )
        }
    }

    fun stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager!!.removeUpdates(locationListener!!)
        }
    }
}