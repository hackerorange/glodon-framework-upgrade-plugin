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
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilBase

private const val NEW_BASE_MAPPER_QNAME = "com.baomidou.mybatisplus.core.mapper.BaseMapper"
private const val NEW_PAGE_QNAME = "com.baomidou.mybatisplus.extension.plugins.pagination.Page"

class UpgradeMorrowFramework : AnAction() {


    companion object {
        private val importReplace: HashMap<String, String> = HashMap()

        init {
            importReplace["com.baomidou.mybatisplus.mapper.BaseMapper"] =
                "com.baomidou.mybatisplus.core.mapper.BaseMapper"

            importReplace["com.baomidou.mybatisplus.plugins.Page"] =
                "com.baomidou.mybatisplus.extension.plugins.pagination.Page"
        }

    }

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        val classQNameToReplaceClassMap: HashMap<String, PsiClass> = HashMap()
        if (project?.modules != null) {

            for (mutableEntry in importReplace) {

                val newBaseMapperClass = JavaPsiFacade.getInstance(project).findClass(
                    mutableEntry.value,
                    GlobalSearchScope.allScope(project)
                )

                if (newBaseMapperClass != null) {
                    classQNameToReplaceClassMap[mutableEntry.key] = newBaseMapperClass
                }
            }


            for (module in project.modules) {
                module.rootManager.sourceRoots.forEach {
                    processSourceFiles(project, it, classQNameToReplaceClassMap)
                }
            }
        }
    }

    private fun processSourceFiles(
        project: Project,
        sourceFile: VirtualFile,
        classQNameToReplaceClassMap: HashMap<String, PsiClass>
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

            val entityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
                "com.baomidou.mybatisplus.mapper.EntityWrapper",
                GlobalSearchScope.allScope(project)
            )


            // 是 java 文件的话，更新 base directory
            if (currentFile.name.endsWith(".java")) {
                val psiFile = PsiUtilBase.getPsiFile(project, currentFile)
                if (psiFile is PsiJavaFile) {


                    psiFile.accept(object : JavaRecursiveElementVisitor() {


                        override fun visitNewExpression(expression: PsiNewExpression) {


                            if (entityWrapperClass != null) {
                                if (expression.type !is PsiClassType) return

                                val resolve = (expression.type as PsiClassType).resolve() ?: return
                                if (resolve == entityWrapperClass) {
                                    val newExpression = expression
                                    val containingFile = newExpression.containingFile

                                    val newExpressionType = newExpression.type ?: return

                                    if (newExpressionType !is PsiClassType) return
                                    val substitutor = newExpressionType.resolveGenerics().substitutor
                                    if (substitutor.substitutionMap.values.size != 1) return
                                    val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                                    if (entityClassType !is PsiClassType) return
                                    val entityClass = entityClassType.resolve() ?: return

                                    var replaceExpression =
                                        "new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<${entityClass.qualifiedName}>()\n"


                                    val createExpressionFromText =
                                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                            replaceExpression,
                                            newExpression
                                        )


                                    WriteCommandAction.runWriteCommandAction(project) {
                                        newExpression.replace(createExpressionFromText)
                                    }

                                }

                            }

                            expression.classReference


                            super.visitNewExpression(expression)
                        }

                        override fun visitImportStatement(statement: PsiImportStatement) {

                            for (item in classQNameToReplaceClassMap) {
                                if (statement.text == "import ${item.key};") {
                                    val newBaseMapperClass = item.value
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

                    WriteCommandAction.runWriteCommandAction(project) {
                        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile)
                    }
                }
            }
            true
        }


    }
}