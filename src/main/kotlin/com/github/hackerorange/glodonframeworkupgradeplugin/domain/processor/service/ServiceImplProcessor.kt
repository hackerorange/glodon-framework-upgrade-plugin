package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.service

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper.MethodCallStatementReplaceInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import java.util.*

class ServiceImplProcessor : PsiFileProcessor {

    //    private var oldServiceImplClass: PsiClass? = null
    private var newServiceImplClass: PsiClass? = null
    private var sqlHelperClass: PsiClass? = null

    private var replaceWithMapperReturnTrueProcessors: ArrayList<ReplaceWithMapperReturnTrueProcessor> = ArrayList()


    override fun processPsiFile(project: Project, psiFile: PsiFile) {


        if (newServiceImplClass == null) {
            return
        }
        if (psiFile !is PsiJavaFile) {
            return
        }


        scene001(psiFile, project)
        scene002(psiFile, project)
        scene003(psiFile, project)
        scene004(psiFile, project)
        scene005(psiFile, project)
        scene006(psiFile, project)

//        changeImportStatement(project, psiFile)

    }

//    private fun changeImportStatement(project: Project, psiFile: PsiFile) {
//        // 查找需要替换的类导入语句
//        val importStatementReplaceContexts = findImportStatementWhichNeedReplace(project, psiFile)
//
//        // 替换类导入语句
//        replaceImportStatements(project, importStatementReplaceContexts)
//    }

