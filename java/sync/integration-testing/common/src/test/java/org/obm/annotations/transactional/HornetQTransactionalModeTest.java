/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.annotations.transactional;

import static org.easymock.EasyMock.createNiceMock;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.Configuration;
import org.obm.StaticConfigurationService;
import org.obm.configuration.TransactionConfiguration;
import org.obm.configuration.module.LoggerModule;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.sync.LifecycleListener;
import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import com.linagora.obm.sync.Producer;

@GuiceModule(HornetQTransactionalModeTest.Module.class)
@RunWith(GuiceRunner.class)
public class HornetQTransactionalModeTest {

	public static class Module extends AbstractModule {
		
		private final MessageQueueModule messageQueueModule;

		public Module() throws Exception {
			messageQueueModule = new MessageQueueModule();
		}
		
		@Override
		protected void configure() {
			Configuration.Transaction transaction = new Configuration.Transaction();
			transaction.timeoutInSeconds = 3600;
			install(new TransactionalModule());
			install(messageQueueModule);
			bind(TransactionConfiguration.class).toInstance(new StaticConfigurationService.Transaction(transaction));
			bind(Logger.class).annotatedWith(Names.named(LoggerModule.CONFIGURATION)).toInstance(createNiceMock(Logger.class));
		}
	}
	
	private final static long TIMEOUT = 1000;
	
	@Inject private TestClass xaMessageQueueInstance;
	@Inject private MessageConsumer consumer;
	@Inject private Set<LifecycleListener> lifecycleListeners;
	
	public static class TestClass {

		@Inject private Producer producer;
		
		@Transactional
		public void put(String text) throws Exception {
			producer.write(text);
		}
		
		@Transactional
		public void putAndthrowException(String text) throws Exception {
			put(text);
			throw new TestRollbackException();
		}
		
		
	}
	
	@After
	public void shutdown() throws Exception {
		for (LifecycleListener listener: lifecycleListeners) {
			listener.shutdown();
		}
	}
	
	@Test
	public void testSimple() throws Exception {
		String testText = "test text";
		xaMessageQueueInstance.put(testText);
		TextMessage messageReceived = (TextMessage)consumer.receive(TIMEOUT);
		assertThat(messageReceived.getText()).isEqualTo(testText);
	}
	
	@Ignore("OBMFULL-2887 : hornetq is not registered in our transaction manager")
	@Test(expected=TestRollbackException.class)
	public void testRollbackOnException() throws Exception {
		String testText = "rollback";
		try {
			xaMessageQueueInstance.putAndthrowException(testText);
		} catch (TestRollbackException e) {
			TextMessage messageReceived = (TextMessage)consumer.receive(TIMEOUT);
			assertThat(messageReceived.getText()).isNotNull();
			throw e;
		}
	}
}
