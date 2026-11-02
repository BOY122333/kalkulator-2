package com.example.calculator.engine

import com.example.calculator.model.IPv6Result
import com.example.calculator.model.SubnetResult
import com.example.calculator.model.VlsmSubnet
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Locale

object SubnetCalculator {

    /**
     * Calculates full subnet metrics for an IPv4 address and CIDR prefix length (0..32).
     */
    fun calculate(ipString: String, cidr: Int): Result<SubnetResult> {
        val trimmedIp = ipString.trim()
        val ipLong = parseIpv4ToLong(trimmedIp)
            ?: return Result.failure(IllegalArgumentException("Format IPv4 tidak valid: '$trimmedIp'"))

        if (cidr !in 0..32) {
            return Result.failure(IllegalArgumentException("CIDR prefix harus antara 0 sampai 32 (diterima: $cidr)"))
        }

        val maskLong = cidrToMask(cidr)
        val wildcardLong = maskLong.inv() and 0xFFFFFFFFL
        val networkLong = ipLong and maskLong
        val broadcastLong = networkLong or wildcardLong

        val totalIps = 1L shl (32 - cidr)

        val usableHosts: Long
        val firstUsableLong: Long
        val lastUsableLong: Long
        val isPointToPoint: Boolean
        val isSingleHost: Boolean

        when (cidr) {
            32 -> {
                // Host route: single IP
                usableHosts = 1L
                firstUsableLong = ipLong
                lastUsableLong = ipLong
                isPointToPoint = false
                isSingleHost = true
            }
            31 -> {
                // RFC 3021: Using 31-Bit Prefixes on IPv4 Point-to-Point Links
                // Both network and broadcast addresses are usable as host interfaces
                usableHosts = 2L
                firstUsableLong = networkLong
                lastUsableLong = broadcastLong
                isPointToPoint = true
                isSingleHost = false
            }
            0 -> {
                // Entire IPv4 space: 0.0.0.0 is network, 255.255.255.255 is broadcast
                usableHosts = 4294967294L
                firstUsableLong = 1L
                lastUsableLong = 4294967294L
                isPointToPoint = false
                isSingleHost = false
            }
            else -> {
                // Standard subnetting: minus 2 for network & broadcast IDs
                usableHosts = totalIps - 2L
                firstUsableLong = networkLong + 1L
                lastUsableLong = broadcastLong - 1L
                isPointToPoint = false
                isSingleHost = false
            }
        }

        val subnetMask = longToIpv4(maskLong)
        val wildcardMask = longToIpv4(wildcardLong)
        val networkAddress = longToIpv4(networkLong)
        val broadcastAddress = longToIpv4(broadcastLong)
        val firstUsableIp = longToIpv4(firstUsableLong)
        val lastUsableIp = longToIpv4(lastUsableLong)

        val firstOctet = ((ipLong shr 24) and 0xFF).toInt()
        val (ipClass, classNote) = determineClassfulInfo(firstOctet)
        val (addressType, addressTypeDetail) = determineAddressScope(ipLong, firstOctet)

        val ipRange = "$firstUsableIp - $lastUsableIp"

        val binaryIp = formatBinary(ipLong)
        val binarySubnetMask = formatBinary(maskLong)
        val binaryNetwork = formatBinary(networkLong)
        val binaryBroadcast = formatBinary(broadcastLong)

        return Result.success(
            SubnetResult(
                inputIp = trimmedIp,
                cidr = cidr,
                subnetMask = subnetMask,
                wildcardMask = wildcardMask,
                networkAddress = networkAddress,
                broadcastAddress = broadcastAddress,
                firstUsableIp = firstUsableIp,
                lastUsableIp = lastUsableIp,
                totalIps = totalIps,
                usableHosts = usableHosts,
                ipClass = ipClass,
                classNote = classNote,
                addressType = addressType,
                addressTypeDetail = addressTypeDetail,
                ipRange = ipRange,
                binaryIp = binaryIp,
                binarySubnetMask = binarySubnetMask,
                binaryNetwork = binaryNetwork,
                binaryBroadcast = binaryBroadcast,
                isPointToPoint = isPointToPoint,
                isSingleHost = isSingleHost
            )
        )
    }

