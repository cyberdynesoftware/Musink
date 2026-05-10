package cyberdynesoftware.musink

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import java.lang.reflect.InvocationTargetException

lateinit var profileProxy: BluetoothA2dp

fun initBluetooth(context: Context) {
    getSystemService(context, BluetoothManager::class.java)?.let {
        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.A2DP) {
                    profileProxy = proxy as BluetoothA2dp
                    player.play()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    player.pause()
                }
            }
        }

        it.adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
    }
}

fun getDevices(context: Context): List<BluetoothDevice> {
    getSystemService(context, BluetoothManager::class.java)?.let {
        if (checkBluetoothPermission(context) && it.adapter.isEnabled) {
            return it.adapter.bondedDevices.toList()
        }
    }
    return emptyList()
}

fun checkBluetoothPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("DiscouragedPrivateApi")
fun connect(device: BluetoothDevice) {
    try {
        val connect =
            BluetoothA2dp::class.java.getDeclaredMethod("connect", BluetoothDevice::class.java)
        if (!connect.isAccessible) {
            connect.isAccessible = true
        }
        connect.invoke(profileProxy, device)
    } catch (e: NoSuchMethodException) {
        e.printStackTrace()
    } catch (e: InvocationTargetException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    }
}