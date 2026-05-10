package cyberdynesoftware.musink

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.getSystemService

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun initBluetooth(context: Context) {
    getSystemService(context, BluetoothManager::class.java)?.let {
        val adapter = it.adapter

        if (adapter.isEnabled) {
            for (device in adapter.bondedDevices) {
                display(device.name)

            }
        }

        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, p1: BluetoothProfile?) {
                if (profile == BluetoothProfile.A2DP) {
                    player.play()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    player.pause()
                }
            }
        }

        adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
    }
}