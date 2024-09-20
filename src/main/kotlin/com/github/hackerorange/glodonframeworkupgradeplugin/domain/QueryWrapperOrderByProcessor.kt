package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class QueryWrapperOrderByProcessor : PsiFileProcessor {

    private var entityWrapperClass: PsiClass? = null
    private val importReplace = HashMap<String, String>()

    init {
        importReplace["com.baomidou.mybatisplus.mapper.BaseMapper"] =
            "com.baomidou.mybatisplus.core.mapper.BaseMapper"
        importReplace["com.baomidou.mybatisplus.plugins.Page"] =
            "com.baomidou.mybatisplus.extension.plugins.pagination.Page"
    }

    override fun processPsiFile(project: Project, psiFile: PsiFile) {
        val classQNameToReplaceClassMap: HashMap<String, PsiClass> = HashMap()
        for (mutableEntry in importReplace) {
            val newBaseMapperClass = JavaPsiFacade.getInstance(project).findClass(
                mutableEntry.value,
                GlobalSearchScope.allScope(project)
            )

            if (newBaseMapperClass != null) {
                classQNameToReplaceClassMap[mutableEntry.key] = newBaseMapperClass
            }
        }


        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitMethodCallExpression(psiMethodCallExpression: PsiMethodCallExpression) {

                val methodExpression = psiMethodCallExpression.methodExpression
                if (methodExpression.referenceName == "orderBy") {
                    processOrderBy(project, psiMethodCallExpression)
                }
                super.visitMethodCallExpression(psiMethodCallExpression)
            }


        })
    }

    private fun processOrderBy(project: Project, psiMethodCallExpression: PsiMethodCallExpression) {
        val argumentList = psiMethodCallExpression.argumentList

        if (argumentList.expressionCount == 1) {
            var aaa = ""
            for (expression in argumentList.expressions) {
                aaa = (aaa + expression.type?.canonicalText + ";")
            }
            println(aaa)

            if ("java.lang.String;" == aaa) {

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
            var aaa = ""
            for (expression in argumentList.expressions) {
                aaa = (aaa + expression.type?.canonicalText + ";")
            }
            println(aaa)

            if ("java.lang.String;boolean;" == aaa) {

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

        if (argumentList.expressionCount == 3) {
            var aaa = ""
            for (expression in argumentList.expressions) {
                aaa = (aaa + expression.type?.canonicalText + ";")
            }
            println(aaa)
        }
    }

    override fun init(project: Project) {
        entityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.mapper.EntityWrapper",
            GlobalSearchScope.allScope(project)
        )
    }

}
