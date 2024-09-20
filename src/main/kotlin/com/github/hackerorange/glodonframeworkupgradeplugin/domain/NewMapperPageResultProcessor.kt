package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class NewMapperPageResultProcessor : PsiFileProcessor {

    private var newBaseMapper: PsiClass? = null


    override fun processPsiFile(project: Project, psiFile: PsiFile) {
        if (newBaseMapper == null) {
            return
        }

        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitMethodCallExpression(psiMethodCallExpression: PsiMethodCallExpression) {

                val resolveMethod = psiMethodCallExpression.resolveMethod()

                if (resolveMethod != null) {
                    if (newBaseMapper == resolveMethod.containingClass) {
                        if (psiMethodCallExpression.methodExpression.referenceName == "selectPage") {
                            WriteCommandAction.runWriteCommandAction(project) {
                                val methodNewIdentity =
                                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                        "${psiMethodCallExpression.text}.getRecords()",
                                        psiMethodCallExpression
                                    )
                                psiMethodCallExpression.replace(methodNewIdentity);
                            }
                        }
                    }
                }

                super.visitMethodCallExpression(psiMethodCallExpression)
            }
        })

    }


    override fun init(project: Project) {
        newBaseMapper = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.mapper.BaseMapper",
            GlobalSearchScope.allScope(project)
        )
    }

}
