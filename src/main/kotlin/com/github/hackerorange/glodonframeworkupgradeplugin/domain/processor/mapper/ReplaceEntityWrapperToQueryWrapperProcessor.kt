package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import java.util.LinkedList

class ReplaceEntityWrapperToQueryWrapperProcessor : PsiFileProcessor {

    private var oldEntityWrapperClass: PsiClass? = null
    private var newEntityWrapperClass: PsiClass? = null

    override fun processPsiFile(project: Project, psiFile: PsiFile) {

        processDeclarationStatement(psiFile, project)

        processNewExpressionStatement(psiFile, project)

    }

    class WrapperReplaceContext(val oldElement: PsiElement, val newElement: PsiElement)


    private fun processNewExpressionStatement(psiFile: PsiFile, project: Project) {

        val wrapperReplaceContexts: LinkedList<WrapperReplaceContext> = LinkedList()

        ApplicationManager.getApplication().runReadAction {


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

                    if (!targetExpressionRealType.className.contains("Wrapper")) {
                        return
                    }

                    val rightType = psiNewExpression.type ?: return

                    if (rightType !is PsiClassType) return

                    if (!InheritanceUtil.isInheritor(rightType, oldEntityWrapperClass!!.qualifiedName!!)) {
                        return
                    }

                    val substitutor = targetExpressionRealType.resolveGenerics().substitutor
                    if (substitutor.substitutionMap.values.size != 1) {
                        return
                    }
                    val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                    if (entityClassType !is PsiClassType) return
                    val entityClass = entityClassType.resolve()
                        ?: return


                    val newElement =
                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                            "new ${newEntityWrapperClass!!.qualifiedName}<${entityClass.qualifiedName}>()\n",
                            null
                        )

                    wrapperReplaceContexts.add(WrapperReplaceContext(psiNewExpression, newElement))

                    super.visitNewExpression(psiNewExpression)
                }
            })
        }

        WriteCommandAction.runWriteCommandAction(project) {
            wrapperReplaceContexts.forEach { it.oldElement.replace(it.newElement) }
        }
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

        val wrapperReplaceContexts: ArrayList<WrapperReplaceContext> = ArrayList()

        ApplicationManager.getApplication().runReadAction {

            psiFile.accept(object : JavaRecursiveElementVisitor() {

                override fun visitDeclarationStatement(psiDeclarationStatement: PsiDeclarationStatement) {
                    super.visitDeclarationStatement(psiDeclarationStatement)
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
                            if (type1 !is PsiClassType) {
                                continue
                            }
                            if (!type1.className.contains("Wrapper")) {
                                return
                            }

                            if (!InheritanceUtil.isInheritor(type1, oldEntityWrapperClass!!.qualifiedName!!))
                                continue

                            val resolveGenerics = type1.resolveGenerics()
                            val substitutor = resolveGenerics.substitutor
                            if (substitutor.substitutionMap.values.size != 1) {

                                val newType =
                                    JavaPsiFacade.getInstance(project).elementFactory.createTypeFromText(
                                        newEntityWrapperClass!!.qualifiedName + "<?>",
                                        null
                                    )
                                val newElement = JavaPsiFacade.getInstance(project).elementFactory.createTypeElement(
                                    newType
                                )
                                wrapperReplaceContexts.add(WrapperReplaceContext(psiTypeElement, newElement))

                            } else {
                                val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                                val newType = JavaPsiFacade.getInstance(project).elementFactory.createTypeFromText(
                                    newEntityWrapperClass!!.qualifiedName + "<${entityClassType?.canonicalText ?: "?"}>",
                                    null
                                )

                                val newElement = JavaPsiFacade.getInstance(project)
                                    .elementFactory
                                    .createTypeElement(newType)
                                wrapperReplaceContexts.add(WrapperReplaceContext(psiTypeElement, newElement))
                            }
                        }
                    }

                }

            })
        }
        WriteCommandAction.runWriteCommandAction(project) {
            wrapperReplaceContexts.forEach { it.oldElement.replace(it.newElement) }
        }
    }
}
