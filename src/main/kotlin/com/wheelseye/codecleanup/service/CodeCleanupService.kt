package com.wheelseye.codecleanup.service

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.wheelseye.codecleanup.toolWindow.CodeCleanupToolWindowManager

class CodeCleanupService(private val project: Project) {

    data class CleanupAnalysis(
        val filesToClean: List<PsiJavaFile>,
        val unusedImports: Map<PsiJavaFile, List<PsiImportStatement>>,
        val unusedFields: Map<PsiJavaFile, List<PsiField>>,
        val unusedEmptyClasses: Map<PsiJavaFile, List<PsiClass>>,
        val unusedNonEmptyClasses: Map<PsiJavaFile, List<PsiClass>>,
        val totalImportsToRemove: Int,
        val totalFieldsToRemove: Int,
        val totalClassesToRemove: Int
    )

    fun performCleanup() {
        // Show tool window and clear previous messages
        val toolWindowService = CodeCleanupToolWindowManager.getInstance(project)
        toolWindowService.showToolWindow()
        toolWindowService.clearMessages()
        toolWindowService.appendMessage("Starting Code Cleanup...")
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Code Cleanup", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Starting code cleanup analysis..."
                toolWindowService.appendMessage("Starting code cleanup analysis...")
                
                // Phase 1: Read-only analysis
                val analysis = runReadAction {
                    performReadOnlyAnalysis(indicator)
                }

                if (analysis.filesToClean.isEmpty()) {
                    showMessage("No files marked with '//Controller Cleaner' found.")
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
        indicator.text = "Finding files marked with '//Controller Cleaner'..."
        val markedFiles = findFilesMarkedForCleanup()
        showMessage("Found ${markedFiles.size} files marked for cleanup")

        if (markedFiles.isEmpty()) {
            return CleanupAnalysis(emptyList(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), 0, 0, 0)
        }

        indicator.text = "Analyzing unused imports..."
        val unusedImports = findUnusedImports(markedFiles)
        val totalImportsToRemove = unusedImports.values.sumOf { it.size }
        showMessage("Found $totalImportsToRemove unused imports")

        indicator.text = "Analyzing unused fields..."
        val unusedFields = findUnusedFields(markedFiles)
        val totalFieldsToRemove = unusedFields.values.sumOf { it.size }
        showMessage("Found $totalFieldsToRemove unused fields")

        indicator.text = "Analyzing unused empty classes..."
        val unusedEmptyClasses = findUnusedEmptyClasses(markedFiles)
        val totalEmptyClassesToRemove = unusedEmptyClasses.values.sumOf { it.size }
        showMessage("Found $totalEmptyClassesToRemove unused empty classes")

        indicator.text = "Analyzing unused non-empty classes..."
        val unusedNonEmptyClasses = findUnusedNonEmptyClasses(markedFiles)
        val totalNonEmptyClassesToRemove = unusedNonEmptyClasses.values.sumOf { it.size }
        showMessage("Found $totalNonEmptyClassesToRemove unused non-empty classes")

        val totalClassesToRemove = totalEmptyClassesToRemove + totalNonEmptyClassesToRemove

        return CleanupAnalysis(
            filesToClean = markedFiles,
            unusedImports = unusedImports,
            unusedFields = unusedFields,
            unusedEmptyClasses = unusedEmptyClasses,
            unusedNonEmptyClasses = unusedNonEmptyClasses,
            totalImportsToRemove = totalImportsToRemove,
            totalFieldsToRemove = totalFieldsToRemove,
            totalClassesToRemove = totalClassesToRemove
        )
    }

    private fun findFilesMarkedForCleanup(): List<PsiJavaFile> {
        val markedFiles = mutableListOf<PsiJavaFile>()
        val fileIndex = ProjectFileIndex.getInstance(project)
        
        fileIndex.iterateContent { file: VirtualFile ->
            if (!file.isDirectory && file.extension == "java") {
                val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile
                if (psiFile != null && isFileMarkedForCleanup(psiFile)) {
                    markedFiles.add(psiFile)
                }
            }
            true
        }
        
        return markedFiles
    }

    private fun isFileMarkedForCleanup(file: PsiJavaFile): Boolean {
        val firstElement = file.findElementAt(0)
        return firstElement?.text?.contains("//Controller Cleaner") == true
    }

    private fun findUnusedImports(files: List<PsiJavaFile>): Map<PsiJavaFile, List<PsiImportStatement>> {
        val unusedImports = mutableMapOf<PsiJavaFile, List<PsiImportStatement>>()
        
        files.forEach { file ->
            val fileUnusedImports = mutableListOf<PsiImportStatement>()
            
            file.importList?.importStatements?.forEach { importStatement ->
                if (isUnusedImport(importStatement, file)) {
                    fileUnusedImports.add(importStatement)
                }
            }
            
            if (fileUnusedImports.isNotEmpty()) {
                unusedImports[file] = fileUnusedImports
            }
        }
        
        return unusedImports
    }

    private fun isUnusedImport(importStatement: PsiImportStatement, file: PsiJavaFile): Boolean {
        try {
            if (!importStatement.isValid) {
                return false
            }
            
            val qualifiedName = importStatement.qualifiedName ?: return false
            
            // Skip wildcard imports
            if (importStatement.isOnDemand) {
                return false
            }
            
            // Skip java.lang imports as they are automatically available
            if (qualifiedName.startsWith("java.lang.")) {
                return true
            }
            
            // Extract the simple class name from the qualified name
            val simpleClassName = qualifiedName.substringAfterLast('.')
            
            return !isImportUsedInFile(file, simpleClassName, importStatement)
        } catch (e: Exception) {
            showMessage("Error checking import statement: ${e.message}")
            return false
        }
    }

    private fun isImportUsedInFile(file: PsiJavaFile, simpleClassName: String, importStatement: PsiImportStatement): Boolean {
        val visitor = object : JavaRecursiveElementVisitor() {
            var foundUsage = false
            
            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                super.visitReferenceElement(reference)
                if (reference.referenceName == simpleClassName) {
                    if (!PsiTreeUtil.isAncestor(importStatement, reference, false)) {
                        foundUsage = true
                    }
                }
            }
            
            override fun visitTypeElement(type: PsiTypeElement) {
                super.visitTypeElement(type)
                val typeName = type.type?.presentableText
                if (typeName == simpleClassName) {
                    foundUsage = true
                }
            }
            
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val methodExpression = expression.methodExpression
                if (methodExpression.referenceName == simpleClassName) {
                    foundUsage = true
                }
            }
            
            override fun visitNewExpression(expression: PsiNewExpression) {
                super.visitNewExpression(expression)
                val classReference = expression.classReference
                if (classReference?.referenceName == simpleClassName) {
                    foundUsage = true
                }
            }
            
            override fun visitClassObjectAccessExpression(expression: PsiClassObjectAccessExpression) {
                super.visitClassObjectAccessExpression(expression)
                val operand = expression.operand
                val typeName = operand.type?.presentableText
                if (typeName == simpleClassName) {
                    foundUsage = true
                }
            }
        }
        
