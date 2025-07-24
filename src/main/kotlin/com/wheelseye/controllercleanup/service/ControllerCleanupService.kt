package com.wheelseye.controllercleanup.service

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.JavaPsiFacade

class ControllerCleanupService(private val project: Project) {

    fun performCleanup() {
        showMessage("Starting controller cleanup analysis...")

        val controllers = findSpringControllers()
        showMessage("Found ${controllers.size} Spring controllers")

        val deprecatedMethods = findDeprecatedMethods(controllers)
        showMessage("Found ${deprecatedMethods.size} deprecated methods")

        val unusedMethods = findUnusedMethods(deprecatedMethods)
        showMessage("Found ${unusedMethods.size} unused deprecated methods")

        if (unusedMethods.isEmpty()) {
            showMessage("No unused deprecated methods found in Spring controllers.")
            return
        }

        showSummary(unusedMethods)
        askForConfirmation(unusedMethods)
    }



    private fun askForConfirmation(unusedMethods: List<PsiMethod>) {
        val message = buildString {
            appendLine("Found ${unusedMethods.size} unused deprecated methods:")
            appendLine()
            unusedMethods.take(10).forEach { method ->
                val className = method.containingClass?.name ?: "Unknown"
                appendLine("• $className.${method.name}()")
            }
            if (unusedMethods.size > 10) {
                appendLine("... and ${unusedMethods.size - 10} more")
            }
            appendLine()
            append("Do you want to remove these methods?")
        }

        val result = Messages.showYesNoDialog(
            project,
            message,
            "Controller Cleanup",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            removeUnusedMethods(unusedMethods)
        } else {
            showMessage("Operation cancelled by user.")
        }
    }

    private fun findSpringControllers(): List<PsiClass> {
        val controllerClasses = mutableListOf<PsiClass>()
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)

        val controllerAnnotation = javaPsiFacade.findClass("org.springframework.stereotype.Controller", scope)
        val restControllerAnnotation = javaPsiFacade.findClass("org.springframework.web.bind.annotation.RestController", scope)

        if (controllerAnnotation != null) {
            val classes = AnnotatedElementsSearch.searchPsiClasses(controllerAnnotation, scope).findAll()
            controllerClasses.addAll(classes)
            showMessage("Found ${classes.size} classes with @Controller annotation")
        }

        if (restControllerAnnotation != null) {
            val classes = AnnotatedElementsSearch.searchPsiClasses(restControllerAnnotation, scope).findAll()
            controllerClasses.addAll(classes)
            showMessage("Found ${classes.size} classes with @RestController annotation")
        }

        if (controllerClasses.isEmpty()) {
            showMessage("No annotated controllers found, searching for classes with 'Controller' in name...")
            val fileIndex = ProjectFileIndex.getInstance(project)
            fileIndex.iterateContent { file: VirtualFile ->
                if (!file.isDirectory && file.extension == "java") {
                    val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile
                    psiFile?.classes?.forEach { psiClass ->
                        if (psiClass.name?.contains("Controller") == true) {
                            controllerClasses.add(psiClass)
                        }
                    }
                }
                true
            }
            showMessage("Found ${controllerClasses.size} classes with 'Controller' in name")
        }

