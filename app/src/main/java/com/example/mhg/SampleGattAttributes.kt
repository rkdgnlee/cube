package com.example.mhg


object SampleGattAttributes {
    private val attributes: HashMap<Any?, Any?> = HashMap<Any?, Any?>()
    var HEART_RATE_MEASUREMENT = "0000fff1-0000-1000-8000-00805f9b34fb"
    var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    init {
        // Sample Services.
        attributes["0000fff0-0000-1000-8000-00805f9b34fb"] = "Heart Rate Service"
        attributes["0000fff0-0000-1000-8000-00805f9b34fb"] = "Device Information Service"
        // Sample Characteristics.
        attributes[HEART_RATE_MEASUREMENT] = "Heart Rate Measurement"
        attributes["0000fff0-0000-1000-8000-00805f9b34fb"] = "Manufacturer Name String"
    }

    fun lookup(uuid: String?, defaultName: String): String {
        val name = attributes[uuid]
        return (name ?: defaultName).toString()
    }
}