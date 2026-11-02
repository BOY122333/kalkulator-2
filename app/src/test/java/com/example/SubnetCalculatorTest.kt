package com.example

import com.example.calculator.engine.SubnetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubnetCalculatorTest {

    @Test
    fun testStandardClassCSubnet() {
        val result = SubnetCalculator.calculate("192.168.1.100", 24).getOrThrow()

        assertEquals("192.168.1.100", result.inputIp)
        assertEquals(24, result.cidr)
        assertEquals("255.255.255.0", result.subnetMask)
        assertEquals("0.0.0.255", result.wildcardMask)
        assertEquals("192.168.1.0", result.networkAddress)
        assertEquals("192.168.1.255", result.broadcastAddress)
        assertEquals("192.168.1.1", result.firstUsableIp)
        assertEquals("192.168.1.254", result.lastUsableIp)
        assertEquals(256L, result.totalIps)
        assertEquals(254L, result.usableHosts)
        assertEquals("Private Network", result.addressType)
        assertFalse(result.isPointToPoint)
        assertFalse(result.isSingleHost)
    }

    @Test
    fun testRfc3021Slash31PointToPoint() {
        // RFC 3021: /31 prefix gives 2 usable hosts
        val result = SubnetCalculator.calculate("10.0.0.0", 31).getOrThrow()

        assertEquals(31, result.cidr)
        assertEquals("255.255.255.254", result.subnetMask)
        assertEquals("0.0.0.1", result.wildcardMask)
        assertEquals("10.0.0.0", result.networkAddress)
        assertEquals("10.0.0.1", result.broadcastAddress)
        assertEquals("10.0.0.0", result.firstUsableIp)
        assertEquals("10.0.0.1", result.lastUsableIp)
        assertEquals(2L, result.totalIps)
        assertEquals(2L, result.usableHosts)
        assertTrue(result.isPointToPoint)
        assertFalse(result.isSingleHost)
    }

    @Test
    fun testSlash32SingleHost() {
        // /32 single host route
        val result = SubnetCalculator.calculate("172.16.5.99", 32).getOrThrow()

        assertEquals(32, result.cidr)
        assertEquals("255.255.255.255", result.subnetMask)
        assertEquals("0.0.0.0", result.wildcardMask)
        assertEquals("172.16.5.99", result.networkAddress)
        assertEquals("172.16.5.99", result.broadcastAddress)
        assertEquals("172.16.5.99", result.firstUsableIp)
        assertEquals("172.16.5.99", result.lastUsableIp)
        assertEquals(1L, result.totalIps)
        assertEquals(1L, result.usableHosts)
        assertTrue(result.isSingleHost)
        assertFalse(result.isPointToPoint)
    }

    @Test
    fun testSlash0EntireIPv4() {
        val result = SubnetCalculator.calculate("0.0.0.0", 0).getOrThrow()

        assertEquals(0, result.cidr)
        assertEquals("0.0.0.0", result.subnetMask)
        assertEquals("255.255.255.255", result.wildcardMask)
        assertEquals("0.0.0.0", result.networkAddress)
        assertEquals("255.255.255.255", result.broadcastAddress)
        assertEquals(4294967296L, result.totalIps)
        assertEquals(4294967294L, result.usableHosts)
    }

    @Test
    fun testAddressTypeClassification() {
        // Loopback
        val loopback = SubnetCalculator.calculate("127.0.0.1", 8).getOrThrow()
        assertEquals("Loopback", loopback.addressType)

        // Link-Local (APIPA)
        val linkLocal = SubnetCalculator.calculate("169.254.12.34", 16).getOrThrow()
        assertEquals("Link-Local (APIPA)", linkLocal.addressType)

        // Public
        val publicIp = SubnetCalculator.calculate("8.8.8.8", 32).getOrThrow()
        assertEquals("Public Internet", publicIp.addressType)

        // Private 10.x
        val private10 = SubnetCalculator.calculate("10.200.1.5", 16).getOrThrow()
        assertEquals("Private Network", private10.addressType)

        // Multicast
        val multicast = SubnetCalculator.calculate("224.0.0.1", 24).getOrThrow()
        assertEquals("Multicast", multicast.addressType)
    }

    @Test
    fun testInvalidInputHandling() {
        val invalidOctet = SubnetCalculator.calculate("192.168.1.300", 24)
        assertTrue(invalidOctet.isFailure)

        val invalidCidr = SubnetCalculator.calculate("192.168.1.1", 33)
        assertTrue(invalidCidr.isFailure)

        val malformed = SubnetCalculator.calculate("not.an.ip.address", 24)
        assertTrue(malformed.isFailure)
    }

    @Test
    fun testVlsmCalculation() {
        val requests = listOf(
            "Sales" to 50,
            "Dev" to 20,
            "P2P" to 2
        )
        val result = SubnetCalculator.calculateVlsm("192.168.10.0", 24, requests).getOrThrow()

        assertEquals(3, result.size)
        // First should be Sales (needs 50 -> gets /26 with 62 usable)
        assertEquals("Sales", result[0].name)
        assertEquals(26, result[0].prefix)
        assertEquals("192.168.10.0", result[0].networkAddress)
        assertEquals("192.168.10.63", result[0].broadcastAddress)

        // Second should be Dev (needs 20 -> gets /27 with 30 usable)
        assertEquals("Dev", result[1].name)
        assertEquals(27, result[1].prefix)
        assertEquals("192.168.10.64", result[1].networkAddress)
        assertEquals("192.168.10.95", result[1].broadcastAddress)

        // Third should be P2P (needs 2 -> gets /30 with 2 usable)
        assertEquals("P2P", result[2].name)
        assertEquals(30, result[2].prefix)
        assertEquals("192.168.10.96", result[2].networkAddress)
        assertEquals("192.168.10.99", result[2].broadcastAddress)
    }

    @Test
    fun testIpv6Calculation() {
        val ipv6Res = SubnetCalculator.calculateIpv6("2001:db8::1", 64).getOrThrow()
        assertNotNull(ipv6Res.expandedAddress)
        assertTrue(ipv6Res.expandedAddress.startsWith("2001:0db8:0000:0000"))
        assertEquals("2001:db8::1", ipv6Res.compressedAddress)
    }
}
