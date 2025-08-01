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
        val emptyClasses: Map<PsiJavaFile, List<PsiClass>>,
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
            return CleanupAnalysis(emptyList(), emptyMap(), emptyMap(), emptyMap(), 0, 0, 0)
        }

        indicator.text = "Analyzing unused imports..."
        val unusedImports = findUnusedImports(markedFiles)
        val totalImportsToRemove = unusedImports.values.sumOf { it.size }
        showMessage("Found $totalImportsToRemove unused imports")

        indicator.text = "Analyzing unused final private fields..."
        val unusedFields = findUnusedFinalPrivateFields(markedFiles)
        val totalFieldsToRemove = unusedFields.values.sumOf { it.size }
        showMessage("Found $totalFieldsToRemove unused final private fields")

        indicator.text = "Analyzing empty classes..."
        val emptyClasses = findEmptyClasses(markedFiles)
        val totalClassesToRemove = emptyClasses.values.sumOf { it.size }
        showMessage("Found $totalClassesToRemove empty classes")

        return CleanupAnalysis(
            filesToClean = markedFiles,
            unusedImports = unusedImports,
            unusedFields = unusedFields,
            emptyClasses = emptyClasses,
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

    private fun findUnusedFinalPrivateFields(files: List<PsiJavaFile>): Map<PsiJavaFile, List<PsiField>> {
        val unusedFields = mutableMapOf<PsiJavaFile, List<PsiField>>()
        
        files.forEach { file ->
            val fileUnusedFields = mutableListOf<PsiField>()
            
            file.classes.forEach { psiClass ->
                psiClass.fields.forEach { field ->
                    if (isUnusedFinalPrivateField(field)) {
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

    private fun isUnusedFinalPrivateField(field: PsiField): Boolean {
        try {
            if (!field.isValid) {
                return false
            }
            
            // Check if field is final and private
            if (!field.hasModifierProperty(PsiModifier.FINAL) || !field.hasModifierProperty(PsiModifier.PRIVATE)) {
                return false
            }
            
            // Skip fields with annotations (they might be used by frameworks)
            if (field.modifierList?.annotations?.isNotEmpty() == true) {
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

    private fun findEmptyClasses(files: List<PsiJavaFile>): Map<PsiJavaFile, List<PsiClass>> {
        val emptyClasses = mutableMapOf<PsiJavaFile, List<PsiClass>>()
        
        files.forEach { file ->
            val fileEmptyClasses = mutableListOf<PsiClass>()
            
            file.classes.forEach { psiClass ->
                if (isClassEmpty(psiClass)) {
                    fileEmptyClasses.add(psiClass)
                }
            }
            
            if (fileEmptyClasses.isNotEmpty()) {
                emptyClasses[file] = fileEmptyClasses
            }
        }
        
        return emptyClasses
    }

    private fun isClassEmpty(psiClass: PsiClass): Boolean {
        try {
            if (!psiClass.isValid) {
                return false
            }
            
            // Check if the class has any methods
            if (psiClass.methods.size > 0) {
                return false
            }
            
            // Check if the class has any fields
            if (psiClass.fields.size > 0) {
                return false
            }
            
            // Check if the class has any nested classes
            if (psiClass.innerClasses.size > 0) {
                return false
            }
            
            return true
        } catch (e: Exception) {
            showMessage("Error checking class ${psiClass.name}: ${e.message}")
            return false
        }
    }

    private fun showSummary(analysis: CleanupAnalysis) {
        val message = buildString {
            appendLine("Found ${analysis.filesToClean.size} files marked for cleanup:")
            appendLine("• ${analysis.totalImportsToRemove} unused imports")
            appendLine("• ${analysis.totalFieldsToRemove} unused final private fields")
            appendLine("• ${analysis.totalClassesToRemove} empty classes")
            appendLine()
            
            analysis.filesToClean.take(10).forEach { file ->
                val importsCount = analysis.unusedImports[file]?.size ?: 0
                val fieldsCount = analysis.unusedFields[file]?.size ?: 0
                val classesCount = analysis.emptyClasses[file]?.size ?: 0
                appendLine("• ${file.name}: $importsCount imports, $fieldsCount fields, $classesCount empty classes")
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
            appendLine("• ${analysis.totalFieldsToRemove} unused final private fields")
            appendLine("• ${analysis.totalClassesToRemove} empty classes")
            appendLine()
            
            analysis.filesToClean.take(10).forEach { file ->
                val importsCount = analysis.unusedImports[file]?.size ?: 0
                val fieldsCount = analysis.unusedFields[file]?.size ?: 0
                val classesCount = analysis.emptyClasses[file]?.size ?: 0
                appendLine("• ${file.name}: $importsCount imports, $fieldsCount fields, $classesCount empty classes")
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

                // Remove empty classes
                currentAnalysis.emptyClasses.forEach { (file, classes) ->
                    classes.forEach { psiClass ->
                        try {
                            val className = psiClass.name ?: "Unknown"
                            psiClass.delete()
                            passRemovedClassesCount++
                            indicator.text = "Pass $passNumber - Removed empty class: $className from ${file.name}"
                            showMessage("Pass $passNumber - Removed empty class: $className from ${file.name}")
                            hasChanges = true
                        } catch (e: Exception) {
                            val className = psiClass.name ?: "Unknown"
                            showMessage("Failed to remove empty class $className from ${file.name}: ${e.message}")
                        }
                    }
                }

                totalRemovedImportsCount += passRemovedImportsCount
                totalRemovedFieldsCount += passRemovedFieldsCount
                totalRemovedClassesCount += passRemovedClassesCount

                showMessage("Pass $passNumber complete: removed $passRemovedImportsCount unused imports, $passRemovedFieldsCount unused fields, and $passRemovedClassesCount empty classes")

                // If no changes were made in this pass, we can stop early
                if (!hasChanges) {
                    showMessage("No changes in pass $passNumber, stopping cleanup.")
                    break
                }

                passNumber++
            }

            // Remove the "//Controller Cleaner" comments from all processed files
            removeControllerCleanerComments(analysis.filesToClean)
            
            val finalMessage = "Successfully removed $totalRemovedImportsCount unused imports, $totalRemovedFieldsCount unused fields, and $totalRemovedClassesCount empty classes from ${analysis.filesToClean.size} files over $passNumber passes. Controller Cleaner comments have been removed."
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