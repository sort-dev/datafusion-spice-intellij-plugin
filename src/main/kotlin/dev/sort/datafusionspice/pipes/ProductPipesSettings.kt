package dev.sort.datafusionspice.pipes

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.brikk.house.sql.intellij.ToggleConfigurable
import dev.brikk.house.sql.intellij.ToggleSettings
import dev.brikk.house.sql.intellij.pipe.DatafusionPipeDialect
import dev.sort.datafusionspice.catalog.datafusionCatalog
import dev.sort.datafusionspice.catalog.spiceCatalog
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect

@Service(Service.Level.PROJECT)
@State(name = "DatafusionPipesSettings", storages = [Storage(ToggleSettings.STORAGE)])
class DatafusionPipesSettings(project: Project) : ToggleSettings(project, DatafusionSqlDialect)

@Service(Service.Level.PROJECT)
@State(name = "SpicePipesSettings", storages = [Storage(ToggleSettings.STORAGE)])
class SpicePipesSettings(project: Project) : ToggleSettings(project, SpiceSqlDialect)

val datafusionPipe = DatafusionPipeDialect(DatafusionSqlDialect, datafusionCatalog) { it.service<DatafusionPipesSettings>().enabled }
val spicePipe = DatafusionPipeDialect(SpiceSqlDialect, spiceCatalog) { it.service<SpicePipesSettings>().enabled }

class DatafusionPipesConfigurable(project: Project) : ToggleConfigurable(
    "dev.sort.datafusionspice.datafusion.pipes", datafusionPipe.label, "Enable DataFusion Brikk PIPE",
    help = "Preview and execute Brikk-generated SQL. Native server verification is unavailable.",
    vetoProperty = datafusionPipe.vetoProperty,
) {
    override val settings = project.service<DatafusionPipesSettings>()
}

class SpicePipesConfigurable(project: Project) : ToggleConfigurable(
    "dev.sort.datafusionspice.spice.pipes", spicePipe.label, "Enable Spice Brikk PIPE",
    help = "Preview and execute Brikk-generated SQL, not native Spice pipe syntax. Transport limits still apply.",
    vetoProperty = spicePipe.vetoProperty,
) {
    override val settings = project.service<SpicePipesSettings>()
}
