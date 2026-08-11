package com.kingpaging.qwelcome.di

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.QWelcomeApplication
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContainerTest {

    private val application = ApplicationProvider.getApplicationContext<QWelcomeApplication>()

    @Test
    fun application_ownsOneContainer() {
        assertSame(application.appContainer, application.appContainer)
    }

    @Test
    fun dependencies_areMemoizedWithinContainer() {
        val container = AppContainer(application)

        assertSame(container.settingsStore, container.settingsStore)
        assertSame(container.resourceProvider, container.resourceProvider)
        assertSame(container.appUpdater, container.appUpdater)
        assertSame(container.importExportRepository, container.importExportRepository)
    }

    @Test
    fun separatelyConstructedContainers_doNotShareCachedDependencies() {
        val first = AppContainer(application)
        val second = AppContainer(application)

        assertNotSame(first.settingsStore, second.settingsStore)
        assertNotSame(first.resourceProvider, second.resourceProvider)
        assertNotSame(first.appUpdater, second.appUpdater)
        assertNotSame(first.importExportRepository, second.importExportRepository)
    }
}