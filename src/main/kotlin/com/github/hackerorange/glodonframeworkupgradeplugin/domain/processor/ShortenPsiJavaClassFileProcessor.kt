package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager

class ShortenPsiJavaClassFileProcessor : PsiFileProcessor {

    override fun processPsiFile(project: Project, psiFile: PsiFile) {

        WriteCommandAction.runWriteCommandAction(project) {
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile)
        }
    }


    override fun init(project: Project) {
    }

}
