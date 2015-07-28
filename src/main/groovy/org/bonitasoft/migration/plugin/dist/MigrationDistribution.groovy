package org.bonitasoft.migration.plugin.dist

import org.bonitasoft.migration.plugin.MigrationConstants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

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
        defineConfigurations(project)
        defineRepositories(project)
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
        project.task('addBonitaHomes', type: Copy) {
            from {
                project.bonitaVersions.collect {
                    def conf = project.configurations."config_$it"
                    conf.files[0].getAbsolutePath()
                }
            }
            into new File(project.projectDir, 'src/main/resources/homes')
        }
        project.task('addVersionsToTheDistribution', type: AddVersionsToTheDistributionTask) {
            versionsToAdd = project.bonitaVersions
            propertiesFile = new File(project.projectDir, 'src/main/resources/bonita-versions.properties')
        }
        project.task('unpackBonitaHomeSource', type: Copy) {
            from {
                def conf = project.configurations."config_${project.source}"
                project.zipTree(conf.files[0].getAbsolutePath())
            }
            into project.rootProject.buildDir

        }
        project.task('migrate', type: MigrateTask)
        project.task('testMigration') {
            description "Run the migration and launch test on it. Optional -D parameters: source.version,target.version"
        }


    }

    private void defineTaskDependencies(Project project) {

        Project testProject = getTestProject(project, project.target)
        def setSystemPropertiesForEngine = {
            systemProperties = [
                    "dbvendor"     : String.valueOf(project.database.properties.dbvendor),
                    "dburl"        : String.valueOf(project.database.properties.dburl),
                    "dbuser"       : String.valueOf(project.database.properties.dbuser),
                    "dbpassword"   : String.valueOf(project.database.properties.dbpassword),
                    "dbdriverClass": String.valueOf(project.database.properties.dbdriverClass),
                    "bonita.home"   : String.valueOf(project.rootProject.buildDir.absolutePath + File.separator + "bonita-home"),
            ]
        }
        testProject.tasks.setupSourceEngine {
            doFirst setSystemPropertiesForEngine
        }
        testProject.tasks.test {
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
        project.tasks.addBonitaHomes.dependsOn project.tasks.addVersionsToTheDistribution
        project.tasks.distZip.dependsOn project.tasks.addBonitaHomes
        project.tasks.unpackBonitaHomeSource.dependsOn project.tasks.addBonitaHomes

        testProject.tasks.setupSourceEngine.dependsOn project.tasks.unpackBonitaHomeSource
        testProject.tasks.setupSourceEngine.dependsOn project.tasks.cleandb

        project.tasks.migrate.dependsOn testProject.tasks.setupSourceEngine

        project.tasks.testMigration.dependsOn testProject.tasks.test
        project.tasks.testMigration.dependsOn project.tasks.migrate
        project.tasks.testMigration.dependsOn project.tasks.distZip
    }

    private defineDependencies(Project project) {
        project.dependencies {
            compile gradleApi()
            filler "org.bonitasoft.engine:bonita-client:${project.source}"
            filler "org.bonitasoft.console:bonita-home:${project.source}:full@zip"
            project.bonitaVersions.each {
                add "config_$it", "org.bonitasoft.console:bonita-home:${project.overridedVersions.containsKey(it) ? project.overridedVersions.get(it) : it}:full@zip"
            }
            drivers group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            compile group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'

            //drivers group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'

        }
    }

    private defineRepositories(Project project) {
        project.repositories {
            mavenLocal()
            def customMavenRepo = System.getProperty("maven.repository")
            //used in jenkins: add in system property $ {JENKINS_HOME}/userContent/m2_repo and archiva
            if (customMavenRepo != null) {
                println "using custom maven.repository: " + customMavenRepo
                maven { url customMavenRepo }
            }
            mavenCentral()
        }
    }

    private defineConfigurations(Project project) {
        project.configurations {
            filler
            project.bonitaVersions.collect { "config_$it" }.each { create it }
            drivers
        }
    }

    private static Project getTestProject(Project project, target) {
        def testProject = project.rootProject.subprojects.find {
            it.name.startsWith(MigrationConstants.MIGRATION_PREFIX) && it.name.endsWith(target.replace('.', '_'))
        }
        return testProject
    }
}
