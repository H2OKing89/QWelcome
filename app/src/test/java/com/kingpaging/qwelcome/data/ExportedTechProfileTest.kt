package com.kingpaging.qwelcome.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for ExportedTechProfile backward compatibility.
 * Ensures that old JSON files using "area" field are correctly imported.
 */
class ExportedTechProfileTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize full backup with techProfile`() {
        // This is the exact JSON structure the user is importing
        val jsonString = """
            {
                "schemaVersion": 1,
                "kind": "full-backup",
                "exportedAt": "2026-02-02T12:23:01.510063Z",
                "appVersion": "2.3.0",
                "techProfile": {
                    "name": "Quentin King",
                    "title": "Installation Technician 1",
                    "dept": "ALLO Fiber | Residential"
                },
                "templates": [],
                "defaults": {
                    "defaultTemplateId": "54c7dcc7-8737-418b-b0d9-9d88c923cd42"
                },
                "settings": null,
                "defaultTemplateId": null
            }
        """.trimIndent()

        val backup = json.decodeFromString<FullBackup>(jsonString)

        assertEquals("Quentin King", backup.techProfile.name)
        assertEquals("Installation Technician 1", backup.techProfile.title)
        assertEquals("ALLO Fiber | Residential", backup.techProfile.dept)
        assertEquals("ALLO Fiber | Residential", backup.techProfile.getDepartment())
    }

    @Test
    fun `deserialize with dept field`() {
        val jsonString = """
            {
                "name": "John Doe",
                "title": "Field Tech",
                "dept": "Network Services"
            }
        """.trimIndent()

        val profile = json.decodeFromString<ExportedTechProfile>(jsonString)

        assertEquals("John Doe", profile.name)
        assertEquals("Field Tech", profile.title)
        assertEquals("Network Services", profile.getDepartment())
        assertEquals("Network Services", profile.dept)
    }

    @Test
    fun `deserialize with legacy area field`() {
        val jsonString = """
            {
                "name": "Jane Smith",
                "title": "Installation Technician",
                "area": "Fiber Services"
            }
        """.trimIndent()

        val profile = json.decodeFromString<ExportedTechProfile>(jsonString)

        assertEquals("Jane Smith", profile.name)
        assertEquals("Installation Technician", profile.title)
        assertEquals("Fiber Services", profile.getDepartment())
        assertEquals("Fiber Services", profile.area)
    }

    @Test
    fun `deserialize with both dept and area prefers dept`() {
        val jsonString = """
            {
                "name": "Bob Johnson",
                "title": "Senior Tech",
                "dept": "Network Services",
                "area": "Fiber Services"
            }
        """.trimIndent()

        val profile = json.decodeFromString<ExportedTechProfile>(jsonString)

        assertEquals("Bob Johnson", profile.name)
        assertEquals("Senior Tech", profile.title)
        // getDepartment() should prefer dept over area
        assertEquals("Network Services", profile.getDepartment())
    }

    @Test
    fun `deserialize with missing fields uses defaults`() {
        val jsonString = """
            {
                "name": "Alice Williams"
            }
        """.trimIndent()

        val profile = json.decodeFromString<ExportedTechProfile>(jsonString)

        assertEquals("Alice Williams", profile.name)
        assertEquals("", profile.title)
        assertEquals("", profile.getDepartment())
    }

    @Test
    fun `deserialize with empty dept falls back to area`() {
        val jsonString = """
            {
                "name": "Charlie Brown",
                "title": "Technician",
                "dept": "",
                "area": "Central Region"
            }
        """.trimIndent()

        val profile = json.decodeFromString<ExportedTechProfile>(jsonString)

        assertEquals("Charlie Brown", profile.name)
        assertEquals("Technician", profile.title)
        // getDepartment() should fall back to area when dept is empty
        assertEquals("Central Region", profile.getDepartment())
    }
}
