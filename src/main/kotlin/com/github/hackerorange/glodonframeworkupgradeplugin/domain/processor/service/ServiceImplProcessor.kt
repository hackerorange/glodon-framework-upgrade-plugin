package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.service

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ServiceImplProcessor : PsiFileProcessor {

    private var oldServiceImplClass: PsiClass? = null
    private var newServiceImplClass: PsiClass? = null

    private var methodCallIdentityRenameProcessors: ArrayList<MethodCallIdentityRenameProcessor> = ArrayList()


    override fun processPsiFile(project: Project, psiFile: PsiFile) {


        if (oldServiceImplClass == null || newServiceImplClass == null) {
            return
        }
        if (psiFile is PsiJavaFile) {

            for (psiClass in psiFile.classes) {

                if (psiClass.isInheritor(oldServiceImplClass!!, true)) {
                    methodCallIdentityRenameProcessors.forEach { methodCallIdentityRenameProcessor ->
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
            methodCallIdentityRenameProcessors.add(
                MethodCallIdentityRenameProcessor("insert", "save", it)
            )
            methodCallIdentityRenameProcessors.add(
                MethodCallIdentityRenameProcessor("delete", "remove", it)
            )
            methodCallIdentityRenameProcessors.add(
                MethodCallIdentityRenameProcessor("deleteById", "deleteById", it)
            )
        }

    }


    class MethodCallIdentityRenameProcessor(
        private val oldName: String,
        private val newMethodName: String,
        private var oldServiceImplClass: PsiClass
    ) {

        fun processRename(project: Project, psiClass: PsiClass) {
            if (!psiClass.isInheritor(oldServiceImplClass, true)) {
                return
            }

            psiClass.accept(object : JavaRecursiveElementVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)
                    if (expression.methodExpression.referenceName != oldName) {
                        return
                    }
                    val resolveMethod = expression.resolveMethod()

                    if (resolveMethod != null) {
                        if (oldServiceImplClass == resolveMethod.containingClass) {
                            println(resolveMethod)
                            val filter = expression.methodExpression.children.filterIsInstance<PsiIdentifier>()
                            if (filter.size == 1) {

                                val psiIdentifier = filter[0]

                                WriteCommandAction.runWriteCommandAction(project) {
                                    val methodNewIdentity =
                                        JavaPsiFacade.getInstance(project).elementFactory.createIdentifier(newMethodName)
                                    psiIdentifier.replace(methodNewIdentity);
                                }
                            }
                        }
                    }

                }
            })
        }
    }

}