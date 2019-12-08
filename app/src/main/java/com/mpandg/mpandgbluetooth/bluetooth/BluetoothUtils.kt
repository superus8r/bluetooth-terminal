package com.mpandg.mpandgbluetooth.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.util.*

object BluetoothUtils {
    private const val TAG = "BluetoothUtils"
    private val uuidsDescriptions: MutableMap<String, String> = HashMap()
    @JvmStatic
    fun getDeviceUuids(device: BluetoothDevice): ArrayList<ParcelUuid> {
        val result = ArrayList<ParcelUuid>()
        try {
            val method = device.javaClass.getMethod("getUuids", null)
            val phoneUuids = method.invoke(device, null) as Array<*>
            for (uuid in phoneUuids) {
                Log.d(TAG, device.name + ": " + uuid.toString())
                result.add(uuid as ParcelUuid)
            }
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            Log.e(TAG, "getDeviceUuids() failed", e)
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            Log.e(TAG, "getDeviceUuids() failed", e)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            Log.e(TAG, "getDeviceUuids() failed", e)
        }
        return result
    }

    /**
     * see http://habrahabr.ru/post/144547/
     */
    @JvmStatic
    fun createRfcommSocket(device: BluetoothDevice): BluetoothSocket? {
        var tmp: BluetoothSocket? = null
        try {
            val class1: Class<*> = device.javaClass
            val aclass: Array<Class<*>?> = arrayOfNulls(1)
            aclass[0] = Integer.TYPE
            val method = class1.getMethod("createRfcommSocket", *aclass)
            val aobj = arrayOfNulls<Any>(1)
            aobj[0] = Integer.valueOf(1)
            tmp = method.invoke(device, *aobj) as BluetoothSocket
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            Log.e(TAG, "createRfcommSocket() failed", e)
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            Log.e(TAG, "createRfcommSocket() failed", e)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            Log.e(TAG, "createRfcommSocket() failed", e)
        }
        return tmp
    }

    init {
        uuidsDescriptions["0001"] = "SDP"
        uuidsDescriptions["0002"] = "UDP"
        uuidsDescriptions["0003"] = "RFCOMM"
        uuidsDescriptions["0004"] = "TCP"
        uuidsDescriptions["0005"] = "TCS-BIN"
        uuidsDescriptions["0006"] = "TCS-AT"
        uuidsDescriptions["0007"] = "ATT"
        uuidsDescriptions["0008"] = "OBEX"
        uuidsDescriptions["0009"] = "IP"
        uuidsDescriptions["000A"] = "FTP"
        uuidsDescriptions["000C"] = "HTTP"
        uuidsDescriptions["000E"] = "WSP"
        uuidsDescriptions["000F"] = "BNEP"
        uuidsDescriptions["0010"] = "UPNP"
        uuidsDescriptions["0011"] = "HIDP"
        uuidsDescriptions["0012"] = "HardcopyControlChannel"
        uuidsDescriptions["0014"] = "HardcopyDataChannel"
        uuidsDescriptions["0016"] = "HardcopyNotification"
        uuidsDescriptions["0017"] = "AVCTP"
        uuidsDescriptions["0019"] = "AVDTP"
        uuidsDescriptions["001B"] = "CMTP"
        uuidsDescriptions["001E"] = "MCAPControlChannel"
        uuidsDescriptions["001F"] = "MCAPDataChannel"
        uuidsDescriptions["0100"] = "L2CAP"
        uuidsDescriptions["1000"] = "ServiceDiscoveryServerService"
        uuidsDescriptions["1001"] = "BrowseGroupDescriptorService"
        uuidsDescriptions["1002"] = "PublicBrowseGroupService"
        uuidsDescriptions["1101"] = "SerialPortService"
        uuidsDescriptions["1102"] = "LANAccessUsingPPPService"
        uuidsDescriptions["1103"] = "DialupNetworkingService"
        uuidsDescriptions["1104"] = "IrMCSyncService"
        uuidsDescriptions["1105"] = "OBEXObjectPushService"
        uuidsDescriptions["1106"] = "OBEXFileTransferService"
        uuidsDescriptions["1107"] = "IrMCSyncCommandService"
        uuidsDescriptions["1108"] = "HeadsetService"
        uuidsDescriptions["1109"] = "CordlessTelephonyService"
        uuidsDescriptions["110A"] = "AudioSourceService"
        uuidsDescriptions["110B"] = "AudioSinkService"
        uuidsDescriptions["110C"] = "AVRemoteControlTargetService"
        uuidsDescriptions["110D"] = "AdvancedAudioDistributionService"
        uuidsDescriptions["110E"] = "AVRemoteControlService"
        uuidsDescriptions["110F"] = "VideoConferencingService"
        uuidsDescriptions["1110"] = "IntercomService"
        uuidsDescriptions["1111"] = "FaxService"
        uuidsDescriptions["1112"] = "HeadsetAudioGatewayService"
        uuidsDescriptions["1113"] = "WAPService"
        uuidsDescriptions["1114"] = "WAPClientService"
        uuidsDescriptions["1115"] = "PANUService"
        uuidsDescriptions["1116"] = "NAPService"
        uuidsDescriptions["1117"] = "GNService"
        uuidsDescriptions["1118"] = "DirectPrintingService"
        uuidsDescriptions["1119"] = "ReferencePrintingService"
        uuidsDescriptions["111A"] = "ImagingService"
        uuidsDescriptions["111B"] = "ImagingResponderService"
        uuidsDescriptions["111C"] = "ImagingAutomaticArchiveService"
        uuidsDescriptions["111D"] = "ImagingReferenceObjectsService"
        uuidsDescriptions["111E"] = "HandsfreeService"
        uuidsDescriptions["111F"] = "HandsfreeAudioGatewayService"
        uuidsDescriptions["1120"] = "DirectPrintingReferenceObjectsService"
        uuidsDescriptions["1121"] = "ReflectedUIService"
        uuidsDescriptions["1122"] = "BasicPringingService"
        uuidsDescriptions["1123"] = "PrintingStatusService"
        uuidsDescriptions["1124"] = "HumanInterfaceDeviceService"
        uuidsDescriptions["1125"] = "HardcopyCableReplacementService"
        uuidsDescriptions["1126"] = "HCRPrintService"
        uuidsDescriptions["1127"] = "HCRScanService"
        uuidsDescriptions["1128"] = "CommonISDNAccessService"
        uuidsDescriptions["1129"] = "VideoConferencingGWService"
        uuidsDescriptions["112A"] = "UDIMTService"
        uuidsDescriptions["112B"] = "UDITAService"
        uuidsDescriptions["112C"] = "AudioVideoService"
        uuidsDescriptions["112D"] = "SIMAccessService"
        uuidsDescriptions["112E"] = "Phonebook Access - PCE"
        uuidsDescriptions["112F"] = "Phonebook Access - PSE"
        uuidsDescriptions["1130"] = "Phonebook Access"
        uuidsDescriptions["1131"] = "Headset - HS"
        uuidsDescriptions["1132"] = "Message Access Server"
        uuidsDescriptions["1133"] = "Message Notification Server"
        uuidsDescriptions["1134"] = "Message Access Profile"
        uuidsDescriptions["1135"] = "GNSS"
        uuidsDescriptions["1136"] = "GNSS_Server"
        uuidsDescriptions["1200"] = "PnPInformationService"
        uuidsDescriptions["1201"] = "GenericNetworkingService"
        uuidsDescriptions["1202"] = "GenericFileTransferService"
        uuidsDescriptions["1203"] = "GenericAudioService"
        uuidsDescriptions["1204"] = "GenericTelephonyService"
    }
}