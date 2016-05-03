package org.bonitasoft.migration.plugin.testmigration

import com.github.zafarkhaja.semver.Version
import org.bonitasoft.migration.plugin.MigrationConstants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test

/**
 *
 *
 *
 * @author Baptiste Mesta
 */
class TestMigration implements Plugin<Project> {

    static final String TEST_MIGRATION_PLUGIN_NAME = "test-migration"
    static final String TEST_MIGRATION_GROUP = TEST_MIGRATION_PLUGIN_NAME

    static final String TASK_SETUP_SOURCE_ENGINE = "setupSourceEngine"
    static final String TASK_UNPACK_BONITA_HOME = "unpackBonitaHomeSource"
    static final String TASK_MIGRATE = "migrate"
    static final String TASK_INTEGRATION_TEST = "integrationTest"

    @Override
    void apply(Project project) {
        configureTestProjects(project)
    }

    /**
     * Configure the projects with name migrateTo_<version> in order to have the source compiled in the <version>-1 of the engine
     * and the tests in the version <version> of the engine
     */
    private void configureTestProjects(Project project) {
        project.ext {
            bonitaVersionUnderScore = project.name.substring(MigrationConstants.MIGRATION_PREFIX.length())
            isSP = bonitaVersionUnderScore.startsWith('SP')
            bonitaVersionUnderScore = isSP ? bonitaVersionUnderScore.substring(3) : bonitaVersionUnderScore.substring(1)

            bonitaVersion = bonitaVersionUnderScore.replace('_', '.')
            bonitaVersionResolved = overridedVersions.containsKey(bonitaVersion) ? overridedVersions.get(bonitaVersion) : bonitaVersion

            previousVersion = bonitaVersions[bonitaVersions.indexOf(bonitaVersion) - 1]
            previousVersionUnderScore = previousVersion.replace('.', '_')
            bonitaPreviousVersionResolved = overridedVersions.containsKey(previousVersion) ? overridedVersions.get(previousVersion) : previousVersion

            target = bonitaVersion
            source = bonitaVersions[bonitaVersions.indexOf(project.target) - 1]
        }
        defineConfigurations(project)
        defineDependencies(project)
        defineTasks(project)
        defineTaskDependencies(project)

    }


    private void defineTasks(Project project) {
        //Define tasks
        project.task(TASK_UNPACK_BONITA_HOME, type: Copy) {
            group = TEST_MIGRATION_GROUP
            description = "Unpack the bonita home to run the migration"
            doFirst {
                logger.info("Unpack bonita home in version ${project.source}")
            }
            from {
                def conf = project.configurations.filler
                project.zipTree(conf.files[0].getAbsolutePath())
            }
            into project.rootProject.buildDir

        }
        project.task(TASK_MIGRATE, type: MigrateTask) {
            group = TEST_MIGRATION_GROUP
            description = "Run the migration"
        }
        project.tasks.clean.doLast {
            project.projectDir.eachFile {
                if (it.isFile() && it.name.startsWith("migration-") && it.name.endsWith(".log"))
                    it.delete()
            }
        }
        project.task(TASK_SETUP_SOURCE_ENGINE, dependsOn: 'classes', type: SetupSourceEngineTask) {
            group = TEST_MIGRATION_GROUP
        }
        //do not run test in that task
        project.tasks.test {
            exclude '**'
        }
        project.task(TASK_INTEGRATION_TEST, type: Test) {
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
        project.tasks.setupSourceEngine {
            doFirst setSystemPropertiesForEngine
        }
        project.tasks.integrationTest {
            doFirst setSystemPropertiesForEngine
        }
        //Define task flow
        project.tasks.migrate.dependsOn project.tasks.unpackBonitaHomeSource
        project.tasks.migrate.dependsOn project.tasks.setupSourceEngine
        project.tasks.unpackBonitaHomeSource.dependsOn project.tasks.jar
        project.tasks.setupSourceEngine.dependsOn project.tasks.unpackBonitaHomeSource
        project.tasks.setupSourceEngine.dependsOn project.tasks.cleandb

        project.tasks.migrate.dependsOn project.tasks.setupSourceEngine
        project.tasks.integrationTest.dependsOn project.tasks.migrate
        project.tasks.setupSourceEngine.dependsOn project.tasks.jar
        project.tasks.check.dependsOn project.tasks.integrationTest
    }


    private defineDependencies(Project project) {
        project.dependencies {
            String engineClientGroup
            String engineClientName
            String engineTestClientGroup
            String engineTestClientName
            if (project.isSP) {
                engineClientGroup = "com.bonitasoft.engine"
                engineClientName = "bonita-client-sp"
                engineTestClientGroup = "com.bonitasoft.engine.test"
                engineTestClientName = "bonita-integration-tests-local-sp"
            } else {
                engineClientGroup = "org.bonitasoft.engine"
                engineClientName = "bonita-client"
                engineTestClientGroup = "org.bonitasoft.engine.test"
                engineTestClientName = "bonita-server-test-utils"
            }
            compile "${engineClientGroup}:${engineClientName}:${project.bonitaPreviousVersionResolved}"
            compile "${engineTestClientGroup}:${engineTestClientName}:${project.bonitaPreviousVersionResolved}${project.isSP ? ':tests' : ''}"
            testCompile "${engineClientGroup}:${engineClientName}:${project.bonitaVersionResolved}"
            testCompile "${engineTestClientGroup}:${engineTestClientName}:${project.bonitaVersionResolved}${project.isSP ? ':tests' : ''}"
            if (Version.valueOf(project.bonitaPreviousVersionResolved) < Version.valueOf("7.3.0")) {
                filler "org.bonitasoft.console:bonita-home${project.isSP ? '-sp' : ''}:${project.bonitaPreviousVersionResolved}:${project.isSP ? '' : 'full'}@zip"
            }
            drivers group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            drivers group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'
            drivers group: 'com.oracle', name: 'ojdbc', version: '6'
            drivers group: 'com.microsoft.jdbc', name: 'sqlserver', version: '4.0.2206.100'
            compile group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            compile group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'
            testRuntime group: 'com.oracle', name: 'ojdbc', version: '6'
            testRuntime group: 'com.microsoft.jdbc', name: 'sqlserver', version: '4.0.2206.100'

        }
    }

    private defineConfigurations(Project project) {
        project.configurations {
            filler
            drivers
        }
    }
}
