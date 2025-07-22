package com.wheelseye.controllercleanup

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ControllerCleanupStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("Controller Cleanup") ?: 
            NotificationGroupManager.getInstance().getNotificationGroup("General")
        
        notificationGroup?.createNotification(
            "Controller Cleanup Plugin",
            "Plugin loaded successfully. Use Tools > Clean Up Unused Deprecated Methods to start.",
            NotificationType.INFORMATION
        )?.notify(project)
    }
} 