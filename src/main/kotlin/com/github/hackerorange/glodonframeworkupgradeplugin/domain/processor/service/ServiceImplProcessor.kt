package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.service

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ServiceImplProcessor : PsiFileProcessor {

    private var oldServiceImplClass: PsiClass? = null
    private var newServiceImplClass: PsiClass? = null

    private var replaceWithMapperReturnTrueProcessors: ArrayList<ReplaceWithMapperReturnTrueProcessor> = ArrayList()


    override fun processPsiFile(project: Project, psiFile: PsiFile) {


        if (oldServiceImplClass == null || newServiceImplClass == null) {
            return
        }
        if (psiFile is PsiJavaFile) {

            for (psiClass in psiFile.classes) {

                if (psiClass.isInheritor(oldServiceImplClass!!, true)) {
                    replaceWithMapperReturnTrueProcessors.forEach { methodCallIdentityRenameProcessor ->
                        methodCallIdentityRenameProcessor.processRename(project, psiClass)
                    }
                    extracted(psiClass, project)
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

    private fun extracted(psiClass: PsiClass, project: Project) {

        val methodNames = ArrayList<String>()
        methodNames.add("selectList")
        methodNames.add("selectById")
        methodNames.add("selectBatchIds")

        psiClass.accept(object : JavaRecursiveElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (methodNames.contains(expression.methodExpression.referenceName)) {
                    val resolveMethod = expression.resolveMethod()
                    if (resolveMethod != null) {
                        for (methodName in methodNames) {
                            if (expression.methodExpression.referenceName == methodName) {

                                if (oldServiceImplClass == resolveMethod.containingClass) {


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
                        }
                    }
                }
                super.visitMethodCallExpression(expression)
            }
        })

        psiClass.accept(object : JavaRecursiveElementVisitor() {

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {

                super.visitMethodCallExpression(expression)

                if (expression.methodExpression.referenceName == "selectCount") {
                    val resolveMethod = expression.resolveMethod()
                    if (resolveMethod == null) {
                        return
                    }
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
                    val resolveMethod = expression.resolveMethod()
                    if (resolveMethod == null) {
                        return
                    }

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
        })
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

        oldServiceImplClass?.let {
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("insert", it))
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("delete", it))
            replaceWithMapperReturnTrueProcessors.add(ReplaceWithMapperReturnTrueProcessor("deleteById", it))
        }

    }


    class ReplaceWithMapperReturnTrueProcessor(
        private val methodName: String,
        private var oldServiceImplClass: PsiClass
    ) {

        fun processRename(project: Project, psiClass: PsiClass) {
            if (!psiClass.isInheritor(oldServiceImplClass, true)) {
                return
            }

            psiClass.accept(object : JavaRecursiveElementVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)
                    if (expression.methodExpression.referenceName != methodName) {
                        return
                    }
                    val resolveMethod = expression.resolveMethod()

                    if (resolveMethod != null) {
                        if (oldServiceImplClass == resolveMethod.containingClass) {
                            println(resolveMethod)
                            var oldMethodCallExpression = expression.text

                            if (oldMethodCallExpression.startsWith("this.")) {
                                oldMethodCallExpression = oldMethodCallExpression.substring(5)
                            }
                            WriteCommandAction.runWriteCommandAction(project) {
                                val createReferenceFromText =
                                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                        "com.baomidou.mybatisplus.extension.toolkit.SqlHelper.retBool(this.baseMapper.$oldMethodCallExpression)",
                                        null
                                    )
                                expression.replace(createReferenceFromText)
                            }
                        }
                    }

                }
            })
        }
    }

}