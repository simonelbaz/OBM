/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2015  Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for OBM
 * software by Linagora pursuant to Section 7 of the GNU Affero General Public
 * License, subsections (b), (c), and (e), pursuant to which you must notably (i)
 * retain the displaying by the interactive user interfaces of the “OBM, Free
 * Communication by Linagora” Logo with the “You are using the Open Source and
 * free version of OBM developed and supported by Linagora. Contribute to OBM R&D
 * by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
 * links between OBM and obm.org, between Linagora and linagora.com, as well as
 * between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks and
 * commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for OBM along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General   Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to the OBM software.
 * ***** END LICENSE BLOCK ***** */
package org.obm.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createNiceMock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.provisioning.mailchooser.LeastDiskUsageImapBackendChooser;
import org.obm.provisioning.mailchooser.LeastMailboxesImapBackendChooser;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import fr.aliacom.obm.common.domain.ObmDomain;

@RunWith(GuiceRunner.class)
@GuiceModule(ServiceLoaderImapBackendChooserSelectorTest.Env.class)
public class ServiceLoaderImapBackendChooserSelectorTest {

	public static class Env extends AbstractModule {

		@Override
		protected void configure() {
			bind(DatabaseConnectionProvider.class).toInstance(createNiceMock(DatabaseConnectionProvider.class));
		}

	}

	@Inject
	private ServiceLoaderImapBackendChooserSelector testee;

	@Test
	public void testGetShouldFindLeastDiskUsageImapBackendChooser() {
		ObmDomain domain = ObmDomain
				.builder()
				.mailChooserHookId(LeastDiskUsageImapBackendChooser.ID)
				.build();

		assertThat(testee.selectImapBackendChooserForDomain(domain)).isInstanceOf(LeastDiskUsageImapBackendChooser.class);
	}

	@Test
	public void testGetShouldFindLeastMailboxesImapBackendChooser() {
		ObmDomain domain = ObmDomain
				.builder()
				.mailChooserHookId(LeastMailboxesImapBackendChooser.ID)
				.build();

		assertThat(testee.selectImapBackendChooserForDomain(domain)).isInstanceOf(LeastMailboxesImapBackendChooser.class);
	}

	@Test(expected = RuntimeException.class)
	public void testGetShouldThrowWithNoMailChooserHookIdDefined() {
		ObmDomain domain = ObmDomain
				.builder()
				.build();

		testee.selectImapBackendChooserForDomain(domain);
	}

	@Test(expected = RuntimeException.class)
	public void testGetShouldThrowWithUnknownMailChooserHookId() {
		ObmDomain domain = ObmDomain
				.builder()
				.mailChooserHookId(0)
				.build();

		testee.selectImapBackendChooserForDomain(domain);
	}

}
