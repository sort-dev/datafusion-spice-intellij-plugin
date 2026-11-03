package dev.sort.datafusionspice.catalog

import com.intellij.database.Dbms
import com.intellij.database.dataSource.LocalDataSource
import dev.brikk.house.sql.intellij.catalog.ConnectionIdentity
import dev.brikk.house.sql.intellij.catalog.DatafusionCatalogProfile

/** Keep user-controlled connection settings separate from metadata learned while connecting. */
internal class ProductCatalogProfile(dbms: Dbms, product: String) : DatafusionCatalogProfile(dbms, product) {
    override fun identity(dataSource: LocalDataSource): ConnectionIdentity = ConnectionIdentity.of(listOf(
        product, dbms.name, dataSource.databaseDriver?.id, dataSource.databaseDriver?.driverClass,
        dataSource.url, dataSource.username, embeddingProfile(dataSource),
        dataSource.connectionProperties.entries.map { it.key.toString() to it.value.toString() }.sortedBy { it.first }.toString(),
        dataSource.driverProperties.toSortedMap().toString(),
    ))
}
