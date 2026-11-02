package com.example.calculator.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubnetResult(
    val inputIp: String,
    val cidr: Int,
    val subnetMask: String,
    val wildcardMask: String,
    val networkAddress: String,
    val broadcastAddress: String,
    val firstUsableIp: String,
    val lastUsableIp: String,
    val totalIps: Long,
    val usableHosts: Long,
    val ipClass: String,
    val classNote: String,
    val addressType: String,
    val addressTypeDetail: String,
    val ipRange: String,
    val binaryIp: String,
    val binarySubnetMask: String,
    val binaryNetwork: String,
    val binaryBroadcast: String,
    val isPointToPoint: Boolean,
    val isSingleHost: Boolean
)

@JsonClass(generateAdapter = true)
data class VlsmSubnet(
    val subnetIndex: Int,
    val name: String,
    val neededHosts: Int,
    val allocatedHosts: Int,
    val prefix: Int,
    val networkAddress: String,
    val subnetMask: String,
    val firstUsable: String,
    val lastUsable: String,
    val broadcastAddress: String
)

@JsonClass(generateAdapter = true)
data class IPv6Result(
    val originalAddress: String,
    val expandedAddress: String,
    val compressedAddress: String,
    val prefix: Int,
    val addressType: String,
    val addressScope: String,
    val totalAddressesExp: String
)

@JsonClass(generateAdapter = true)
data class CalculationHistoryItem(
    val id: String,
    val timestamp: Long,
    val ipWithCidr: String,
    val networkAddress: String,
    val usableRange: String,
    val usableHosts: Long,
    val subnetMask: String
)
