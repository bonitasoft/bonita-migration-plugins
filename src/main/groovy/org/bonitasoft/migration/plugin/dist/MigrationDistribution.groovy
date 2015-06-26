package org.bonitasoft.migration.plugin.dist

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * comes in addition of the application plugin to add bonita homes and do all common things on the migration distribution
 *
 * @author Baptiste Mesta
 */
class MigrationDistribution implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.mainClassName = "org.bonitasoft.migration.core.Migration"

    }
}
