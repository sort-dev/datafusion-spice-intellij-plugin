package dev.sort.datafusionspice

import com.intellij.database.Dbms
import com.intellij.icons.AllIcons

object DatafusionDbms {
    @JvmField
    val DATAFUSION_BRIKK: Dbms = Dbms.create(
        "DATAFUSION_BRIKK", "DataFusion (sort.dev)", { AllIcons.Nodes.DataTables }, Dbms.defaultPattern("datafusion"),
    )
}
