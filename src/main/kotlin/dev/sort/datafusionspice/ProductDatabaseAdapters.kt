package dev.sort.datafusionspice

import com.intellij.database.Dbms
import com.intellij.database.dataSource.DatabaseConnectionCore
import com.intellij.database.dialects.base.TypeHelper
import com.intellij.database.dialects.postgres.PgDialect
import com.intellij.database.dialects.postgresbase.PgDialectBase
import com.intellij.database.model.DasObject
import com.intellij.database.util.DdlBuilder
import com.intellij.database.util.SearchPath
import dev.brikk.house.sql.intellij.catalog.GenericJdbcIntrospectorGate
import dev.brikk.house.sql.intellij.catalog.PgFamilyModelFacade
import dev.sort.datafusionspice.catalog.productSearchPath
import dev.sort.datafusionspice.catalog.NAMESPACE_QUERY

// The PG family supplies model/PSI machinery, not a protocol or session-command claim.
abstract class ProductDatabaseDialect(private val product: Dbms) : PgDialectBase(TypeHelper.PgTypeHelper()) {
    private val quoting by lazy { PgDialect(product) }
    override fun getDbms(): Dbms = product
    override fun getDisplayName(): String = product.displayName
    override fun qualifiedIdentifier(builder: DdlBuilder, identifier: String, definition: DasObject?, qualifier: DasObject): DdlBuilder =
        quoting.qualifiedIdentifier(builder, identifier, definition, qualifier)
    override fun sqlSetSearchPath(path: SearchPath): String? = null
    override fun sqlResetSearchPath(): String? = null
    override fun getSearchPathQuery(): String = NAMESPACE_QUERY
    override fun tryToLoadSearchPath(connection: DatabaseConnectionCore): SearchPath? = productSearchPath.readBack(connection)
}

class DatafusionDatabaseDialect : ProductDatabaseDialect(DatafusionDbms.DATAFUSION_BRIKK)
class SpiceDatabaseDialect : ProductDatabaseDialect(SpiceDbms.SPICE_BRIKK)
class DatafusionModelFacade(dbms: Dbms) : PgFamilyModelFacade(dbms, databasesAsCatalogs = true)
class SpiceModelFacade(dbms: Dbms) : PgFamilyModelFacade(dbms, databasesAsCatalogs = true)
class DatafusionIntrospectorGate : GenericJdbcIntrospectorGate()
class SpiceIntrospectorGate : GenericJdbcIntrospectorGate()
