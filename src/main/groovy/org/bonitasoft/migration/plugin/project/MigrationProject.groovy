package org.bonitasoft.migration.plugin.project

import org.bonitasoft.migration.plugin.MigrationConstants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
/**
 *
 *
 *
 * @author Baptiste Mesta
 */
class MigrationProject implements Plugin<Project> {


    @Override
    void apply(Project project) {
        //define dependencies for sub projects



        project.allprojects {
            apply plugin: 'maven'

            ext.source = System.getProperty("source.version", bonitaVersions[bonitaVersions.size() - 2])
            ext.target = System.getProperty("target.version", bonitaVersions.last())
            uploadArchives {
                repositories {
                    mavenDeployer {
                        repository url: 'file://' + new File(
                                System.getProperty('user.home'), '.m2/repository').absolutePath
                    }
                }
            }
        }
        project.subprojects {
            apply plugin: 'groovy'
            repositories {
                mavenLocal()
                def customMavenRepo = System.getProperty("maven.repository")
                //used in jenkins: add in system property $ {JENKINS_HOME}/userContent/m2_repo and archiva
                if (customMavenRepo != null) {
                    println "using custom maven.repository: " + customMavenRepo
                    maven { url customMavenRepo }
                }
                mavenCentral()
            }
            dependencies {
                compile localGroovy()
                testCompile "junit:junit:4.12"
            }
        }
        //configure migration filler/checker projects
        project.configure( project.subprojects.findAll { it.name.startsWith(MigrationConstants.MIGRATION_PREFIX) }) {

            ext {
                bonitaVersionUnderScore = name.substring(MigrationConstants.MIGRATION_PREFIX.length())
                isSP =  bonitaVersionUnderScore.startsWith('SP')
                bonitaVersionUnderScore = isSP ? bonitaVersionUnderScore.substring(3):bonitaVersionUnderScore.substring(1)
                bonitaVersion = bonitaVersionUnderScore.replace('_', '.')
                bonitaVersionResolved = overridedVersions.containsKey(bonitaVersion) ? overridedVersions.get(bonitaVersion) : bonitaVersion
                previousVersion = bonitaVersions[bonitaVersions.indexOf(bonitaVersion)-1]
                previousVersionUnderScore =  previousVersion.replace('.', '_')
                bonitaPreviousVersionResolved = overridedVersions.containsKey(previousVersion) ? overridedVersions.get(previousVersion) : previousVersion

            }

            dependencies {
                compile group: "org.bonitasoft.migration", name:'bonita-migration-common', version:project.getVersion()
                compile "${isSP?'com':'org'}.bonitasoft.engine:bonita-client${isSP?'-sp':''}:${bonitaVersionResolved}"
                compile "${isSP?'com.bonitasoft.engine.test:bonita-integration-tests-local-sp':'org.bonitasoft.engine.test:bonita-server-test-utils'}:${bonitaPreviousVersionResolved}"
                testCompile "${isSP?'com':'org'}.bonitasoft.engine:bonita-client${isSP?'-sp':''}:${bonitaVersionResolved}"
                testCompile "${isSP?'com.bonitasoft.engine.test:bonita-integration-tests-local-sp':'org.bonitasoft.engine.test:bonita-server-test-utils'}:${bonitaVersionResolved}"
            }

            task('setupSourceEngine', dependsOn: 'classes', type: JavaExec) {
                doFirst {
                    println "bonita version declared = $bonitaVersionUnderScore or $bonitaVersion"
                    println "bonita version resolved = $bonitaVersionResolved"
                    println "properties = $systemProperties"
                }
                description "Setup the engine in order to run migration tests on it."

                //FIXME iterate on all fillers ?
                main "org.bonitasoft.migration.filler.FillerRunner"
                args "org.bonitasoft.migration.FillBeforeMigratingTo${isSP?'SP':''}" + bonitaVersionUnderScore
                classpath = sourceSets.main.runtimeClasspath
            }
            tasks.setupSourceEngine.dependsOn tasks.jar

        }
    }
}
