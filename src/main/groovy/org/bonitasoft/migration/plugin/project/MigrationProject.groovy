package org.bonitasoft.migration.plugin.project

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

/**
 *
 *
 *
 * @author Baptiste Mesta
 */
class MigrationProject implements Plugin<Project> {

    static final String MIGRATION_PROJECT_PLUGIN_NAME = "migration-project"
    static final String MIGRATION_PROJECT_GROUP = MIGRATION_PROJECT_PLUGIN_NAME


    @Override
    void apply(Project project) {
        project.extensions.create("migrationConf", MigrationProjectExtension)
        configureAllProjects(project)
        configureSubProjects(project)
            }


    private configureSubProjects(Project project) {
        project.subprojects {
            apply plugin: 'groovy'
            repositories {
                mavenLocal()
                mavenCentral()
            }
            dependencies {
                compile "org.codehaus.groovy:groovy-all:2.4.4"
                testCompile "junit:junit:4.12"
                testRuntime 'com.oracle:ojdbc:6'
                testRuntime 'com.microsoft.jdbc:sqlserver:4.0.2206.100'

            }
        }
    }

    private configureAllProjects(Project project) {
        project.allprojects {
            apply plugin: 'maven'
        }
    }


}
