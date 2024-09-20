package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class QueryWrapperOrderByProcessor : PsiFileProcessor {

    private var entityWrapperClass: PsiClass? = null

    override fun processPsiFile(project: Project, psiFile: PsiFile) {

        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitMethodCallExpression(psiMethodCallExpression: PsiMethodCallExpression) {

                val methodExpression = psiMethodCallExpression.methodExpression
                if (methodExpression.referenceName == "orderBy") {
                    processOrderBy(project, psiMethodCallExpression)
                }
                if (methodExpression.referenceName == "orderAsc") {
                    processOrderAsc(project, psiMethodCallExpression)
                }
                if (methodExpression.referenceName == "orderDesc") {
                    processOrderDesc(project, psiMethodCallExpression)
                }
                if (methodExpression.referenceName == "setSqlSelect") {
                    processSetSelect(project, psiMethodCallExpression)
                }
                super.visitMethodCallExpression(psiMethodCallExpression)
            }


        })
    }

    private fun processOrderAsc(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val methodExpression = psiMethodCallExpression.methodExpression

        val filter = methodExpression.children.filterIsInstance<PsiIdentifier>()


        if (filter.size == 1) {
            val psiIdentifier = filter[0]

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("orderByAsc")
            WriteCommandAction.runWriteCommandAction(project) {
                psiIdentifier.replace(createIdentifier)
            }
        }
    }

    private fun processSetSelect(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val methodExpression = psiMethodCallExpression.methodExpression

        val filter = methodExpression.children.filterIsInstance<PsiIdentifier>()


        if (filter.size == 1) {
            val psiIdentifier = filter[0]

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("select")
            WriteCommandAction.runWriteCommandAction(project) {
                psiIdentifier.replace(createIdentifier)
            }
        }
    }

    private fun processOrderDesc(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val methodExpression = psiMethodCallExpression.methodExpression

        val filter = methodExpression.children.filterIsInstance<PsiIdentifier>()

        if (filter.size == 1) {
            val psiIdentifier = filter[0]

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("orderByDesc")
            WriteCommandAction.runWriteCommandAction(project) {
                psiIdentifier.replace(createIdentifier)
            }
        }
    }

    private fun processOrderBy(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val argumentList = psiMethodCallExpression.argumentList

        if (argumentList.expressionCount == 1) {
            var temp = ""
            for (expression in argumentList.expressions) {
                val type = expression.type
                if (type == null) {
                    temp = ("$temp;")
                } else {
                    temp = (temp + type.canonicalText + ";")
                }

                temp = (temp + type?.canonicalText + ";")
            }

            if ("java.lang.String;" == temp) {

                val psiExpressions = ArrayList<PsiElement>()

                for (expression in argumentList.expressions) {
                    psiExpressions.add(expression.copy())
                }

                WriteCommandAction.runWriteCommandAction(project) {
                    argumentList.expressions.forEach { it.delete() }

                    val trueExpression =
                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText("true", null)
                    argumentList.add(trueExpression)
                    argumentList.add(trueExpression)
                    argumentList.add(psiExpressions[0])
                }
            }
        }

        if (argumentList.expressionCount == 2) {
            var temp = ""
            for (expression in argumentList.expressions) {
                val type = expression.type
                if (type == null) {
                    temp = ("$temp;")
                } else {
                    temp = (temp + type.canonicalText + ";")
                }

                temp = (temp + type?.canonicalText + ";")
            }


            if ("java.lang.String;boolean;" == temp) {

                val psiExpressions = ArrayList<PsiElement>()

                for (expression in argumentList.expressions) {
                    psiExpressions.add(expression.copy())
                }

                WriteCommandAction.runWriteCommandAction(project) {
                    argumentList.expressions.forEach { it.delete() }

                    val trueExpression =
                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText("true", null)
                    argumentList.add(trueExpression)
                    argumentList.add(psiExpressions[1])
                    argumentList.add(psiExpressions[0])
                }
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
