package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import java.util.LinkedList

class QueryWrapperOrderByProcessor : PsiFileProcessor {

    private var entityWrapperClass: PsiClass? = null

    class OrderByMethodCallReplaceInfo(val oldMethodCallExpression: PsiElement, val newMethodCallExpression: PsiElement)


    override fun processPsiFile(project: Project, psiFile: PsiFile) {


        while (true) {
            if (tryToFixOrderBy(psiFile, project)) {
                return
            }
        }


    }

    private fun tryToFixOrderBy(
        psiFile: PsiFile,
        project: Project
    ): Boolean {

        val methodCallReplaceInfos = LinkedList<OrderByMethodCallReplaceInfo>()

        ApplicationManager.getApplication().runReadAction {

            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitMethodCallExpression(psiMethodCallExpression: PsiMethodCallExpression) {


                    val methodExpression = psiMethodCallExpression.methodExpression

                    when (methodExpression.referenceName) {
                        "orderBy" -> {
                            val copy = psiMethodCallExpression.copy() as PsiMethodCallExpression
                            processOrderBy(project, copy)
                            if (copy.text != psiMethodCallExpression.text) {
                                methodCallReplaceInfos.add(
                                    OrderByMethodCallReplaceInfo(
                                        psiMethodCallExpression,
                                        copy
                                    )
                                )
                                return
                            }
                        }

                        "orderAsc" -> {
                            val orderByCopy = psiMethodCallExpression.copy() as PsiMethodCallExpression
                            processOrderAsc(project, orderByCopy)
                            if (orderByCopy.text != psiMethodCallExpression.text) {
                                methodCallReplaceInfos.add(
                                    OrderByMethodCallReplaceInfo(
                                        psiMethodCallExpression,
                                        orderByCopy
                                    )
                                )
                                return
                            }
                        }

                        "orderDesc" -> {
                            val orderByCopy = psiMethodCallExpression.copy() as PsiMethodCallExpression
                            processOrderDesc(project, orderByCopy)
                            if (orderByCopy.text != psiMethodCallExpression.text) {
                                methodCallReplaceInfos.add(
                                    OrderByMethodCallReplaceInfo(
                                        psiMethodCallExpression,
                                        orderByCopy
                                    )
                                )
                                return
                            }
                        }

                        "setSqlSelect" -> {
                            val orderByCopy = psiMethodCallExpression.copy() as PsiMethodCallExpression
                            processSetSelect(project, orderByCopy)
                            if (orderByCopy.text != psiMethodCallExpression.text) {
                                methodCallReplaceInfos.add(
                                    OrderByMethodCallReplaceInfo(
                                        psiMethodCallExpression,
                                        orderByCopy
                                    )
                                )
                                return
                            }
                        }
                    }

                    super.visitMethodCallExpression(psiMethodCallExpression)

                }
            })
        }

        if (methodCallReplaceInfos.isEmpty()) {
            return true;
        }

        WriteCommandAction.runWriteCommandAction(project) {
            methodCallReplaceInfos.forEach {
                it.oldMethodCallExpression.replace(it.newMethodCallExpression)
            }
        }

        return false
    }

    private fun processOrderAsc(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val methodExpression = psiMethodCallExpression.methodExpression

        val filter = methodExpression.children.filterIsInstance<PsiIdentifier>()


        if (filter.size == 1) {
            val psiIdentifier = filter[0]

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("orderByAsc")
            psiIdentifier.replace(createIdentifier)
        }
    }

    private fun processSetSelect(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val methodExpression = psiMethodCallExpression.methodExpression

        val filter = methodExpression.children.filterIsInstance<PsiIdentifier>()


        if (filter.size == 1) {
            val psiIdentifier = filter[0]

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("select")
            psiIdentifier.replace(createIdentifier)
        }
    }

    private fun processOrderDesc(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val methodExpression = psiMethodCallExpression.methodExpression

        val filter = methodExpression.children.filterIsInstance<PsiIdentifier>()

        if (filter.size == 1) {
            val psiIdentifier = filter[0]
            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("orderByDesc")
            psiIdentifier.replace(createIdentifier)

        }
    }

    private fun processOrderBy(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val argumentList = psiMethodCallExpression.argumentList

        if (argumentList.expressionCount == 1) {
            var aaa = ""
            for (expression in argumentList.expressions) {
                aaa = (aaa + expression.type?.canonicalText + ";")
            }

            if ("java.lang.String;" == aaa) {

                val psiExpressions = ArrayList<PsiElement>()

                for (expression in argumentList.expressions) {
                    psiExpressions.add(expression.copy())
                }

                argumentList.expressions.forEach { it.delete() }

                val trueExpression =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText("true", null)
                argumentList.add(trueExpression)
                argumentList.add(trueExpression)
                argumentList.add(psiExpressions[0])
            }
        }

        if (argumentList.expressionCount == 2) {
            var aaa = ""
            for (expression in argumentList.expressions) {
                aaa = (aaa + expression.type?.canonicalText + ";")
            }

            if ("java.lang.String;boolean;" == aaa) {

                val psiExpressions = ArrayList<PsiElement>()

                for (expression in argumentList.expressions) {
                    psiExpressions.add(expression.copy())
                }

                argumentList.expressions.forEach { it.delete() }

                val trueExpression =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText("true", null)
                argumentList.add(trueExpression)
                argumentList.add(psiExpressions[1])
                argumentList.add(psiExpressions[0])
            }
        }

        if (argumentList.expressionCount == 3) {
            var aaa = ""
            for (expression in argumentList.expressions) {
                aaa = (aaa + expression.type?.canonicalText + ";")
            }
        }
    }

    override fun init(project: Project) {
        entityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.conditions.query.QueryWrapper",
            GlobalSearchScope.allScope(project)
        )
    }

}
