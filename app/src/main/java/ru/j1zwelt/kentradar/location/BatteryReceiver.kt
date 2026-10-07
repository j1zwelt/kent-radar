package ru.j1zwelt.kentradar.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

class BatteryReceiver(private val onBatteryChanged: (charge: Int, temperature: Float) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val temperature =
            intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1).toFloat() / 10

        onBatteryChanged( (level * 100 / scale.toFloat()).toInt(), temperature)
    }
}