        file.accept(visitor)
        return visitor.foundUsage
    }

    private fun findUnusedFields(files: List<PsiJavaFile>): Map<PsiJavaFile, List<PsiField>> {
        val unusedFields = mutableMapOf<PsiJavaFile, List<PsiField>>()
        
        files.forEach { file ->
            val fileUnusedFields = mutableListOf<PsiField>()
            
            file.classes.forEach { psiClass ->
                psiClass.fields.forEach { field ->
                    if (isUnusedField(field)) {
                        fileUnusedFields.add(field)
                    }
                }
            }
            
            if (fileUnusedFields.isNotEmpty()) {
                unusedFields[file] = fileUnusedFields
            }
        }
        
        return unusedFields
    }

    private fun isUnusedField(field: PsiField): Boolean {
        try {
            if (!field.isValid) {
                return false
            }
            
            // Skip public fields as they might be used externally
            if (field.hasModifierProperty(PsiModifier.PUBLIC)) {
                return false
            }
            
            // Skip static fields as they might be used by reflection or external frameworks
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                return false
            }
            
            val scope = GlobalSearchScope.projectScope(project)
            val references = ReferencesSearch.search(field, scope).findAll()
            
            // Filter out references that are not the field declaration itself
            val externalReferences = references.filter { ref ->
                val element = ref.element
                element != field.nameIdentifier
            }
            
            return externalReferences.isEmpty()
        } catch (e: Exception) {
            showMessage("Error checking field ${field.name}: ${e.message}")
            return false
        }
    }

    private fun findUnusedEmptyClasses(files: List<PsiJavaFile>): Map<PsiJavaFile, List<PsiClass>> {
        val unusedEmptyClasses = mutableMapOf<PsiJavaFile, List<PsiClass>>()
        
        files.forEach { file ->
            val fileUnusedEmptyClasses = mutableListOf<PsiClass>()
            
            file.classes.forEach { psiClass ->
                if (isClassUnusedAndEmpty(psiClass)) {
                    fileUnusedEmptyClasses.add(psiClass)
                }
            }
            
            if (fileUnusedEmptyClasses.isNotEmpty()) {
                unusedEmptyClasses[file] = fileUnusedEmptyClasses
            }
        }
        
        return unusedEmptyClasses
    }

    private fun findUnusedNonEmptyClasses(files: List<PsiJavaFile>): Map<PsiJavaFile, List<PsiClass>> {
        val unusedNonEmptyClasses = mutableMapOf<PsiJavaFile, List<PsiClass>>()
        
        files.forEach { file ->
            val fileUnusedNonEmptyClasses = mutableListOf<PsiClass>()
            
            file.classes.forEach { psiClass ->
                if (isClassUnused(psiClass)) {
                    fileUnusedNonEmptyClasses.add(psiClass)
                }
            }
            
            if (fileUnusedNonEmptyClasses.isNotEmpty()) {
                unusedNonEmptyClasses[file] = fileUnusedNonEmptyClasses
            }
        }
        
        return unusedNonEmptyClasses
    }

    private fun isClassUnusedAndEmpty(psiClass: PsiClass): Boolean {
        try {
            if (!psiClass.isValid) {
                return false
            }
            
            // Check if the class has any methods
            if (psiClass.methods.size > 0) {
                return false
            }
            
            // Check if the class has any nested classes
            if (psiClass.innerClasses.size > 0) {
                return false
            }
            
            // Check if the class is unused
            val scope = GlobalSearchScope.projectScope(project)
            val references = ReferencesSearch.search(psiClass, scope).findAll()
            
            // Filter out references that are within the same class (self-references)
            val externalReferences = references.filter { ref ->
                val element = ref.element
                val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                containingClass != psiClass
            }
            
            return externalReferences.isEmpty()
        } catch (e: Exception) {
            showMessage("Error checking class ${psiClass.name}: ${e.message}")
            return false
        }
    }

    private fun isClassUnused(psiClass: PsiClass): Boolean {
        try {
            if (!psiClass.isValid) {
                return false
            }
            
            // Skip REST controller classes (they should be preserved even if unused)
            if (isRestControllerClass(psiClass)) {
                return false
            }
            
            // Check if the class is unused
            val scope = GlobalSearchScope.projectScope(project)
            val references = ReferencesSearch.search(psiClass, scope).findAll()
            
            // Filter out references that are within the same class (self-references)
            val externalReferences = references.filter { ref ->
                val element = ref.element
                val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                containingClass != psiClass
            }
            
            return externalReferences.isEmpty()
        } catch (e: Exception) {
            showMessage("Error checking class ${psiClass.name}: ${e.message}")
            return false
        }
    }

    private fun isRestControllerClass(psiClass: PsiClass): Boolean {
        // Check for @Controller annotation
        if (psiClass.modifierList?.hasAnnotation("org.springframework.stereotype.Controller") == true ||
            psiClass.modifierList?.hasAnnotation("Controller") == true) {
            return true
        }
        // Check for @RestController annotation
        if (psiClass.modifierList?.hasAnnotation("org.springframework.web.bind.annotation.RestController") == true ||
            psiClass.modifierList?.hasAnnotation("RestController") == true) {
            return true
        }
        // Check if class name contains "Controller"
        if (psiClass.name?.contains("Controller") == true) {
            return true
        }
        return false
    }

    private fun showSummary(analysis: CleanupAnalysis) {
        val message = buildString {
            appendLine("Found ${analysis.filesToClean.size} files marked for cleanup:")
            appendLine("• ${analysis.totalImportsToRemove} unused imports")
            appendLine("• ${analysis.totalFieldsToRemove} unused fields")
            appendLine("• ${analysis.unusedEmptyClasses.values.sumOf { it.size }} unused empty classes")
            appendLine("• ${analysis.unusedNonEmptyClasses.values.sumOf { it.size }} unused non-empty classes")
            appendLine()
            
            analysis.filesToClean.take(10).forEach { file ->
                val importsCount = analysis.unusedImports[file]?.size ?: 0
                val fieldsCount = analysis.unusedFields[file]?.size ?: 0
                val emptyClassesCount = analysis.unusedEmptyClasses[file]?.size ?: 0
                val nonEmptyClassesCount = analysis.unusedNonEmptyClasses[file]?.size ?: 0
                appendLine("• ${file.name}: $importsCount imports, $fieldsCount fields, $emptyClassesCount empty classes, $nonEmptyClassesCount non-empty classes")
            }
            if (analysis.filesToClean.size > 10) {
                appendLine("... and ${analysis.filesToClean.size - 10} more files")
            }
            appendLine()
            append("Do you want to remove these unused elements?")
        }

        showMessage(message)
    }

    private fun askForConfirmation(analysis: CleanupAnalysis) {
        val message = buildString {
            appendLine("Found ${analysis.filesToClean.size} files marked for cleanup:")
            appendLine("• ${analysis.totalImportsToRemove} unused imports")
            appendLine("• ${analysis.totalFieldsToRemove} unused fields")
            appendLine("• ${analysis.unusedEmptyClasses.values.sumOf { it.size }} unused empty classes")
            appendLine("• ${analysis.unusedNonEmptyClasses.values.sumOf { it.size }} unused non-empty classes")
            appendLine()
            
            analysis.filesToClean.take(10).forEach { file ->
                val importsCount = analysis.unusedImports[file]?.size ?: 0
                val fieldsCount = analysis.unusedFields[file]?.size ?: 0
                val emptyClassesCount = analysis.unusedEmptyClasses[file]?.size ?: 0
                val nonEmptyClassesCount = analysis.unusedNonEmptyClasses[file]?.size ?: 0
                appendLine("• ${file.name}: $importsCount imports, $fieldsCount fields, $emptyClassesCount empty classes, $nonEmptyClassesCount non-empty classes")
            }
            if (analysis.filesToClean.size > 10) {
                appendLine("... and ${analysis.filesToClean.size - 10} more files")
            }
            appendLine()
            append("Do you want to remove these unused elements?")
        }

        val result = Messages.showYesNoDialog(
            project,
            message,
            "Code Cleanup",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            // Run write operations on background thread
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Removing Unused Elements", false) {
                override fun run(indicator: ProgressIndicator) {
                    performWriteOperations(analysis, indicator)
                }
            })
        } else {
            showMessage("Operation cancelled by user.")
        }
    }

    private fun performWriteOperations(analysis: CleanupAnalysis, indicator: ProgressIndicator) {
        showMessage("Starting removal of unused elements...")

        WriteCommandAction.runWriteCommandAction(project) {
            var totalRemovedImportsCount = 0
            var totalRemovedFieldsCount = 0
            var totalRemovedClassesCount = 0
            var passNumber = 1
            val maxPasses = 3

            // Run multiple passes to catch elements that become unused after other elements are removed
            while (passNumber <= maxPasses) {
                showMessage("Starting cleanup pass $passNumber of $maxPasses...")
                
                var passRemovedImportsCount = 0
                var passRemovedFieldsCount = 0
                var passRemovedClassesCount = 0
                var hasChanges = false

                // Re-analyze files for each pass
                val currentAnalysis = runReadAction {
                    performReadOnlyAnalysis(indicator)
                }

                // Remove unused imports
                currentAnalysis.unusedImports.forEach { (file, imports) ->
                    imports.forEach { importStatement ->
                        try {
                            val qualifiedName = importStatement.qualifiedName ?: "unknown"
                            importStatement.delete()
                            passRemovedImportsCount++
                            indicator.text = "Pass $passNumber - Removed import: $qualifiedName from ${file.name}"
                            showMessage("Pass $passNumber - Removed unused import: $qualifiedName from ${file.name}")
                            hasChanges = true
                        } catch (e: Exception) {
                            val qualifiedName = try {
                                importStatement.qualifiedName ?: "unknown"
                            } catch (ex: Exception) {
                                "unknown"
                            }
                            showMessage("Failed to remove import $qualifiedName from ${file.name}: ${e.message}")
                        }
                    }
                }

                // Remove unused fields
                currentAnalysis.unusedFields.forEach { (file, fields) ->
                    fields.forEach { field ->
                        try {
                            field.delete()
                            passRemovedFieldsCount++
                            indicator.text = "Pass $passNumber - Removed field: ${field.name} from ${file.name}"
                            showMessage("Pass $passNumber - Removed unused field: ${field.name} from ${file.name}")
                            hasChanges = true
                        } catch (e: Exception) {
                            showMessage("Failed to remove field ${field.name} from ${file.name}: ${e.message}")
                        }
                    }
                }

                // Remove unused empty classes
                currentAnalysis.unusedEmptyClasses.forEach { (file, classes) ->
                    classes.forEach { psiClass ->
                        try {
                            val className = psiClass.name ?: "Unknown"
                            psiClass.delete()
                            passRemovedClassesCount++
                            indicator.text = "Pass $passNumber - Removed unused empty class: $className from ${file.name}"
                            showMessage("Pass $passNumber - Removed unused empty class: $className from ${file.name}")
                            hasChanges = true
                        } catch (e: Exception) {
                            val className = psiClass.name ?: "Unknown"
                            showMessage("Failed to remove unused empty class $className from ${file.name}: ${e.message}")
                        }
                    }
                }

                // Remove unused non-empty classes
                currentAnalysis.unusedNonEmptyClasses.forEach { (file, classes) ->
                    classes.forEach { psiClass ->
                        try {
                            val className = psiClass.name ?: "Unknown"
                            psiClass.delete()
                            passRemovedClassesCount++
                            indicator.text = "Pass $passNumber - Removed unused non-empty class: $className from ${file.name}"
                            showMessage("Pass $passNumber - Removed unused non-empty class: $className from ${file.name}")
                            hasChanges = true
                        } catch (e: Exception) {
                            val className = psiClass.name ?: "Unknown"
                            showMessage("Failed to remove unused non-empty class $className from ${file.name}: ${e.message}")
                        }
                    }
                }

                totalRemovedImportsCount += passRemovedImportsCount
                totalRemovedFieldsCount += passRemovedFieldsCount
                totalRemovedClassesCount += passRemovedClassesCount

                showMessage("Pass $passNumber complete: removed $passRemovedImportsCount unused imports, $passRemovedFieldsCount unused fields, and $passRemovedClassesCount unused classes")

                // If no changes were made in this pass, we can stop early
                if (!hasChanges) {
                    showMessage("No changes in pass $passNumber, stopping cleanup.")
                    break
                }

                passNumber++
            }

            // Remove the "//Controller Cleaner" comments from all processed files
            removeControllerCleanerComments(analysis.filesToClean)
            
            val finalMessage = "Successfully removed $totalRemovedImportsCount unused imports, $totalRemovedFieldsCount unused fields, and $totalRemovedClassesCount unused classes from ${analysis.filesToClean.size} files over $passNumber passes. Controller Cleaner comments have been removed."
            showMessage(finalMessage)

            // Show completion message on EDT
            ApplicationManager.getApplication().invokeLater {
                            Messages.showInfoMessage(
                project,
                finalMessage,
                "Code Cleanup Complete"
            )
            }
        }
    }

    private fun removeControllerCleanerComments(files: List<PsiJavaFile>) {
        showMessage("Removing Controller Cleaner comments from processed files...")
        
        files.forEach { file ->
            try {
                val firstElement = file.findElementAt(0)
                if (firstElement?.text?.contains("//Controller Cleaner") == true) {
                    firstElement.delete()
                    showMessage("Removed Controller Cleaner comment from ${file.name}")
                }
            } catch (e: Exception) {
                showMessage("Failed to remove Controller Cleaner comment from ${file.name}: ${e.message}")
            }
        }
        
        showMessage("Controller Cleaner comments removed from ${files.size} files")
    }

    private fun showMessage(message: String) {
        println("Code Cleanup: $message")
        // Also send to tool window if available
        try {
            val toolWindowService = CodeCleanupToolWindowManager.getInstance(project)
            toolWindowService.appendMessage(message)
        } catch (e: Exception) {
            // Tool window might not be available, ignore
        }
    }
} 