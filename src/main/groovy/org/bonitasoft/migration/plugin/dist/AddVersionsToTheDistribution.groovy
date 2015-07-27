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
        propertiesFile.withInputStream {
            properties.load(it)
        }
        println "before = " + properties.toMapString()
        properties.put "versions",versionsToAdd.toString()
        println "after = " + properties.toMapString()
        properties.store(new FileWriter(propertiesFile),"");

    }
}
