package com.github.hackerorange.glodonframeworkupgradeplugin.inspections

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase
import com.intellij.refactoring.util.CommonRefactoringUtil

class MoveAndStatementToLambdaExpression : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(problemsHolder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        println("112321313")

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

            val myHandler = MyHandler()
            myHandler.invoke(project, startElement, editor)
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

    class MyHandler : IntroduceVariableBase() {
        override fun showErrorMessage(project: Project?, editor: Editor?, message: String?) {
            println("发生异常")
        }

        override fun invokeImpl(project: Project?, expr: PsiExpression?, editor: Editor?): Boolean {
            println(expr?.text)
            return true
        }

    }


}
