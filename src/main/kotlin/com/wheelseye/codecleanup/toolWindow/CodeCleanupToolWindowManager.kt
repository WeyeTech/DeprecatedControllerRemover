package com.wheelseye.codecleanup.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

class CodeCleanupToolWindowManager(private val project: Project) {
    
    fun showToolWindow() {
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Code Cleanup")
            toolWindow?.show()
        }
    }
    
    fun appendMessage(message: String) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Code Cleanup")
            val content = toolWindow?.contentManager?.selectedContent
            val panel = content?.component as? CodeCleanupToolWindowPanel
            panel?.appendMessage(message)
        }
    }
    
    fun clearMessages() {
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Code Cleanup")
            val content = toolWindow?.contentManager?.selectedContent
            val panel = content?.component as? CodeCleanupToolWindowPanel
            panel?.clearMessages()
        }
    }
    
    companion object {
        fun getInstance(project: Project): CodeCleanupToolWindowManager {
            return CodeCleanupToolWindowManager(project)
        }
    }
} 