        return controllerClasses
    }

    private fun findDeprecatedMethods(controllers: List<PsiClass>): List<PsiMethod> {
        val deprecatedMethods = mutableListOf<PsiMethod>()

        controllers.forEach { controller ->
            showMessage("Analyzing controller: ${controller.name}")
            controller.methods.forEach { method ->
                if (isDeprecated(method)) {
                    deprecatedMethods.add(method)
                    showMessage("Found deprecated method: ${controller.name}.${method.name}()")
                }
            }
        }

        return deprecatedMethods
    }

    private fun isDeprecated(method: PsiMethod): Boolean {
        if (method.modifierList?.hasAnnotation("java.lang.Deprecated") == true) return true
        method.docComment?.findTagByName("deprecated")?.let { return true }
        if (method.name.lowercase().contains("deprecated")) return true
        return false
    }

    private fun findUnusedMethods(deprecatedMethods: List<PsiMethod>): List<PsiMethod> {
        val unusedMethods = mutableListOf<PsiMethod>()

        deprecatedMethods.forEach { method ->
            showMessage("Checking if method is used: ${method.containingClass?.name}.${method.name}()")
            if (!isMethodUsed(method)) {
                unusedMethods.add(method)
                showMessage("Method is unused: ${method.containingClass?.name}.${method.name}()")
            } else {
                showMessage("Method is used: ${method.containingClass?.name}.${method.name}()")
            }
        }

        return unusedMethods
    }

    private fun isMethodUsed(method: PsiMethod): Boolean {
        val scope = GlobalSearchScope.projectScope(project)
        val references = ReferencesSearch.search(method, scope).findAll()

        val externalReferences = references.filter { ref ->
            val element = ref.element
            val containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
            containingMethod != method
        }

        if (externalReferences.isNotEmpty()) {
            showMessage("Still used: ${method.containingClass?.name}.${method.name}()")
            externalReferences.forEach { ref ->
                showMessage("  - Used at: ${ref.element.containingFile?.name}:${ref.element.text}")
            }
        }

        return externalReferences.isNotEmpty()
    }

    private fun showSummary(unusedMethods: List<PsiMethod>) {
        val message = buildString {
            appendLine("Found ${unusedMethods.size} unused deprecated methods:")
            appendLine()
            unusedMethods.forEach { method ->
                val className = method.containingClass?.name ?: "Unknown"
                appendLine("• $className.${method.name}()")
            }
            appendLine()
            append("Do you want to remove these methods?")
        }

        showMessage(message)
    }

    private fun removeUnusedMethods(unusedMethods: List<PsiMethod>) {
        showMessage("Starting removal of ${unusedMethods.size} unused deprecated methods...")

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                val removedMethods = mutableListOf<PsiMethod>()
                val methodsToCleanup = collectAllCalledMethods(unusedMethods)
                val affectedFiles = mutableSetOf<PsiFile>()

                var removedCount = 0
                unusedMethods.forEach { method ->
                    try {
                        // Track the file before removing the method
                        method.containingFile?.let { affectedFiles.add(it) }
                        method.delete()
                        removedMethods.add(method)
                        removedCount++
                        showMessage("Removed: ${method.containingClass?.name}.${method.name}()")
                    } catch (e: Exception) {
                        showMessage("Failed to remove ${method.containingClass?.name}.${method.name}(): ${e.message}")
                    }
                }

                // Track files affected by transitive method removal
                val transitivelyAffectedFiles = removeTransitivelyUnusedMethods(methodsToCleanup)
                affectedFiles.addAll(transitivelyAffectedFiles)

                // Clean up unused imports and fields only in affected files
                if (affectedFiles.isNotEmpty()) {
                    showMessage("Cleaning up unused imports and fields in ${affectedFiles.size} affected files...")
                    val codeCleanupService = com.wheelseye.codecleanup.service.CodeCleanupService(project)
                    val cleanupStats = codeCleanupService.cleanupAffectedFiles(affectedFiles)
                    
                    val finalMessage = buildString {
                        append("Successfully removed $removedCount out of ${unusedMethods.size} unused deprecated methods.")
                        if (cleanupStats.first > 0 || cleanupStats.second > 0) {
                            append(" Also removed ${cleanupStats.first} unused fields and ${cleanupStats.second} unused imports from affected files.")
                        }
                    }
                    showMessage(finalMessage)

                    Messages.showInfoMessage(
                        project,
                        finalMessage,
                        "Controller Cleanup Complete"
                    )
                } else {
                    val finalMessage = "Successfully removed $removedCount out of ${unusedMethods.size} unused deprecated methods."
                    showMessage(finalMessage)

                    Messages.showInfoMessage(
                        project,
                        finalMessage,
                        "Controller Cleanup Complete"
                    )
                }
            }
        }
    }

    private fun removeTransitivelyUnusedMethods(initialMethods: List<PsiMethod>): Set<PsiFile> {
        showMessage("Starting transitive cleanup...")

        val toCheck = ArrayDeque(initialMethods)
        val removed = mutableSetOf<PsiMethod>()
        val affectedFiles = mutableSetOf<PsiFile>()

        while (toCheck.isNotEmpty()) {
            val method = toCheck.removeFirst()
            if (removed.contains(method)) continue

            if (!isMethodUsed(method)) {
                if (method.containingClass?.isInterface == true) continue
                if (method.findSuperMethods().isNotEmpty()) continue

                try {
                    // Track the file before removing the method
                    method.containingFile?.let { affectedFiles.add(it) }
                    
                    val className = method.containingClass?.name ?: "Unknown"
                    val more = collectMethodCalls(method)
                    method.delete()
                    showMessage("Transitively removed: $className.${method.name}()")
                    removed.add(method)
                    toCheck.addAll(more)
                } catch (e: Exception) {
                    showMessage("Failed to remove ${method.containingClass?.name}.${method.name}(): ${e.message}")
                }
            }
        }

        showMessage("Transitive cleanup complete. Removed ${removed.size} additional methods.")
        return affectedFiles
    }

    private fun collectAllCalledMethods(methods: List<PsiMethod>): List<PsiMethod> {
        return methods.flatMap { collectMethodCalls(it) }.distinct()
    }

    private fun collectMethodCalls(method: PsiMethod): List<PsiMethod> {
        val calledMethods = mutableSetOf<PsiMethod>()
        method.body?.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                expression.resolveMethod()?.let {
                    calledMethods.add(it)
                }
            }
        })
        return calledMethods.toList()
    }

    private fun showMessage(message: String) {
        println("Controller Cleanup: $message")
    }
}
