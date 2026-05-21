package com.chronoswing.buddydash.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BambuddyOpenApiTest {

    @Test
    fun clearPlateEndpoint_matchesOpenApiSpec() {
        val spec = findOpenApiSpec().readText()

        assertTrue(
            "Expected ${BambuddyApi.CLEAR_PLATE_PATH} in bambuddy-openapi.json",
            spec.contains("\"${BambuddyApi.CLEAR_PLATE_PATH}\""),
        )
        assertTrue(
            spec.contains("clear_plate_api_v1_printers__printer_id__clear_plate_post"),
        )
        assertTrue(spec.contains("\"summary\":\"Clear Plate\"") || spec.contains("\"summary\": \"Clear Plate\""))
    }

    private fun findOpenApiSpec(): File {
        var dir: File? = File(System.getProperty("user.dir"))
        while (dir != null) {
            val candidate = File(dir, "bambuddy-openapi.json")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        error("bambuddy-openapi.json not found")
    }
}
