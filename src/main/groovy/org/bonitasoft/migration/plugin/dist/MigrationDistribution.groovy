package org.bonitasoft.migration.plugin.dist

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec

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

        project.configurations {
            filler
            project.bonitaVersions.collect { "config_$it" }.each { create it }
            drivers
        }

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
        project.dependencies {
            compile gradleApi()
            filler "org.bonitasoft.engine:bonita-client:${project.source}"
            filler "org.bonitasoft.engine:bonita-home:${project.source}"
            project.bonitaVersions.each {
                add "config_$it", "org.bonitasoft.engine:bonita-home:${project.overridedVersions.containsKey(it) ? project.overridedVersions.get(it) : it}@zip"
            }
            drivers group: 'org.postgresql', name: 'postgresql', version: '9.3-1102-jdbc41'
            //drivers group: 'mysql', name: 'mysql-connector-java', version: '5.1.26'

        }


        project.database {
            dbvendor = System.getProperty("db.vendor", "postgres")
            dburl = System.getProperty("db.url", "jdbc:postgresql://localhost:5432/migration");
            dbuser = System.getProperty("db.user", "bonita")
            dbpassword = System.getProperty("db.password", "bpm")
            dbdriverClass = System.getProperty("db.driverClass", "org.postgresql.Driver")
            dbRootUser = System.getProperty("db.root.user", "postgres")
            dbRootPassword = System.getProperty("db.root.password", "admin")
            classpath = project.configurations.drivers
        }

        project.task('putMigrationPathsInDist') << {
            //new File(projectDir, 'src/main/dist/bin/migration_paths').write(bonitaVersions.toString())
        }
        project.task('addBonitaHomes', type: Copy) {
            from {
                project.bonitaVersions.collect {
                    def conf = project.configurations."config_$it"
                    conf.files[0].getAbsolutePath()
                }
            }
            into new File(project.buildDir, 'homes')
        }


        project.distributions {
            main {
                contents {
                    from('build/homes') {
                        into "bonita-home"
                    }
                }
            }
        }
        project.tasks.addBonitaHomes.dependsOn project.tasks.putMigrationPathsInDist
        project.tasks.distZip.dependsOn project.tasks.addBonitaHomes
/*
    TODO prepare test resource: create the installation with the source version
    clean and create the database
    unpack the source bonita home
    start an engine in source version
    launch the classes to execute processes in source version
 */

        project.task('unpackBonitaHomeSource', type: Copy) {
            from {
                def conf = project.configurations."config_${project.source}"
                project.zipTree(conf.files[0].getAbsolutePath())
            }
            into project.rootProject.buildDir

        }
        project.tasks.unpackBonitaHomeSource.dependsOn project.tasks.addBonitaHomes
/*
    Migration from source to target version
 */

        project.task('migrate', type: JavaExec) {
            main = "org.bonitasoft.migration.core.Migration"
            classpath = project.sourceSets.main.runtimeClasspath
        }
        project.tasks.migrate.dependsOn project.tasks.unpackBonitaHomeSource
        project.tasks.migrate.dependsOn {
            def sourceFiller = project.rootProject.subprojects.find {
                it.name.startsWith("migration") &&  it.name.endsWith(project.target.replace('.', '_'))
            }
            sourceFiller.tasks.setupSourceEngine
        }

        project.task('testMigration') {

        }
        project.tasks.testMigration.dependsOn {
            project.rootProject.subprojects.find {
                it.name.startsWith("migration") &&  it.name.endsWith(project.target.replace('.', '_'))
            }.tasks.test
        }
        project.tasks.testMigration.dependsOn project.tasks.migrate
        project.tasks.testMigration.dependsOn project.tasks.distZip


    }
}