    /**
     * Divides a major IPv4 network into subnets according to needed host counts (VLSM).
     */
    fun calculateVlsm(
        majorNetwork: String,
        majorCidr: Int,
        subnetsNeeded: List<Pair<String, Int>> // (Name, NeededHosts)
    ): Result<List<VlsmSubnet>> {
        val baseNetworkLong = parseIpv4ToLong(majorNetwork.trim())
            ?: return Result.failure(IllegalArgumentException("Alamat jaringan awal tidak valid: '$majorNetwork'"))

        if (majorCidr !in 0..30) {
            return Result.failure(IllegalArgumentException("CIDR jaringan awal harus antara /0 hingga /30 untuk VLSM"))
        }

        val majorMask = cidrToMask(majorCidr)
        val normalizedBaseNetwork = baseNetworkLong and majorMask
        val totalAvailableIps = 1L shl (32 - majorCidr)
        val majorBroadcast = normalizedBaseNetwork + totalAvailableIps - 1L

        // Sort subnets descending by needed hosts (standard VLSM algorithm)
        val sortedRequests = subnetsNeeded.mapIndexed { originalIdx, pair ->
            Triple(originalIdx, pair.first, pair.second)
        }.sortedByDescending { it.third }

        var currentNetworkLong = normalizedBaseNetwork
        val results = mutableListOf<VlsmSubnet>()

        for ((_, name, needed) in sortedRequests) {
            if (needed <= 0) continue

            // Determine required prefix
            // Needed + 2 (network & broadcast), or special case for 2 hosts (point-to-point /30 or /31)
            val neededTotal = when (needed) {
                1 -> 4 // /30
                2 -> 4 // /30 for standard point-to-point with net+bcast
                else -> needed + 2
            }

            var allocatedPower = 1
            var prefix = 32
            while (allocatedPower < neededTotal && prefix > 0) {
                allocatedPower = allocatedPower shl 1
                prefix--
            }

            if (prefix < majorCidr) {
                return Result.failure(
                    IllegalStateException("Kebutuhan $needed host untuk subnet '$name' melebihi kapasitas jaringan induk (membutuhkan /$prefix)")
                )
            }

            // Align current network to the prefix boundary
            val subnetMask = cidrToMask(prefix)
            if (currentNetworkLong and (allocatedPower.toLong() - 1L) != 0L) {
                currentNetworkLong = (currentNetworkLong or (allocatedPower.toLong() - 1L)) + 1L
            }

            val subnetBroadcast = currentNetworkLong + allocatedPower - 1L

            if (subnetBroadcast > majorBroadcast) {
                return Result.failure(
                    IllegalStateException("Alokasi IP habis! Subnet '$name' meluap keluar dari batas jaringan induk.")
                )
            }

            val firstUsable = currentNetworkLong + 1L
            val lastUsable = subnetBroadcast - 1L

            results.add(
                VlsmSubnet(
                    subnetIndex = results.size + 1,
                    name = name.ifBlank { "Subnet ${results.size + 1}" },
                    neededHosts = needed,
                    allocatedHosts = allocatedPower - 2,
                    prefix = prefix,
                    networkAddress = longToIpv4(currentNetworkLong),
                    subnetMask = longToIpv4(subnetMask),
                    firstUsable = longToIpv4(firstUsable),
                    lastUsable = longToIpv4(lastUsable),
                    broadcastAddress = longToIpv4(subnetBroadcast)
                )
            )

            currentNetworkLong = subnetBroadcast + 1L
        }

        return Result.success(results)
    }

    /**
     * IPv6 Parsing, Expansion, Compression, and Address Classification.
     */
    fun calculateIpv6(ipv6String: String, prefix: Int = 64): Result<IPv6Result> {
        val trimmed = ipv6String.trim()
        val inetAddr = try {
            val addr = InetAddress.getByName(trimmed)
            if (addr !is Inet6Address) {
                return Result.failure(IllegalArgumentException("Bukan alamat IPv6 yang valid"))
            }
            addr
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Format IPv6 tidak valid: ${e.message}"))
        }

        val bytes = inetAddr.address
        val hexWords = mutableListOf<Int>()
        val hexGroups = mutableListOf<String>()
        for (i in 0 until 16 step 2) {
            val high = bytes[i].toInt() and 0xFF
            val low = bytes[i + 1].toInt() and 0xFF
            val word = (high shl 8) or low
            hexWords.add(word)
            hexGroups.add(String.format(Locale.US, "%04x", word))
        }
        val expanded = hexGroups.joinToString(":")
        val compressed = compressIpv6Hextets(hexWords)

        val (type, scope) = classifyIpv6(bytes)
        val remainingBits = 128 - prefix.coerceIn(0, 128)
        val totalExp = "2^$remainingBits alamat"

        return Result.success(
            IPv6Result(
                originalAddress = trimmed,
                expandedAddress = expanded,
                compressedAddress = compressed,
                prefix = prefix,
                addressType = type,
                addressScope = scope,
                totalAddressesExp = totalExp
            )
        )
    }

