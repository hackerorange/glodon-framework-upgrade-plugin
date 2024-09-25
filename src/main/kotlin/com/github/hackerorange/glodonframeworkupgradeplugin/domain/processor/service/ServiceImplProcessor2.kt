package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.service

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper.MethodCallStatementReplaceInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil

class ServiceImplProcessor2 : PsiFileProcessor {

    //    private var oldServiceImplClass: PsiClass? = null
    private var newServiceImplClass: PsiClass? = null


    override fun processPsiFile(project: Project, psiFile: PsiFile) {


        if (newServiceImplClass == null) {
            return
        }
        if (psiFile !is PsiJavaFile) {
            return
        }

        val methodNames = HashSet<String>()
        methodNames.add("selectBatchIds")
        methodNames.add("updateBatchById")
        methodNames.add("updateAllColumnBatchById")
        methodNames.add("updateAllColumnBatchById")
        methodNames.add("updateBatchById")
        methodNames.add("selectById")
        methodNames.add("selectBatchIds")
        methodNames.add("selectByMap")
        methodNames.add("selectOne")
        methodNames.add("selectMap")
        methodNames.add("selectObj")
        methodNames.add("selectCount")
        methodNames.add("selectList")
        methodNames.add("selectPage")
        methodNames.add("selectMaps")
        methodNames.add("selectObjs")
        methodNames.add("selectMapsPage")
        methodNames.add("selectPage")

        // 查找需要替换的方法调用语句
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()


        ApplicationManager.getApplication().runReadAction {

            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)


                    if (!methodNames.contains(expression.methodExpression.referenceName)) {
                        return
                    }

                    val qualifierExpression = expression.methodExpression.qualifierExpression ?: return

                    if (qualifierExpression.type?.let {
                            InheritanceUtil.isInheritor(
                                it,
                                newServiceImplClass!!.qualifiedName!!
                            )
                        } == false) {
                        return
                    }
                    val resolveMethod = expression.resolveMethod()

                    if (resolveMethod == null) {
                        val createReferenceFromText =
                            JavaPsiFacade.getInstance(project)
                                .elementFactory
                                .createExpressionFromText("${qualifierExpression.text}.getBaseMapper()", null)

                        val element = MethodCallStatementReplaceInfo(qualifierExpression, createReferenceFromText)

                        methodCallStatementReplaceInfos.add(element)

                    }
                }
            })

        }
        // 替换方法调用语句
        for (methodCallStatementReplaceInfo in methodCallStatementReplaceInfos.reversed()) {

            println()

            WriteCommandAction.runWriteCommandAction(project) {
                methodCallStatementReplaceInfo.oldMethodCallExpression.replace(methodCallStatementReplaceInfo.newMethodCallExpression)
            }
        }

    }

    override fun init(project: Project) {

        newServiceImplClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.extension.service.IService",
            GlobalSearchScope.allScope(project)
        )

    }

}