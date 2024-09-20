package com.github.hackerorange.glodonframeworkupgradeplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.modules

class UpgradeMorrowFramework : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        project?.modules?.forEach {
            val moduleDirPath: String = ModuleUtil.getModuleDirPath(it)
            println(moduleDirPath)
        }

    }
}