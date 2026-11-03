package dev.sort.datafusionspice.validation

import dev.brikk.house.sql.intellij.diagnostics.DatafusionNativeErrorProfile
import dev.brikk.house.sql.intellij.diagnostics.NativeErrorAnnotator
import dev.brikk.house.sql.intellij.diagnostics.NativeErrorPsiFilter
import dev.sort.datafusionspice.catalog.datafusionCatalog
import dev.sort.datafusionspice.catalog.spiceCatalog
import dev.sort.datafusionspice.pipes.datafusionPipe
import dev.sort.datafusionspice.pipes.spicePipe
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect

val datafusionErrors = DatafusionNativeErrorProfile(DatafusionSqlDialect, datafusionCatalog, datafusionPipe)
val spiceErrors = DatafusionNativeErrorProfile(SpiceSqlDialect, spiceCatalog, spicePipe)

class DatafusionErrorAnnotator : NativeErrorAnnotator(datafusionErrors)
class SpiceErrorAnnotator : NativeErrorAnnotator(spiceErrors)
class DatafusionErrorFilter : NativeErrorPsiFilter(datafusionErrors)
class SpiceErrorFilter : NativeErrorPsiFilter(spiceErrors)
