package org.bonitasoft.migration.plugin.dist

import org.gradle.api.tasks.JavaExec
/**
 * @author Baptiste Mesta
 */
class MigrateTask extends JavaExec {


    @Override
    void exec() {
        def testValues = [
                "db.vendor"     : String.valueOf(project.database.dbvendor),
                "db.url"        : String.valueOf(project.database.dburl),
                "db.user"       : String.valueOf(project.database.dbuser),
                "db.password"   : String.valueOf(project.database.dbpassword),
                "db.driverClass": String.valueOf(project.database.dbdriverClass),
                "bonita.home"   : String.valueOf(project.rootProject.buildDir.absolutePath + File.separator + "bonita-home"),
                "target.version": String.valueOf(project.target)
        ]
        setSystemProperties testValues
        logger.info "execute migration with properties $systemProperties"
        setMain "org.bonitasoft.migration.core.Migration"
        setClasspath project.sourceSets.main.runtimeClasspath
        super.exec()
    }
}
