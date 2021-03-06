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
package org.obm.provisioning.json;

import static org.obm.provisioning.utils.SerializationUtils.addFieldValueToBuilder;
import static org.obm.provisioning.utils.SerializationUtils.getMailHostValue;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.obm.provisioning.bean.UserJsonFields;
import org.obm.provisioning.mailchooser.ImapBackendChooserSelector;
import org.obm.sync.host.ObmHost;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserAddress;
import fr.aliacom.obm.common.user.UserEmails;
import fr.aliacom.obm.common.user.UserIdentity;
import fr.aliacom.obm.common.user.UserNomad;
import fr.aliacom.obm.common.user.UserPhones;
import fr.aliacom.obm.common.user.UserWork;

public class ObmUserJsonDeserializer extends JsonDeserializer<ObmUser> {

	private final Provider<ObmDomain> domainProvider;
	private final ObmUser.Builder userBuilder;
	private final UserIdentity.Builder userIdentityBuilder;
	private final UserAddress.Builder userAddressBuilder;
	private final UserPhones.Builder userPhonesBuilder;
	private final UserWork.Builder userWorkBuilder;
	private final UserEmails.Builder userEmailsBuilder;
	private final ImapBackendChooserSelector imapBackendChooserSelector;
	private final UserNomad.Builder userNomadBuilder;

	@Inject
	public ObmUserJsonDeserializer(Provider<ObmDomain> domainProvider, ImapBackendChooserSelector imapBackendChooserSelector) {
		this.domainProvider = domainProvider;
		this.imapBackendChooserSelector = imapBackendChooserSelector;
		this.userBuilder = ObmUser.builder();
		this.userIdentityBuilder = UserIdentity.builder();
		this.userAddressBuilder = UserAddress.builder();
		this.userPhonesBuilder = UserPhones.builder();
		this.userWorkBuilder = UserWork.builder();
		this.userEmailsBuilder = UserEmails.builder();
		this.userNomadBuilder = UserNomad.builder();
	}

	public ObmUserJsonDeserializer(Provider<ObmDomain> domainProvider, ImapBackendChooserSelector imapBackendChooserSelector,
			ObmUser fromUser) {
		this(domainProvider, imapBackendChooserSelector);

		this.userBuilder.from(fromUser);
		this.userIdentityBuilder.from(fromUser.getIdentity());
		this.userAddressBuilder.from(fromUser.getAddress());
		this.userPhonesBuilder.from(fromUser.getPhones());
		this.userWorkBuilder.from(fromUser.getWork());
		this.userEmailsBuilder.from(fromUser.getUserEmails());
		this.userNomadBuilder.from(fromUser.getNomad());
	}

	@Override
	public ObmUser deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode jsonNode = jp.readValueAsTree();
		ObmDomain domain = domainProvider.get();

		for (UserJsonFields field : UserJsonFields.fields) {
			addFieldValueToBuilder(jsonNode, field, userBuilder, userIdentityBuilder, 
					userAddressBuilder, userPhonesBuilder, userWorkBuilder, userEmailsBuilder, userNomadBuilder);
		}

		ObmHost mailServer = getMailHostValue(jsonNode, domain, imapBackendChooserSelector.selectImapBackendChooserForDomain(domain));

		if (mailServer != null) {
			userEmailsBuilder.server(mailServer);
		}
		
		userEmailsBuilder.domain(domain);
		
		userBuilder.identity(userIdentityBuilder.build());
		userBuilder.address(userAddressBuilder.build());
		userBuilder.phones(userPhonesBuilder.build());
		userBuilder.work(userWorkBuilder.build());
		userBuilder.emails(userEmailsBuilder.build());
		userBuilder.nomad(userNomadBuilder.build());

		ObmUser newUser = userBuilder.domain(domain).build();

		Preconditions.checkArgument(newUser.getProfileName() != null, UserJsonFields.PROFILE.asSpecificationValue() + " is required.");
		Preconditions.checkArgument(newUser.getLastName() != null, UserJsonFields.LASTNAME.asSpecificationValue() + " is required.");
		Preconditions.checkArgument(!hasMailHostButNoEmails(newUser), "Cannot set " + UserJsonFields.MAIL_SERVER + " when no " + UserJsonFields.MAILS + " are defined.");

		return newUser;
	}

	private boolean hasMailHostButNoEmails(ObmUser user) {
		return user.getMailHost() != null && !user.isEmailAvailable();
	}

}
