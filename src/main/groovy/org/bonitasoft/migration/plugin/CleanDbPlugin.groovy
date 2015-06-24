package org.bonitasoft.migration.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author Baptiste Mesta
 */
class CleanDbPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('hello') << {
            println "Hello from the CleanDbPlugin"
        }
        project.tasks.'jar'.dependsOn project.tasks.'hello'
    }
}
