/* ***** BEGIN LICENSE BLOCK *****
 *
 * Copyright (C) 2014  Linagora
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

package org.obm.push.minig.imap.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;

import org.apache.mina.transport.socket.SocketConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.push.mail.imap.IMAPException;
import org.obm.push.minig.imap.StoreClient;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.icegreen.greenmail.util.GreenMail;


@GuiceModule(org.obm.push.minig.imap.MailEnvModule.class)
@RunWith(GuiceRunner.class)
public class ClientSupportTest {

	@Inject StoreClient.Factory storeClientFactory;
	@Inject GreenMail greenMail;
	@Inject Provider<SocketConnector> socketConnectorProvider;

	private String mailbox;
	private String password;
	private ClientSupport clientSupport;

	@Before
	public void setUp() {
		greenMail.start();
		mailbox = "to@localhost.com";
		password = "password";
		greenMail.setUser(mailbox, password);
		
		IResponseCallback cb = new StoreClientCallback();
		ClientHandler handler = new ClientHandler(cb);
		SessionFactoryImpl sessionFactory = new SessionFactoryImpl(socketConnectorProvider, handler, 3600000);
		clientSupport = new ClientSupport(sessionFactory, 3600000);
		cb.setClient(clientSupport);
	}
	
	@After
	public void tearDown() {
		greenMail.stop();
	}
	
	@Test
	public void logoutShouldDisconnect() throws Exception {
		login();
		clientSupport.logout();
		
		assertThat(clientSupport.isConnected()).isFalse();
	}
	
	@Test
	public void multipleLoginAndLogoutCallsShouldWaitBeforeOpeningANewSocketConnector() throws Exception {
		for (int i = 0; i < 200; i++) {
			login();
			clientSupport.logout();
		}
		
		assertThat(clientSupport.isConnected()).isFalse();
	}

	private void login() throws IMAPException {
		clientSupport.login(mailbox, password.toCharArray(), new InetSocketAddress(greenMail.getImap().getBindTo(), greenMail.getImap().getPort()), false);
	}
	
}
