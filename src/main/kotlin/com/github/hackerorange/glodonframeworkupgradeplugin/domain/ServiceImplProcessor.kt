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
                                        renameMethod(project, expression, "save")
                                    }
                                    if (expression.methodExpression.referenceName == "delete") {
                                        renameMethod(project, expression, "remove")
                                    }
                                    if (expression.methodExpression.referenceName == "deleteById") {
                                        renameMethod(project, expression, "removeById")
                                    }
                                    useBaseMapperInsteadMethod(expression, project)


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

    private fun useBaseMapperInsteadMethod(
        expression: PsiMethodCallExpression,
        project: Project
    ) {
        val methodNames = ArrayList<String>()
        methodNames.add("selectList")
        methodNames.add("selectById")
        methodNames.add("selectBatchIds")

        for (methodName in methodNames) {


            if (expression.methodExpression.referenceName == methodName) {

                val reference = expression.methodExpression.reference

                WriteCommandAction.runWriteCommandAction(project) {
                    val createReferenceFromText =
                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                            "this.baseMapper.${methodName}",
                            null
                        )
                    reference?.element?.replace(createReferenceFromText)
                }
            }
        }

        if (expression.methodExpression.referenceName == "selectCount") {

            val reference = expression.methodExpression.reference

            WriteCommandAction.runWriteCommandAction(project) {
                val createReferenceFromText =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                        "this.baseMapper.selectCount",
                        null
                    )
                reference?.element?.replace(createReferenceFromText)

                val newStatement =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                        "java.util.Optional.ofNullable(${expression.text}).map(Long::intValue).orElse(0)",
                        null
                    )
                expression.replace(newStatement)

            }
        }
        if (expression.methodExpression.referenceName == "selectObj") {

            val reference = expression.methodExpression.reference

            WriteCommandAction.runWriteCommandAction(project) {
                val createReferenceFromText =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                        "this.baseMapper.selectObjs",
                        null
                    )
                reference?.element?.replace(createReferenceFromText)

                val newStatement =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                        "${expression.text}.stream().filter(java.util.Objects::nonNull).findFirst().orElse(null)",
                        null
                    )
                expression.replace(newStatement)

            }
        }

    }

    private fun renameMethod(project: Project, expression: PsiMethodCallExpression, methodNewName: String) {
        val filter = expression.methodExpression.children.filterIsInstance<PsiIdentifier>()
        if (filter.size == 1) {

            val psiIdentifier = filter[0]

            WriteCommandAction.runWriteCommandAction(project) {
                val methodNewIdentity =
                    JavaPsiFacade.getInstance(project).elementFactory.createIdentifier(methodNewName)
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
            "com.baomidou.mybatisplus.extension.service.impl.ServiceImpl",
            GlobalSearchScope.allScope(project)
        )
    }

}
