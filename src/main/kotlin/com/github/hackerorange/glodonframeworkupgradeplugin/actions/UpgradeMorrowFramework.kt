package com.github.hackerorange.glodonframeworkupgradeplugin.actions

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiUtilBase


class UpgradeMorrowFramework : AnAction() {


    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
            ?: return

        val processors: ArrayList<PsiFileProcessor> = ArrayList()

        processors.add(ClassImportPsiFileProcessor())
        processors.add(ReplaceEntityWrapperToQueryWrapperProcessor())
        processors.add(ServiceImplProcessor())
        processors.add(QueryWrapperOrderByProcessor())

        processors.forEach { it.init(project) }

        ProgressManager.getInstance()
            .run(object :
                Task.Backgroundable(anActionEvent.project, "Upgrading Morrow Framework from [v3.4.0] to [5.0.0]") {
                override fun run(indicator: ProgressIndicator) {
                    for (module in project.modules) {
                        module.rootManager.sourceRoots.forEach {
                            processSourceFiles(project, it, processors, indicator)
                        }
                    }
                }
            })


    }

    private fun processSourceFiles(
        project: Project,
        sourceFile: VirtualFile,
        processors: ArrayList<PsiFileProcessor>,
        indicator: ProgressIndicator
    ) {
        VfsUtilCore.iterateChildrenRecursively(sourceFile, VirtualFileFilter.ALL) { currentFile: VirtualFile ->

            // 不是有效的文件，继续后面的文件
            if (!currentFile.isValid) {
                return@iterateChildrenRecursively true
            }
            // 是文件夹，继续后面的文件
            if (currentFile.isDirectory) {
                return@iterateChildrenRecursively true
            }

            // 是 java 文件的话，更新 base directory
            if (!currentFile.name.endsWith(".java")) {
                return@iterateChildrenRecursively true
            }

            val psiFile = PsiUtilBase.getPsiFile(project, currentFile)
            if (psiFile !is PsiJavaFile) {
                return@iterateChildrenRecursively true
            }
            indicator.text2 = "Upgrading Java File ${currentFile.canonicalPath}"
            ApplicationManager.getApplication().runReadAction {
                processJavaFile(project, processors, psiFile)
            }
            true
        }


    }

    private fun processJavaFile(
        project: Project,
        processors: ArrayList<PsiFileProcessor>,
        psiJavaFile: PsiJavaFile
    ) {
        for (processor in processors) {
            processor.processPsiFile(project, psiJavaFile)
        }

        WriteCommandAction.runWriteCommandAction(project) {
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiJavaFile)
        }
    }
}