package com.github.hackerorange.glodonframeworkupgradeplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilBase

private const val NEW_BASE_MAPPER_QNAME = "com.baomidou.mybatisplus.core.mapper.BaseMapper"

class UpgradeMorrowFramework : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        if (project?.modules != null) {
            for (module in project.modules) {
                module.rootManager.sourceRoots.forEach {
                    processSourceFiles(project, it)
                }
            }
        }
    }

    private fun processSourceFiles(project: Project, sourceFile: VirtualFile) {
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
            if (currentFile.name.endsWith(".java")) {
                val psiFile = PsiUtilBase.getPsiFile(project, currentFile)
                if (psiFile is PsiJavaFile) {

                    val newBaseMapperClass = JavaPsiFacade.getInstance(project).findClass(
                        NEW_BASE_MAPPER_QNAME,
                        GlobalSearchScope.allScope(project)
                    )

                    psiFile.accept(object : JavaRecursiveElementVisitor() {


                        override fun visitImportStatement(statement: PsiImportStatement) {

                            println(statement.text)

                            if (statement.text == "import com.baomidou.mybatisplus.mapper.BaseMapper;") {
                                if (newBaseMapperClass != null) {
                                    val createImportStatement =
                                        JavaPsiFacade.getInstance(project).elementFactory.createImportStatement(
                                            newBaseMapperClass
                                        )
                                    WriteCommandAction.runWriteCommandAction(project) {
                                        statement.replace(createImportStatement)
                                    }
                                }
                            }


                            super.visitImportStatement(statement)
                        }
                    })
                }
            }
            true
        }


    }
}