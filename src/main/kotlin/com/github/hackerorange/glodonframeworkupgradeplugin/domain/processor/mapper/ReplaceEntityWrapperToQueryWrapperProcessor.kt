package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ReplaceEntityWrapperToQueryWrapperProcessor : PsiFileProcessor {

    private var oldEntityWrapperClass: PsiClass? = null
    private var newEntityWrapperClass: PsiClass? = null

    override fun processPsiFile(project: Project, psiFile: PsiFile) {

        processDeclarationStatement(psiFile, project)

        processNewExpressionStatement(psiFile, project)

    }

    private fun processNewExpressionStatement(psiFile: PsiFile, project: Project) {
        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitNewExpression(psiNewExpression: PsiNewExpression) {

                if (oldEntityWrapperClass == null) {
                    return
                }
                if (newEntityWrapperClass == null) {
                    return
                }

                val targetExpressionRealType: PsiType?

                if (psiNewExpression.parent is PsiLocalVariable) {
                    val realType = (psiNewExpression.parent as PsiLocalVariable).type
                    targetExpressionRealType = realType
                } else {
                    targetExpressionRealType = psiNewExpression.type
                }

                if (targetExpressionRealType !is PsiClassType) return

                val rightType = psiNewExpression.type
                if (rightType !is PsiClassType) return

                val resolve = rightType.resolve() ?: return
                val isOldEntityWrapper =
                    resolve == oldEntityWrapperClass || resolve.isInheritor(oldEntityWrapperClass!!, true)
                if (!isOldEntityWrapper) {
                    return
                }
                val substitutor = targetExpressionRealType.resolveGenerics().substitutor
                if (substitutor.substitutionMap.values.size != 1) return
                val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                if (entityClassType !is PsiClassType) return
                val entityClass = entityClassType.resolve() ?: return

                val replaceExpression =
                    "new ${newEntityWrapperClass!!.qualifiedName}<${entityClass.qualifiedName}>()\n"


                val createExpressionFromText =
                    JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                        replaceExpression,
                        psiNewExpression
                    )

                WriteCommandAction.runWriteCommandAction(project) {
                    psiNewExpression.replace(createExpressionFromText)
                }

                super.visitNewExpression(psiNewExpression)
            }
        })
    }


    override fun init(project: Project) {
        oldEntityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.mapper.Wrapper",
            GlobalSearchScope.allScope(project)
        )
        newEntityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.conditions.query.QueryWrapper",
            GlobalSearchScope.allScope(project)
        )
    }


    private fun processDeclarationStatement(psiFile: PsiFile, project: Project) {
        psiFile.accept(object : JavaRecursiveElementVisitor() {


            override fun visitDeclarationStatement(psiDeclarationStatement: PsiDeclarationStatement) {
                if (oldEntityWrapperClass == null) {
                    return
                }
                if (newEntityWrapperClass == null) {
                    return
                }

                for (declaredElement in psiDeclarationStatement.declaredElements) {

                    if (declaredElement is PsiVariable) {
                        val psiTypeElement = declaredElement.typeElement ?: continue

                        val type1 = psiTypeElement.type
                        if (type1 is PsiClassType) {
                            val resolveGenerics = type1.resolveGenerics()
                            val currentClass = resolveGenerics.element ?: continue
                            if (currentClass == oldEntityWrapperClass || currentClass.isInheritor(
                                    oldEntityWrapperClass!!,
                                    true
                                )
                            ) {
                                val substitutor = resolveGenerics.substitutor
                                if (substitutor.substitutionMap.values.size != 1) {

                                    val newType =
                                        JavaPsiFacade.getInstance(project).elementFactory.createTypeFromText(
                                            newEntityWrapperClass!!.qualifiedName + "<?>",
                                            null
                                        )

                                    WriteCommandAction.runWriteCommandAction(project) {
                                        psiTypeElement.replace(
                                            JavaPsiFacade.getInstance(project).elementFactory.createTypeElement(
                                                newType
                                            )
                                        )
                                    }
                                } else {
                                    val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                                    val newType = JavaPsiFacade.getInstance(project).elementFactory.createTypeFromText(
                                        newEntityWrapperClass!!.qualifiedName + "<${entityClassType?.canonicalText ?: "?"}>",
                                        null
                                    )

                                    WriteCommandAction.runWriteCommandAction(project) {
                                        psiTypeElement.replace(
                                            JavaPsiFacade.getInstance(project).elementFactory.createTypeElement(
                                                newType
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                super.visitDeclarationStatement(psiDeclarationStatement)
            }
        })
    }
}
