package org.obm.push.java.mail;

import com.google.inject.AbstractModule;

public class MailEnvModule extends AbstractModule {
	
	@Override
	protected void configure() {
		install(new ImapModule());
		install(new org.obm.push.mail.MailEnvModule());
	}
}
