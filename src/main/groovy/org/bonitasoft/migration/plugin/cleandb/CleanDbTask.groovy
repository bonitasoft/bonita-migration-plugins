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

package org.bonitasoft.migration.plugin.cleandb

import groovy.sql.Sql
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
/**
 * @author Baptiste Mesta
 */
class CleanDbTask extends DefaultTask {

    @Override
    String getDescription() {
        return "Drop and recreate the database in order to launch test in a clean one."
    }

    @TaskAction
    def cleanDb() {
        def CleanDbPluginExtension properties = project.database
        logger.info "Migration of ${properties.dbvendor}"
        List<URL> urls = new ArrayList<URL>()
        properties.classpath.each { File file ->
            urls.add(file.toURI().toURL())
        }
        //workaround classpath issue
        def loader1 = Sql.class.getClassLoader()
        properties.classpath.each { File file ->
            loader1.addURL(file.toURI().toURL())
        }
        Sql.class.getClassLoader().loadClass(properties.dbdriverClass)

        logger.info "dbvendor business data dialect is $properties.dbvendor"

        if (properties.dbvendor.equals("oracle")) {
            logger.info "cleaning oracle database $properties.dbuser"
            if (properties.dbRootUser == null || properties.dbRootUser.isEmpty() || properties.dbRootPassword == null || properties.dbRootPassword.isEmpty()) {
                throw new IllegalStateException("must specify db.root.user and db.root.password for oracle")
            }
            def props = [user: properties.dbRootUser, password: properties.dbRootPassword] as Properties
            def Sql sql = Sql.newInstance(properties.dburl, props, properties.dbdriverClass)
            try {
                sql.executeUpdate("DROP user " + properties.dbuser + " cascade");
            } catch (Exception e) {
                logger.info "can't drop database, maybe it did not exist, cause = "
                e.printStackTrace()

            }
            //do not use gstring because it create prepared statement
            sql.executeUpdate("CREATE USER " + properties.dbuser + " IDENTIFIED BY " + properties.dbpassword);
            sql.executeUpdate("GRANT connect, resource TO " + properties.dbuser + " IDENTIFIED BY " + properties.dbpassword);
            sql.executeUpdate("GRANT select ON sys.dba_pending_transactions TO " + properties.dbuser);
            sql.executeUpdate("GRANT select ON sys.pending_trans\$ TO " + properties.dbuser);
            sql.executeUpdate("GRANT select ON sys.dba_2pc_pending TO " + properties.dbuser);
            sql.executeUpdate("GRANT execute ON sys.dbms_system TO " + properties.dbuser);
        } else if (properties.dbvendor.equals("sqlserver")) {
            logger.info "cleaning sqlserver database $properties.dburl"
            if (properties.dbRootUser == null || properties.dbRootUser.isEmpty() || properties.dbRootPassword == null || properties.dbRootPassword.isEmpty()) {
                throw new IllegalStateException("must specify db.root.user and db.root.password for sqlserver")
            }

            def parsedUrl = (properties.dburl =~ /(jdbc:\w+:\/\/)([\w\d\.-]+):(\d+);database=([\w\-_\d]+).*/)
            def serverName = parsedUrl[0][2]
            def portNumber = parsedUrl[0][3]
            def databaseName = parsedUrl[0][4]
            def genericUrl = parsedUrl[0][1] + serverName + ":" + portNumber
            logger.info "recreate database $databaseName on server $serverName and port $portNumber with driver $properties.dbdriverClass"
            logger.info "url is  $genericUrl"
            def Sql sql = Sql.newInstance(genericUrl, properties.dbRootUser, properties.dbRootPassword, properties.dbdriverClass)
            def script = this.getClass().getResourceAsStream("/init-sqlserver.sql").text
            script = script.replace("@sqlserver.db.name@", databaseName)
            script = script.replace("@sqlserver.connection.username@", properties.dbuser)
            script = script.replace("@sqlserver.connection.password@", properties.dbpassword)
            script.split("GO").each { sql.executeUpdate(it) }
            sql.close()
        } else {
            logger.info "drop and create: $properties.dburl"
            def parsedUrl = (properties.dburl =~ /(jdbc:\w+:\/\/)([\w\d\.-]+):(\d+)\/([\w\-_\d]+).*/)
            def serverName = parsedUrl[0][2]
            def portNumber = parsedUrl[0][3]
            def databaseName = parsedUrl[0][4]
            def genericUrl = parsedUrl[0][1] + serverName + ":" + portNumber + "/"
            logger.info "recreate database $databaseName on server $serverName and port $portNumber with driver $properties.dbdriverClass"
            logger.info "url is  $genericUrl"
            if (properties.dbvendor.equals("postgres")) {
                logger.info "cleaning postgres database $databaseName"
                if (properties.dbRootUser == null || properties.dbRootUser.isEmpty() || properties.dbRootPassword == null || properties.dbRootPassword.isEmpty()) {
                    throw new IllegalStateException("must specify db.root.user and db.root.password for postgres")
                }
                def Sql sql = Sql.newInstance((String) genericUrl, (String) properties.dbRootUser, (String) properties.dbRootPassword, (String) properties.dbdriverClass)
                sql.executeUpdate("DROP DATABASE IF EXISTS $databaseName;".toString())
                sql.executeUpdate("CREATE DATABASE $databaseName OWNER $properties.dbuser;".toString())
                sql.executeUpdate("GRANT ALL PRIVILEGES ON DATABASE $databaseName TO $properties.dbuser;".toString())
                sql.close()
            } else {
                def Sql sql = Sql.newInstance(genericUrl, properties.dbuser, properties.dbpassword, properties.dbdriverClass)
                try {
                    sql.executeUpdate("drop database " + databaseName)
                } catch (Exception e) {
                    e.printStackTrace()
                    logger.info "can't drop database, maybe it did not exist"
                }
                sql.executeUpdate("create database " + databaseName + (properties.dbvendor.equals("mysql") ? " DEFAULT CHARACTER SET utf8" : ""))
                sql.close()
            }
        }

    }
}
