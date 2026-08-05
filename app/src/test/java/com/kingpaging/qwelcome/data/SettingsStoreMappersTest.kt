package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsStoreMappersTest {

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `template toProto and fromProto preserves sortOrder and tags`() {
        val template = Template(
            id = "template-1",
            name = "Custom",
            content = "Hello {{ customer_name }} {{ ssid }}",
            createdAt = "2026-02-01T00:00:00Z",
            modifiedAt = "2026-02-02T00:00:00Z",
            slug = "custom",
            sortOrder = 42,
            tags = listOf("fiber", "install", "vip")
        )

        val proto = template.toProto()
        val roundTrip = Template.fromProto(proto)

        assertEquals(42, proto.sortOrder)
        assertEquals(listOf("fiber", "install", "vip"), proto.tagsList)
        assertEquals(42, roundTrip.sortOrder)
        assertEquals(listOf("fiber", "install", "vip"), roundTrip.tags)
    }

    @Test
    fun `template fromProto defaults to empty tags when absent`() {
        val proto = TemplateProto.newBuilder()
            .setId("template-2")
            .setName("Legacy")
            .setContent("Hello {{ customer_name }} {{ ssid }}")
            .setCreatedAt("2026-02-01T00:00:00Z")
            .setModifiedAt("2026-02-02T00:00:00Z")
            .setSlug("legacy")
            .build()

        val mapped = Template.fromProto(proto)

        assertEquals(0, mapped.sortOrder)
        assertTrue(mapped.tags.isEmpty())
    }

    @Test
    fun `privacy settings toProto and fromProto preserve values`() {
        val settings = PrivacySettings(
            crashReportingEnabled = false,
            screenCaptureProtectionEnabled = true
        )

        val roundTrip = PrivacySettings.fromProto(settings.toProto())

        assertEquals(settings, roundTrip)
    }

    @Test
    fun `privacy settings from legacy proto enables crash reporting by default`() {
        val mapped = PrivacySettings.fromProto(PrivacySettingsProto.getDefaultInstance())

        assertTrue(mapped.crashReportingEnabled)
        assertTrue(!mapped.screenCaptureProtectionEnabled)
    }

    @Test
    fun `privacy settings I O fallback disables crash reporting and protects screen capture`() = runTest {
        val settings = flow<UserPreferences> { throw IOException("read failed") }
            .readPrivacySettings()
            .first()

        assertTrue(!settings.crashReportingEnabled)
        assertTrue(settings.screenCaptureProtectionEnabled)
    }

}
