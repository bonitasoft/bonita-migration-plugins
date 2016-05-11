/**
 * Copyright (C) 2016 BonitaSoft S.A.
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
package org.bonitasoft.migration

import org.bonitasoft.engine.api.PlatformAPIAccessor
import org.bonitasoft.engine.api.TenantAPIAccessor
import org.bonitasoft.engine.bdm.BusinessObjectModelConverter
import org.bonitasoft.engine.bdm.model.BusinessObject
import org.bonitasoft.engine.bdm.model.BusinessObjectModel
import org.bonitasoft.engine.bdm.model.field.FieldType
import org.bonitasoft.engine.bdm.model.field.SimpleField
import org.bonitasoft.engine.test.TestEngineImpl
import org.bonitasoft.migration.filler.*

/**
 * @author Baptiste Mesta
 */
class FillBeforeMigratingTo7_3_0 {


    @FillerInitializer
    public void init() {
        FillerUtils.initializeEngineSystemProperties()
        TestEngineImpl.instance.start()
    }


    def static BusinessObjectModel createBusinessObjectModel() {
        final SimpleField firstName = new SimpleField()
        firstName.setName("name")
        firstName.setType(FieldType.STRING)
        firstName.setLength(Integer.valueOf(100))
        final BusinessObject testEntity = new BusinessObject()
        testEntity.setQualifiedName("com.company.model.TestEntity")
        testEntity.addField(firstName)
        final BusinessObjectModel model = new BusinessObjectModel();
        model.addBusinessObject(testEntity)
        model
    }

    @FillerBdmInitializer
    def deployBDM() {
        def businessObjectModel = createBusinessObjectModel()
        def session = TenantAPIAccessor.getLoginAPI().login("install", "install")
        def tenantAdministrationAPI = TenantAPIAccessor.getTenantAdministrationAPI(session)

        final BusinessObjectModelConverter converter = new BusinessObjectModelConverter()
        final byte[] zip = converter.zip(businessObjectModel)

        tenantAdministrationAPI.pause()
        tenantAdministrationAPI.installBusinessDataModel(zip)
        tenantAdministrationAPI.resume()
    }

    @FillAction
    public void fillSomething() {

    }


    @FillerShutdown
    public void stop() {
        //to be replaced by BonitaEngineRule with keepPlatformOnShutdown option
        def session = PlatformAPIAccessor.getPlatformLoginAPI().login("platformAdmin", "platform")
        PlatformAPIAccessor.getPlatformAPI(session).stopNode()
        PlatformAPIAccessor.getPlatformLoginAPI().logout(session)
    }


}