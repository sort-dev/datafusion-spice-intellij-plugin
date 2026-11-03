package dev.sort.datafusionspice.sql

import com.intellij.database.Dbms
import com.intellij.openapi.components.service
import dev.brikk.house.sql.intellij.dialect.ConnectedDatafusionEditorProfile
import dev.brikk.house.sql.intellij.dialect.DatafusionFamilyDialect
import dev.brikk.house.sql.intellij.dialect.DatafusionIdeProfile
import dev.sort.datafusionspice.SpiceDbms
import dev.sort.datafusionspice.catalog.SpiceLiveCatalogService
import dev.sort.datafusionspice.catalog.spiceCatalog
import dev.brikk.house.sql.intellij.catalog.DasRelationFacts

object SpiceSqlDialect : DatafusionFamilyDialect(DatafusionIdeProfile.SPICE) {
    override fun getDbms(): Dbms = SpiceDbms.SPICE_BRIKK
    override val editorProfile by lazy {
        ConnectedDatafusionEditorProfile(this, { service<SpiceLiveCatalogService>() },
            listOf(DasRelationFacts(spiceCatalog.dataSources)))
    }
}
