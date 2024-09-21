package com.github.hackerorange.glodonframeworkupgradeplugin.actions

import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.serializer.SerializerFeature
import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.*
import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper.BaseMapperSelectCountConvertProcessor
import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper.BaseMapperSelectPageTypeConvertProcessor
import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.mapper.ReplaceEntityWrapperToQueryWrapperProcessor
import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.service.ServiceImplProcessor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiUtilBase
import java.io.File
import java.io.FileWriter


class UpgradeMorrowFramework : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
            ?: return

        val processors: ArrayList<PsiFileProcessor> = ArrayList()

        processors.add(ClassImportPsiFileProcessor())
        processors.add(ReplaceEntityWrapperToQueryWrapperProcessor())
        processors.add(ServiceImplProcessor())
        processors.add(QueryWrapperOrderByProcessor())
        processors.add(BaseMapperSelectPageTypeConvertProcessor())
        processors.add(BaseMapperSelectCountConvertProcessor())
        processors.add(ShortenPsiJavaClassFileProcessor())

        processors.forEach { it.init(project) }

        val morrowFrameworkUpgradeBackgroundTask = MorrowFrameworkUpgradeBackgroundTask(project, processors)
        morrowFrameworkUpgradeBackgroundTask.queue()
    }
}


class MorrowFrameworkUpgradeBackgroundTask(project: Project, private val processors: ArrayList<PsiFileProcessor>) :
    Task.Backgroundable(
        project,
        "Upgrading Morrow Framework from [v3.4.0] to [5.0.0]",
        true,
        PerformInBackgroundOption.ALWAYS_BACKGROUND
    ) {

    override fun run(progressIndicator: ProgressIndicator) {
        val modules = project.modules
        progressIndicator.isIndeterminate = false

        val psiFiles: ArrayList<PsiJavaFile> = scanAllJavaPsiFileFromModules(modules)
        if (psiFiles.isNotEmpty()) {
            upgradePsiJavaFiles(psiFiles, progressIndicator)
        }
    }

    private fun upgradePsiJavaFiles(
        psiFiles: ArrayList<PsiJavaFile>,
        progressIndicator: ProgressIndicator
    ) {
        var current = 0;
        val total = psiFiles.size * (processors.size)




        for (processor in processors) {


            var costTimes = ArrayList<ProcessTimeCostInfo>()


            for (currentPsiJavaFile in psiFiles) {
                val startTime = System.currentTimeMillis()

                current++
                progressIndicator.isIndeterminate = false
                progressIndicator.fraction = current / (total.toDouble())
                progressIndicator.text2 =
                    "Upgrading Java File ${currentPsiJavaFile.packageName}.${currentPsiJavaFile.name} by process [${processor::class.java.name}]"
                progressIndicator.checkCanceled()
                processor.processPsiFile(project, currentPsiJavaFile)


                val endTime = System.currentTimeMillis()
                val costTime = endTime - startTime

                val processTimeCostInfo = ProcessTimeCostInfo()
                processTimeCostInfo.fileName = "${currentPsiJavaFile.packageName}.${currentPsiJavaFile.name}"
                processTimeCostInfo.costTime = costTime
                processTimeCostInfo.processorName = processor::class.java.name

                costTimes.add(processTimeCostInfo)

                project.guessProjectDir()?.let { virtualFile ->
                    val dir = File(virtualFile.path + "/.idea")
                    if (dir.exists().not()) {
                        dir.mkdirs()
                    }
                    val file = File(dir, "${processor::class.java.name}.json")
                    if (file.exists()) {
                        file.delete()
                    }
                    val fileWriter = FileWriter(file)

                    JSONObject.writeJSONString(
                        fileWriter,
                        costTimes, SerializerFeature.PrettyFormat
                    )
                }
            }
        }

//        for (currentPsiJavaFile in psiFiles) {
//            current++
//            progressIndicator.isIndeterminate = false
//            progressIndicator.fraction = current / (total.toDouble())
//            progressIndicator.text2 =
//                "shorten class reference of Java File ${currentPsiJavaFile.packageName}.${currentPsiJavaFile.name}"
//            WriteCommandAction.runWriteCommandAction(project) {
//                JavaCodeStyleManager.getInstance(project).shortenClassReferences(currentPsiJavaFile)
//            }
//        }
    }

    class ProcessTimeCostInfo {
        var fileName: String? = null
        var costTime: Long? = null
        var processorName: String? = null
    }

    private fun scanAllJavaPsiFileFromModules(modules: Array<Module>): ArrayList<PsiJavaFile> {
        val psiFiles: ArrayList<PsiJavaFile> = ArrayList()
        for (module in modules) {
            module.rootManager.sourceRoots.forEach {
                collectAllJavaFile(project, it, psiFiles)
            }
        }
        return psiFiles
    }

    private fun collectAllJavaFile(
        project: Project,
        sourceFile: VirtualFile,
        psiFiles: ArrayList<in PsiJavaFile>
    ) {
        VfsUtilCore.iterateChildrenRecursively(sourceFile, VirtualFileFilter.ALL) { currentFile: VirtualFile ->

            // 不是有效的文件，继续后面的文件
            if (!currentFile.isValid) {
                return@iterateChildrenRecursively true
            }
            // 是文件夹，继续后面的文件
            if (currentFile.isDirectory) {
                return@iterateChildrenRecursively true
            }

            // 是 java 文件的话，更新 base directory
            if (!currentFile.name.endsWith(".java")) {
                return@iterateChildrenRecursively true
            }

            ApplicationManager
                .getApplication()
                .runReadAction {
                    val psiFile = PsiUtilBase.getPsiFile(project, currentFile)
                    if (psiFile is PsiJavaFile) {
                        psiFiles.add(psiFile)
                    }
                }
            true
        }
    }

}
