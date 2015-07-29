package org.bonitasoft.migration.plugin.project
import org.bonitasoft.migration.plugin.MigrationConstants
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 *
 *
 *
 * @author Baptiste Mesta
 */
class MigrationProject implements Plugin<Project> {

    static final String MIGRATION_PROJECT_PLUGIN_NAME = "migration-project"
    static final String MIGRATION_PROJECT_GROUP = MIGRATION_PROJECT_PLUGIN_NAME

    static final String TASK_SETUP_SOURCE_ENGINE = "setupSourceEngine"

    @Override
    void apply(Project project) {
        project.extensions.create("migrationConf", MigrationProjectExtension)
        configureAllProjects(project)
        configureSubProjects(project)
        configureTestProjects(project)
    }

    private Iterable<?> configureTestProjects(Project project) {
        //configuration applied to all projects that are filler/checker project (e.g. migrateTo_7_0_1)
        return project.configure(project.subprojects.findAll {
            it.name.startsWith(MigrationConstants.MIGRATION_PREFIX)
        }) {

            ext {
                bonitaVersionUnderScore = name.substring(MigrationConstants.MIGRATION_PREFIX.length())
                isSP = bonitaVersionUnderScore.startsWith('SP')
                bonitaVersionUnderScore = isSP ? bonitaVersionUnderScore.substring(3) : bonitaVersionUnderScore.substring(1)

                bonitaVersion = bonitaVersionUnderScore.replace('_', '.')
                bonitaVersionResolved = overridedVersions.containsKey(bonitaVersion) ? overridedVersions.get(bonitaVersion) : bonitaVersion

                previousVersion = bonitaVersions[bonitaVersions.indexOf(bonitaVersion) - 1]
                previousVersionUnderScore = previousVersion.replace('.', '_')
                bonitaPreviousVersionResolved = overridedVersions.containsKey(previousVersion) ? overridedVersions.get(previousVersion) : previousVersion
            }
            dependencies {
                String engineClientGroup
                String engineClientName
                String engineTestClientGroup
                String engineTestClientName
                if(isSP){
                    engineClientGroup = "com.bonitasoft.engine"
                    engineClientName = "bonita-client-sp"
                    engineTestClientGroup = "com.bonitasoft.engine.test"
                    engineTestClientName = "bonita-integration-tests-local-sp"
                }else{
                    engineClientGroup = "org.bonitasoft.engine"
                    engineClientName = "bonita-client"
                    engineTestClientGroup = "org.bonitasoft.engine.test"
                    engineTestClientName = "bonita-server-test-utils"
                }
                compile group: "org.bonitasoft.migration", name: 'bonita-migration-common', version: project.getVersion()
                compile "${engineClientGroup}:${engineClientName}:${bonitaPreviousVersionResolved}"
                compile "${engineTestClientGroup}:${engineTestClientName}:${bonitaPreviousVersionResolved}"
                testCompile "${engineClientGroup}:${engineClientName}:${bonitaVersionResolved}"
                testCompile "${engineTestClientGroup}:${engineTestClientName}:${bonitaVersionResolved}"
            }

            task(TASK_SETUP_SOURCE_ENGINE, dependsOn: 'classes', type: SetupSourceEngineTask) {
                group = MIGRATION_PROJECT_GROUP
            }
            tasks.setupSourceEngine.dependsOn tasks.jar
        }
    }

    private configureSubProjects(Project project) {
        project.subprojects {
            apply plugin: 'groovy'
            repositories {
                mavenLocal()
                def customMavenRepo = System.getProperty("maven.repository")
                //used in jenkins: add in system property $ {JENKINS_HOME}/userContent/m2_repo and archiva
                if (customMavenRepo != null) {
                    logger.info "using custom maven.repository: " + customMavenRepo
                    maven { url customMavenRepo }
                }
                mavenCentral()
            }
            dependencies {
                compile localGroovy()
                testCompile "junit:junit:4.12"
            }
        }
    }

    private configureAllProjects(Project project) {
        project.allprojects {
            apply plugin: 'maven'

            ext.target = System.getProperty("target.version", bonitaVersions.last())
            ext.source = bonitaVersions[bonitaVersions.indexOf(project.target)-1]
            uploadArchives {
                repositories {
                    mavenDeployer {
                        repository url: 'file://' + new File(
                                System.getProperty('user.home'), '.m2/repository').absolutePath
                    }
                }
            }
        }
    }
}
