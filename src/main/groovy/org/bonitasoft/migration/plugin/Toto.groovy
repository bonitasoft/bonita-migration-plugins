package org.bonitasoft.migration.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author Baptiste Mesta
 */
class Toto implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('toto') << {
            println "I'm toto, not tata"
        }
        project.tasks.'compileJava'.dependsOn project.tasks.'toto'
    }
}
