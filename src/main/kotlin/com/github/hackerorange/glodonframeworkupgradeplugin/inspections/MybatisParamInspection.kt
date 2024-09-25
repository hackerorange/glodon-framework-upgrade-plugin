package com.github.hackerorange.glodonframeworkupgradeplugin.inspections

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class MybatisParamInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(problemsHolder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        val project = problemsHolder.project

        val baseMapperClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.mapper.BaseMapper",
            GlobalSearchScope.allScope(project)
        )
        val pageClass = JavaPsiFacade.getInstance(project).findClass(
            "com.baomidou.mybatisplus.core.metadata.IPage",
            GlobalSearchScope.allScope(project)
        )

        return object : JavaElementVisitor() {


            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)

                if (baseMapperClass != null) {

                    val psiClass = method.containingClass ?: return

                    if (!psiClass.isInheritor(baseMapperClass, true)) {
                        return
                    }

                    method.parameterList.parameters.forEach {

                        if (it.hasAnnotation("org.apache.ibatis.annotations.Param")) {
                            return@forEach
                        }

                        if (pageClass != null) {

                            val paramType = it.type
                            if (paramType is PsiClassType) {
                                val resolve = paramType.resolve()
                                if (resolve?.isInheritor(pageClass, true) == true) {
                                    return@forEach
                                }
                            }
                        }
                        problemsHolder.registerProblem(it, "[ MyBatis Plus Upgrade ] 需要指定 @Param 参数")
                    }
                }
            }

        }
    }


}