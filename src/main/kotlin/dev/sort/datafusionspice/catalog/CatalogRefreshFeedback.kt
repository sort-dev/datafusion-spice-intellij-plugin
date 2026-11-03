package dev.sort.datafusionspice.catalog

import com.intellij.database.dataSource.LocalDataSource
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import dev.brikk.house.sql.intellij.catalog.LiveCatalogProfile
import dev.brikk.house.sql.intellij.catalog.RefreshOutcome
import dev.brikk.house.sql.intellij.catalog.RefreshReason
import dev.brikk.house.sql.intellij.notifyEscaped

/** The shared service hook and notification helper used by peers; no permanent editor UI. */
internal fun catalogRefreshCompleted(
    project: Project, source: LocalDataSource, profile: LiveCatalogProfile,
    outcome: RefreshOutcome, reason: RefreshReason,
) {
    if (project.isDisposed) return
    if (outcome is RefreshOutcome.Refreshed) restartProductHighlighting(project)
    // Profiles with a channel are already handled by super.onRefreshed. Background
    // execution/connect events stay quiet even while our current profiles lack one.
    if (profile.notificationGroupId != null || reason != RefreshReason.Manual || outcome == RefreshOutcome.Disabled) return
    val (type, message) = when (outcome) {
        is RefreshOutcome.Refreshed -> NotificationType.INFORMATION to buildString {
            val count = outcome.inventory.functions.size
            append("${source.name}: refreshed $count ${if (count == 1) "function" else "functions"}")
            outcome.inventory.nativeCatalog?.let {
                append(" and ${it.relations.size} ${if (it.relations.size == 1) "relation" else "relations"}")
            }
            append('.')
        }
        is RefreshOutcome.ConnectFailed -> NotificationType.WARNING to
            "Could not open the connection for catalog refresh. Check the data source settings."
        is RefreshOutcome.HarvestFailed -> NotificationType.WARNING to
            "Catalog refresh did not complete. Existing metadata was left unchanged."
        RefreshOutcome.Superseded -> NotificationType.INFORMATION to
            "Connection settings changed during catalog refresh. Refresh again using the new settings."
        RefreshOutcome.SessionInTransaction -> NotificationType.INFORMATION to
            "Catalog refresh was skipped to avoid interfering with the console transaction."
        RefreshOutcome.Disabled -> return
    }
    // Do not display raw driver exceptions: they can contain URLs or credentials.
    notifyEscaped(project, profile.productLabel, "${profile.productLabel} catalog", message, type)
}
