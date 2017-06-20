/**
 * Copyright (C) 2017 Bonitasoft S.A.
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
package org.bonitasoft.migration.core.database

import groovy.sql.Sql
import org.bonitasoft.migration.core.Logger

/**
 * Created by laurentleseigneur on 01/06/2017.
 */
class ConfigurationHelper {

    Sql sql
    Logger logger
    DatabaseHelper databaseHelper

    def insertConfigurationFile(String fileName, long tenantId, String type, byte[] content) {
        logger.debug(String.format("insert configuration file | tenant id: %3d | type: %-25s | file name: %s", tenantId, type, fileName))
        sql.executeInsert("INSERT INTO configuration(tenant_id,content_type,resource_name,resource_content) VALUES (${tenantId}, ${type}, ${fileName}, ${content})");
    }


    def updateConfigurationFileContent(String fileName, long tenantId, String type, byte[] content) {
        logger.debug(String.format("update configuration file | tenant id: %3d | type: %-25s | file name: %s", tenantId, type, fileName))
        sql.execute("UPDATE configuration SET resource_content = ${content} WHERE tenant_id = ${tenantId} AND content_type = ${type} AND resource_name = ${fileName}");
    }


    def appendToAllConfigurationFilesWithName(String filename, String toAppend) {
        def count = sql.firstRow("SELECT count(*) FROM configuration WHERE resource_name = ${filename}")
        if (count == 0) {
            throw new IllegalArgumentException('Configuration file ' + filename + ' does not exist in database.')
        }
        def results = sql.rows("SELECT tenant_id, content_type, resource_content FROM configuration WHERE resource_name = ${filename} ")
        results.each {
            def tenantId = it.tenant_id as long
            def contentType = it.content_type as String
            String content = databaseHelper.getBlobContentAsString(it.resource_content)

            content += "\n${toAppend}"
            updateConfigurationFileContent(filename, tenantId, contentType, content.bytes)
        }
    }


    def updateKeyInAllPropertyFiles(String fileName, String propertyKey, String newPropertyValue, String comment) {
        def updatedFiles = 0
        def results = sql.rows("""
                SELECT tenant_id, content_type, resource_content
                FROM configuration
                WHERE resource_name=${fileName}       
                ORDER BY content_type, tenant_id 
                """)
        results.each {
            String content = databaseHelper.getBlobContentAsString(it.resource_content)
            def properties = new Properties()
            def stringReader = new StringReader(content)
            def tenantId = it.tenant_id as long
            properties.load(stringReader)
            def newEntry = "# ${comment}\n${propertyKey}=${newPropertyValue}"
            if (!properties.containsKey(propertyKey)) {
                content += "\n$newEntry"
                updateConfigurationFileContent(fileName, tenantId, it.content_type, content.bytes)
                logger.info(String.format("add new property in configuration file | tenant id: %3d | type: %-25s | file name: %s | new property: %s", tenantId, it.content_type, fileName, newEntry))
            } else {
                def oldValue = properties.get(propertyKey)
                if (oldValue != newPropertyValue) {
                    def oldEntry="${propertyKey}=${ oldValue}"
                    def newContent = content.replace(oldEntry,newEntry)
                    updateConfigurationFileContent(fileName, tenantId, it.content_type, newContent.bytes)
                    logger.info(String.format("update property in configuration file | tenant id: %3d | type: %-25s | file name: %s | new property: %s", tenantId, it.content_type, fileName, "${propertyKey}=${newPropertyValue}"))
                } else {
                    logger.info(String.format("property file is already up to date "))
                }
            }
            updatedFiles++
        }
        if (updatedFiles == 0) {
            throw new IllegalArgumentException("configuration file ${fileName} not found in database." )
        }
    }

    def appendToAllConfigurationFilesIfPropertyIsMissing(String fileName, String propertyKey, String propertyValue) {
        def foundFiles = 0
        def results = sql.rows("""
                SELECT tenant_id, content_type, resource_content
                FROM configuration
                WHERE resource_name=${fileName}       
                ORDER BY content_type, tenant_id 
                """)
        results.each {
            foundFiles++
            String content = databaseHelper.getBlobContentAsString(it.resource_content)
            def properties = new Properties()
            def stringReader = new StringReader(content)
            def tenantId = it.tenant_id as long
            properties.load(stringReader)
            if (!properties.containsKey(propertyKey)) {
                def newEntry = "${propertyKey}=${propertyValue}"
                content += "\n$newEntry"
                updateConfigurationFileContent(fileName, tenantId, it.content_type, content.bytes)
                logger.info(String.format("update configuration file | tenant id: %3d | type: %-25s | file name: %s | new property: %s", tenantId, it.content_type, fileName, newEntry))
            } else {
                logger.info(String.format("configuration file already up to date | tenant id: %3d | type: %-25s | file name: %s", tenantId, it.content_type, fileName))
            }
        }
        if (foundFiles == 0) {
            throw new IllegalArgumentException("configuration file ${fileName} not found in database.")
        }
    }
}