package com.github.hackerorange.glodonframeworkupgradeplugin.actions

import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.ProgressTitle
import javax.swing.JComponent

class A : Task.Backgroundable {
    constructor(project: Project?, title: @ProgressTitle String) : super(project, title)
    constructor(project: Project?, title: @ProgressTitle String, canBeCancelled: Boolean) : super(
        project,
        title,
        canBeCancelled
    )

    constructor(
        project: Project?,
        title: @ProgressTitle String,
        canBeCancelled: Boolean,
        backgroundOption: PerformInBackgroundOption?
    ) : super(project, title, canBeCancelled, backgroundOption)

    constructor(
        project: Project?,
        parentComponent: JComponent?,
        title: @ProgressTitle String,
        canBeCancelled: Boolean,
        backgroundOption: PerformInBackgroundOption?
    ) : super(project, parentComponent, title, canBeCancelled, backgroundOption)

    override fun run(progressIndicator: ProgressIndicator) {}
}
