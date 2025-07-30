package com.wheelseye.deprecatedcontrollerremover.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

class DeprecatedControllerRemoverToolWindowManager(private val project: Project) {
    
    fun showToolWindow() {
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Deprecated Controller Remover")
            toolWindow?.show()
        }
    }
    
    fun appendMessage(message: String) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Deprecated Controller Remover")
            val content = toolWindow?.contentManager?.selectedContent
            val panel = content?.component as? DeprecatedControllerRemoverToolWindowPanel
            panel?.appendMessage(message)
        }
    }
    
    fun clearMessages() {
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Deprecated Controller Remover")
            val content = toolWindow?.contentManager?.selectedContent
            val panel = content?.component as? DeprecatedControllerRemoverToolWindowPanel
            panel?.clearMessages()
        }
    }
    
    companion object {
        fun getInstance(project: Project): DeprecatedControllerRemoverToolWindowManager {
            return DeprecatedControllerRemoverToolWindowManager(project)
        }
    }
} 