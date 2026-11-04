package dev.sort.datafusionspice.catalog

import com.intellij.database.dataSource.LocalDataSource
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import dev.brikk.house.sql.intellij.catalog.*
import dev.brikk.house.sql.metadata.FunctionDef
import dev.brikk.house.sql.metadata.FunctionKind
import dev.sort.datafusionspice.PluginPlatformTestCase

class CatalogRefreshFeedbackTest : PluginPlatformTestCase() {
    private val received = mutableListOf<Notification>()

    override fun setUp() {
        super.setUp()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId in setOf("Spice", "DataFusion")) {
                    notification.setSuppressShowingPopup(true)
                    received += notification
                }
            }
        })
    }

    override fun tearDown() {
        received.forEach(Notification::expire)
        super.tearDown()
    }

    fun testExplicitSuccessUsesTheCorrectProductAndEscapesSourceNames() {
        val inventory = LiveInventory("2.4.0", listOf(LiveFunction(FunctionDef("abs", FunctionKind.SCALAR))), false)
        for (profile in listOf(datafusionCatalog, spiceCatalog)) {
            val source = LocalDataSource().apply { name = "<b>Playground & sample</b>" }
            catalogRefreshCompleted(project, source, profile, RefreshOutcome.Refreshed(inventory), RefreshReason.Manual)
            val notification = received.last()
            assertEquals(profile.productLabel, notification.groupId)
            assertEquals("${profile.productLabel} catalog", notification.title)
            assertEquals(NotificationType.INFORMATION, notification.type)
            assertTrue(notification.content.contains("refreshed 1 function"))
            assertTrue(notification.content.contains("&lt;b&gt;"))
            assertFalse(notification.content.contains("<b>"))
        }
        assertEquals(2, received.size)
    }

    fun testAutomaticEventsAndDisabledProfilesDoNotProduceFeedback() {
        val source = LocalDataSource()
        val outcome = RefreshOutcome.Refreshed(LiveInventory("2.4.0", emptyList(), false))
        catalogRefreshCompleted(project, source, spiceCatalog, outcome, RefreshReason.Executed(false))
        catalogRefreshCompleted(project, source, spiceCatalog, outcome, RefreshReason.Executed(true))
        catalogRefreshCompleted(project, source, spiceCatalog, RefreshOutcome.Disabled, RefreshReason.Manual)
        assertEmpty(received)
    }

    fun testConnectionFailureDoesNotExposeDriverExceptionCredentials() {
        catalogRefreshCompleted(project, LocalDataSource(), spiceCatalog,
            RefreshOutcome.ConnectFailed("jdbc:fixture?password=do-not-display"), RefreshReason.Manual)
        val notification = received.single()
        assertEquals(NotificationType.WARNING, notification.type)
        assertTrue(notification.content.contains("Check the data source settings"))
        assertFalse(notification.content.contains("password"))
        assertFalse(notification.content.contains("do-not-display"))
    }

    fun testIncompleteSupersededAndTransactionOutcomesAreNotReportedAsSuccess() {
        for (outcome in listOf(RefreshOutcome.HarvestFailed(HarvestOutcome.Incomplete),
            RefreshOutcome.Superseded, RefreshOutcome.SessionInTransaction)) {
            catalogRefreshCompleted(project, LocalDataSource(), datafusionCatalog, outcome, RefreshReason.Manual)
        }
        assertEquals(3, received.size)
        assertTrue(received[0].content.contains("metadata was left unchanged"))
        assertTrue(received[1].content.contains("superseded"))
        assertTrue(received[1].content.contains("newer refresh"))
        assertTrue(received[2].content.contains("transaction"))
        assertTrue(received.none { it.content.contains("refreshed") })
    }

    fun testSharedNotificationChannelDoesNotGetADuplicateFallback() {
        val withChannel = object : LiveCatalogProfile by spiceCatalog {
            override val notificationGroupId = "Spice"
        }
        catalogRefreshCompleted(project, LocalDataSource(), withChannel,
            RefreshOutcome.Refreshed(LiveInventory("2.4.0", emptyList(), false)), RefreshReason.Manual)
        assertEmpty(received)
    }
}
