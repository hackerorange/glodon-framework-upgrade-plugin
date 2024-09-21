package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.service

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.codeInsight.codeVision.CodeVisionState.NotReady.result
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import java.util.*

class ServiceImplProcessor : PsiFileProcessor {
    class ImportStatementReplaceContext(val oldImportStatement: PsiElement, val newImportStatement: PsiElement)
    class MethodCallStatementReplaceInfo(
        val oldMethodCallExpression: PsiElement,
        val newMethodCallExpression: PsiElement
    )

    private var oldServiceImplClass: PsiClass? = null
    private var newServiceImplClass: PsiClass? = null

    private var replaceWithMapperReturnTrueProcessors: ArrayList<ReplaceWithMapperReturnTrueProcessor> = ArrayList()


    override fun processPsiFile(project: Project, psiFile: PsiFile) {


        if (oldServiceImplClass == null || newServiceImplClass == null) {
            return
        }
        if (psiFile !is PsiJavaFile) {
            return
        }


        scene001(psiFile, project)
        println("场景一处理完成")
        scene002(psiFile, project)
        println("场景二处理完成")
        scene003(psiFile, project)
        println("场景三处理完成")
        scene004(psiFile, project)
        println("场景四处理完成")

        changeImportStatement(project, psiFile)

    }

    private fun changeImportStatement(project: Project, psiFile: PsiFile) {
        // 查找需要替换的类导入语句
        val importStatementReplaceContexts = findImportStatementWhichNeedReplace(project, psiFile)

        // 替换类导入语句
        replaceImportStatements(project, importStatementReplaceContexts)
    }

    private fun scene001(psiFile: PsiJavaFile, project: Project) {
        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()
        ApplicationManager.getApplication().runReadAction {
            for (psiClass in psiFile.classes) {
                if (!psiClass.isInheritor(oldServiceImplClass!!, true)) {
                    continue
                }
                replaceWithMapperReturnTrueProcessors.forEach { replaceWithMapperReturnTrueProcessor ->
                    methodCallStatementReplaceInfos.addAll(
                        replaceWithMapperReturnTrueProcessor.processRename(
                            project,
                            psiClass
                        )
                    )
                }
            }
        }
        // 替换方法调用语句
        replaceMethodCallStatements(project, methodCallStatementReplaceInfos)
    }

