package org.bonitasoft.migration.plugin.cleandb

import org.gradle.api.file.FileCollection

/**
 * @author Baptiste Mesta
 */
class CleanDbPluginExtension{

    def String dbvendor
    def String dbRootUser
    def String dbRootPassword
    def String dbuser
    def String dbpassword
    def String dbdriverClass
    def String dburl
    def FileCollection classpath

}
