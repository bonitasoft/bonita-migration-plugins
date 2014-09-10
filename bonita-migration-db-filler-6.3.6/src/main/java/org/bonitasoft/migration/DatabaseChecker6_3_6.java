/**
 * Copyright (C) 214 BonitaSoft S.A.
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
package org.bonitasoft.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.bonitasoft.engine.identity.ContactDataCreator;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCreator;
import org.bonitasoft.engine.identity.UserWithContactData;
import org.junit.Test;
import org.junit.runner.JUnitCore;


public class DatabaseChecker6_3_6 extends SimpleDatabaseChecker6_3_2 {

    public static void main(final String[] args) throws Exception {
        JUnitCore.main(DatabaseChecker6_3_6.class.getName());
    }

    //BS-8991
    @Test
    public void can_create_user_with_255_char_in_fields() throws Exception {
        //given
        final UserCreator creator = new UserCreator(completeWithZeros("user"), "bpm");
        creator.setJobTitle(completeWithZeros("Engineer"));
        creator.setFirstName(completeWithZeros("First"));
        creator.setLastName(completeWithZeros("Last"));

        final ContactDataCreator contactDataCreator = new ContactDataCreator();
        contactDataCreator.setAddress(completeWithZeros("32 Rue Gustave Eiffel"));
        creator.setProfessionalContactData(contactDataCreator);

        //when
        final User user = identityApi.createUser(creator);

        //then
        assertThat(user).isNotNull();
        assertThat(user.getUserName()).hasSize(255);
        assertThat(user.getFirstName()).hasSize(255);
        assertThat(user.getLastName()).hasSize(255);
        assertThat(user.getJobTitle()).hasSize(255);

        //when
        final UserWithContactData userWithContactData = identityApi.getUserWithProfessionalDetails(user.getId());

        //then
        assertThat(userWithContactData).isNotNull();
        assertThat(userWithContactData.getContactData().getAddress()).hasSize(255);

    }

    private String completeWithZeros(final String prefix) {

        final StringBuilder stb = new StringBuilder(prefix);
        for (int i = 0; i < 255 - prefix.length(); i++) {
            stb.append("0");
        }

        return stb.toString();
    }

}
