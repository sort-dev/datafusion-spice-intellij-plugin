package dev.sort.datafusionspice.catalog

import com.intellij.openapi.components.Service
import dev.brikk.house.sql.intellij.catalog.*
import dev.sort.datafusionspice.SpiceDbms
import kotlinx.coroutines.CoroutineScope
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.openapi.project.Project

val spiceCatalog: DatafusionCatalogProfile = ProductCatalogProfile(SpiceDbms.SPICE_BRIKK, "spice")

@Service(Service.Level.APP)
class SpiceLiveCatalogService(scope: CoroutineScope) : LiveCatalogService(scope) {
    override val profile = spiceCatalog
    override fun onRefreshed(project: Project, dataSource: LocalDataSource, outcome: RefreshOutcome, reason: RefreshReason) {
        super.onRefreshed(project, dataSource, outcome, reason)
        catalogRefreshCompleted(project, dataSource, profile, outcome, reason)
    }
}

class SpiceHarvest : ProductHarvest(SpiceLiveCatalogService::class.java)
class SpiceCleanup : LiveCatalogCleanup(SpiceLiveCatalogService::class.java)
class SpiceRefreshCatalogAction : RefreshLiveCatalogAction(SpiceLiveCatalogService::class.java)
