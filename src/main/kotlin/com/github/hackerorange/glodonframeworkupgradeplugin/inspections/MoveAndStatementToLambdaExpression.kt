package com.github.hackerorange.glodonframeworkupgradeplugin.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase
import com.intellij.refactoring.util.CommonRefactoringUtil

private const val QUERY_WRAPPER_QNAME = "com.baomidou.mybatisplus.core.conditions.query.QueryWrapper"

class MoveAndStatementToLambdaExpression : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(problemsHolder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        val project = problemsHolder.project

        val queryWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.conditions.query.QueryWrapper",
            GlobalSearchScope.allScope(project)
        )

        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(methodCallExpression: PsiMethodCallExpression) {


                processAndNew(methodCallExpression)
                processOrMethodCall(methodCallExpression)


                super.visitMethodCallExpression(methodCallExpression)
            }

            private fun processAndNew(
                methodCallExpression: PsiMethodCallExpression
            ) {
                val methodExpression = methodCallExpression.methodExpression

                if ("andNew" != methodExpression.referenceName) {
                    return
                }

                val reference = methodExpression.qualifierExpression
                    ?: return

                val isQueryWrapper =
                    reference.type?.let { InheritanceUtil.isInheritor(it, QUERY_WRAPPER_QNAME) }

                if (isQueryWrapper != true) {
                    return
                }

                val psiIdentifier = methodExpression.referenceNameElement
                    ?: return

                if (!methodCallExpression.argumentList.isEmpty) {
                    problemsHolder.registerProblem(
                        psiIdentifier,
                        "Wrap lambda expression ",
                        ProblemHighlightType.ERROR,
                        IntroduceVariableErrorFixAction1(),
                        IntroduceVariableErrorFixAction3(),
                        IntroduceVariableErrorFixAction(methodCallExpression)
                    )
                } else {
                    problemsHolder.registerProblem(
                        psiIdentifier,
                        "Wrap lambda expression ",
                        ProblemHighlightType.ERROR,
                        IntroduceVariableErrorFixAction(methodCallExpression)
                    )
                }



                return
            }


            private fun processOrMethodCall(methodCallExpression: PsiMethodCallExpression) {
                val methodExpression = methodCallExpression.methodExpression

                if ("or" != methodExpression.referenceName) {
                    return
                }

                val reference = methodExpression.qualifierExpression
                    ?: return

                var isQueryWrapper = false;
                if (reference.type != null) {
                    isQueryWrapper = InheritanceUtil.isInheritor(reference.type, QUERY_WRAPPER_QNAME)
                }
                if (isQueryWrapper != true) {
                    return
                }
                val argumentList = methodCallExpression.argumentList

                if (argumentList.isEmpty) {
                    return
                }

                if (argumentList.expressions.any { it is PsiLambdaExpression }) {
                    return
                }

                val psiIdentifier = methodExpression.referenceNameElement
                    ?: return

                if (!methodCallExpression.argumentList.isEmpty) {
                    problemsHolder.registerProblem(
                        psiIdentifier,
                        "Wrap lambda expression ",
                        ProblemHighlightType.ERROR,
                        IntroduceVariableErrorFixAction2(),
                    )
                }

                return
            }

        }
    }

    class IntroduceVariableErrorFixAction1 :
        LocalQuickFix {


        override fun startInWriteAction(): Boolean {
            return false
        }

        override fun getFamilyName(): String {
            return "[Upgrade MyBatis Plus] 将当前语句，作为Lambda表达式，添加到 and 语句中"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            val psiElement = descriptor.psiElement

            val psiMethodCallExpression =
                PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression::class.java) ?: return

            val replacedExpression = psiMethodCallExpression.copy() as PsiMethodCallExpression

            println(replacedExpression.methodExpression.referenceNameElement)

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("and")

            replacedExpression.methodExpression.referenceNameElement?.replace(createIdentifier)

            var conditionExpression = JavaPsiFacade
                .getInstance(project)
                .elementFactory
                .createExpressionFromText("true", null)

            if (replacedExpression.argumentList.isEmpty.not()) {
                val psiExpression: PsiExpression = replacedExpression.argumentList.expressions[0]
                if (isBooleanType(psiExpression.type)) {
                    conditionExpression = psiExpression.copy() as PsiExpression
                    psiExpression.delete()
                }
            }

            val expressionInLambda = replacedExpression.argumentList.text

            replacedExpression.argumentList.expressions.forEach { it.delete() }

            replacedExpression.argumentList.add(conditionExpression)

            replacedExpression.argumentList.add(
                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                    "qw->qw.apply${expressionInLambda}",
                    null
                )
            )
            WriteCommandAction.runWriteCommandAction(project) {
                psiMethodCallExpression.replace(replacedExpression)
            }

        }
    }

    class IntroduceVariableErrorFixAction3 :
        LocalQuickFix {


        override fun startInWriteAction(): Boolean {
            return false
        }

        override fun getFamilyName(): String {
            return "[Upgrade MyBatis Plus] 将当前语句,直接修改为 apply 语句"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            val psiElement = descriptor.psiElement

            val psiMethodCallExpression =
                PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression::class.java) ?: return

            val replacedExpression = psiMethodCallExpression.copy() as PsiMethodCallExpression

            println(replacedExpression.methodExpression.referenceNameElement)

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("apply")

            replacedExpression.methodExpression.referenceNameElement?.replace(createIdentifier)

            WriteCommandAction.runWriteCommandAction(project) {
                psiMethodCallExpression.replace(replacedExpression)
            }
        }
    }


    class IntroduceVariableErrorFixAction2 :
        LocalQuickFix {


        override fun startInWriteAction(): Boolean {
            return false
        }

        override fun getFamilyName(): String {
            return "[Upgrade MyBatis Plus] 将当前语句，作为Lambda表达式，添加到 or 语句中"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            val psiElement = descriptor.psiElement

            val psiMethodCallExpression =
                PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression::class.java) ?: return

            val replacedExpression = psiMethodCallExpression.copy() as PsiMethodCallExpression

            println(replacedExpression.methodExpression.referenceNameElement)

            val createIdentifier = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("or")

            replacedExpression.methodExpression.referenceNameElement?.replace(createIdentifier)

            var conditionExpression = JavaPsiFacade
                .getInstance(project)
                .elementFactory
                .createExpressionFromText("true", null)

            if (replacedExpression.argumentList.isEmpty.not()) {
                val psiExpression: PsiExpression = replacedExpression.argumentList.expressions[0]
                if (isBooleanType(psiExpression.type)) {
                    conditionExpression = psiExpression.copy() as PsiExpression
                    psiExpression.delete()
                }
            }

            val expressionInLambda = replacedExpression.argumentList.text

            replacedExpression.argumentList.expressions.forEach { it.delete() }

            replacedExpression.argumentList.add(conditionExpression)

            replacedExpression.argumentList.add(
                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                    "qw->qw.apply${expressionInLambda}",
                    null
                )
            )
            WriteCommandAction.runWriteCommandAction(project) {
                psiMethodCallExpression.replace(replacedExpression)
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

            return "[Upgrade MyBatis Plus] 选择此条语句以及后面的语句，作为Lambda表达式，添加到 and 语句中"
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


            val oldArgumentList = replacedExpression.argumentList.copy() as PsiExpressionList

            replacedExpression.argumentList.expressions.forEach { it.delete() }
            replacedExpression.argumentList.add(
                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                    "true",
                    null
                )
            )

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


    }

    companion object {
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

}
