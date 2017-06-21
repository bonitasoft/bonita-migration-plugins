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

import com.github.zafarkhaja.semver.Version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test

/**
 *
 * comes in addition of the application plugin to add bonita homes and do all common things on the migration distribution
 *
 * @author Baptiste Mesta
 */
class MigrationDistribution implements Plugin<Project> {

    static final String MIGRATION_DISTRIBUTION_PLUGIN_NAME = "migration-dist"
    static final String MIGRATION_DISTRIBUTION_GROUP = MIGRATION_DISTRIBUTION_PLUGIN_NAME

    static final String TASK_ADD_BONITA_HOMES = "addBonitaHomes"
    static final String TASK_ADD_VERSION_IN_DIST = "addVersionsToTheDistribution"
    static final String TASK_INTEGRATION_TEST = "integrationTest"


    @Override
    void apply(Project project) {
        project.mainClassName = "${project.isSP ? 'com' : 'org'}.bonitasoft.migration.core.Migration"
        project.applicationDefaultJvmArgs = ["-Dfile.encoding=UTF-8"]
        defineConfigurations(project)
        defineExtraSourceSets(project)
        defineDependencies(project)
        defineTasks(project)
        defineTaskDependencies(project)
        project.distributions {
            main {
                contents {
                    from('build/homes') {
                        into "bonita-home"
                    }
                }
            }
        }
    }


    private void defineTasks(Project project) {
        //Define tasks
        project.task(TASK_ADD_BONITA_HOMES, type: Copy) {
            group = MIGRATION_DISTRIBUTION_GROUP
            description = "Get all bonita home for each version and put it in the distribution"
            project.bonitaVersions.findAll { (Version.valueOf(it) < Version.valueOf("7.3.0")) }.collect { version ->
                from(project.configurations."config_$version".files[0].getParent()) {
                    include project.configurations."config_$version".files[0].getName()
                    rename 'bonita-home-(sp-)?([0-9\\.]+[0-9])(.[A-Z1-9]+)?(-full)?.zip', 'bonita-home-$1' + version + '$4.zip'
                }
            }
            into new File(project.projectDir, 'src/main/resources/homes')
        }
        project.task(TASK_ADD_VERSION_IN_DIST, type: AddVersionsToTheDistributionTask) {
            group = MIGRATION_DISTRIBUTION_GROUP
            description = "Put possible bonita version inside the distribution"
            versionsToAdd = project.bonitaVersions
            propertiesFile = new File(project.projectDir, 'src/main/resources/bonita-versions.properties')
        }

        project.task(TASK_INTEGRATION_TEST, type: Test) {
            testClassesDir = project.sourceSets.integrationTest.output.classesDir
            classpath = project.sourceSets.integrationTest.runtimeClasspath
            reports.html.destination = project.file("${project.buildDir}/reports/integrationTests")
        }

        project.tasks.clean.doLast {
            def homesDirectory = new File(project.projectDir, "src/main/resources/homes")
            if (homesDirectory.exists())
                homesDirectory.eachFile {
                    it.delete()
                }

            def versionsFile = new File(project.projectDir, "src/main/resources/bonita-versions.properties")
            if (versionsFile.exists()) versionsFile.delete()
        }
    }

    private void defineTaskDependencies(Project project) {

        def setSystemPropertiesForEngine = {
            systemProperties = [
                    "db.vendor"     : String.valueOf(project.database.properties.dbvendor),
                    "db.url"        : String.valueOf(project.database.properties.dburl),
                    "db.user"       : String.valueOf(project.database.properties.dbuser),
                    "db.password"   : String.valueOf(project.database.properties.dbpassword),
                    "db.driverClass": String.valueOf(project.database.properties.dbdriverClass),
                    "bonita.home"   : String.valueOf(project.rootProject.buildDir.absolutePath + File.separator + "bonita-home"),
            ]
        }

        project.tasks.integrationTest {
            doFirst setSystemPropertiesForEngine
        }
        project.tasks.processResources.dependsOn project.tasks.addBonitaHomes
        project.tasks.processResources.dependsOn project.tasks.addVersionsToTheDistribution
        project.tasks.integrationTest.dependsOn project.tasks.cleandb
        project.tasks.check.dependsOn project.tasks.integrationTest
    }

    private defineDependencies(Project project) {
        project.dependencies {
            project.bonitaVersions.each {
                add "config_$it", "org.bonitasoft.console:bonita-home${project.isSP ? '-sp' : ''}:${project.overridedVersions.containsKey(it) ? project.overridedVersions.get(it) : it}:${project.isSP ? '' : 'full'}@zip"
            }
            drivers group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            drivers group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'
            drivers group: 'com.oracle', name: 'ojdbc', version: '6'
            drivers group: 'com.microsoft.jdbc', name: 'sqlserver', version: '6.0.8112.100_41'
            compile group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            compile group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'

            integrationTestCompile project.sourceSets.main.output
            integrationTestCompile project.configurations.testCompile
            integrationTestCompile project.sourceSets.test.output
            integrationTestRuntime project.configurations.testRuntime

        }
    }

    private defineConfigurations(Project project) {
        project.configurations {
            project.bonitaVersions.collect { "config_$it" }.each { create it }
            drivers
        }
    }

    private defineExtraSourceSets(Project project) {
        project.sourceSets {
            integrationTest {
                groovy.srcDir project.file('src/it/groovy')
                resources.srcDir project.file('src/it/resources')
            }
        }
    }
}
