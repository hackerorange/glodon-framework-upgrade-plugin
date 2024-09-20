package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ReplaceEntityWrapperToQueryWrapperProcessor : PsiFileProcessor {

    private var oldEntityWrapperClass: PsiClass? = null
    private var newEntityWrapperClass: PsiClass? = null

    override fun processPsiFile(project: Project, psiFile: PsiFile) {


        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitNewExpression(psiNewExpression: PsiNewExpression) {

                if (oldEntityWrapperClass == null) {
                    return
                }
                if (newEntityWrapperClass == null) {
                    return
                }


                if (psiNewExpression.type !is PsiClassType) return

                val resolve = (psiNewExpression.type as PsiClassType).resolve() ?: return
                if (resolve == oldEntityWrapperClass) {

                    val newExpressionType = psiNewExpression.type ?: return

                    if (newExpressionType !is PsiClassType) return
                    val substitutor = newExpressionType.resolveGenerics().substitutor
                    if (substitutor.substitutionMap.values.size != 1) return
                    val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                    if (entityClassType !is PsiClassType) return
                    val entityClass = entityClassType.resolve() ?: return

                    var replaceExpression =
                        "new ${newEntityWrapperClass!!.qualifiedName}<${entityClass.qualifiedName}>()\n"


                    val createExpressionFromText =
                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                            replaceExpression,
                            psiNewExpression
                        )

                    WriteCommandAction.runWriteCommandAction(project) {
                        psiNewExpression.replace(createExpressionFromText)
                    }

                }

                psiNewExpression.classReference


                super.visitNewExpression(psiNewExpression)
            }

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
                            if (currentClass.isInheritor(oldEntityWrapperClass!!, true)) {
                                val substitutor = resolveGenerics.substitutor
                                if (substitutor.substitutionMap.values.size != 1) return
                                val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                                val newType =
                                    JavaPsiFacade.getInstance(project).elementFactory.createTypeFromText(
                                        newEntityWrapperClass!!.qualifiedName + "<${entityClassType.canonicalText}>",
                                        null
                                    )

                                val createTypeElement =
                                    JavaPsiFacade.getInstance(project).elementFactory.createTypeElement(newType)

                                WriteCommandAction.runWriteCommandAction(project) {
                                    psiTypeElement.replace(createTypeElement)
                                }
                            }
                        }
                    }
                }

                super.visitDeclarationStatement(psiDeclarationStatement)
            }
        })
    }

    override fun init(project: Project) {
        oldEntityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.mapper.EntityWrapper",
            GlobalSearchScope.allScope(project)
        )
        newEntityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.conditions.query.QueryWrapper",
            GlobalSearchScope.allScope(project)
        )
    }

}