    // Helper functions

    fun parseIpv4ToLong(ip: String): Long? {
        val parts = ip.split(".")
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            val num = part.toLongOrNull() ?: return null
            if (num !in 0..255) return null
            result = (result shl 8) or num
        }
        return result
    }

    fun longToIpv4(ip: Long): String {
        val o1 = (ip shr 24) and 0xFF
        val o2 = (ip shr 16) and 0xFF
        val o3 = (ip shr 8) and 0xFF
        val o4 = ip and 0xFF
        return "$o1.$o2.$o3.$o4"
    }

    fun cidrToMask(cidr: Int): Long {
        return if (cidr == 0) 0L else (-1L shl (32 - cidr)) and 0xFFFFFFFFL
    }

    fun maskToCidr(maskIp: String): Int? {
        val maskLong = parseIpv4ToLong(maskIp) ?: return null
        var count = 0
        var foundZero = false
        for (i in 31 downTo 0) {
            val bit = (maskLong shr i) and 1L
            if (bit == 1L) {
                if (foundZero) return null // Discontiguous mask
                count++
            } else {
                foundZero = true
            }
        }
        return count
    }

    private fun formatBinary(value: Long): String {
        val o1 = String.format(Locale.US, "%8s", java.lang.Long.toBinaryString((value shr 24) and 0xFF)).replace(' ', '0')
        val o2 = String.format(Locale.US, "%8s", java.lang.Long.toBinaryString((value shr 16) and 0xFF)).replace(' ', '0')
        val o3 = String.format(Locale.US, "%8s", java.lang.Long.toBinaryString((value shr 8) and 0xFF)).replace(' ', '0')
        val o4 = String.format(Locale.US, "%8s", java.lang.Long.toBinaryString(value and 0xFF)).replace(' ', '0')
        return "$o1.$o2.$o3.$o4"
    }

    private fun determineClassfulInfo(firstOctet: Int): Pair<String, String> {
        val historicalDisclaimer = "Konsep classful addressing (RFC 791) adalah arsitektur historis yang telah digantikan oleh CIDR (Classless Inter-Domain Routing, RFC 4632) sejak 1993."
        return when (firstOctet) {
            in 0..126 -> Pair("Class A (Default /8)", "$historicalDisclaimer Digunakan untuk jaringan berskala sangat besar.")
            127 -> Pair("Class A (Khusus Loopback)", "$historicalDisclaimer Blok 127.0.0.0/8 dialokasikan untuk antarmuka loopback lokal.")
            in 128..191 -> Pair("Class B (Default /16)", "$historicalDisclaimer Dirancang untuk jaringan skala menengah hingga besar.")
            in 192..223 -> Pair("Class C (Default /24)", "$historicalDisclaimer Dirancang untuk jaringan lokal (LAN) berskala kecil.")
            in 224..239 -> Pair("Class D (Multicast)", "Digunakan untuk pengiriman paket multicast (RFC 1112/5771). Tidak digunakan untuk pengalamatan host individual.")
            in 240..255 -> Pair("Class E (Eksperimental)", "Dicadangkan untuk penelitian masa depan (RFC 1700). Tidak dialokasikan di internet publik.")
            else -> Pair("Tidak Diketahui", historicalDisclaimer)
        }
    }

    private fun determineAddressScope(ipLong: Long, firstOctet: Int): Pair<String, String> {
        // RFC 1918 Private ranges:
        // 10.0.0.0/8 (10.0.0.0 - 10.255.255.255)
        if (firstOctet == 10) {
            return Pair("Private Network", "RFC 1918 (10.0.0.0/8) - Tidak dapat dirutekan langsung di Internet publik.")
        }
        // 172.16.0.0/12 (172.16.0.0 - 172.31.255.255)
        val secondOctet = ((ipLong shr 16) and 0xFF).toInt()
        if (firstOctet == 172 && secondOctet in 16..31) {
            return Pair("Private Network", "RFC 1918 (172.16.0.0/12) - Ruang alamat privat untuk intranet & LAN.")
        }
        // 192.168.0.0/16 (192.168.0.0 - 192.168.255.255)
        if (firstOctet == 192 && secondOctet == 168) {
            return Pair("Private Network", "RFC 1918 (192.168.0.0/16) - Umum digunakan pada jaringan rumah & kantor.")
        }
        // Loopback 127.0.0.0/8
        if (firstOctet == 127) {
            return Pair("Loopback", "RFC 5735 (127.0.0.0/8) - Berkomunikasi dengan sistem lokal (localhost).")
        }
        // Link-Local 169.254.0.0/16 (APIPA)
        if (firstOctet == 169 && secondOctet == 254) {
            return Pair("Link-Local (APIPA)", "RFC 3927 (169.254.0.0/16) - Alokasi otomatis saat DHCP server tidak merespons.")
        }
        // Carrier-Grade NAT 100.64.0.0/10
        if (firstOctet == 100 && (secondOctet in 64..127)) {
            return Pair("Shared Address (CGNAT)", "RFC 6598 (100.64.0.0/10) - Digunakan oleh ISP untuk Carrier-Grade NAT.")
        }
        // Multicast 224.0.0.0/4
        if (firstOctet in 224..239) {
            return Pair("Multicast", "RFC 5771 (224.0.0.0/4) - Grup pengiriman pesan ke banyak penerima sekaligus.")
        }
        // Documentation RFC 5737
        val thirdOctet = ((ipLong shr 8) and 0xFF).toInt()
        if ((firstOctet == 192 && secondOctet == 0 && thirdOctet == 2) ||
            (firstOctet == 198 && secondOctet == 51 && thirdOctet == 100) ||
            (firstOctet == 203 && secondOctet == 0 && thirdOctet == 113)) {
            return Pair("Dokumentasi / Contoh", "RFC 5737 - Dicadangkan untuk materi dokumentasi & buku teks.")
        }
        // Broadcast
        if (ipLong == 0xFFFFFFFFL) {
            return Pair("Limited Broadcast", "RFC 919 (255.255.255.255) - Broadcast ke seluruh host di link lokal.")
        }
        if (ipLong == 0L) {
            return Pair("Current Network (This Host)", "RFC 1122 (0.0.0.0) - Menunjuk host saat ini saat inisialisasi boot.")
        }

        return Pair("Public Internet", "Alamat IP publik yang dapat dirutekan secara global di Internet.")
    }

    private fun classifyIpv6(bytes: ByteArray): Pair<String, String> {
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF

        // Loopback ::1
        var allZero = true
        for (i in 0..14) {
            if (bytes[i] != 0.toByte()) { allZero = false; break }
        }
        if (allZero && bytes[15] == 1.toByte()) {
            return Pair("Loopback (::1)", "Node-Local")
        }
        if (allZero && bytes[15] == 0.toByte()) {
            return Pair("Unspecified (::)", "Node-Local")
        }

        // Multicast ff00::/8
        if (b0 == 0xFF) {
            val scopeNibble = b1 and 0x0F
            val scopeStr = when (scopeNibble) {
                1 -> "Interface-Local"
                2 -> "Link-Local"
                5 -> "Site-Local"
                8 -> "Organization-Local"
                14 -> "Global"
                else -> "Multicast Scope ($scopeNibble)"
            }
            return Pair("Multicast (ff00::/8)", scopeStr)
        }

        // Link-Local fe80::/10
        if (b0 == 0xFE && (b1 and 0xC0) == 0x80) {
            return Pair("Link-Local Unicast (fe80::/10)", "Link-Local")
        }

        // Unique Local fc00::/7
        if ((b0 and 0xFE) == 0xFC) {
            return Pair("Unique Local Address / ULA (fc00::/7)", "Site/Private Intranet")
        }

        // Global Unicast 2000::/3
        if ((b0 and 0xE0) == 0x20) {
            return Pair("Global Unicast Address (2000::/3)", "Global Public Internet")
        }

        return Pair("IPv6 Unicast / Reserved", "Global")
    }

    private fun compressIpv6Hextets(hextets: List<Int>): String {
        var maxStart = -1
        var maxLen = 0
        var curStart = -1
        var curLen = 0

        for (i in hextets.indices) {
            if (hextets[i] == 0) {
                if (curStart == -1) {
                    curStart = i
                    curLen = 1
                } else {
                    curLen++
                }
                if (curLen > maxLen) {
                    maxLen = curLen
                    maxStart = curStart
                }
            } else {
                curStart = -1
                curLen = 0
            }
        }

        if (maxLen > 1) {
            val left = hextets.subList(0, maxStart).joinToString(":") { Integer.toHexString(it) }
            val right = hextets.subList(maxStart + maxLen, hextets.size).joinToString(":") { Integer.toHexString(it) }
            return when {
                left.isEmpty() && right.isEmpty() -> "::"
                left.isEmpty() -> "::$right"
                right.isEmpty() -> "$left::"
                else -> "$left::$right"
            }
        } else {
            return hextets.joinToString(":") { Integer.toHexString(it) }
        }
    }
}
