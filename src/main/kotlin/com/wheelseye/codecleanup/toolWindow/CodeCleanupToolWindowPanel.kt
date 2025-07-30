package com.wheelseye.codecleanup.toolWindow

import com.intellij.openapi.project.Project
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JScrollPane
import javax.swing.BorderFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font

class CodeCleanupToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val textArea: JTextArea
    private val scrollPane: JScrollPane
    
    init {
        // Create text area with black background and white text
        textArea = JTextArea().apply {
            background = Color.BLACK
            foreground = Color.WHITE
            font = Font("Monospaced", Font.PLAIN, 12)
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            caretColor = Color.WHITE
            selectedTextColor = Color.BLACK
            selectionColor = Color.LIGHT_GRAY
        }
        
        // Create scroll pane with proper styling
        scrollPane = JScrollPane(textArea).apply {
            background = Color.BLACK
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            
            // Style the scroll bars
            verticalScrollBar?.apply {
                background = Color.DARK_GRAY
                foreground = Color.LIGHT_GRAY
            }
            horizontalScrollBar?.apply {
                background = Color.DARK_GRAY
                foreground = Color.LIGHT_GRAY
            }
        }
        
        // Add welcome message
        textArea.text = """
            Code Cleanup Tool Window
            ========================
            
            This tool window provides a console-like interface for the Code Cleanup plugin.
            
            Features:
            • View cleanup progress and results
            • Monitor unused imports removal
            • Track unused fields cleanup
            • Monitor empty class removal
            
            Use the Clean Marked Files action from Tools menu to start cleanup.
        """.trimIndent()
        
        add(scrollPane, BorderLayout.CENTER)
    }
    
    fun appendMessage(message: String) {
        textArea.append("$message\n")
        // Auto-scroll to bottom
        textArea.caretPosition = textArea.document.length
    }
    
    fun clearMessages() {
        textArea.text = ""
    }
} 