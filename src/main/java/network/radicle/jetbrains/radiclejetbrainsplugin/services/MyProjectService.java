package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.MyBundle;

public class MyProjectService {

    public MyProjectService(Project project) {
        System.out.println(MyBundle.message("projectService", project.getName()));
    }
}