    private fun scene001(psiFile: PsiJavaFile, project: Project) {
        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()
        ApplicationManager.getApplication().runReadAction {
            for (psiClass in psiFile.classes) {
                if (!psiClass.isInheritor(newServiceImplClass!!, true)) {
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
                if (!psiClass.isInheritor(newServiceImplClass!!, true)) {
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

//                            val qualifierExpression = expression.methodExpression.qualifierExpression ?: return

                            val qualifierExpression = expression.methodExpression.qualifierExpression

                            if (qualifierExpression != null) {
                                if (qualifierExpression.type?.let {
                                        InheritanceUtil.isInheritor(
                                            it, newServiceImplClass!!.qualifiedName!!
                                        )
                                    } == false) {
                                    return
                                }
                            }

                            var oldMethodCallExpression = expression.text

                            if (oldMethodCallExpression.startsWith("this.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }
                            if (oldMethodCallExpression.startsWith("super.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(6)
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
                if (!psiClass.isInheritor(newServiceImplClass!!, true)) {
                    continue
                }

                psiClass.accept(object : JavaRecursiveElementVisitor() {

                    override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {

                        super.visitMethodCallExpression(methodCallExpression)

                        if (methodCallExpression.methodExpression.referenceName == "selectCount") {
                            val qualifierExpression = methodCallExpression.methodExpression.qualifierExpression

                            if (qualifierExpression != null) {
                                if (qualifierExpression.type?.let {
                                        InheritanceUtil.isInheritor(
                                            it,
                                            newServiceImplClass!!.qualifiedName!!
                                        )
                                    } == false) {
                                    return
                                }
                            }
                            var oldMethodCallExpression = methodCallExpression.text

                            if (oldMethodCallExpression.startsWith("this.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }
                            if (oldMethodCallExpression.startsWith("super.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(6)
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
                if (!psiClass.isInheritor(newServiceImplClass!!, true)) {
                    continue
                }

                psiClass.accept(object : JavaRecursiveElementVisitor() {

                    override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {

                        super.visitMethodCallExpression(methodCallExpression)

                        if (methodCallExpression.methodExpression.referenceName == "selectObj") {
                            val qualifierExpression = methodCallExpression.methodExpression.qualifierExpression

                            if (qualifierExpression != null) {
                                if (qualifierExpression.type?.let {
                                        InheritanceUtil.isInheritor(
                                            it,
                                            newServiceImplClass!!.qualifiedName!!
                                        )
                                    } == false) {
                                    return
                                }
                            }

                            var oldMethodCallExpression = methodCallExpression.text

                            if (oldMethodCallExpression.startsWith("this.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }
                            if (oldMethodCallExpression.startsWith("super.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(6)
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

    private fun scene005(psiFile: PsiJavaFile, project: Project) {
        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()
        ApplicationManager.getApplication().runReadAction {
            for (psiClass in psiFile.classes) {
                if (!psiClass.isInheritor(newServiceImplClass!!, true)) {
                    continue
                }

                psiClass.accept(object : JavaRecursiveElementVisitor() {

                    override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {

                        super.visitMethodCallExpression(methodCallExpression)

                        if (methodCallExpression.methodExpression.referenceName != "insertBatch") {
                            return
                        }
                        val qualifierExpression = methodCallExpression.methodExpression.qualifierExpression

                        if (qualifierExpression != null) {
                            if (qualifierExpression.type?.let {
                                    InheritanceUtil.isInheritor(
                                        it,
                                        newServiceImplClass!!.qualifiedName!!
                                    )
                                } == false) {
                                return
                            }
                        }

                        val newStatement = JavaPsiFacade.getInstance(project).elementFactory
                            .createExpressionFromText(
                                methodCallExpression.text.replaceFirst("insertBatch", "saveBatch"), null
                            )
                        methodCallStatementReplaceInfos.add(
                            MethodCallStatementReplaceInfo(
                                methodCallExpression,
                                newStatement
                            )
                        )
                    }
                })
            }
        }
        // 替换方法调用语句
        replaceMethodCallStatements(project, methodCallStatementReplaceInfos)
    }

    private fun scene006(psiFile: PsiJavaFile, project: Project) {
        if(sqlHelperClass==null){
            return
        }
        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()

        ApplicationManager.getApplication().runReadAction {
            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {

                    super.visitMethodCallExpression(methodCallExpression)

                    if (methodCallExpression.methodExpression.referenceName != "retBool") {
                        return
                    }
                    val qualifierExpression = methodCallExpression.methodExpression.qualifierExpression

                    if (qualifierExpression != null) {
                        if (qualifierExpression.type?.let {
                                InheritanceUtil.isInheritor(
                                    it,
                                    sqlHelperClass!!.qualifiedName!!
                                )
                            } == false) {
                            return
                        }
                    }

                    val elementPattern: ElementPattern<PsiElement> = StandardPatterns
                        .or(
                            // 参数中
                            PsiJavaPatterns.psiElement()
                                .inside(PsiJavaPatterns.psiParameter()),
                            // 定义语句
                            PsiJavaPatterns.psiElement()
                                .inside(PsiJavaPatterns.psiElement(PsiDeclarationStatement::class.java)),
                            // 赋值语句
                            PsiJavaPatterns.psiElement()
                                .inside(PsiJavaPatterns.psiElement(PsiAssignmentExpression::class.java)),
                        )
                    if (elementPattern.accepts(methodCallExpression)) {
                        return
                    }

                    if (methodCallExpression.argumentList.expressionCount == 1) {
                        methodCallStatementReplaceInfos.add(
                            MethodCallStatementReplaceInfo(
                                methodCallExpression,
                                methodCallExpression.argumentList.expressions[0].copy()
                            )
                        )
                    }
                }
            })
        }
        // 替换方法调用语句
        replaceMethodCallStatements(project, methodCallStatementReplaceInfos)
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

    override fun init(project: Project) {

        newServiceImplClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.extension.service.impl.ServiceImpl",
            GlobalSearchScope.allScope(project)
        )

        sqlHelperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.extension.toolkit.SqlHelper",
            GlobalSearchScope.allScope(project)
        )

        newServiceImplClass?.let {
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("insert", it))
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("delete", it))
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("deleteById", it))
        }

    }


    class ReplaceWithMapperReturnTrueProcessor(
        private val methodName: String,
        private var newServiceImplClass: PsiClass
    ) {

        fun processRename(project: Project, psiClass: PsiClass): List<MethodCallStatementReplaceInfo> {
            if (!psiClass.isInheritor(newServiceImplClass, true)) {
                return Collections.emptyList()
            }
            val result = ArrayList<MethodCallStatementReplaceInfo>()

            psiClass.accept(object : JavaRecursiveElementVisitor() {

                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)
                    if (expression.methodExpression.referenceName != methodName) {
                        return
                    }

                    val qualifierExpression = expression.methodExpression.qualifierExpression

                    if (qualifierExpression != null) {
                        if (qualifierExpression.type?.let {
                                InheritanceUtil.isInheritor(
                                    it,
                                    newServiceImplClass.qualifiedName!!
                                )
                            } == false) {
                            return
                        }
                    }


                    var oldMethodCallExpression = expression.text

                    if (oldMethodCallExpression.startsWith("this.")) {
                        oldMethodCallExpression = oldMethodCallExpression.substring(5)
                    }
                    if (oldMethodCallExpression.startsWith("super.")) {
                        oldMethodCallExpression = oldMethodCallExpression.substring(6)
                    }

                    val elementPattern: ElementPattern<PsiElement> = StandardPatterns
                        .or(
                            // 参数中
                            PsiJavaPatterns.psiElement()
                                .inside(PsiJavaPatterns.psiParameter()),
                            // 定义语句
                            PsiJavaPatterns.psiElement()
                                .inside(PsiJavaPatterns.psiElement(PsiDeclarationStatement::class.java)),
                            // 赋值语句
                            PsiJavaPatterns.psiElement()
                                .inside(PsiJavaPatterns.psiElement(PsiAssignmentExpression::class.java)),
                        )

                    if (elementPattern.accepts(expression)) {

                        val createReferenceFromText = JavaPsiFacade.getInstance(project).elementFactory
                            .createExpressionFromText(
                                "com.baomidou.mybatisplus.extension.toolkit.SqlHelper.retBool(this.baseMapper.$oldMethodCallExpression)",
                                null
                            )

                        result.add(MethodCallStatementReplaceInfo(expression, createReferenceFromText))
                    } else {

                        val createReferenceFromText =
                            JavaPsiFacade.getInstance(project).elementFactory
                                .createExpressionFromText("this.baseMapper.$oldMethodCallExpression", null)

                        result.add(MethodCallStatementReplaceInfo(expression, createReferenceFromText))
                    }


                }
            })
            return result
        }
    }

}