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
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.JavaRecursiveElementVisitor

class CodeCleanupService(private val project: Project) {

    fun performCleanup() {
        showMessage("Starting code cleanup analysis...")

        val javaFiles = findJavaFiles()
        showMessage("Found ${javaFiles.size} Java files")

        if (javaFiles.isEmpty()) {
            showMessage("No Java files found in the project.")
            return
        }

        val cleanupStats = performComprehensiveCleanup(javaFiles)
        
        val finalMessage = buildString {
            append("Code cleanup complete: ")
            append("removed ${cleanupStats.first} unused fields and ${cleanupStats.second} unused imports")
            append(" across ${javaFiles.size} files.")
        }
        
        showMessage(finalMessage)

        Messages.showInfoMessage(
            project,
            finalMessage,
            "Code Cleanup Complete"
        )
    }

    fun cleanupAffectedFiles(affectedFiles: Set<PsiFile>): Pair<Int, Int> {
        showMessage("Starting cleanup of affected files...")
        
        val javaFiles = affectedFiles.filterIsInstance<PsiJavaFile>()
        showMessage("Found ${javaFiles.size} Java files to clean up")

        if (javaFiles.isEmpty()) {
            showMessage("No Java files to clean up.")
            return Pair(0, 0)
        }

        return performComprehensiveCleanup(javaFiles)
    }

    private fun findJavaFiles(): List<PsiJavaFile> {
        val javaFiles = mutableListOf<PsiJavaFile>()
        val fileIndex = ProjectFileIndex.getInstance(project)
        
        fileIndex.iterateContent { file: VirtualFile ->
            if (!file.isDirectory && file.extension == "java") {
                val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile
                if (psiFile != null) {
                    javaFiles.add(psiFile)
                }
            }
            true
        }
        
        return javaFiles
    }

    private fun performComprehensiveCleanup(javaFiles: List<PsiJavaFile>): Pair<Int, Int> {
        showMessage("Starting comprehensive cleanup across ${javaFiles.size} Java files...")
        
        var totalUnusedFieldsRemoved = 0
        var totalUnusedImportsRemoved = 0
        var processedFiles = 0

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                javaFiles.forEach { file ->
                    val unusedFieldsRemoved = removeUnusedFinalPrivateFields(file)
                    val unusedImportsRemoved = removeUnusedImports(file)
                    
                    totalUnusedFieldsRemoved += unusedFieldsRemoved
                    totalUnusedImportsRemoved += unusedImportsRemoved
                    processedFiles++
                    
                    if (unusedFieldsRemoved > 0 || unusedImportsRemoved > 0) {
                        showMessage("Cleaned up ${file.name}: $unusedFieldsRemoved unused fields, $unusedImportsRemoved unused imports")
                    }
                }

                showMessage("Comprehensive cleanup complete: processed $processedFiles files, removed $totalUnusedFieldsRemoved unused fields and $totalUnusedImportsRemoved unused imports")
            }
        }
        
        return Pair(totalUnusedFieldsRemoved, totalUnusedImportsRemoved)
    }

    private fun removeUnusedFinalPrivateFields(file: PsiJavaFile): Int {
        var removedCount = 0
        
        file.classes.forEach { psiClass ->
            val fieldsToRemove = mutableListOf<PsiField>()
            
            psiClass.fields.forEach { field ->
                if (isUnusedFinalPrivateField(field)) {
                    fieldsToRemove.add(field)
                }
            }
            
            fieldsToRemove.forEach { field ->
                try {
                    field.delete()
                    removedCount++
                    showMessage("Removed unused final private field: ${field.name}")
                } catch (e: Exception) {
                    showMessage("Failed to remove field ${field.name}: ${e.message}")
                }
            }
        }
        
        return removedCount
    }

    private fun isUnusedFinalPrivateField(field: PsiField): Boolean {
        try {
            // Check if the field is still valid
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
            
            // Check if field is used anywhere
            val scope = GlobalSearchScope.projectScope(project)
            val references = ReferencesSearch.search(field, scope).findAll()
            
            // Filter out references that are not the field declaration itself
            val externalReferences = references.filter { ref ->
                val element = ref.element
                // Exclude the field declaration itself
                element != field.nameIdentifier
            }
            
            return externalReferences.isEmpty()
        } catch (e: Exception) {
            showMessage("Error checking field ${field.name}: ${e.message}")
            return false
        }
    }

    private fun removeUnusedImports(file: PsiJavaFile): Int {
        var removedCount = 0
        
        file.importList?.importStatements?.forEach { importStatement ->
            try {
                // Check if the import statement is still valid
                if (!importStatement.isValid) {
                    return@forEach
                }
                
                if (isUnusedImport(importStatement, file)) {
                    val qualifiedName = importStatement.qualifiedName ?: "unknown"
                    importStatement.delete()
                    removedCount++
                    showMessage("Removed unused import: $qualifiedName")
                }
            } catch (e: Exception) {
                val qualifiedName = try {
                    importStatement.qualifiedName ?: "unknown"
                } catch (ex: Exception) {
                    "unknown"
                }
                showMessage("Failed to remove import $qualifiedName: ${e.message}")
            }
        }
        
        return removedCount
    }

    private fun isUnusedImport(importStatement: PsiImportStatement, file: PsiJavaFile): Boolean {
        try {
            // Check if the import statement is still valid
            if (!importStatement.isValid) {
                return false
            }
            
            val qualifiedName = importStatement.qualifiedName ?: return false
            
            // Skip wildcard imports for now
            if (importStatement.isOnDemand) {
                return false
            }
            
            // Skip java.lang imports as they are automatically available
            if (qualifiedName.startsWith("java.lang.")) {
                return true
            }
            
            // Extract the simple class name from the qualified name
            val simpleClassName = qualifiedName.substringAfterLast('.')
            
            // Use a simpler but more reliable approach
            return !isImportUsedInFile(file, simpleClassName, importStatement)
        } catch (e: Exception) {
            showMessage("Error checking import statement: ${e.message}")
            return false
        }
    }

    private fun isImportUsedInFile(file: PsiJavaFile, simpleClassName: String, importStatement: PsiImportStatement): Boolean {
        // Use PSI tree traversal to find actual usages
        val visitor = object : JavaRecursiveElementVisitor() {
            var foundUsage = false
            
            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                super.visitReferenceElement(reference)
                if (reference.referenceName == simpleClassName) {
                    // Check if this reference is not part of the import statement itself
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
        }
        
        file.accept(visitor)
        return visitor.foundUsage
    }

    private fun showMessage(message: String) {
        println("Code Cleanup: $message")
    }
} 