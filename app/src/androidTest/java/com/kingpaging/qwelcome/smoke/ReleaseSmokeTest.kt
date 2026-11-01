package com.kingpaging.qwelcome.smoke

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.QWelcomeApplication
import com.kingpaging.qwelcome.data.ExportResult
import com.kingpaging.qwelcome.data.ImportApplyResult
import com.kingpaging.qwelcome.data.ImportValidationResult
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UserPreferences
import com.kingpaging.qwelcome.data.protoDataStore
import com.kingpaging.qwelcome.di.AppContainer
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateSelectionViewModel
import com.kingpaging.qwelcome.navigation.AppNavGraph
import com.kingpaging.qwelcome.navigation.Routes
import com.kingpaging.qwelcome.testutil.FakeNavigator
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on debug and the release-derived, R8-minified smoke variant.
 * Uses the real navigation graph, serializers and Proto DataStore.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var application: QWelcomeApplication
    private lateinit var appContainer: AppContainer
    private lateinit var originalPreferences: UserPreferences
    private lateinit var navController: NavHostController

    @Before
    fun setUp() =
        runBlocking {
            application = ApplicationProvider.getApplicationContext()
            appContainer = application.appContainer
            originalPreferences = application.protoDataStore.data.first()
            application.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
            Unit
        }

    @After
    fun tearDown() =
        runBlocking {
            application.protoDataStore.updateData { originalPreferences }
            Unit
        }

    @Test
    fun typedNavigation_roundTripsEditorArgumentsAndReturnsThroughDestinations() {
        val template = Template.create("Smoke welcome", "Hello {{ customer_name }}, network {{ ssid }}")
        runBlocking { appContainer.settingsStore.saveTemplate(template) }
        setNavigationContent()

        navigateTo(Routes.Settings)
        composeRule.runOnIdle { navController.currentBackStackEntry!!.toRoute<Routes.Settings>() }
        navigateTo(Routes.TemplateList)
        navigateTo(Routes.TemplateEditor(template.id))
        composeRule.runOnIdle {
            assertEquals(template.id, navController.currentBackStackEntry!!.toRoute<Routes.TemplateEditor>().templateId)
        }
        popDestination()
        composeRule.runOnIdle { navController.currentBackStackEntry!!.toRoute<Routes.TemplateList>() }
        popDestination()
        navigateTo(Routes.Export)
        composeRule.runOnIdle { navController.currentBackStackEntry!!.toRoute<Routes.Export>() }
        popDestination()
        navigateTo(Routes.Import)
        composeRule.runOnIdle { navController.currentBackStackEntry!!.toRoute<Routes.Import>() }
        popDestination()
        popDestination()
        composeRule.runOnIdle { navController.currentBackStackEntry!!.toRoute<Routes.Main>() }
    }

    @Test
    fun fullBackup_roundTripsRealProfileTemplatesAndActiveSelection() =
        runBlocking {
            val store = appContainer.settingsStore
            val repository = appContainer.importExportRepository
            val profile = TechProfile(name = "Seán O’Neil", title = "Field Tech", dept = "Install")
            val template =
                Template
                    .create("Welcome", "Hi {{ customer_name }}, connect to {{ ssid }}. {{ tech_signature }}")
                    .copy(tags = listOf("Residential", "Fiber"))
            store.saveTechProfile(profile)
            store.saveTemplate(template)
            store.setActiveTemplate(template.id)

            val exported = repository.exportFullBackup()
            assertTrue(exported is ExportResult.Success)
            application.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
            val validated = repository.validateImport((exported as ExportResult.Success).json)
            assertTrue(validated is ImportValidationResult.ValidFullBackup)
            val applied =
                repository.applyFullBackup(
                    (validated as ImportValidationResult.ValidFullBackup).backup,
                    importTechProfile = true,
                )

            assertEquals(ImportApplyResult.Success(1, techProfileImported = true), applied)
            assertEquals(profile, store.getTechProfile())
            assertEquals(listOf(template), store.getUserTemplates())
            assertEquals(template.id, store.getActiveTemplateId())
        }

    private fun setNavigationContent() {
        composeRule.setContent {
            val factory = remember(appContainer) { AppViewModelProvider(appContainer) }
            val customerIntakeViewModel: CustomerIntakeViewModel = viewModel(factory = factory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val templateSelectionViewModel: TemplateSelectionViewModel = viewModel(factory = factory)
            val controller = rememberNavController()
            SideEffect { navController = controller }

            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalCustomerIntakeViewModel provides customerIntakeViewModel,
                    LocalSettingsViewModel provides settingsViewModel,
                    LocalTemplateSelectionViewModel provides templateSelectionViewModel,
                    LocalNavigator provides FakeNavigator(),
                    LocalSoundPlayer provides FakeSoundPlayer(),
                ) {
                    AppNavGraph(navController = controller, appContainer = appContainer)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun navigateTo(route: Any) {
        composeRule.runOnIdle { navController.navigate(route) }
        composeRule.waitForIdle()
    }

    private fun popDestination() {
        composeRule.runOnIdle { assertTrue(navController.popBackStack()) }
        composeRule.waitForIdle()
    }
}
