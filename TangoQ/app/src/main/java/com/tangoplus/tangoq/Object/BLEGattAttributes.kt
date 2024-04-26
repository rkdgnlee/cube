package com.tangoplus.tangoq.Object

object BLEGattAttributes {
    private val attributes: HashMap<Any?, Any?> = HashMap<Any?, Any?>()
    var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    var READ_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb"
    var READ = "0000fff2-0000-1000-8000-00805f9b34fb"
    var WRITE = "0000fff3-0000-1000-8000-00805f9b34fb"
    var NOTIFY = "0000fff4-0000-1000-8000-00805f9b34fb"
    var READ2 = "0000fff5-0000-1000-8000-00805f9b34fb"


    init {
        // Sample Services.
        attributes["0000fff0-0000-1000-8000-00805f9b34fb"] = "Exercise Management Service"
//        attributes["0000fff0-0000-1000-8000-00805f9b34fb"] = "Device Information Service"
        // Sample Characteristics.
        attributes[READ_WRITE] = "READ_WRITE"
        attributes[READ] = "READ"
        attributes[WRITE] = "WRITE"
        attributes[NOTIFY] = "NOTIFY"
        attributes[READ2] = "READ2"
    }

    fun lookup(uuid: String?, defaultName: String): String {
        val name = attributes[uuid]
        return (name ?: defaultName).toString()
    }
}