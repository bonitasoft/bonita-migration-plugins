package org.bonitasoft.migration.plugin.cleandb

import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * @author Baptiste Mesta
 */
class CleanDbPlugin implements Plugin<Project> {


    @Override
    void apply(Project project) {
        project.extensions.create("database", CleanDbPluginExtension)
        project.task('cleandb', type: CleanDbTask)
    }


}
