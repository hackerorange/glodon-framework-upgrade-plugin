package com.github.hackerorange.glodonframeworkupgradeplugin.inspections

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase
import com.intellij.refactoring.util.CommonRefactoringUtil

class MoveAndStatementToLambdaExpression : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(problemsHolder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {
                val methodExpression = methodCallExpression.methodExpression

                if ("andNew" == methodExpression.referenceName) {

                    val psiIdentifier =
                        methodExpression.children.filterIsInstance<PsiIdentifier>().firstOrNull() ?: return

                    problemsHolder.registerProblem(
                        psiIdentifier,
                        "Wrap lambda expression ",
                        IntroduceVariableErrorFixAction(methodCallExpression)
                    )
                }


                super.visitMethodCallExpression(methodCallExpression)
            }
        }
    }

    class IntroduceVariableErrorFixAction(expression: PsiExpression) :
        LocalQuickFixAndIntentionActionOnPsiElement(expression) {
        override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            if (editor == null) {
                return
            }

            if (!CommonRefactoringUtil.checkReadOnlyStatus(project, file)) {
                return
            }

            PsiDocumentManager.getInstance(project).commitAllDocuments()

            val caretCount = editor.caretModel.caretCount
            if (caretCount != 1) {
                return
            }

            val myHandler = MyHandler(startElement as PsiMethodCallExpression)
            myHandler.invoke(project, editor, startElement.containingFile, null)
        }

        override fun startInWriteAction(): Boolean {
            return false
        }

        override fun getText(): String {

            return "测试折叠代码"
        }

        override fun getFamilyName(): String {
            return text
        }
    }

    class MyHandler(private val psiMethodCallExpression: PsiMethodCallExpression) : IntroduceVariableBase() {
        override fun showErrorMessage(project: Project?, editor: Editor?, message: String?) {
            println("发生异常")
        }

        override fun invokeImpl(project: Project, psiExpression: PsiExpression, editor: Editor?): Boolean {

            if (psiExpression !is PsiMethodCallExpression) {
                return true
            }

            extracted(project, psiExpression, psiMethodCallExpression)

            return true
        }

        private fun extracted(
            project: Project,
            longPsiMethodCallExpression: PsiMethodCallExpression,
            shortPsiMethodCallExpression: PsiMethodCallExpression
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

            replacedExpression.argumentList.add(
                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                    "true",
                    null
                )
            )

            val expressionInLambda = selectText.replace(subText, "")

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

    }


}
