package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

interface PsiFileProcessor {
    fun processPsiFile(project: Project, psiFile: PsiFile)
    fun init(project: Project)

}
