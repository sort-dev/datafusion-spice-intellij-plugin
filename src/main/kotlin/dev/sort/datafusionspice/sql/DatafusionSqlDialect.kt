package dev.sort.datafusionspice.sql

import com.intellij.database.Dbms
import com.intellij.openapi.components.service
import dev.brikk.house.sql.intellij.dialect.ConnectedDatafusionEditorProfile
import dev.brikk.house.sql.intellij.dialect.DatafusionFamilyDialect
import dev.brikk.house.sql.intellij.dialect.DatafusionIdeProfile
import dev.sort.datafusionspice.DatafusionDbms
import dev.sort.datafusionspice.catalog.DatafusionLiveCatalogService
import dev.sort.datafusionspice.catalog.datafusionCatalog
import dev.brikk.house.sql.intellij.catalog.DasRelationFacts

object DatafusionSqlDialect : DatafusionFamilyDialect(DatafusionIdeProfile.DATAFUSION) {
    override fun getDbms(): Dbms = DatafusionDbms.DATAFUSION_BRIKK
    override val editorProfile by lazy {
        ConnectedDatafusionEditorProfile(this, { service<DatafusionLiveCatalogService>() },
            listOf(DasRelationFacts(datafusionCatalog.dataSources)))
    }
}
