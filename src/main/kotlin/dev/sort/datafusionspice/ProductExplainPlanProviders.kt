package dev.sort.datafusionspice

import com.intellij.database.Dbms
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.datagrid.DataRequest
import com.intellij.database.plan.ExplainPlanProvider
import com.intellij.database.plan.PlanModel
import com.intellij.database.util.Version
import com.intellij.util.Consumer

// Only explicitly authored EXPLAIN in the user's console is supported.
abstract class ProductExplainPlanProvider(dbms: Dbms) : ExplainPlanProvider(dbms) {
    override fun isSupported(version: Version, analyze: Boolean): Boolean = false
    override fun isRawSupported(version: Version, analyze: Boolean): Boolean = false
    override fun createExplainRequest(
        owner: DataRequest.OwnerEx, consumer: Consumer<in PlanModel>, dataSource: LocalDataSource,
        statement: String, analyze: Boolean,
    ): DataRequest.RawRequest = throw UnsupportedOperationException("Execute authored EXPLAIN in the console")
    override fun createRawExplainTask(dataSource: LocalDataSource, analyze: Boolean): RawExplainTask =
        throw UnsupportedOperationException("Execute authored EXPLAIN in the console")
}

class DatafusionExplainPlanProvider : ProductExplainPlanProvider(DatafusionDbms.DATAFUSION_BRIKK)
class SpiceExplainPlanProvider : ProductExplainPlanProvider(SpiceDbms.SPICE_BRIKK)
