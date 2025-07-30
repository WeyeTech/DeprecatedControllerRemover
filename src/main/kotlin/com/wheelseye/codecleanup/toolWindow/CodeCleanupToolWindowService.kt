package com.wheelseye.codecleanup.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager

class CodeCleanupToolWindowService : StartupActivity.Background {
    
    companion object {
        private const val TOOL_WINDOW_ID = "Code Cleanup"
    }
    
    override fun runActivity(project: Project) {
        // Register tool window on EDT
        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            
            // Register the tool window programmatically
            val toolWindow = toolWindowManager.registerToolWindow(
                TOOL_WINDOW_ID,
                true,
                ToolWindowAnchor.BOTTOM
            )
            
            // Set tool window properties
            toolWindow.setIcon(AllIcons.Actions.Refresh)
            
            // Set up the tool window content
            setupToolWindowContent(project, toolWindow)
        }
    }
    
    private fun setupToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = CodeCleanupToolWindowPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(toolWindowPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
} 