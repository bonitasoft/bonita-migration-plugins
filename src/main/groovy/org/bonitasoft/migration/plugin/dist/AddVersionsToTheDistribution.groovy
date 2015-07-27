package org.bonitasoft.migration.plugin.dist

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * @author Laurent Leseigneur
 */
class AddVersionsToTheDistribution extends DefaultTask {

    String[] versionsToAdd = []
    File propertiesFile

    @Override
    String getDescription() {
        return "Add available version in the configuration file of the ditribution"
    }

    @TaskAction
    def addVersionsToTheDistribution() {
        Properties properties = new Properties()
        properties.put "versions", versionsToAdd.toString()
        if (!propertiesFile.exists()) {
            propertiesFile.parentFile.mkdirs()
            propertiesFile.createNewFile()
        }
        properties.store(new FileWriter(propertiesFile), "Bonita versions for migration");

    }
}
