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
package org.obm.opush.env;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.naming.ConfigurationException;

import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;

import org.obm.configuration.ConfigurationService;
import org.obm.configuration.EmailConfiguration;
import org.obm.configuration.LocatorConfiguration;
import org.obm.configuration.SyncPermsConfigurationService;
import org.obm.opush.env.Configuration.Mail;
import org.obm.push.EhCacheStoresPercentageLoader;
import org.obm.push.configuration.RemoteConsoleConfiguration;
import org.obm.push.store.ehcache.EhCacheConfiguration;

import com.google.common.base.Throwables;

public class StaticConfigurationService implements ConfigurationService {

	public static class RemoteConsole implements RemoteConsoleConfiguration {

		private final Configuration.RemoteConsole configuration;

		public RemoteConsole(Configuration.RemoteConsole configuration) {
			this.configuration = configuration;
		}

		@Override
		public boolean enable() {
			return configuration.enable;
		}

		@Override
		public int port() {
			return configuration.port;
		}

		@Override
		public String authoritativeDomain() {
			return DEFAULT_AUTHORITATIVE_DOMAIN;
		}

	}

	public static class SyncPerms implements SyncPermsConfigurationService {

		private final org.obm.opush.env.Configuration.SyncPerms configuration;

		public SyncPerms(org.obm.opush.env.Configuration.SyncPerms configuration) {
			this.configuration = configuration;
		}

		@Override
		public String getBlackListUser() {
			return configuration.blacklist;
		}

		@Override
		public Boolean allowUnknownPdaToSync() {
			return configuration.allowUnknownDevice;
		}
	}

	public static class EhCache implements EhCacheConfiguration {

		private final org.obm.opush.env.Configuration.EhCache configuration;
		private final Map<String, Percentage> percentageByStoreMap;

		public EhCache(org.obm.opush.env.Configuration.EhCache configuration) {
			this.configuration = configuration;
			percentageByStoreMap = EhCacheStoresPercentageLoader.loadStoresPercentage();
		}

		@Override
		public int maxMemoryInMB() {
			return configuration.maxMemoryInMB;
		}

		@Override
		public Percentage percentageAllowedToCache(String cacheName) {
			Percentage defaultValue = percentageByStoreMap.get(cacheName);
			if (defaultValue != null) {
				return defaultValue;
			}
			return configuration.percentageAllowedToCache;
		}
		
		@Override
		public Map<String, Percentage> percentageAllowedToCaches() {
			return percentageByStoreMap;
		}

		@Override
		public long timeToLiveInSeconds() {
			return 60;
		}

		@Override
		public TransactionalMode transactionalMode() {
			return TransactionalMode.XA;
		}
		
		@Override
		public int statsSampleToRecordCount() {
			return configuration.statsSampleToRecordCount;
		}

		@Override
		public int statsShortSamplingTimeInSeconds() {
			return configuration.statsShortSamplingTimeInSeconds;
		}
		
		@Override
		public int statsMediumSamplingTimeInSeconds() {
			return configuration.statsMediumSamplingTimeInSeconds;
		}
		
		@Override
		public int statsLongSamplingTimeInSeconds() {
			return configuration.statsLongSamplingTimeInSeconds;
		}

		@Override
		public int statsSamplingTimeStopInMinutes() {
			return configuration.statsSamplingTimeStopInMinutes;
		}
	}

	public static class Locator implements LocatorConfiguration {

		private final org.obm.opush.env.Configuration.Locator configuration;

		public Locator(org.obm.opush.env.Configuration.Locator configuration) {
			this.configuration = configuration;
		}
		
		@Override
		public String getLocatorUrl() throws ConfigurationException {
			return configuration.url;
		}
		
		@Override
		public int getLocatorPort() {
			return configuration.port;
		}

		@Override
		public int getLocatorClientTimeoutInSeconds() {
			return configuration.clientTimeout;
		}

		@Override
		public int getLocatorCacheTimeout() {
			return configuration.cacheTimeout;
		}

		@Override
		public TimeUnit getLocatorCacheTimeUnit() {
			return configuration.cacheTimeUnit;
		}
		
		
	}
	
	public static class Email implements EmailConfiguration {

		private final Mail configuration;

		public Email(Mail configuration) {
			this.configuration = configuration;
		}

		@Override
		public boolean loginWithDomain() {
			return configuration.loginWithDomain;
		}

		@Override
		public int imapTimeoutInMilliseconds() {
			return configuration.timeoutInMilliseconds;
		}

		@Override
		public ExpungePolicy expungePolicy() {
			return ExpungePolicy.ALWAYS;
		}
		
		@Override
		public int imapPort() {
			return configuration.imapPort;
		}

		@Override
		public String imapMailboxTrash() {
			return EmailConfiguration.IMAP_TRASH_NAME;
		}

		@Override
		public String imapMailboxSent() {
			return EmailConfiguration.IMAP_SENT_NAME;
		}

		@Override
		public String imapMailboxDraft() {
			return EmailConfiguration.IMAP_DRAFTS_NAME;
		}

		@Override
		public int getMessageMaxSize() {
			return configuration.maxMessageSize;
		}

		@Override
		public int getImapFetchBlockSize() {
			return configuration.fetchBlockSize;
		}

		@Override
		public boolean activateTls() {
			return configuration.activateTls;
		}
	}

	private final Configuration configuration;

	public StaticConfigurationService(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getObmUIBaseUrl() {
		return configuration.obmUiBaseUrl;
	}

	@Override
	public String getObmSyncUrl(String obmSyncHost) {
		return configuration.obmSyncUrl;
	}

	@Override
	public ResourceBundle getResourceBundle(Locale locale) {
		return configuration.bundle;
	}

	@Override
	public String getActiveSyncServletUrl() {
		return configuration.activeSyncServletUrl;
	}

	@Override
	public Charset getDefaultEncoding() {
		return configuration.defautEncoding;
	}

	@Override
	public int transactionTimeoutInSeconds() {
		return configuration.transaction.timeoutInSeconds;
	}

	@Override
	public boolean usePersistentCache() {
		return configuration.transaction.usePersistentCache;
	}

	@Override
	public int trustTokenTimeoutInSeconds() {
		return configuration.trustTokenTimeoutInSeconds;
	}

	@Override
	public int solrCheckingInterval() {
		return configuration.solrCheckingInterval;
	}

	@Override
	public String getDataDirectory() {
		try {
			return configuration.dataDir.getCanonicalPath();
		} catch (IOException e) {
			Throwables.propagate(e);
		}
		throw new IllegalStateException();
	}

	@Override
	public String getGlobalDomain() {
		return "global.test";
	}

	@Override
	public int getTransactionToleranceTimeoutInSeconds() {
		return configuration.transaction.toleranceInSeconds;
	}

}