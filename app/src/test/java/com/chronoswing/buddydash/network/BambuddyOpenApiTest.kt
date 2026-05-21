package com.chronoswing.buddydash.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BambuddyOpenApiTest {

    @Test
    fun inventoryAssignmentsEndpoint_matchesOpenApiSpec() {
        val spec = findOpenApiSpec().readText()
        assertTrue(
            spec.contains("\"${BambuddyApi.INVENTORY_ASSIGNMENTS_PATH}\""),
        )
    }

    @Test
    fun cameraSnapshotEndpoint_matchesOpenApiSpec() {
        val spec = findOpenApiSpec().readText()
        assertTrue(spec.contains("\"${BambuddyApi.CAMERA_SNAPSHOT_PATH}\""))
        assertTrue(spec.contains("camera_snapshot_api_v1_printers__printer_id__camera_snapshot_get"))
    }

    @Test
    fun archivesEndpoints_matchOpenApiSpec() {
        val spec = findOpenApiSpec().readText()
        assertTrue(spec.contains("\"${BambuddyApi.ARCHIVES_PATH}\""))
        assertTrue(spec.contains("\"${BambuddyApi.ARCHIVE_DETAIL_PATH}\""))
        assertTrue(spec.contains("list_archives_api_v1_archives__get"))
        assertTrue(spec.contains("get_archive_api_v1_archives__archive_id__get"))
    }

    @Test
    fun spoolInventoryEndpoints_matchOpenApiSpec() {
        val spec = findOpenApiSpec().readText()
        assertTrue(spec.contains("\"${BambuddyApi.INVENTORY_SPOOLS_PATH}\""))
        assertTrue(spec.contains("\"${BambuddyApi.SETTINGS_PATH}\""))
        assertTrue(spec.contains("list_spools_api_v1_inventory_spools_get"))
    }

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
