package com.github.hackerorange.glodonframeworkupgradeplugin.actions

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.*
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.DocumentUtil


class UpgradeMorrowFramework : AnAction() {


    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
            ?: return

        val processors: ArrayList<PsiFileProcessor> = ArrayList()

        processors.add(ClassImportPsiFileProcessor())
        processors.add(ReplaceEntityWrapperToQueryWrapperProcessor())
        processors.add(ServiceImplProcessor())
        processors.add(QueryWrapperOrderByProcessor())
        processors.add(NewMapperPageResultProcessor())

        processors.forEach { it.init(project) }

        for (module in project.modules) {
            module.rootManager.sourceRoots.forEach {
                processSourceFiles(project, it, processors)
            }
        }
    }

    private fun processSourceFiles(
        project: Project,
        sourceFile: VirtualFile,
        processors: ArrayList<PsiFileProcessor>
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
            processJavaFile(project, processors, psiFile)
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
        DocumentUtil.writeInRunUndoTransparentAction { OptimizeImportsProcessor(project, psiJavaFile).run() }
    }
}