package com.github.hackerorange.glodonframeworkupgradeplugin.inspections

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class OrderByInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(problemsHolder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        val project = problemsHolder.project

        val baseWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.conditions.Wrapper",
            GlobalSearchScope.allScope(project)
        )


        return object : JavaElementVisitor() {


            override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(methodCallExpression)

                if (baseWrapperClass == null) {
                    return
                }

                if (methodCallExpression.methodExpression.referenceName == "orderBy") {
                    val type = methodCallExpression.methodExpression.qualifierExpression?.type ?: return
                    if (type is PsiClassType) {
                        val psiClass = type.resolve() ?: return
                        if (psiClass.isInheritor(baseWrapperClass, true)) {
                            var signature = ""
                            val argumentList = methodCallExpression.argumentList
                            for (expression in argumentList.expressions) {
                                var tempType = expression.type
                                if (tempType is PsiPrimitiveType) {
                                    tempType = tempType.getBoxedType(expression)
                                }

                                val currentTypeCanonicalText = tempType?.canonicalText

                                signature = ("$signature$currentTypeCanonicalText;")
                            }
                            //
                            if (signature == "java.lang.Boolean;java.lang.Boolean;java.lang.String;") {
                                val psiExpression = argumentList.expressions[1]

                                if (psiExpression.text == "true" || psiExpression.text == "Boolean.TRUE") {

                                    problemsHolder.registerProblem(
                                        methodCallExpression.methodExpression.referenceNameElement!!,
                                        "替换为 orderByAsc 方法调用",
                                        ReplaceWithOrderByAsc()
                                    )
                                }
                                if (psiExpression.text == "false" || psiExpression.text == "Boolean.FALSE") {

                                    problemsHolder.registerProblem(
                                        methodCallExpression.methodExpression.referenceNameElement!!,
                                        "替换为 orderByDesc 方法调用",
                                        ReplaceWithOrderByDesc()
                                    )
                                }
                            }
                        }
                    }
                }

                if (methodCallExpression.methodExpression.referenceName == "orderByAsc" || methodCallExpression.methodExpression.referenceName == "orderByDesc") {
                    val type = methodCallExpression.methodExpression.qualifierExpression?.type ?: return
                    if (type is PsiClassType) {
                        val psiClass = type.resolve() ?: return
                        if (psiClass.isInheritor(baseWrapperClass, true)) {
                            var signature = ""
                            val argumentList = methodCallExpression.argumentList
                            for (expression in argumentList.expressions) {
                                var tempType = expression.type
                                if (tempType is PsiPrimitiveType) {
                                    tempType = tempType.getBoxedType(expression)
                                }

                                val currentTypeCanonicalText = tempType?.canonicalText

                                signature = ("$signature$currentTypeCanonicalText;")
                            }
                            //
                            if (signature == "java.lang.Boolean;java.lang.String;") {
                                val psiExpression: PsiExpression = argumentList.expressions[0]

                                if (psiExpression.text == "true" || psiExpression.text == "Boolean.TRUE") {

                                    problemsHolder.registerProblem(
                                        psiExpression,
                                        "此条件参数永远为 true ，移除此条件参数",
                                        RemoveConditionParamForOrderBy()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    class ReplaceWithOrderByAsc : LocalQuickFix {
        override fun getFamilyName(): String {
            return "替换为 orderByAsc 方法调用"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val referenceNameElement: PsiIdentifier = descriptor.psiElement as PsiIdentifier

            val psiMethodCallExpression =
                PsiTreeUtil.getParentOfType(referenceNameElement, PsiMethodCallExpression::class.java) ?: return

            psiMethodCallExpression.argumentList.expressions[1].delete()
            referenceNameElement.replace(JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("orderByAsc"))

        }


    }

    class ReplaceWithOrderByDesc : LocalQuickFix {
        override fun getFamilyName(): String {
            return "替换为 orderByDesc 方法调用"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val referenceNameElement: PsiIdentifier = descriptor.psiElement as PsiIdentifier

            val psiMethodCallExpression =
                PsiTreeUtil.getParentOfType(referenceNameElement, PsiMethodCallExpression::class.java) ?: return

            psiMethodCallExpression.argumentList.expressions[1].delete()
            referenceNameElement.replace(JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("orderByDesc"))
        }
    }

    class RemoveConditionParamForOrderBy : LocalQuickFix {
        override fun getFamilyName(): String {
            return "此条件参数永远为 true ，移除此条件参数"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression: PsiExpression = descriptor.psiElement as PsiExpression
            expression.delete()
        }
    }

}