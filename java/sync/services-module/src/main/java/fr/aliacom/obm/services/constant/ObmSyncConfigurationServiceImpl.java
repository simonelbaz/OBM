/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2012  Linagora
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
package fr.aliacom.obm.services.constant;

import org.obm.configuration.ConfigurationServiceImpl;
import org.obm.push.utils.IniFile;
import org.obm.sync.auth.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import fr.aliacom.obm.common.calendar.CalendarEncoding;

/**
 * Configuration service
 */
@Singleton
public class ObmSyncConfigurationServiceImpl extends ConfigurationServiceImpl implements ObmSyncConfigurationService {

	private static final String DEFAULT_TEMPLATE_FOLDER = "/usr/share/obm-sync/resources";
	private static final String OVERRIDE_TEMPLATE_FOLDER = "/etc/obm-sync/resources/template/";
	private static final String OBM_SYNC_MAILER = "x-obm-sync";
	private static final String GLOBAL_ADDRESS_BOOK_SYNC = "globalAddressBookSync";
	private static final boolean GLOBAL_ADDRESS_BOOK_SYNC_DEFAULT_VALUE = true;
	
	private static final String DB_AUTO_TRUNCATE_PARAMETER = "database-auto-truncate";
	private static final boolean DB_AUTO_TRUNCATE_DEFAULT_VALUE = true;
	private static final String EMAIL_CALENDAR_ENCODING_PARAMETER = "email-calendar-encoding";
	private static final CalendarEncoding DEFAULT_EMAIL_CALENDAR_ENCODING = CalendarEncoding.Auto;

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Inject
	@VisibleForTesting
	ObmSyncConfigurationServiceImpl(IniFile.Factory iniFileFactory, @Named("application-name")String applicationName, @Named("globalConfigurationFile") String globalConfigurationFile) {
		super(iniFileFactory, applicationName, globalConfigurationFile);
	}

	@Override
	public String getDefaultTemplateFolder() {
		return DEFAULT_TEMPLATE_FOLDER;
	}

	@Override
	public String getOverrideTemplateFolder() {
		return OVERRIDE_TEMPLATE_FOLDER;
	}

	@Override
	public String getObmSyncMailer(AccessToken at) {
		return OBM_SYNC_MAILER + "@" + at.getDomain().getName();
	}

	@Override
	public String getLdapServer() {
		return iniFile.getStringValue("auth-ldap-server");
	}

	@Override
	public String getLdapBaseDn() {
		return iniFile.getStringValue("auth-ldap-basedn").replace("\"", "");
	}

	@Override
	public String getLdapFilter() {
		return iniFile.getStringValue("auth-ldap-filter").replace("\"", "");
	}

	@Override
	public String getLdapBindDn() {
		String bindDn = iniFile.getStringValue("auth-ldap-binddn");
		if (bindDn != null) {
			return bindDn.replace("\"", "");
		}
		return null;
	}

	@Override
	public String getLdapBindPassword() {
		String bindPassword = iniFile.getStringValue("auth-ldap-bindpw");
		if (bindPassword != null) {
			return bindPassword.replace("\"", "");
		}
		return null;
	}

	@Override
	public Iterable<String> getLemonLdapIps() {
		String lemonIPs = iniFile.getStringValue("lemonLdapIps");
		return Splitter.on(',').trimResults().split(lemonIPs);
	}

	@Override
	public String getRootAccounts() {
		return iniFile.getStringValue("rootAccounts");
	}

	@Override
	public String getAppliAccounts() {
		return iniFile.getStringValue("appliAccounts");
	}

	@Override
	public String getAnyUserAccounts() {
		return iniFile.getStringValue("anyUserAccounts");
	}

	@Override
	public boolean syncUsersAsAddressBook() {
		return iniFile.getBooleanValue(GLOBAL_ADDRESS_BOOK_SYNC,
				GLOBAL_ADDRESS_BOOK_SYNC_DEFAULT_VALUE);
	}
	
	@Override
	public boolean usePersistentCache() {
		return false;
	}

	@Override
	public CalendarEncoding getEmailCalendarEncoding() {
		String strEncoding = iniFile.getStringValue(EMAIL_CALENDAR_ENCODING_PARAMETER);
		
		if (strEncoding == null) {
			return DEFAULT_EMAIL_CALENDAR_ENCODING;
		}
		
		try {
			return CalendarEncoding.valueOf(strEncoding);
		}
		catch (Exception e) {
			logger.warn("Invalid calendar encoding '{}', using default behaviour (automatic detection of appropriate encoding)", strEncoding);
			return DEFAULT_EMAIL_CALENDAR_ENCODING;
		}
	}
	
	@Override
	public boolean isAutoTruncateEnabled() {
		return iniFile.getBooleanValue(DB_AUTO_TRUNCATE_PARAMETER, DB_AUTO_TRUNCATE_DEFAULT_VALUE);
	}
}