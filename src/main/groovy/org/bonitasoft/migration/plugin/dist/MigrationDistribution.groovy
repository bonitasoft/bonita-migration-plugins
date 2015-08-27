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

    static final String MIGRATION_DISTRIBUTION_PLUGIN_NAME = "migration-dist"
    static final String MIGRATION_DISTRIBUTION_GROUP = MIGRATION_DISTRIBUTION_PLUGIN_NAME

    static final String TASK_ADD_BONITA_HOMES = "addBonitaHomes"
    static final String TASK_ADD_VERSION_IN_DIST = "addVersionsToTheDistribution"
    static final String TASK_UNPACK_BONITA_HOME = "unpackBonitaHomeSource"
    static final String TASK_MIGRATE = "migrate"
    static final String TASK_TEST_MIGRATION = "testMigration"

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
        project.task(TASK_ADD_BONITA_HOMES, type: Copy) {
            group = MIGRATION_DISTRIBUTION_GROUP
            description = "Get all bonita home for each version and put it in the distribution"
            from {
                project.bonitaVersions.collect {
                    def conf = project.configurations."config_$it"
                    conf.files[0].getAbsolutePath()
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
                windowsScript.text = windowsScript.text.replaceAll('set CLASSPATH=.*','set CLASSPATH=%APP_HOME%/lib/*')
            }
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
                    "bonita.home"  : String.valueOf(project.rootProject.buildDir.absolutePath + File.separator + "bonita-home"),
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
        project.tasks.distZip.dependsOn project.tasks.workaroundScript
        project.tasks.testMigration.dependsOn project.tasks.distZip
    }

    private defineDependencies(Project project) {
        project.dependencies {
            filler "org.bonitasoft.engine:bonita-client:${project.source}"
            filler "org.bonitasoft.console:bonita-home:${project.source}:full@zip"
            project.bonitaVersions.each {
                add "config_$it", "org.bonitasoft.console:bonita-home:${project.overridedVersions.containsKey(it) ? project.overridedVersions.get(it) : it}:full@zip"
            }
            drivers group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            drivers group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'
            drivers group: 'com.oracle', name: 'ojdbc', version: '6'
            drivers group: 'com.microsoft.jdbc', name: 'sqlserver', version: '4.0.2206.100'
            testRuntime group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            testRuntime group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'
            testRuntime group: 'com.oracle', name: 'ojdbc', version: '6'
            testRuntime group: 'com.microsoft.jdbc', name: 'sqlserver', version: '4.0.2206.100'

        }
    }

    private defineRepositories(Project project) {
        project.repositories {
            mavenLocal()
            def customMavenRepo = System.getProperty("maven.repository")
            //used in jenkins: add in system property $ {JENKINS_HOME}/userContent/m2_repo and archiva
            if (customMavenRepo != null) {
                logger.info "using custom maven.repository: " + customMavenRepo
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
            it.name.startsWith('migrateTo') && it.name.endsWith(target.replace('.', '_'))
        }
        return testProject
    }
}
