package dev.sort.datafusionspice

import com.intellij.database.Dbms
import com.intellij.icons.AllIcons

object SpiceDbms {
    @JvmField
    val SPICE_BRIKK: Dbms = Dbms.create(
        "SPICE_BRIKK", "Spice (sort.dev)", { AllIcons.Nodes.DataTables }, Dbms.defaultPattern("spice"),
    )
}
