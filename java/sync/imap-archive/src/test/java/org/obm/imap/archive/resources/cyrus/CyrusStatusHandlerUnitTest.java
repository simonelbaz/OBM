/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.imap.archive.resources.cyrus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.obm.cyrus.imap.admin.CyrusImapService;
import org.obm.cyrus.imap.admin.CyrusManager;
import org.obm.domain.dao.UserSystemDao;
import org.obm.locator.store.LocatorService;

import fr.aliacom.obm.common.system.ObmSystemUser;

public class CyrusStatusHandlerUnitTest {
	
	private ObmSystemUser cyrusUser;

	private IMocksControl mocks;
	private UserSystemDao userSystemDao;
	private LocatorService locatorService;
	private CyrusImapService cyrusImapService;

	private CyrusStatusHandler testee;
	
	@Before
	public void setUp() {
		mocks = createControl();
		cyrusImapService = mocks.createMock(CyrusImapService.class);
		testee = new CyrusStatusHandler(locatorService, cyrusImapService, userSystemDao);
		
		cyrusUser = ObmSystemUser.builder()
				.id(5)
				.login("cyrus")
				.password("cyrus")
				.build();
	}

	@Test
	public void testcanConnectToCyrusCallClose() throws Exception {
		String host = "the host";
		
		CyrusManager cyrusManager = mocks.createMock(CyrusManager.class);
		expect(cyrusImapService.buildManager(host, cyrusUser.getLogin(), cyrusUser.getPassword())).andReturn(cyrusManager);
		cyrusManager.close();
		expectLastCall();
		
		mocks.replay();
		boolean success = testee.canConnectToCyrus(cyrusUser, host);
		mocks.verify();
		
		assertThat(success).isTrue();
	}

	@Test
	public void testcanConnectToCyrusWhenNullCyrusManager() throws Exception {
		String host = "the host";
		expect(cyrusImapService.buildManager(host, cyrusUser.getLogin(), cyrusUser.getPassword())).andReturn(null);
		
		mocks.replay();
		boolean success = testee.canConnectToCyrus(cyrusUser, host);
		mocks.verify();
		
		assertThat(success).isFalse();
	}

	@Test(expected=RuntimeException.class)
	public void testcanConnectToCyrusLetsPropagateException() throws Exception {
		String host = "the host";
		expect(cyrusImapService.buildManager(host, cyrusUser.getLogin(), cyrusUser.getPassword())).andThrow(new RuntimeException());
		
		mocks.replay();
		try {
			testee.canConnectToCyrus(cyrusUser, host);
		} catch (Exception e) {
			mocks.verify();
			throw e;
		}
	}
}