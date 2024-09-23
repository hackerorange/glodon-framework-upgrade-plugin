package com.github.hackerorange.glodonframeworkupgradeplugin.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

class ExpressionIntentionAction : PsiElementBaseIntentionAction() {
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {

        val selectionModel = editor.selectionModel
        val s = selectionModel.selectedText ?: return

        if (s.isEmpty()) {
            return
        }
        val shortMethod: PsiMethodCallExpression =
            findMethodCallExpressionByOffset(element, selectionModel.selectionStart)
                ?: return
        val longMethod: PsiMethodCallExpression = findMethodCallExpressionByOffset(element, selectionModel.selectionEnd)
            ?: return

        processAndLambdaExpression(project, shortMethod, longMethod)

        println(shortMethod)
    }


    private fun findMethodCallExpressionByOffset(element: PsiElement, offset: Int): PsiMethodCallExpression? {
        val containingFile = element.containingFile

        val viewProvider: FileViewProvider = containingFile.viewProvider

        val startElement = viewProvider.findElementAt(offset, containingFile.language)


        if (startElement != null) {
            val psiMethodCallExpression = PsiTreeUtil.getParentOfType(startElement, PsiMethodCallExpression::class.java)
            if (psiMethodCallExpression != null) {
                return psiMethodCallExpression
            }
        }
        return null
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val selectionModel = editor.selectionModel
        val s = selectionModel.selectedText ?: return false

        if (s.length == 0) {
            return false
        }

        val file = element.containingFile

        val viewProvider: FileViewProvider = file.viewProvider


        val startElement = viewProvider.findElementAt(selectionModel.selectionStart, file.language)
        if (startElement != null) {
            val psiMethodCallExpression = PsiTreeUtil.getParentOfType(startElement, PsiMethodCallExpression::class.java)
            if (psiMethodCallExpression != null) {
                if (psiMethodCallExpression.methodExpression.referenceName == "andNew") {
                    return true
                }
            }
        }
        return false
    }

    private fun processAndLambdaExpression(
        project: Project,
        shortPsiMethodCallExpression: PsiMethodCallExpression,
        longPsiMethodCallExpression: PsiMethodCallExpression
    ) {

        val selectText = longPsiMethodCallExpression.text
            ?: return
        val subText: String = shortPsiMethodCallExpression.text

        if (shortPsiMethodCallExpression == longPsiMethodCallExpression) {
            return
        }
        if (!selectText.contains(subText)) {
            return
        }
        val replacedExpression = shortPsiMethodCallExpression.copy() as PsiMethodCallExpression

        println(replacedExpression.methodExpression.referenceNameElement)

        val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("and")

        replacedExpression.methodExpression.referenceNameElement?.replace(createIdentifier)


        val oldArgumentList = replacedExpression.argumentList.copy() as PsiExpressionList

        replacedExpression.argumentList.expressions.forEach { it.delete() }

        var conditionExpression = JavaPsiFacade
            .getInstance(project)
            .elementFactory
            .createExpressionFromText("true", null)

        if (oldArgumentList.isEmpty.not()) {
            val psiExpression: PsiExpression = oldArgumentList.expressions[0]
            if (isBooleanType(psiExpression.type)) {
                conditionExpression = psiExpression.copy() as PsiExpression
                psiExpression.delete()
            }
        }

        replacedExpression.argumentList.expressions.forEach { it.delete() }
        replacedExpression.argumentList.add(conditionExpression)

        var expressionInLambda = selectText.replace(subText, "")

        if (oldArgumentList.isEmpty.not()) {
            expressionInLambda = ".apply${oldArgumentList.text}$expressionInLambda"
        }


        replacedExpression.argumentList.add(
            JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                "qw->qw${expressionInLambda}",
                null
            )
        )

        WriteCommandAction.runWriteCommandAction(project) {
            longPsiMethodCallExpression.replace(replacedExpression)
        }
    }


    override fun getFamilyName(): @IntentionFamilyName String {
        return "[SOHO] 将这些条件添加到 add 语句中"
    }

    override fun getText(): String {
        return familyName
    }


    private fun isBooleanType(type: PsiType?): Boolean {
        if (type is PsiPrimitiveType && type.name == "boolean") {
            return true
        }
        if (type is PsiClassType && type.className == "Boolean") {
            return true
        }
        return false
    }


}
