package dev.sort.datafusionspice.live

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.concurrency.AppExecutorUtil
import dev.brikk.house.sql.intellij.catalog.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Opt-in desired-contract reproducers. Failures identify released shared-service blockers. */
class SharedFlightCatalogRaceTest : RemoteFlightTestCase() {
    private class ProbeService(scope: CoroutineScope, override val profile: LiveCatalogProfile, private val directory: Path) : LiveCatalogService(scope) {
        override fun persistenceDirectory() = directory
    }

    private fun service(harvester: LiveCatalogHarvester): LiveCatalogService {
        val directory = Files.createTempDirectory(Files.createDirectories(Path.of("build", "readiness")), "catalog-race-")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        Disposer.register(testRootDisposable) { directory.toFile().deleteRecursively() }
        Disposer.register(testRootDisposable) { scope.cancel() }
        return ProbeService(scope, object : LiveCatalogProfile by catalog.profile {
            override val harvester = harvester
        }, directory)
    }

    private fun refresh(service: LiveCatalogService) = AppExecutorUtil.getAppExecutorService().submit(Callable {
        runBlocking { service.refresh(project, source) }
    })

    fun testForgetFencesAnInventoryThatWasAlreadyBeingRead() {
        val captured = CountDownLatch(1)
        val release = CountDownLatch(1)
        val service = service(LiveCatalogHarvester { rows ->
            val result = catalog.profile.harvester.harvest(rows)
            captured.countDown()
            check(release.await(15, TimeUnit.SECONDS)) { "Fixture gate was not released" }
            result
        })
        val pending = refresh(service)
        try {
            PlatformTestUtil.waitWithEventsDispatching("real helper has captured native inventory", { captured.count == 0L }, 20)
            service.forget(source.uniqueId)
            assertNull(service.selection(source).inventory)
            release.countDown()
            PlatformTestUtil.waitWithEventsDispatching("old helper finishes after forget", { pending.isDone }, 20)
            pending.get()
            assertNull("brikk-sql-intellij-support 0.4.0 must not resurrect a forgotten inventory", service.selection(source).inventory)
        } finally {
            release.countDown()
            if (!pending.isDone) pending.cancel(true)
            service.forget(source.uniqueId)
        }
    }

    fun testAnOlderSameIdentityHelperCannotReplaceANewerCompletedRefresh() {
        val captured = CountDownLatch(1)
        val release = CountDownLatch(1)
        val sequence = AtomicInteger()
        val service = service(LiveCatalogHarvester { rows ->
            val index = sequence.incrementAndGet()
            val result = requireNotNull(catalog.profile.harvester.harvest(rows))
            // Deterministic test markers on real native harvests, not claims
            // about different endpoint versions or synthetic SQL execution.
            val marked = result.copy(shared = result.shared.copy(nativeCatalog = result.shared.nativeCatalog!!.copy(
                revision = if (index == 1) "older-captured-inventory" else "newer-captured-inventory")))
            if (index == 1) {
                captured.countDown()
                check(release.await(15, TimeUnit.SECONDS)) { "Fixture gate was not released" }
            }
            marked
        })
        val older = refresh(service)
        try {
            PlatformTestUtil.waitWithEventsDispatching("first real helper captures inventory", { captured.count == 0L }, 20)
            val newer = refresh(service)
            PlatformTestUtil.waitWithEventsDispatching("newer real helper completes first", { newer.isDone }, 20)
            assertInstanceOf(newer.get(), RefreshOutcome.Refreshed::class.java)
            assertEquals("newer-captured-inventory", service.selection(source).inventory?.nativeCatalog?.revision)
            release.countDown()
            PlatformTestUtil.waitWithEventsDispatching("older helper completes last", { older.isDone }, 20)
            older.get()
            assertEquals("brikk-sql-intellij-support 0.4.0 must fence obsolete same-identity refreshes",
                "newer-captured-inventory", service.selection(source).inventory?.nativeCatalog?.revision)
        } finally {
            release.countDown()
            if (!older.isDone) older.cancel(true)
            service.forget(source.uniqueId)
        }
    }
}
