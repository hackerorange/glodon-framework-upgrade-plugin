package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class BaseMapperSelectCountConvertProcessor : PsiFileProcessor {
    class MethodCallStatementReplaceInfo(
        val oldMethodCallExpression: PsiElement,
        val newMethodCallExpression: PsiElement
    )

    private var mybatisPlusPageClass: PsiClass? = null
    private var listClass: PsiClass? = null

    override fun processPsiFile(project: Project, psiFile: PsiFile) {
        if (mybatisPlusPageClass == null || listClass == null) {
            return
        }

        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()

        ApplicationManager.getApplication().runReadAction {
            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {

                    val lExpression = expression.lExpression
                    val rExpression = expression.rExpression ?: return
                    convertLongToIntIfMatched(
                        lExpression.type ?: return,
                        rExpression.type ?: return,
                        project,
                        rExpression,
                        methodCallStatementReplaceInfos
                    )

                    // 先处理好语句后，再做判断
                    super.visitAssignmentExpression(expression)
                }

                override fun visitReturnStatement(psiReturnStatement: PsiReturnStatement) {

                    super.visitReturnStatement(psiReturnStatement)

                    val psiMethod = PsiTreeUtil.getParentOfType(psiReturnStatement, PsiMethod::class.java) ?: return

                    val lType = psiMethod.returnType ?: return

                    val returnValue = psiReturnStatement.returnValue ?: return

                    convertLongToIntIfMatched(
                        lType,
                        returnValue.type ?: return,
                        project,
                        returnValue,
                        methodCallStatementReplaceInfos
                    )
                }

                override fun visitClass(aClass: PsiClass) {
                    super.visitClass(aClass)
                }

                override fun visitLambdaExpression(lambdaExpression: PsiLambdaExpression) {
                    super.visitLambdaExpression(lambdaExpression)
                }

                override fun visitDeclarationStatement(statestatementment: PsiDeclarationStatement) {

                    super.visitDeclarationStatement(statestatementment)

                    for (declaredElement in statestatementment.declaredElements) {

                        if (declaredElement is PsiLocalVariable) {
                            val variableType = declaredElement.type
                            val psiExpression = declaredElement.initializer ?: continue

                            val initialType = psiExpression.type ?: return
                            if (initialType == variableType) {
                                continue
                            }
                            convertLongToIntIfMatched(
                                variableType,
                                initialType,
                                project,
                                declaredElement.initializer!!,
                                methodCallStatementReplaceInfos
                            )
                        }
                    }
                }
            })
        }

        WriteCommandAction.runWriteCommandAction(project) {
            methodCallStatementReplaceInfos.forEach {
                it.oldMethodCallExpression.replace(it.newMethodCallExpression)
            }
        }

    }


    private fun convertLongToIntIfMatched(
        leftType: PsiType,
        rightType: PsiType,
        project: Project,
        rightExpression: PsiExpression,
        methodCallStatementReplaceInfos: ArrayList<MethodCallStatementReplaceInfo>
    ) {
        if (leftType == rightType) {
            return
        }

        // Integer a =(int) long b
        if (rightType is PsiPrimitiveType && rightType.name == "long") {
            val isResultTypeInteger =
                (leftType is PsiPrimitiveType && leftType.name == "int") || (leftType is PsiClassType && leftType.canonicalText == "java.lang.Integer")
            if (!isResultTypeInteger) {
                return
            }
            val methodNewIdentity =
                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                    "(int) ${rightExpression.text}", null
                ) as PsiMethodCallExpression

            methodCallStatementReplaceInfos.add(MethodCallStatementReplaceInfo(rightExpression, methodNewIdentity))
            return
        }
        if (rightExpression !is PsiMethodCallExpression) {
            return
        }

        if (rightType is PsiClassType && rightType.canonicalText == "java.lang.Long") {

            // int a == Optional.ofNullable(Long b).map(Long::intValue).orElse(0)
            if (leftType is PsiPrimitiveType && leftType.name == "int") {
                val newMethodCallExpression =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                        "java.util.Optional.ofNullable(${rightExpression.text}).map(Long::intValue).orElse(0)",
                        null
                    )
                methodCallStatementReplaceInfos.add(
                    MethodCallStatementReplaceInfo(
                        rightExpression,
                        newMethodCallExpression
                    )
                )
                return
            }

            // Integer a == Optional.ofNullable(Long b).map(Long::intValue).orElse(0)
            if (leftType is PsiClassType && leftType.canonicalText == "java.lang.Integer") {
                val newMethodCallExpression =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                        "java.util.Optional.ofNullable(${rightExpression.text}).map(Long::intValue).orElse(0)",
                        null
                    )
                methodCallStatementReplaceInfos.add(
                    MethodCallStatementReplaceInfo(
                        rightExpression,
                        newMethodCallExpression
                    )
                )
                return
            }
        }


    }


    override fun init(project: Project) {
        mybatisPlusPageClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.metadata.IPage",
            GlobalSearchScope.allScope(project)
        )
        listClass = JavaPsiFacade.getInstance(project).findClass(
            "java.util.List",
            GlobalSearchScope.allScope(project)
        )
    }

}
