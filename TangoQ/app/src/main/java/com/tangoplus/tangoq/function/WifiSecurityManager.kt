package com.tangoplus.tangoq.function

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.widget.Toast

@Suppress("DEPRECATION")
class WifiSecurityManager(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // 현재 연결된 Wi-Fi의 보안 수준을 확인
    @SuppressLint("MissingPermission")
    fun checkWifiSecurity(): String {
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val networkId = wifiInfo.networkId
        val configurations = wifiManager.configuredNetworks

        // 연결된 네트워크의 보안 설정 확인
        val securityType = configurations?.find { it.networkId == networkId }?.let {
            getSecurityType(it)
        } ?: "UNKNOWN"

        if (securityType == "WEP") {
            // WEP 네트워크에 연결된 경우 경고 표시
            Toast.makeText(context, "현재 연결된 Wi-Fi는 보안에 취약한 WEP 암호화를 사용합니다.", Toast.LENGTH_LONG).show()
        }

        return securityType
    }
    // 네트워크 보안 수준 반환
    private fun getSecurityType(configuration: WifiConfiguration): String {
        return when {
            configuration.wepKeys[0] != null -> "WEP"
            configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK) -> "WPA/WPA2"
            configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) -> "OPEN"
            else -> "UNKNOWN"
        }
    }
}