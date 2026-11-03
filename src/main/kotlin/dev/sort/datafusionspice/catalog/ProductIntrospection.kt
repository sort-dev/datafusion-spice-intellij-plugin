package dev.sort.datafusionspice.catalog

import com.intellij.database.model.ObjectKind
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sql.psi.SqlReferenceExpression
import dev.brikk.house.sql.intellij.catalog.*
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect
import kotlinx.coroutines.CoroutineScope

// Same CatalogProfile/AutoIntrospectService split as Trino and DuckDB. Share the
// native profile's exact resolver and namespace rules, not another product alias.
val datafusionIntrospection = object : CatalogProfile by datafusionCatalog {
    override val autoIntrospect = true
    override val outOfScopeReferences = true
}
val spiceIntrospection = object : CatalogProfile by spiceCatalog {
    override val autoIntrospect = true
    override val outOfScopeReferences = true
}

@Service(Service.Level.PROJECT)
class DatafusionAutoIntrospectService(project: Project, scope: CoroutineScope) : AutoIntrospectService(project, scope) {
    override val profile = datafusionIntrospection
}

@Service(Service.Level.PROJECT)
class SpiceAutoIntrospectService(project: Project, scope: CoroutineScope) : AutoIntrospectService(project, scope) {
    override val profile = spiceIntrospection
}

class DatafusionQualifiedPathIntrospection : QualifiedPathIntrospectContributor(DatafusionAutoIntrospectService::class.java)
class SpiceQualifiedPathIntrospection : QualifiedPathIntrospectContributor(SpiceAutoIntrospectService::class.java)
class DatafusionOutOfScopeFilter : OutOfScopeReferenceFilter(DatafusionAutoIntrospectService::class.java)
class SpiceOutOfScopeFilter : OutOfScopeReferenceFilter(SpiceAutoIntrospectService::class.java)

/** DuckDB's reference-trigger pattern: works on file load as well as completion. */
abstract class ProductReferenceIntrospection : Annotator {
    protected abstract fun requests(file: PsiFile): AutoIntrospectService?

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element as? PsiFile ?: return
        if (DumbService.isDumb(file.project)) return
        val requests = requests(file) ?: return
        val classifier = OutOfScopeClassifier(requests)
        for (reference in PsiTreeUtil.findChildrenOfType(file, SqlReferenceExpression::class.java)) {
            if (reference.referenceElementType.targetKind != ObjectKind.TABLE) continue
            val classified = classifier.classify(reference)
            if (classified is ReferenceScope.OutOfScope) {
                requests.requestPath(file, classified.namespace.path)
                continue
            }
            val path = generateSequence(reference) { it.qualifierExpression as? SqlReferenceExpression }
                .mapNotNull { it.name }.toList().asReversed()
            if (path.size > 1) requests.requestPath(file, path.dropLast(1))
            else requests.profile.dataSources.consoleNamespace(file).takeIf { it.isNotEmpty() }
                ?.let { requests.requestPath(file, it) }
        }
    }
}

class DatafusionReferenceIntrospection : ProductReferenceIntrospection() {
    override fun requests(file: PsiFile) = if (file.language === DatafusionSqlDialect)
        file.project.getService(DatafusionAutoIntrospectService::class.java) else null
}
class SpiceReferenceIntrospection : ProductReferenceIntrospection() {
    override fun requests(file: PsiFile) = if (file.language === SpiceSqlDialect)
        file.project.getService(SpiceAutoIntrospectService::class.java) else null
}
