package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil

private const val PAGE_QNAME = "com.baomidou.mybatisplus.core.metadata.IPage"

class BaseMapperSelectPageTypeConvertProcessor : PsiFileProcessor {

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

        val methodCallStatementReplaceInfos = prepareMethodCallExpressionWhichNeedReplace(psiFile, project)

        WriteCommandAction.runWriteCommandAction(project) {
            methodCallStatementReplaceInfos.forEach {
                it.oldMethodCallExpression.replace(it.newMethodCallExpression)
            }
        }

    }

    private fun prepareMethodCallExpressionWhichNeedReplace(
        psiFile: PsiFile,
        project: Project
    ): ArrayList<MethodCallStatementReplaceInfo> {
        val methodCallStatementReplaceInfos = ArrayList<MethodCallStatementReplaceInfo>()

        ApplicationManager.getApplication().runReadAction {
            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {

                    val lExpression = expression.lExpression
                    val rExpression = expression.rExpression ?: return
                    getRecordsOfPageIfMatched(
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
                            getRecordsOfPageIfMatched(
                                lType,
                                returnValue.type ?: return,
                                project,
                                returnValue,
                                methodCallStatementReplaceInfos
                            )
                        }
                    }
                }

                override fun visitMethodCallExpression(psiMethodCallExpression: PsiMethodCallExpression) {

                    val referenceName = psiMethodCallExpression.methodExpression.referenceName

                    if ("stream" == referenceName || "forEach" == referenceName) {
                        val qualifierExpression = psiMethodCallExpression.methodExpression.qualifierExpression ?: return
                        val isPage =
                            qualifierExpression.type?.let { InheritanceUtil.isInheritor(it, PAGE_QNAME) } ?: false

                        if (isPage) {
                            val createExpressionFromText =
                                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                    "${qualifierExpression.text}.getRecords()",
                                    null
                                )

                            methodCallStatementReplaceInfos.add(
                                MethodCallStatementReplaceInfo(
                                    qualifierExpression,
                                    createExpressionFromText
                                )
                            )
                        }
                    }

                    if ("getCurrent" == referenceName || "getTotal" == referenceName || "getSize" == referenceName) {
                        val qualifierExpression = psiMethodCallExpression.methodExpression.qualifierExpression ?: return
                        val isPage =
                            qualifierExpression.type?.let { InheritanceUtil.isInheritor(it, PAGE_QNAME) } ?: false

                        if (isPage) {

                            val parentOfType =
                                PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiTypeCastExpression::class.java)

                            if (parentOfType == null) {
                                val createExpressionFromText =
                                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                        "(int) (${psiMethodCallExpression.text})",
                                        null
                                    )

                                methodCallStatementReplaceInfos.add(
                                    MethodCallStatementReplaceInfo(
                                        psiMethodCallExpression,
                                        createExpressionFromText
                                    )
                                )
                            }
                        }
                    }




                    super.visitMethodCallExpression(psiMethodCallExpression)
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
                            getRecordsOfPageIfMatched(
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
        return methodCallStatementReplaceInfos
    }

    private fun getRecordsOfPageIfMatched(
        leftType: PsiType,
        rightType: PsiType,
        project: Project,
        rightExpression: PsiExpression,
        methodCallStatementReplaceInfos: ArrayList<MethodCallStatementReplaceInfo>
    ) {
        if (leftType == rightType) {
            return
        }
        if (rightExpression !is PsiMethodCallExpression) {
            return
        }
//        val referenceName = rightExpression.methodExpression.referenceName
//        if (referenceName != "selectPage") {
//            return
//        }
        if (leftType !is PsiClassType || rightType !is PsiClassType) {
            return
        }
        val variableTypeClass = leftType.resolve()
            ?: return

        val initialTypeClass = rightType.resolve()
            ?: return


        if (variableTypeClass != listClass && !variableTypeClass.isInheritor(listClass!!, true)) {
            return
        }
        if (initialTypeClass != mybatisPlusPageClass && !initialTypeClass.isInheritor(mybatisPlusPageClass!!, true)) {
            return
        }
        val newMethodCallExpression =
            JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                "${rightExpression.text}.getRecords()", null
            )

        methodCallStatementReplaceInfos.add(
            MethodCallStatementReplaceInfo(
                rightExpression,
                newMethodCallExpression
            )
        )
    }


    override fun init(project: Project) {
        mybatisPlusPageClass = JavaPsiFacade.getInstance(project).findClass(
            PAGE_QNAME,
            GlobalSearchScope.allScope(project)
        )
        listClass = JavaPsiFacade.getInstance(project).findClass(
            "java.util.List",
            GlobalSearchScope.allScope(project)
        )
    }

}
