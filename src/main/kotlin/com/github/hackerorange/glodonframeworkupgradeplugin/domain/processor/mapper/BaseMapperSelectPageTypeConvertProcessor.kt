package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class BaseMapperSelectPageTypeConvertProcessor : PsiFileProcessor {

    private var mybatisPlusPageClass: PsiClass? = null
    private var listClass: PsiClass? = null

    override fun processPsiFile(project: Project, psiFile: PsiFile) {
        if (mybatisPlusPageClass == null || listClass == null) {
            return
        }

        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {

                val lExpression = expression.lExpression
                val rExpression = expression.rExpression ?: return
                getRecordsOfPageIfMatched(lExpression.type ?: return, rExpression.type ?: return, project, rExpression)

                // 先处理好语句后，再做判断
                super.visitAssignmentExpression(expression)
            }

            override fun visitReturnStatement(psiReturnStatement: PsiReturnStatement) {

                super.visitReturnStatement(psiReturnStatement)

                var psiElement: PsiElement = psiReturnStatement

                while (psiElement !is PsiMethod) {
                    psiElement = psiElement.parent

                    if (psiElement == null) {
                        return
                    }

                    if (psiElement is PsiClass) {
                        return
                    }

                    if (psiElement is PsiLambdaExpression) {
                        return
                    }

                    if (psiElement is PsiMethod) {
                        val lType = psiElement.returnType ?: return
                        val returnValue = psiReturnStatement.returnValue ?: return
                        getRecordsOfPageIfMatched(lType, returnValue.type ?: return, project, returnValue)
                    }
                }
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
                        getRecordsOfPageIfMatched(variableType, initialType, project, declaredElement.initializer!!)
                    }
                }
            }
        })

    }

    private fun getRecordsOfPageIfMatched(
        leftType: PsiType,
        rightType: PsiType,
        project: Project,
        rightExpression: PsiExpression
    ) {
        if (leftType == rightType) {
            return
        }
        if (rightExpression !is PsiMethodCallExpression) {
            return
        }
        val referenceName = rightExpression.methodExpression.referenceName
//        if (referenceName != "selectPage") {
//            return
//        }
        if (leftType is PsiClassType && rightType is PsiClassType) {


            val variableTypeClass = leftType.resolve() ?: return
            val initialTypeClass = rightType.resolve() ?: return


            val isVariableList =
                variableTypeClass == listClass || variableTypeClass.isInheritor(listClass!!, true)
            val isInitialPage =
                initialTypeClass == mybatisPlusPageClass || initialTypeClass.isInheritor(
                    mybatisPlusPageClass!!,
                    true
                )
            if (isVariableList && isInitialPage) {
                WriteCommandAction.runWriteCommandAction(project) {
                    val methodNewIdentity =
                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                            "${rightExpression.text}.getRecords()", null
                        )
                    rightExpression.replace(methodNewIdentity);
                }
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
