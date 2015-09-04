/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/

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
        setMain "${project.isSP ? 'com' : 'org'}.bonitasoft.migration.core.Migration"
        logger.info "using classpath:"
        project.sourceSets.test.runtimeClasspath.each { File it ->
            logger.info it.path
        }
        setClasspath project.sourceSets.test.runtimeClasspath
        setDebug System.getProperty("migration.debug") != null
        super.exec()
    }
}
