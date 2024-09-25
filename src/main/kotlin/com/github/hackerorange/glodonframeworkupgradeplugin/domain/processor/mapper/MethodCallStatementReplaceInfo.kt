package com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper

import com.intellij.psi.PsiElement

class MethodCallStatementReplaceInfo(val oldMethodCallExpression: PsiElement, val newMethodCallExpression: PsiElement)