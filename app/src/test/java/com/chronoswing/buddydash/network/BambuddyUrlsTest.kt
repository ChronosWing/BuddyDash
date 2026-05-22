package com.chronoswing.buddydash.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BambuddyUrlsTest {

    @Test
    fun printerCameraSnapshotUrl_includesTokenAndCacheBust() {
        val url = printerCameraSnapshotUrl(
            serverUrl = "http://bambuddy.local",
            printerId = 3,
            cameraToken = "secret token",
            cacheBust = 1_700_000_000L,
        )
        assertNotNull(url)
        assertTrue(url!!.contains("/api/v1/printers/3/camera/snapshot"))
        assertTrue(url.contains("token="))
        assertTrue(url.contains("v=1700000000"))
        assertEquals(
            "http://bambuddy.local/api/v1/printers/3/camera/snapshot?v=1700000000&token=secret+token",
            url,
        )
    }

    @Test
    fun printerCameraStreamUrl_includesTokenAndFps() {
        val url = printerCameraStreamUrl(
            serverUrl = "http://bambuddy.local",
            printerId = 3,
            cameraToken = "secret token",
            fps = 15,
        )
        assertNotNull(url)
        assertTrue(url!!.contains("/api/v1/printers/3/camera/stream"))
        assertTrue(url.contains("token="))
        assertTrue(url.contains("fps=15"))
    }

    @Test
    fun printerCoverUrl_omitsCacheBust() {
        val url = printerCoverUrl("http://bambuddy.local", 1, "tok")
        assertNotNull(url)
        assertTrue(url!!.contains("/cover?token=tok"))
        assertTrue(!url.contains("&t="))
    }
}
