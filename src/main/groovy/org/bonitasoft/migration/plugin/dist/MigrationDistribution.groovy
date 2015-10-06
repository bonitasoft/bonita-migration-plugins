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

import org.bonitasoft.migration.plugin.MigrationConstants
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
    static final String TASK_UNPACK_BONITA_HOME = "unpackBonitaHomeSource"
    static final String TASK_MIGRATE = "migrate"
    static final String TASK_TEST_MIGRATION = "testMigration"
    static final String TASK_INTEGRATION_TEST = "integrationTest"

    @Override
    void apply(Project project) {
        project.mainClassName = "${project.isSP ? 'com' : 'org'}.bonitasoft.migration.core.Migration"
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
            project.bonitaVersions.collect { version ->
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
        project.task(TASK_UNPACK_BONITA_HOME, type: Copy) {
            group = MIGRATION_DISTRIBUTION_GROUP
            description = "Unpack the bonita home to run the migration"
            doFirst {
                logger.info("Unpack bonita home in version ${project.source}")
            }
            from {
                def conf = project.configurations."config_${project.source}"
                project.zipTree(conf.files[0].getAbsolutePath())
            }
            into project.rootProject.buildDir

        }
        project.task(TASK_MIGRATE, type: MigrateTask) {
            group = MIGRATION_DISTRIBUTION_GROUP
            description = "Run the migration"
        }
        project.task(TASK_TEST_MIGRATION) {
            group = MIGRATION_DISTRIBUTION_GROUP
            description = "Launch tests after the migration. Optional -D parameters: target.version"
        }
        project.task("workaroundScript") {
            group = MIGRATION_DISTRIBUTION_GROUP
            description = "workaround for https://issues.gradle.org/browse/GRADLE-2992"
            doFirst {
                project.logger.warn "/!\\ Using the workaround to generate windows startup script"
                def File windowsScript = project.tasks.startScripts.getWindowsScript()
                windowsScript.text = windowsScript.text.replaceAll('set CLASSPATH=.*', 'set CLASSPATH=%APP_HOME%/lib/*')
            }
        }
        project.task(TASK_INTEGRATION_TEST, type: Test) {
            testClassesDir = project.sourceSets.integrationTest.output.classesDir
            classpath = project.sourceSets.integrationTest.runtimeClasspath
            reports.html.destination = project.file("${project.buildDir}/reports/integrationTests")
        }

        project.tasks.clean.doLast {
            project.projectDir.eachFile {
                if (it.isFile() && it.name.startsWith("migration-") && it.name.endsWith(".log"))
                    it.delete()
            }
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

        Project testProject = getTestProject(project, project.target)
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
        testProject.tasks.setupSourceEngine {
            doFirst setSystemPropertiesForEngine
        }

        testProject.task(TASK_TEST_MIGRATION, type: Test) {
            description = "Launch tests after the migration. Optional -D parameters: target.version"
            testClassesDir = testProject.sourceSets.test.output.classesDir
            classpath = testProject.sourceSets.test.runtimeClasspath
        }

        project.tasks.integrationTest {
            doFirst setSystemPropertiesForEngine
        }

        testProject.tasks.testMigration {
            doFirst setSystemPropertiesForEngine
        }

        //Define task flow
        project.tasks.migrate.dependsOn project.tasks.unpackBonitaHomeSource
        project.tasks.migrate.dependsOn {
            def sourceFiller = project.rootProject.subprojects.find {
                it.name.startsWith(MigrationConstants.MIGRATION_PREFIX) && it.name.endsWith(project.target.replace('.', '_'))
            }
            sourceFiller.tasks.setupSourceEngine
        }
        project.tasks.processResources.dependsOn project.tasks.addBonitaHomes
        project.tasks.processResources.dependsOn project.tasks.addVersionsToTheDistribution
        project.tasks.distTar.dependsOn project.tasks.workaroundScript

        project.tasks.unpackBonitaHomeSource.dependsOn project.tasks.jar
        testProject.tasks.setupSourceEngine.dependsOn project.tasks.unpackBonitaHomeSource
        testProject.tasks.setupSourceEngine.dependsOn project.tasks.cleandb

        project.tasks.migrate.dependsOn testProject.tasks.setupSourceEngine

        project.tasks.testMigration.dependsOn testProject.tasks.testMigration
        project.tasks.testMigration.dependsOn project.tasks.migrate
        project.tasks.testMigration.dependsOn project.tasks.distZip
        project.tasks.integrationTest.dependsOn project.tasks.cleandb
    }

    private defineDependencies(Project project) {
        project.dependencies {
            filler "org.bonitasoft.engine:bonita-client:${project.source}"
            filler "org.bonitasoft.console:bonita-home${project.isSP ? '-sp' : ''}:${project.source}:${project.isSP ? '' : 'full'}@zip"
            project.bonitaVersions.each {
                add "config_$it", "org.bonitasoft.console:bonita-home${project.isSP ? '-sp' : ''}:${project.overridedVersions.containsKey(it) ? project.overridedVersions.get(it) : it}:${project.isSP ? '' : 'full'}@zip"
            }
            drivers group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            drivers group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'
            drivers group: 'com.oracle', name: 'ojdbc', version: '6'
            drivers group: 'com.microsoft.jdbc', name: 'sqlserver', version: '4.0.2206.100'
            compile group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            compile group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'
            testRuntime group: 'com.oracle', name: 'ojdbc', version: '6'
            testRuntime group: 'com.microsoft.jdbc', name: 'sqlserver', version: '4.0.2206.100'

            integrationTestCompile project.sourceSets.main.output
            integrationTestCompile project.configurations.testCompile
            integrationTestCompile project.sourceSets.test.output
            integrationTestRuntime project.configurations.testRuntime

        }
    }

    private defineConfigurations(Project project) {
        project.configurations {
            filler
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


    private static Project getTestProject(Project project, target) {
        def testProject = project.rootProject.subprojects.find {
            it.name.startsWith('migrateTo') && it.name.endsWith(target.replace('.', '_'))
        }
        return testProject
    }
}
