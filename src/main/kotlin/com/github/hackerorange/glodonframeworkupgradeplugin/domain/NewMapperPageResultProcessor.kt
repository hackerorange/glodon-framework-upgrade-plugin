package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class NewMapperPageResultProcessor : PsiFileProcessor {

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
                super.visitAssignmentExpression(expression)
            }

            override fun visitReturnStatement(psiReturnStatement: PsiReturnStatement) {

                val psiMethod = PsiTreeUtil.getParentOfType(psiReturnStatement, PsiMethod::class.java) ?: return

                val lType = psiMethod.returnType ?: return

                val returnValue = psiReturnStatement.returnValue ?: return

                getRecordsOfPageIfMatched(lType, returnValue.type ?: return, project, returnValue)



                super.visitReturnStatement(psiReturnStatement)
            }


            override fun visitDeclarationStatement(statestatementment: PsiDeclarationStatement) {

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
                super.visitDeclarationStatement(statestatementment)
            }
        })

    }

    private fun getRecordsOfPageIfMatched(
        leftType: PsiType,
        rightType: PsiType,
        project: Project,
        rightExpression: PsiExpression
    ) {
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
