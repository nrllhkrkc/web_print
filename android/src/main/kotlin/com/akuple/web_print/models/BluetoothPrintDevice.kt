package com.akuple.web_print.models

data class BluetoothPrintDevice(val name: String, val address: String) {
    fun toJson(): Map<String, Any> = mapOf("name" to name, "address" to address)
}