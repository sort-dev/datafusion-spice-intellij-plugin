package dev.sort.datafusionspice.pipes

import dev.brikk.house.sql.intellij.CooperativeExecuteConfiguration
import dev.brikk.house.sql.intellij.CooperativeExecutePromoter
import dev.brikk.house.sql.intellij.ExecuteInterceptor
import dev.brikk.house.sql.intellij.ExecutionClaim
import dev.brikk.house.sql.intellij.pipe.*

class DatafusionSpiceExecuteConfiguration : CooperativeExecuteConfiguration() {
    private val datafusion = PipeExecuteInterceptor(datafusionPipe)
    private val spice = PipeExecuteInterceptor(spicePipe)
    // Each interceptor declines foreign languages/sessions before preparing any input.
    override val interceptor = ExecuteInterceptor { event, option ->
        if (datafusion.execute(event, option) == ExecutionClaim.HANDLED) ExecutionClaim.HANDLED
        else spice.execute(event, option)
    }
}

class DatafusionSpiceExecutePromoter : CooperativeExecutePromoter()
class DatafusionPipeStageCompletion : PipeStageCompletionContributor(datafusionPipe)
class SpicePipeStageCompletion : PipeStageCompletionContributor(spicePipe)
class DatafusionPipeStageAnnotator : PipeStageKeywordAnnotator(datafusionPipe)
class SpicePipeStageAnnotator : PipeStageKeywordAnnotator(spicePipe)
class DatafusionPipePreview : PipePreviewAction(datafusionPipe)
class SpicePipePreview : PipePreviewAction(spicePipe)
class DatafusionPipeRunToStage : PipeRunToStageAction(datafusionPipe)
class SpicePipeRunToStage : PipeRunToStageAction(spicePipe)
class DatafusionPipePreviewIntention : PipePreviewIntention(datafusionPipe)
class SpicePipePreviewIntention : PipePreviewIntention(spicePipe)
class DatafusionPipeRunIntention : PipeRunToStageIntention(datafusionPipe)
class SpicePipeRunIntention : PipeRunToStageIntention(spicePipe)
