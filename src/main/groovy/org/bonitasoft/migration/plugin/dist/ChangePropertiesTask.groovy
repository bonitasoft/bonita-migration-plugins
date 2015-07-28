package org.bonitasoft.migration.plugin.dist

import org.bonitasoft.migration.plugin.cleandb.CleanDbPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * @author Laurent Leseigneur
 */
class ChangePropertiesTask extends DefaultTask {
    @Override
    String getDescription() {
        return "replace Config.properties content with system parameters"
    }

    @TaskAction
    def ChangeProperties() {


    }
}
