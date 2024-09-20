package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ReplaceEntityWrapperToQueryWrapperProcessor : PsiFileProcessor {

    private var entityWrapperClass: PsiClass? = null
    private val importReplace = HashMap<String, String>()

    init {
        importReplace["com.baomidou.mybatisplus.mapper.BaseMapper"] =
            "com.baomidou.mybatisplus.core.mapper.BaseMapper"
        importReplace["com.baomidou.mybatisplus.plugins.Page"] =
            "com.baomidou.mybatisplus.extension.plugins.pagination.Page"
        importReplace["com.baomidou.mybatisplus.service.impl.ServiceImpl"] =
            "com.baomidou.mybatisplus.extension.service.impl.ServiceImpl"
    }

    override fun processPsiFile(project: Project, psiFile: PsiFile) {
        val classQNameToReplaceClassMap: HashMap<String, PsiClass> = HashMap()
        for (mutableEntry in importReplace) {
            val newBaseMapperClass = JavaPsiFacade.getInstance(project).findClass(
                mutableEntry.value,
                GlobalSearchScope.allScope(project)
            )

            if (newBaseMapperClass != null) {
                classQNameToReplaceClassMap[mutableEntry.key] = newBaseMapperClass
            }
        }


        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitNewExpression(expression: PsiNewExpression) {

                if (entityWrapperClass != null) {
                    if (expression.type !is PsiClassType) return

                    val resolve = (expression.type as PsiClassType).resolve() ?: return
                    if (resolve == entityWrapperClass) {
                        val newExpression = expression

                        val newExpressionType = newExpression.type ?: return

                        if (newExpressionType !is PsiClassType) return
                        val substitutor = newExpressionType.resolveGenerics().substitutor
                        if (substitutor.substitutionMap.values.size != 1) return
                        val entityClassType = ArrayList(substitutor.substitutionMap.values)[0]

                        if (entityClassType !is PsiClassType) return
                        val entityClass = entityClassType.resolve() ?: return

                        var replaceExpression =
                            "new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<${entityClass.qualifiedName}>()\n"


                        val createExpressionFromText =
                            JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                replaceExpression,
                                newExpression
                            )

                        WriteCommandAction.runWriteCommandAction(project) {
                            newExpression.replace(createExpressionFromText)
                        }

                    }

                }

                expression.classReference


                super.visitNewExpression(expression)
            }
        })
    }

    override fun init(project: Project) {
        entityWrapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.mapper.EntityWrapper",
            GlobalSearchScope.allScope(project)
        )
    }

}
