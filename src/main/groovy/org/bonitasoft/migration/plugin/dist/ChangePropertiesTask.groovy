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

        def CleanDbPluginExtension properties = project.database
        def path= project.rootDir.absolutePath+"/bonita-migration-distrib"
        def propFile = new File(path, "Config.properties")
        if (!propFile.exists()) {
            propFile.createNewFile()
        }
        println "replace Config.properties content with test values for ${properties.dbvendor}"
        println "setup ${propFile.absolutePath}"

        def fis = new FileInputStream(propFile)
        def Properties props = new Properties()
        props.load(fis)
        fis.close()

        props.put("db.vendor", String.valueOf(properties.dbvendor))
        props.put("db.url", String.valueOf(properties.dburl))
        props.put("db.user", String.valueOf(properties.dbuser))
        props.put("db.password", String.valueOf(properties.dbpassword))
        props.put("db.driverClass", String.valueOf(properties.dbdriverClass))
        props.put("bonita.home", String.valueOf(project.rootProject.buildDir.absolutePath + File.separator + "bonita-home"))

        props.put("source.version", String.valueOf(project.source))
        props.put("target.version", String.valueOf(project.target))
        props.store(new FileWriter(propFile), "config");
    }
}
