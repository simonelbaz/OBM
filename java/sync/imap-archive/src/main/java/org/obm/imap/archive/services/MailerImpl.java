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


package org.obm.imap.archive.services;

import java.net.URISyntaxException;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.http.client.utils.URIBuilder;
import org.obm.configuration.ConfigurationService;
import org.obm.imap.archive.ImapArchiveModule.LoggerModule;
import org.obm.imap.archive.beans.ArchiveTreatmentRunId;
import org.obm.imap.archive.beans.Mailing;
import org.obm.sync.ObmSmtpService;
import org.obm.sync.base.EmailAddress;
import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.linagora.scheduling.ScheduledTask.State;

import fr.aliacom.obm.common.domain.ObmDomain;

@Singleton
public class MailerImpl implements Mailer {

	private static final String IMAP_ARCHIVE_PATH = "/imap_archive/imap_archive_index.php";
	private static final String LINE_DELIMITER = "\r\n";
	private static final String EMAIL_FROM = "imap-archive";
	private static final String EMAIL_AT = "@";
	private static final String EMAIL_FROM_DISPlAY_NAME = "IMAP Archive notifier";
	
	private final ConfigurationService configurationService;
	private final ObmSmtpService smtpService;
	private final Logger logger;
	private final Session session;

	@Inject
	protected MailerImpl(
			ConfigurationService configurationService,
			ObmSmtpService smtpService,
			@Named(LoggerModule.NOTIFICATION) Logger logger) {
		this.configurationService = configurationService;
		this.smtpService = smtpService;
		this.logger = logger;
		this.session = Session.getDefaultInstance(new Properties());
	}

	@Override
	public void send(ObmDomain domain, ArchiveTreatmentRunId runId, State state, Mailing mailing) throws MessagingException, URISyntaxException {
		try {
			if (!mailing.getEmailAddresses().isEmpty()) {
				MimeMessage message = new MimeMessage(session);
				message.setFrom(from(domain));
				message.addRecipients(RecipientType.TO, internetAddresses(mailing));
				message.setSubject("End of IMAP Archive for domain " + domain.getName());
				message.setText(text(domain, runId, state), Charsets.UTF_8.name());
				
				smtpService.sendEmail(message, session);
			}
		} catch (MessagingException | URISyntaxException e) {
			logger.error("Error when mailing", e);
			throw e;
		}
	}

	@VisibleForTesting Address from(ObmDomain domain) {
		try {
			return new InternetAddress(EMAIL_FROM + EMAIL_AT + domain.getName(), EMAIL_FROM_DISPlAY_NAME, Charsets.UTF_8.name());
		} catch (Exception e) {
			logger.error("Cannot build From address", e);
			return null;
		}
	}

	@VisibleForTesting String text(ObmDomain domain, ArchiveTreatmentRunId runId, State state) throws URISyntaxException {
		return new StringBuilder()
			.append("IMAP Archive treatment has ended with state ")
			.append(state)
			.append(" for the domain ")
			.append(domain.getName())
			.append(LINE_DELIMITER)
			.append("Logs are available at ")
			.append(link(runId))
			.append(LINE_DELIMITER)
			.toString();
	}

	@VisibleForTesting String link(ArchiveTreatmentRunId runId) throws URISyntaxException {
		return new URIBuilder()
			.setScheme(configurationService.getObmUIUrlProtocol())
			.setHost(configurationService.getObmUIUrlHost())
			.setPath(configurationService.getObmUIUrlPrefix() + IMAP_ARCHIVE_PATH)
			.setParameter("action", "log_page")
			.setParameter("run_id", runId.serialize())
			.build()
			.toString();
	}

	@VisibleForTesting Address[] internetAddresses(Mailing mailing) {
		return FluentIterable.from(mailing.getEmailAddresses())
				.transform(new Function<EmailAddress, Address>() {

					@Override
					public Address apply(EmailAddress emailAddress) {
						try {
							return new InternetAddress(emailAddress.get());
						} catch (AddressException e) {
							logger.error("Can't get an internet address for " + emailAddress.get(), e);
							return null;
						}
					}
				}).filter(Predicates.notNull())
				.toArray(Address.class);
	}
}