    private fun scene002(psiFile: PsiJavaFile, project: Project) {
        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()
        ApplicationManager.getApplication().runReadAction {
            for (psiClass in psiFile.classes) {
                if (!psiClass.isInheritor(oldServiceImplClass!!, true)) {
                    continue
                }
                val methodNames = ArrayList<String>()
                methodNames.add("selectList")
                methodNames.add("selectById")
                methodNames.add("selectBatchIds")
                psiClass.accept(object : JavaRecursiveElementVisitor() {

                    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {

                        super.visitMethodCallExpression(expression)


                        methodNames.firstOrNull { expression.methodExpression.referenceName == it }?.let { _ ->

                            val resolveMethod = expression.resolveMethod()
                                ?: return

                            if (oldServiceImplClass != resolveMethod.containingClass) {
                                return
                            }
                            var oldMethodCallExpression = expression.text

                            if (oldMethodCallExpression.startsWith("this.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }
                            if (oldMethodCallExpression.startsWith("super.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }


                            val newMethodCallExpression = JavaPsiFacade.getInstance(project).elementFactory
                                .createExpressionFromText("this.baseMapper.${oldMethodCallExpression}", null)

                            methodCallStatementReplaceInfos.add(
                                MethodCallStatementReplaceInfo(
                                    expression,
                                    newMethodCallExpression
                                )
                            )

                            return
                        }
                    }
                })

            }
        }
        // 替换方法调用语句
        replaceMethodCallStatements(project, methodCallStatementReplaceInfos)
    }

    private fun scene003(psiFile: PsiJavaFile, project: Project) {
        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()
        ApplicationManager.getApplication().runReadAction {
            for (psiClass in psiFile.classes) {
                if (!psiClass.isInheritor(oldServiceImplClass!!, true)) {
                    continue
                }

                psiClass.accept(object : JavaRecursiveElementVisitor() {

                    override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {

                        super.visitMethodCallExpression(methodCallExpression)

                        if (methodCallExpression.methodExpression.referenceName == "selectCount") {
                            val resolveMethod = methodCallExpression.resolveMethod()
                            if (resolveMethod == null) {
                                return
                            }
                            if (oldServiceImplClass != resolveMethod.containingClass) {
                                return
                            }
                            var oldMethodCallExpression = methodCallExpression.text

                            if (oldMethodCallExpression.startsWith("this.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }
                            if (oldMethodCallExpression.startsWith("super.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }

                            val newMethodCallExpression =
                                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                    "java.util.Optional.ofNullable(this.baseMapper.${oldMethodCallExpression}).map(Long::intValue).orElse(0)",
                                    null
                                )

                            println(newMethodCallExpression.text)

                            methodCallStatementReplaceInfos.add(
                                MethodCallStatementReplaceInfo(
                                    methodCallExpression,
                                    newMethodCallExpression
                                )
                            )
                        }

                    }
                })
            }
        }
        // 替换方法调用语句
        replaceMethodCallStatements(project, methodCallStatementReplaceInfos)
    }

    private fun scene004(psiFile: PsiJavaFile, project: Project) {
        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()
        ApplicationManager.getApplication().runReadAction {
            for (psiClass in psiFile.classes) {
                if (!psiClass.isInheritor(oldServiceImplClass!!, true)) {
                    continue
                }

                psiClass.accept(object : JavaRecursiveElementVisitor() {

                    override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {

                        super.visitMethodCallExpression(methodCallExpression)

                        if (methodCallExpression.methodExpression.referenceName == "selectObj") {
                            val resolveMethod = methodCallExpression.resolveMethod()
                            if (resolveMethod == null) {
                                return
                            }
                            if (oldServiceImplClass != resolveMethod.containingClass) {
                                return
                            }

                            var oldMethodCallExpression = methodCallExpression.text

                            if (oldMethodCallExpression.startsWith("this.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }
                            if (oldMethodCallExpression.startsWith("super.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }

                            oldMethodCallExpression = oldMethodCallExpression.replaceFirst("selectObj", "selectObjs")

                            val newStatement =
                                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                    "this.baseMapper.${oldMethodCallExpression}.stream().filter(java.util.Objects::nonNull).findFirst().orElse(null)",
                                    null
                                )
                            methodCallStatementReplaceInfos.add(
                                MethodCallStatementReplaceInfo(
                                    methodCallExpression,
                                    newStatement
                                )
                            )
                        }
                    }
                })
            }
        }
        // 替换方法调用语句
        replaceMethodCallStatements(project, methodCallStatementReplaceInfos)
    }

    private fun replaceImportStatements(
        project: Project,
        importStatementReplaceContexts: ArrayList<ImportStatementReplaceContext>
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            importStatementReplaceContexts.forEach {
                it.oldImportStatement.replace(it.newImportStatement)
            }
        }
    }

    private fun replaceMethodCallStatements(
        project: Project,
        importStatementReplaceContexts: ArrayList<MethodCallStatementReplaceInfo>
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            importStatementReplaceContexts.forEach {
                try {
                    it.oldMethodCallExpression.replace(it.newMethodCallExpression)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun findImportStatementWhichNeedReplace(
        project: Project,
        psiFile: PsiFile
    ): ArrayList<ImportStatementReplaceContext> {
        val importStatementReplaceContexts = ArrayList<ImportStatementReplaceContext>()

        ApplicationManager.getApplication().runReadAction {
            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitImportStatement(statement: PsiImportStatement) {
                    if (statement.text == "import ${oldServiceImplClass!!.qualifiedName};") {
                        val newImportStatement = JavaPsiFacade.getInstance(project).elementFactory
                            .createImportStatement(newServiceImplClass!!)

                        importStatementReplaceContexts.add(
                            ImportStatementReplaceContext(
                                statement,
                                newImportStatement
                            )
                        )
                    }

                }
            })
        }
        return importStatementReplaceContexts
    }

    override fun init(project: Project) {
        oldServiceImplClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.service.impl.ServiceImpl",
            GlobalSearchScope.allScope(project)
        )
        newServiceImplClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.extension.service.impl.ServiceImpl",
            GlobalSearchScope.allScope(project)
        )

        oldServiceImplClass?.let {
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("insert", it))
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("delete", it))
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("deleteById", it))
        }

    }


    class ReplaceWithMapperReturnTrueProcessor(
        private val methodName: String,
        private var oldServiceImplClass: PsiClass
    ) {

        fun processRename(project: Project, psiClass: PsiClass): List<MethodCallStatementReplaceInfo> {
            if (!psiClass.isInheritor(oldServiceImplClass, true)) {
                return Collections.emptyList()
            }
            val result = ArrayList<MethodCallStatementReplaceInfo>();

            psiClass.accept(object : JavaRecursiveElementVisitor() {

                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)
                    if (expression.methodExpression.referenceName != methodName) {
                        return
                    }
                    val resolveMethod = expression.resolveMethod()
                        ?: return

                    if (oldServiceImplClass != resolveMethod.containingClass) {
                        return
                    }

                    var oldMethodCallExpression = expression.text

                    if (oldMethodCallExpression.startsWith("this.")) {
                        oldMethodCallExpression = oldMethodCallExpression.substring(5)
                    }
                    if (oldMethodCallExpression.startsWith("super.")) {
                        oldMethodCallExpression = oldMethodCallExpression.substring(5)
                    }
                    val createReferenceFromText =
                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                            "com.baomidou.mybatisplus.extension.toolkit.SqlHelper.retBool(this.baseMapper.$oldMethodCallExpression)",
                            null
                        )

                    result.add(MethodCallStatementReplaceInfo(expression, createReferenceFromText))

                }
            })
            return result
        }
    }

}