package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.MyProjectService;

public class MyProjectManagerListener implements ProjectManagerListener {

    @Override
    public void projectOpened(Project project) {
        var srv = project.getService(MyProjectService.class);
        System.out.println("got service: " + srv);
    }
}