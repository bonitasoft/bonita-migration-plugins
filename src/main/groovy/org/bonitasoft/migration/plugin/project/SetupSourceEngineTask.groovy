package org.bonitasoft.migration.plugin.project

import org.gradle.api.tasks.JavaExec

/**
 * Initialize and start an engine on a version
 *
 * @author Baptiste Mesta
 */
class SetupSourceEngineTask extends JavaExec {

    @Override
    void exec() {
        description = "Setup the engine in order to run migration tests on it."
        main = "org.bonitasoft.migration.filler.FillerRunner"
        args "org.bonitasoft.migration.FillBeforeMigratingTo${project.isSP ? 'SP' : ''}" + project.bonitaVersionUnderScore
        setClasspath(project.sourceSets.main.runtimeClasspath)
        super.exec()
    }
}
