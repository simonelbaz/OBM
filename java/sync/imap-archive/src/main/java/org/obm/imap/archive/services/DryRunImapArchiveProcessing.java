/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2014-2016  Linagora
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

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.obm.imap.archive.beans.ArchiveTreatmentKind;
import org.obm.imap.archive.dao.ArchiveTreatmentDao;
import org.slf4j.Logger;

import com.linagora.scheduling.DateTimeProvider;

import fr.aliacom.obm.common.domain.ObmDomain;

@Singleton
public class DryRunImapArchiveProcessing extends ImapArchiveProcessing {

	@Inject
	/* visibility for injection */ 
		DryRunImapArchiveProcessing(DateTimeProvider dateTimeProvider,
			SchedulingDatesService schedulingDatesService,
			StoreClientFactory storeClientFactory,
			ArchiveTreatmentDao archiveTreatmentDao,
			DryMailboxProcessing mailboxProcessing,
			Set<MailboxesProcessor> mailboxesProcessors) {
		
		super(dateTimeProvider, schedulingDatesService, storeClientFactory, archiveTreatmentDao, mailboxProcessing, mailboxesProcessors);
	}
	
	@Override
	protected void logStart(Logger logger, ObmDomain domain) {
		logger.info("Starting IMAP Archive in {} for domain {}", ArchiveTreatmentKind.DRY_RUN, domain.getName());
	}
}
