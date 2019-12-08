package com.mpandg.mpandgbluetooth.model

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import com.mpandg.mpandgbluetooth.bluetooth.BluetoothUtils.getDeviceUuids
import java.util.*

data class DeviceData(
        var device: BluetoothDevice,
        var emptyName: String?,
        var name: String?,
        var address: String?,
        var bondState: Int = BluetoothDevice.BOND_NONE,
        var uuids: ArrayList<ParcelUuid>? = null,
        var deviceClass: Int?,
        var majorDeviceClass: Int?) {

    init {
        name = device.name
        address = device.address
        bondState = device.bondState
        if (name == null || name!!.isEmpty()) name = emptyName
        deviceClass = device.bluetoothClass.deviceClass
        majorDeviceClass = device.bluetoothClass.majorDeviceClass
        uuids = getDeviceUuids(device)
    }
}