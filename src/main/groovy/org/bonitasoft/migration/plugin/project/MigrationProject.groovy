package org.bonitasoft.migration.plugin.project

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


        project.extensions.create("migrationConfiguration", MigrationProjectExtension)

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
        project.configure( project.subprojects.findAll { it.name.startsWith('migration') }) {

            ext {
                bonitaVersionUnderScore = name.substring('migration'.length())
                isSP =  bonitaVersionUnderScore.startsWith('SP')
                bonitaVersionUnderScore = isSP ? bonitaVersionUnderScore.substring(3):bonitaVersionUnderScore.substring(1)
                bonitaVersion = bonitaVersionUnderScore.replace('_', '.')
                previousVersion = bonitaVersions[bonitaVersions.indexOf(bonitaVersion)-1]
                bonitaVersionResolved = overridedVersions.containsKey(bonitaVersion) ? overridedVersions.get(bonitaVersion) : bonitaVersion
            }

            dependencies {
                compile "${isSP?'com':'org'}.bonitasoft.engine:bonita-client${isSP?'-sp':''}:${bonitaVersionResolved}"
                compile "${isSP?'com.bonitasoft.engine.test:bonita-integration-tests-sp':'org.bonitasoft.engine.test:bonita-server-test-utils'}:${previousVersion}"
                testCompile "${isSP?'com':'org'}.bonitasoft.engine:bonita-client${isSP?'-sp':''}:${bonitaVersionResolved}"
                testCompile "${isSP?'com.bonitasoft.engine.test:bonita-integration-tests-sp':'org.bonitasoft.engine.test:bonita-server-test-utils'}:${bonitaVersionResolved}"
            }

            task('setupSourceEngine', dependsOn: 'classes', type: JavaExec) {
                doFirst {
                    println "bonita version declared = $bonitaVersionUnderScore or $bonitaVersion"
                    println "bonita version resolved = $bonitaVersionResolved"
                    println "properties = $systemProperties"
                }
                main "org.bonitasoft.migration.MigrationFiller${isSP?'SP':''}" + bonitaVersionUnderScore
                classpath = sourceSets.main.runtimeClasspath
                systemProperties =["bonita.home":project.rootProject.buildDir.absolutePath+"/bonita-home"]
            }
            tasks.setupSourceEngine.dependsOn tasks.jar


        }
    }
}
