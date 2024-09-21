package com.github.hackerorange.glodonframeworkupgradeplugin.domain

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ClassImportPsiFileProcessor : PsiFileProcessor {

    private val classQNameToReplaceClassMap: HashMap<String, PsiClass> = HashMap()


    override fun processPsiFile(project: Project, psiFile: PsiFile) {

        psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitImportStatement(statement: PsiImportStatement) {

                for (item in classQNameToReplaceClassMap) {
                    val text = ApplicationManager.getApplication().runReadAction(Computable { statement.text })
                    if (text == "import ${item.key};") {
                        val newBaseMapperClass = item.value
                        val createImportStatement =
                            JavaPsiFacade.getInstance(project).elementFactory.createImportStatement(
                                newBaseMapperClass
                            )
                        WriteCommandAction.runWriteCommandAction(project) {
                            statement.replace(createImportStatement)
                        }
                    }
                }

                super.visitImportStatement(statement)
            }
        })
    }

    override fun init(project: Project) {

        val temp = HashMap<String, String>()

        temp["com.baomidou.mybatisplus.mapper.BaseMapper"] = "com.baomidou.mybatisplus.core.mapper.BaseMapper"
        temp["com.baomidou.mybatisplus.plugins.Page"] = "com.baomidou.mybatisplus.extension.plugins.pagination.Page"

        for (mutableEntry in temp) {
            val newBaseMapperClass = JavaPsiFacade.getInstance(project).findClass(
                mutableEntry.value,
                GlobalSearchScope.allScope(project)
            )

            if (newBaseMapperClass != null) {
                classQNameToReplaceClassMap[mutableEntry.key] = newBaseMapperClass
            }
        }
    }

}
