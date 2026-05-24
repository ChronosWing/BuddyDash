package com.chronoswing.buddydash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidationTest {

    @Test
    fun validateConnectionSettings_acceptsUrlWithoutScheme() {
        val result = validateConnectionSettings(
            serverUrl = "192.168.1.50:8080",
            apiKey = "secret-key",
        )
        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.50:8080", result.getOrNull()?.serverUrl)
        assertEquals("secret-key", result.getOrNull()?.apiKey)
    }

    @Test
    fun validateConnectionSettings_stripsApiV1Suffix() {
        val result = validateConnectionSettings(
            serverUrl = "https://bambuddy.local/api/v1",
            apiKey = "secret-key",
        )
        assertTrue(result.isSuccess)
        assertEquals("https://bambuddy.local", result.getOrNull()?.serverUrl)
    }

    @Test
    fun validateConnectionSettings_rejectsBlankApiKey() {
        val result = validateConnectionSettings(
            serverUrl = "http://bambuddy.local",
            apiKey = "   ",
        )
        assertFalse(result.isSuccess)
        assertEquals("API key is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateConnectionSettings_rejectsBlankServerUrl() {
        val result = validateConnectionSettings(
            serverUrl = " ",
            apiKey = "secret-key",
        )
        assertFalse(result.isSuccess)
        assertEquals("Server URL is invalid", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateConnectionSettings_rejectsMalformedServerUrl() {
        val result = validateConnectionSettings(
            serverUrl = "http://",
            apiKey = "secret-key",
        )
        assertFalse(result.isSuccess)
        assertEquals("Server URL is invalid", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateConnectionSettings_trimsCameraToken() {
        val result = validateConnectionSettings(
            serverUrl = "http://bambuddy.local",
            apiKey = "secret-key",
            cameraToken = "  tok  ",
        )
        assertTrue(result.isSuccess)
        assertEquals("tok", result.getOrNull()?.cameraToken)
    }

    @Test
    fun normalizeServerUrlForStorage_returnsNullForInvalidInput() {
        assertEquals(null, normalizeServerUrlForStorage(""))
        assertEquals(null, normalizeServerUrlForStorage("not a url"))
    }
}
