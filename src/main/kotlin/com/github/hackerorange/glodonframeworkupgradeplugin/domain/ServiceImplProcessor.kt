package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ServiceImplProcessor : PsiFileProcessor {

    private var oldServiceImplClass: PsiClass? = null
    private var newServiceImplClass: PsiClass? = null


    override fun processPsiFile(project: Project, psiFile: PsiFile) {
        if (oldServiceImplClass == null || newServiceImplClass == null) {
            return
        }
        if (psiFile is PsiJavaFile) {

            for (psiClass in psiFile.classes) {

                if (psiClass.isInheritor(oldServiceImplClass!!, true)) {

                    psiClass.accept(object : JavaRecursiveElementVisitor() {

                        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {

                            val resolveMethod = expression.resolveMethod()

                            if (resolveMethod != null) {
                                if (oldServiceImplClass == resolveMethod.containingClass) {
                                    println(resolveMethod)
                                    if (expression.methodExpression.referenceName == "insert") {

                                        processServiceInsertMethodCall(project, expression)

                                    }

                                }
                            }

                            super.visitMethodCallExpression(expression)
                        }
                    })
                }
            }


            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitImportStatement(statement: PsiImportStatement) {


                    if (statement.text == "import ${oldServiceImplClass!!.qualifiedName};") {
                        val createImportStatement =
                            JavaPsiFacade.getInstance(project).elementFactory.createImportStatement(
                                newServiceImplClass!!
                            )
                        WriteCommandAction.runWriteCommandAction(project) {
                            statement.replace(createImportStatement)
                        }
                    }

                    super.visitImportStatement(statement)
                }
            })


        }


    }

    private fun processServiceInsertMethodCall(project: Project, expression: PsiMethodCallExpression) {
        val filter = expression.methodExpression.children.filterIsInstance<PsiIdentifier>()
        if (filter.size == 1) {

            val psiIdentifier = filter[0]

            WriteCommandAction.runWriteCommandAction(project) {
                val methodNewIdentity = JavaPsiFacade.getInstance(project).elementFactory.createIdentifier("save")
                psiIdentifier.replace(methodNewIdentity);
            }
        }
    }

    override fun init(project: Project) {
        oldServiceImplClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.service.impl.ServiceImpl",
            GlobalSearchScope.allScope(project)
        )
        newServiceImplClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.service.impl.ServiceImpl",
            GlobalSearchScope.allScope(project)
        )
    }

}
