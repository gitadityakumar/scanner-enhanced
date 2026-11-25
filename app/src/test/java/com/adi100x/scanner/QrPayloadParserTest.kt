package com.adi100x.scanner

import com.adi100x.scanner.scanner.QrPayloadParser
import com.adi100x.scanner.scanner.ScanContentType
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadParserTest {

    @Test
    fun parsesUpiPayUriCorrectly() {
        val raw = "upi://pay?pa=merchant@paytm&pn=Chai%20Point&am=45.00&cu=INR&tn=Order%2012"
        val result = QrPayloadParser.parse(raw, Barcode.FORMAT_QR_CODE)
        assertEquals(ScanContentType.UPI, result.type)
        assertNotNull(result.upi)
        assertEquals("merchant@paytm", result.upi?.pa)
        assertEquals("Chai Point", result.upi?.pn)
        assertEquals("45.00", result.upi?.am)
        assertEquals("Order 12", result.upi?.tn)
    }

    @Test
    fun parsesWifiQrCorrectly() {
        val raw = "WIFI:T:WPA;S:Office\\;Net;P:pass\\:123;H:true;;"
        val result = QrPayloadParser.parse(raw, Barcode.FORMAT_QR_CODE)
        assertEquals(ScanContentType.WIFI, result.type)
        assertEquals("Office;Net", result.wifi?.ssid)
        assertEquals("pass:123", result.wifi?.password)
        assertEquals("WPA/WPA2", result.wifi?.encryption)
        assertTrue(result.wifi?.hidden == true)
    }

    @Test
    fun parsesUrlAndBarcodeCorrectly() {
        val urlRes = QrPayloadParser.parse("https://example.org/test?a=1", Barcode.FORMAT_QR_CODE)
        assertEquals(ScanContentType.URL, urlRes.type)
        assertEquals("example.org", urlRes.title)

        val eanRes = QrPayloadParser.parse("8901058851234", Barcode.FORMAT_EAN_13)
        assertEquals(ScanContentType.PRODUCT_BARCODE, eanRes.type)
        assertEquals("EAN-13", eanRes.formatLabel)
    }
}
