package cyberdynesoftware.musink

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

var headsetProfileProxy: BluetoothHeadset? = null
var a2dpProfileProxy: BluetoothA2dp? = null

fun initBluetoothProfileProxy(context: Context) {
    getSystemService(context, BluetoothManager::class.java)?.let { blMan ->
        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HEADSET) {
                    headsetProfileProxy = proxy as BluetoothHeadset
                }
                if (profile == BluetoothProfile.A2DP) {
                    a2dpProfileProxy = proxy as BluetoothA2dp
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    player?.pause()
                }
            }
        }

        blMan.adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
        blMan.adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
    }
}

fun getDevices(context: Context): List<BluetoothDevice> {
    getSystemService(context, BluetoothManager::class.java)?.let { blMan ->
        if (checkBluetoothPermission(context) && blMan.adapter.isEnabled) {
            return blMan.adapter.bondedDevices.toList()
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
        a2dpProfileProxy?.let {
            getA2dpProfileConnectMethod().invoke(it, device)
        }
        headsetProfileProxy?.let {
            getHeadsetProfileConnectMethod().invoke(it, device)
        }
    } catch (e: NoSuchMethodException) {
        e.printStackTrace()
    } catch (e: InvocationTargetException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    }
}

@SuppressLint("DiscouragedPrivateApi")
fun getA2dpProfileConnectMethod(): Method {
    return BluetoothA2dp::class.java.getDeclaredMethod("connect", BluetoothDevice::class.java)
        .apply {
            isAccessible = true
        }
}

@SuppressLint("DiscouragedPrivateApi")
fun getHeadsetProfileConnectMethod(): Method {
    return BluetoothHeadset::class.java.getDeclaredMethod("connect", BluetoothDevice::class.java)
        .apply {
            isAccessible = true
        }
}