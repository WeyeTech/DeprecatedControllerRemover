package com.wheelseye.deprecatedcontrollerremover.service

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
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.PsiElementFactory
import com.wheelseye.deprecatedcontrollerremover.toolWindow.DeprecatedControllerRemoverToolWindowManager

class DeprecatedControllerRemoverService(private val project: Project) {

    data class CleanupAnalysis(
        val unusedDeprecatedMethods: List<SmartPsiElementPointer<PsiMethod>>,
        val transitivelyUnusedMethods: List<SmartPsiElementPointer<PsiMethod>>,
        val totalMethodsToRemove: Int
    )

    fun performCleanup() {
        // Show tool window and clear previous messages
        val toolWindowService = DeprecatedControllerRemoverToolWindowManager.getInstance(project)
        toolWindowService.showToolWindow()
        toolWindowService.clearMessages()
        toolWindowService.appendMessage("Starting Deprecated Controller Remover...")
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Deprecated Controller Remover", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Starting deprecated controller remover analysis..."
                toolWindowService.appendMessage("Starting deprecated controller remover analysis...")
                
                // Phase 1: Read-only analysis
                val analysis = runReadAction {
                    performReadOnlyAnalysis(indicator)
                }

                if (analysis.unusedDeprecatedMethods.isEmpty() && analysis.transitivelyUnusedMethods.isEmpty()) {
                    showMessage("No unused deprecated methods found in Spring controllers.")
                    return
                }

                // Show confirmation dialog on EDT
                ApplicationManager.getApplication().invokeLater {
                    showSummary(analysis)
                    askForConfirmation(analysis)
                }
            }
        })
    }

    private fun performReadOnlyAnalysis(indicator: ProgressIndicator): CleanupAnalysis {
        indicator.text = "Finding Spring controllers..."
        val controllers = findSpringControllers()
        showMessage("Found ${controllers.size} Spring controllers")

        indicator.text = "Finding deprecated methods..."
        val deprecatedMethods = findDeprecatedMethods(controllers)
        showMessage("Found ${deprecatedMethods.size} deprecated methods")

        indicator.text = "Finding unused deprecated methods..."
        val unusedDeprecatedMethods = findUnusedMethods(deprecatedMethods)
        showMessage("Found ${unusedDeprecatedMethods.size} unused deprecated methods")

        indicator.text = "Finding transitively unused methods..."
        val transitivelyUnusedMethods = findTransitivelyUnusedMethods(unusedDeprecatedMethods)
        showMessage("Found ${transitivelyUnusedMethods.size} transitively unused methods")

        val totalMethodsToRemove = unusedDeprecatedMethods.size + transitivelyUnusedMethods.size

        return CleanupAnalysis(
            unusedDeprecatedMethods = unusedDeprecatedMethods.map { SmartPointerManager.createPointer(it) },
            transitivelyUnusedMethods = transitivelyUnusedMethods.map { SmartPointerManager.createPointer(it) },
            totalMethodsToRemove = totalMethodsToRemove
        )
    }

    private fun askForConfirmation(analysis: CleanupAnalysis) {
        val message = buildString {
            appendLine("Found ${analysis.totalMethodsToRemove} methods to remove:")
            appendLine("• ${analysis.unusedDeprecatedMethods.size} unused deprecated methods")
            appendLine("• ${analysis.transitivelyUnusedMethods.size} transitively unused methods")
            appendLine()
            
            val allMethods = analysis.unusedDeprecatedMethods + analysis.transitivelyUnusedMethods
            allMethods.take(10).forEach { methodPointer ->
                val method = methodPointer.element
                if (method != null) {
                    val className = method.containingClass?.name ?: "Unknown"
                    appendLine("• $className.${method.name}()")
                }
            }
            if (allMethods.size > 10) {
                appendLine("... and ${allMethods.size - 10} more")
            }
            appendLine()
            append("Do you want to remove these methods?")
        }

        val result = Messages.showYesNoDialog(
            project,
            message,
            "Deprecated Controller Remover",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            // Run write operations on background thread
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Removing Methods", false) {
                override fun run(indicator: ProgressIndicator) {
                    performWriteOperations(analysis, indicator)
                }
            })
        } else {
            showMessage("Operation cancelled by user.")
        }
    }

    internal fun findSpringControllers(): List<PsiClass> {
        val controllerClasses = mutableListOf<PsiClass>()
        val fileIndex = ProjectFileIndex.getInstance(project)
        
        fileIndex.iterateContent { file: VirtualFile ->
            if (!file.isDirectory && file.extension == "java") {
                val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile
                psiFile?.classes?.forEach { psiClass ->
                    // Check for @Controller annotation
                    if (psiClass.modifierList?.hasAnnotation("org.springframework.stereotype.Controller") == true ||
                        psiClass.modifierList?.hasAnnotation("Controller") == true) {
                        controllerClasses.add(psiClass)
                        showMessage("Found @Controller class: ${psiClass.name}")
                    }
                    // Check for @RestController annotation
                    else if (psiClass.modifierList?.hasAnnotation("org.springframework.web.bind.annotation.RestController") == true ||
                             psiClass.modifierList?.hasAnnotation("RestController") == true) {
                        controllerClasses.add(psiClass)
                        showMessage("Found @RestController class: ${psiClass.name}")
                    }
                }
            }
            true
        }

        if (controllerClasses.isEmpty()) {
            showMessage("No annotated controllers found, searching for classes with 'Controller' in name...")
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

    internal fun findDeprecatedMethods(controllers: List<PsiClass>): List<PsiMethod> {
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

    internal fun findUnusedMethods(deprecatedMethods: List<PsiMethod>): List<PsiMethod> {
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

    private fun findTransitivelyUnusedMethods(initialMethods: List<PsiMethod>): List<PsiMethod> {
        showMessage("Starting transitive analysis...")

        val toCheck = ArrayDeque(initialMethods)
        val transitivelyUnused = mutableSetOf<PsiMethod>()
        val usageCountMap = mutableMapOf<PsiMethod, Int>()
        var i = toCheck.size
        showMessage("toCheck.size: ${i}")

        while (toCheck.isNotEmpty()) {
            val method = toCheck.removeFirst()
            
            // Initialize usage count if not in map
            var usageCount = usageCountMap.getOrPut(method) { getMethodUsageCount(method) }
            usageCount--
            usageCountMap[method] = usageCount
            
            if (usageCount <= 0) {
                if (method.containingClass?.isInterface == true) continue
                if (method.findSuperMethods().isNotEmpty()) continue

                if (i-- <= 0) {
                    transitivelyUnused.add(method)
                    showMessage("Transitively unused: ${method.containingClass?.name}.${method.name}() (usage count: $usageCount)")
                }
                
                
                // Add called methods to check queue
                val calledMethods = collectMethodCalls(method)
                toCheck.addAll(calledMethods)
            }
        }

        showMessage("Transitive analysis complete. Found ${transitivelyUnused.size} transitively unused methods.")
        return transitivelyUnused.toList()
    }

    private fun getMethodUsageCount(method: PsiMethod): Int {
        val scope = GlobalSearchScope.projectScope(project)
        val references = ReferencesSearch.search(method, scope).findAll()

        val externalReferences = references.filter { ref ->
            val element = ref.element
            val containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
            containingMethod != method
        }

        return externalReferences.size
    }

    private fun showSummary(analysis: CleanupAnalysis) {
        val message = buildString {
            appendLine("Found ${analysis.totalMethodsToRemove} methods to remove:")
            appendLine("• ${analysis.unusedDeprecatedMethods.size} unused deprecated methods")
            appendLine("• ${analysis.transitivelyUnusedMethods.size} transitively unused methods")
            appendLine()
            
            val allMethods = analysis.unusedDeprecatedMethods + analysis.transitivelyUnusedMethods
            allMethods.take(10).forEach { methodPointer ->
                val method = methodPointer.element
                if (method != null) {
                    val className = method.containingClass?.name ?: "Unknown"
                    appendLine("• $className.${method.name}()")
                }
            }
            if (allMethods.size > 10) {
                appendLine("... and ${allMethods.size - 10} more")
            }
            appendLine()
            append("Do you want to remove these methods?")
        }

        showMessage(message)
    }

    private fun performWriteOperations(analysis: CleanupAnalysis, indicator: ProgressIndicator) {
        showMessage("Starting removal of ${analysis.totalMethodsToRemove} methods...")

        WriteCommandAction.runWriteCommandAction(project) {
            var removedCount = 0
            val allMethods = analysis.unusedDeprecatedMethods + analysis.transitivelyUnusedMethods
            val filesWithRemovedMethods = mutableSetOf<PsiFile>()

            allMethods.forEach { methodPointer ->
                val method = methodPointer.element
                if (method != null && method.isValid) {
                    // Safety check: only delete from source files
                    val fileExtension = method.containingFile?.virtualFile?.extension
                    if (fileExtension != "java") {
                        showMessage("Skipping ${method.containingClass?.name}.${method.name}() - not a source file (${fileExtension})")
                        return@forEach
                    }
                    
                    try {
                        val className = method.containingClass?.name ?: "Unknown"
                        // Track the file before removing the method
                        method.containingFile?.let { filesWithRemovedMethods.add(it) }
                        method.delete()
                        removedCount++
                        indicator.text = "Removed: $className.${method.name}()"
                        showMessage("Removed: $className.${method.name}()")
                    } catch (e: Exception) {
                        showMessage("Failed to remove ${method.containingClass?.name}.${method.name}(): ${e.message}")
                    }
                }
            }

            // Only mark files that actually had methods removed
            if (filesWithRemovedMethods.isNotEmpty()) {
                markFilesForCleanup(filesWithRemovedMethods)
            }

            val finalMessage = "Successfully removed $removedCount out of ${analysis.totalMethodsToRemove} methods and marked ${filesWithRemovedMethods.size} files for cleanup."
            showMessage(finalMessage)

            // Show completion message on EDT
            ApplicationManager.getApplication().invokeLater {
                            Messages.showInfoMessage(
                project,
                finalMessage,
                "Deprecated Controller Remover Complete"
            )
            }
        }
    }

    private fun markFilesForCleanup(affectedFiles: Set<PsiFile>) {
        affectedFiles.forEach { psiFile ->
            if (psiFile is PsiJavaFile) {
                try {
                    val firstElement = psiFile.firstChild
                    if (firstElement != null) {
                        // Check if the comment already exists
                        val existingComment = psiFile.findElementAt(0)
                        if (existingComment?.text?.contains("//Controller Cleaner") != true) {
                            // Add the comment at the very beginning
                            val elementFactory = PsiElementFactory.getInstance(project)
                            val comment = elementFactory.createCommentFromText("//Controller Cleaner", null)
                            psiFile.addBefore(comment, firstElement)
                            showMessage("Marked file for cleanup: ${psiFile.name}")
                        }
                    }
                } catch (e: Exception) {
                    showMessage("Failed to mark file ${psiFile.name} for cleanup: ${e.message}")
                }
            }
        }
    }

    internal fun collectAllCalledMethods(methods: List<PsiMethod>): List<PsiMethod> {
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
        println("Deprecated Controller Remover: $message")
        // Also send to tool window if available
        try {
            val toolWindowService = DeprecatedControllerRemoverToolWindowManager.getInstance(project)
            toolWindowService.appendMessage(message)
        } catch (e: Exception) {
            // Tool window might not be available, ignore
        }
    }
}
