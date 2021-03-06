/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2014  Linagora
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

package org.obm.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.configuration.utils.IniFile;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;


public class ConfigurationServiceImplTest {

	private ConfigurationServiceImpl configurationServiceImpl;
	private IniFile iniFile;
	private IMocksControl control;

	@Before
	public void setup() {
		control = createControl();
		iniFile = control.createMock(IniFile.class);

		configurationServiceImpl = new ConfigurationServiceImpl(iniFile, "obm");
	}

	@Test
	public void testGetGlobalDomain() {
		assertThat(configurationServiceImpl.getGlobalDomain()).isEqualTo(ConfigurationServiceImpl.GLOBAL_DOMAIN);
	}

	@Test
	public void testGetObmUIBaseUrl() {
		String protocol = "http";
		expect(iniFile.getStringValue("external-protocol")).andReturn(protocol);
		String externalUrl = "10.69.1.23";
		expect(iniFile.getStringValue(LocatorConfigurationImpl.EXTERNAL_URL_KEY)).andReturn(externalUrl);
		String obmPrefix = "/obm/";
		expect(iniFile.getStringValue("obm-prefix", "")).andReturn(obmPrefix);

		control.replay();
		String obmUIBaseUrl = configurationServiceImpl.getObmUIBaseUrl();
		control.verify();

		assertThat(obmUIBaseUrl).isEqualTo(protocol + "://" + externalUrl + obmPrefix);
	}

	@Test
	public void testGetObmSyncUrl() {
		assertThat(configurationServiceImpl.getObmSyncServicesUrl("10.69.1.23")).isEqualTo("http://10.69.1.23:8080/obm-sync/services");
	}

	@Test
	public void testGetObmSyncBaseUrl() {
		assertThat(configurationServiceImpl.getObmSyncBaseUrl("10.69.1.23")).isEqualTo("http://10.69.1.23:8080/obm-sync");
	}

	@Test
	public void testIsPrivateEventAnonymizationShouldBeEnabledWhenDefaultValue(){
		iniFile = buildIniFileFromResourceFile("obm_conf.ini");
		configurationServiceImpl = new ConfigurationServiceImpl(iniFile, "obm");
		assertThat(configurationServiceImpl.isPrivateEventAnonymizationEnabled()).isTrue();
	}

	@Test
	public void testIsPrivateEventAnonymizationShouldBeEnabledWhenSetToTrue(){
		iniFile = buildIniFileFromResourceFile("anonymizePrivateEventsTrue.ini");
		configurationServiceImpl = new ConfigurationServiceImpl(iniFile, "obm");
		assertThat(configurationServiceImpl.isPrivateEventAnonymizationEnabled()).isTrue();
	}

	@Test
	public void testIsPrivateEventAnonymizationShouldNotBeEnabledWhenSetToFalse(){
		iniFile = buildIniFileFromResourceFile("anonymizePrivateEventsFalse.ini");
		configurationServiceImpl = new ConfigurationServiceImpl(iniFile, "obm");
		assertThat(configurationServiceImpl.isPrivateEventAnonymizationEnabled()).isFalse();
	}

	@Test
	public void testIsPrivateEventAnonymizationShouldNotBeEnabledWhenSetWithAString(){
		iniFile = buildIniFileFromResourceFile("anonymizePrivateEventsString.ini");
		configurationServiceImpl = new ConfigurationServiceImpl(iniFile, "obm");
		assertThat(configurationServiceImpl.isPrivateEventAnonymizationEnabled()).isFalse();
	}

	private IniFile buildIniFileFromResourceFile(String file) {
		return new IniFile.Factory().build(Resources.getResource(file).getFile());
	}

	@Test
	public void testGetPasswordHashShouldReturnDefaultWhenNotSet() {
		expect(iniFile.getStringValue("password-hash")).andReturn(null);
		control.replay();

		try {
			assertThat(configurationServiceImpl.getPasswordHash()).isEqualTo(Hash.NONE);
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetPasswordHashShouldReturnCorrespondingEnumWhenSet() {
		expect(iniFile.getStringValue("password-hash")).andReturn("SHA256");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getPasswordHash()).isEqualTo(Hash.SHA256);
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetPasswordHashShouldBeCaseInsensitive() {
		expect(iniFile.getStringValue("password-hash")).andReturn("sha512");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getPasswordHash()).isEqualTo(Hash.SHA512);
		} finally {
			control.verify();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPasswordHashShouldThrowIAEWhenSetToInvalidValue() {
		expect(iniFile.getStringValue("password-hash")).andReturn("crc32");
		control.replay();

		try {
			configurationServiceImpl.getPasswordHash();
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldReturnEmptyListWhenNotSet() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn(null);
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).hasSize(0);
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldReturnEmptyListWhenEmpty() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn("");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).hasSize(0);
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldReturnListWhenSetToASingleElement() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn("Drafts");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).isEqualTo(ImmutableList.of("Drafts"));
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldReturnListWhenSetToAList() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn("Drafts,Sent,CyrusRocks");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).isEqualTo(ImmutableList.of("Drafts", "Sent", "CyrusRocks"));
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldReturnListWhenSetToAMalformedList() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn("Drafts Sent-CyrusRocks");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).isEqualTo(ImmutableList.of("Drafts Sent-CyrusRocks"));
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldTrimAndSkipEmptyFolders() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn("Drafts ,, Sent");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).isEqualTo(ImmutableList.of("Drafts", "Sent"));
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldRemoveEnclosingSingleQuotes() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn("'Drafts,Sent'");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).isEqualTo(ImmutableList.of("Drafts", "Sent"));
		} finally {
			control.verify();
		}
	}

	@Test
	public void testGetUserMailboxDefaultFoldersShouldRemoveEnclosingDoubleQuotes() {
		expect(iniFile.getStringValue("userMailboxDefaultFolders")).andReturn("\"Drafts,Sent\"");
		control.replay();

		try {
			assertThat(configurationServiceImpl.getUserMailboxDefaultFolders()).isEqualTo(ImmutableList.of("Drafts", "Sent"));
		} finally {
			control.verify();
		}
	}

}
