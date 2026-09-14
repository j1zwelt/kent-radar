package ru.j1zwelt.kentradar.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import ru.j1zwelt.kentradar.R
import ru.j1zwelt.kentradar.currentUser

class SharingService : Service() {
    private val channelIdStr = "location_sharing_channel"
    private val channelId = "location_sharing_channel"

    private var locationHelper: LocationHelper? = null

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(applicationContext)

        database = Firebase.database
        reference = database.getReference(currentUser!!.uid)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                sendNotificationAndStartForeground()

                reference.child("status").setValue(1)

                locationHelper!!.addLocationUpdateListener {
                    reference.child("latitude").setValue(it.latitude)
                    reference.child("longitude").setValue(it.longitude)
                }
            }

            ACTION_STOP -> {
                locationHelper?.stopLocationUpdates()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun sendNotificationAndStartForeground() {
        val title = getString(R.string.you_are_sharing_your_location)
        val text = getString(R.string.the_app_broadcasts_your_location_to_your_friends)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelIdStr, title, NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = text
            }
            manager.createNotificationChannel(channel)
        }

        val notification: Notification =
            NotificationCompat.Builder(this, channelId).setContentTitle(title).setContentText(text)
                .setSmallIcon(R.drawable.ic_friends).setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW).build()

        startForeground(101, notification)
    }
}