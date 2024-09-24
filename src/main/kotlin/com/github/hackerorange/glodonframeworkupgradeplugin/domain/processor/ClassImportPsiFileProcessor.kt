package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class ClassImportPsiFileProcessor : PsiFileProcessor {

    private val classQNameToReplaceClassMap: HashMap<String, PsiElement> = HashMap()


    override fun processPsiFile(project: Project, psiFile: PsiFile) {

        val replaceContexts: ArrayList<ReplaceContext> = ArrayList()


        ApplicationManager.getApplication().runReadAction {
            psiFile.accept(object : JavaRecursiveElementVisitor() {
                override fun visitImportStatement(statement: PsiImportStatement) {
                    for (item in classQNameToReplaceClassMap) {
                        val text = statement.text
                        if (text == "import ${item.key};") {
                            replaceContexts.add(ReplaceContext(statement, item.value))
                        }
                    }
                }
            })
        }

        WriteCommandAction.runWriteCommandAction(project) {
            replaceContexts.forEach { replaceContext ->
                replaceContext.oldElement.replace(replaceContext.newElement)
            }
        }
    }

    override fun init(project: Project) {

        val temp = HashMap<String, String>()

        temp["com.baomidou.mybatisplus.mapper.BaseMapper"] = "com.baomidou.mybatisplus.core.mapper.BaseMapper"
        temp["com.baomidou.mybatisplus.plugins.Page"] = "com.baomidou.mybatisplus.extension.plugins.pagination.Page"
        temp["com.baomidou.mybatisplus.service.IService"] = "com.baomidou.mybatisplus.extension.service.IService"
        temp["com.baomidou.mybatisplus.annotations.TableName"] = "com.baomidou.mybatisplus.annotation.TableName"
        temp["com.baomidou.mybatisplus.annotations.TableField"] = "com.baomidou.mybatisplus.annotation.TableField"
        temp["com.baomidou.mybatisplus.annotations.TableId"] = "com.baomidou.mybatisplus.annotation.TableId"
        temp["com.baomidou.mybatisplus.annotations.Version"] = "com.baomidou.mybatisplus.annotation.Version"
        temp["com.baomidou.mybatisplus.enums.FieldFill"] = "com.baomidou.mybatisplus.annotation.FieldFill"
        temp["com.baomidou.mybatisplus.enums.FieldStrategy"] = "com.baomidou.mybatisplus.annotation.FieldStrategy"
        temp["com.baomidou.mybatisplus.activerecord.Model"] = "com.baomidou.mybatisplus.extension.activerecord.Model"
        temp["com.baomidou.mybatisplus.service.impl.ServiceImpl"] = "com.baomidou.mybatisplus.extension.service.impl.ServiceImpl"

        for (mutableEntry in temp) {
            val newBaseMapperClass = JavaPsiFacade.getInstance(project).findClass(
                mutableEntry.value,
                GlobalSearchScope.allScope(project)
            )

            if (newBaseMapperClass != null) {
                val createImportStatement = JavaPsiFacade
                    .getInstance(project)
                    .elementFactory
                    .createImportStatement(newBaseMapperClass)

                classQNameToReplaceClassMap[mutableEntry.key] = createImportStatement
            }
        }
    }

    class ReplaceContext(val oldElement: PsiElement, val newElement: PsiElement)


}
