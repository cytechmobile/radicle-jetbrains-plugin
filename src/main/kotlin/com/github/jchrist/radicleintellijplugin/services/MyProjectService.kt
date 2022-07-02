package com.github.jchrist.radicleintellijplugin.services

import com.intellij.openapi.project.Project
import com.github.jchrist.radicleintellijplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
