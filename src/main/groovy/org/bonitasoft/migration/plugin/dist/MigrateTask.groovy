package org.bonitasoft.migration.plugin.dist

import org.bonitasoft.migration.plugin.cleandb.CleanDbPluginExtension
import org.gradle.api.tasks.JavaExec
/**
 * @author Baptiste Mesta
 */
class MigrateTask extends JavaExec {


    @Override
    void exec() {
        def CleanDbPluginExtension properties = project.database
        def testValues = [
                "db.vendor"     : String.valueOf(properties.dbvendor),
                "db.url"        : String.valueOf(properties.dburl),
                "db.user"       : String.valueOf(properties.dbuser),
                "db.password"   : String.valueOf(properties.dbpassword),
                "db.driverClass": String.valueOf(properties.dbdriverClass),
                "bonita.home"   : String.valueOf(project.rootProject.buildDir.absolutePath + File.separator + "bonita-home"),
                "source.version": String.valueOf(project.source),
                "target.version": String.valueOf(project.target)
        ]
        println "Using test values:"+testValues
        setSystemProperties(testValues)
        setMain("org.bonitasoft.migration.core.Migration")
        setClasspath(project.sourceSets.main.runtimeClasspath)
        super.exec()
    }
}
