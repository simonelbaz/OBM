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
package org.obm.imap.archive.resources;

import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.obm.dao.utils.H2Destination;
import org.obm.dao.utils.H2InMemoryDatabase;
import org.obm.dao.utils.H2InMemoryDatabaseTestRule;
import org.obm.domain.dao.UserSystemDao;
import org.obm.guice.GuiceRule;
import org.obm.imap.archive.Expectations;
import org.obm.imap.archive.TestImapArchiveModules;
import org.obm.imap.archive.TestImapArchiveModules.TimeBasedModule.TestDateProvider;
import org.obm.imap.archive.beans.ArchiveStatus;
import org.obm.imap.archive.beans.ArchiveTreatmentRunId;
import org.obm.imap.archive.beans.RepeatKind;
import org.obm.imap.archive.dao.DomainConfigurationJdbcImpl;
import org.obm.imap.archive.dao.SqlTables;
import org.obm.imap.archive.dao.SqlTables.MailArchiveRun;
import org.obm.server.WebServer;

import com.github.restdriver.clientdriver.ClientDriverRule;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.icegreen.greenmail.util.GreenMail;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.operation.Insert;
import com.ninja_squad.dbsetup.operation.Operation;

import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.system.ObmSystemUser;

public class TreatmentResourceTest {

	private ClientDriverRule driver = new ClientDriverRule();

	@Rule public TestRule chain = RuleChain
			.outerRule(driver)
			.around(new TemporaryFolder())
			.around(new GuiceRule(this, new TestImapArchiveModules.WithGreenmail(driver, new Provider<TemporaryFolder>() {

				@Override
				public TemporaryFolder get() {
					return temporaryFolder;
				}
				
			})))
			.around(new H2InMemoryDatabaseTestRule(new Provider<H2InMemoryDatabase>() {
				@Override
				public H2InMemoryDatabase get() {
					return db;
				}
			}, "sql/initial.sql"));
	
	@Inject TemporaryFolder temporaryFolder;
	@Inject H2InMemoryDatabase db;
	@Inject WebServer server;
	@Inject GreenMail imapServer;
	@Inject UserSystemDao userSystemDao;
	@Inject TestDateProvider testDateProvider;
	@Inject IMocksControl control;
	Expectations expectations;

	@Before
	public void setUp() {
		expectations = new Expectations(driver);
		
		imapServer.start();
		imapServer.setUser("cyrus", "cyrus");
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
		imapServer.stop();
		control.verify();
	}

	private Operation cleanDB() {
		return Operations.sequenceOf(
				Operations.deleteAllFrom(DomainConfigurationJdbcImpl.TABLE.NAME),
				Operations.deleteAllFrom(SqlTables.MailArchiveRun.NAME));
	}

	private Insert insertDomainConfiguration() {
		return Operations.insertInto(DomainConfigurationJdbcImpl.TABLE.NAME)
			.columns(DomainConfigurationJdbcImpl.TABLE.FIELDS.DOMAIN_UUID, 
					DomainConfigurationJdbcImpl.TABLE.FIELDS.ACTIVATED, 
					DomainConfigurationJdbcImpl.TABLE.FIELDS.REPEAT_KIND, 
					DomainConfigurationJdbcImpl.TABLE.FIELDS.DAY_OF_WEEK, 
					DomainConfigurationJdbcImpl.TABLE.FIELDS.DAY_OF_MONTH, 
					DomainConfigurationJdbcImpl.TABLE.FIELDS.DAY_OF_YEAR, 
					DomainConfigurationJdbcImpl.TABLE.FIELDS.HOUR, 
					DomainConfigurationJdbcImpl.TABLE.FIELDS.MINUTE)
			.values("2f096466-5a2a-463e-afad-4196c2952de3", 
					Boolean.TRUE, 
					RepeatKind.DAILY, 
					2, 10, 355, 10, 32)
			.build();
	}
	
	private Insert insertArchiveTreatment(ArchiveTreatmentRunId runId, ObmDomainUuid domainId) {
		return Operations.insertInto(MailArchiveRun.NAME)
			.columns(MailArchiveRun.Fields.UUID,
					MailArchiveRun.Fields.DOMAIN_UUID,
					MailArchiveRun.Fields.STATUS, 
					MailArchiveRun.Fields.SCHEDULE,
					MailArchiveRun.Fields.START, 
					MailArchiveRun.Fields.END, 
					MailArchiveRun.Fields.HIGHER_BOUNDARY, 
					MailArchiveRun.Fields.RECURRENT)
			.values(runId.serialize(),
					domainId.get(),
					ArchiveStatus.SUCCESS, 
					DateTime.parse("2014-06-01T00:00:00.000Z").toDate(), 
					DateTime.parse("2014-06-01T00:01:00.000Z").toDate(), 
					DateTime.parse("2014-06-01T00:02:00.000Z").toDate(), 
					DateTime.parse("2014-06-01T00:03:00.000Z").toDate(),
					true)
			.build();
	}

	private void play(Operation operation) {
		DbSetup dbSetup = new DbSetup(H2Destination.from(db), operation);
		dbSetup.launch();
	}
	
	@Test
	public void getShouldReturnArchiveTreatment() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("2f096466-5a2a-463e-afad-4196c2952de3");
		expectations
			.expectTrustedLogin(domainId)
			.expectGetDomain(domainId);
		
		UUID runId = TestImapArchiveModules.uuid;
		play(Operations.sequenceOf(cleanDB(), 
				insertDomainConfiguration(), 
				insertArchiveTreatment(ArchiveTreatmentRunId.from(runId), domainId)));
		
		control.replay();
		server.start();
		
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.contentType(ContentType.JSON)
			.body("runId", equalTo(runId.toString()),
				"domainUuid", equalTo(domainId.getUUID().toString()),
				"archiveStatus", equalTo(ArchiveStatus.SUCCESS.asSpecificationValue()),
				"scheduledTime", equalTo(DateTime.parse("2014-06-01T00:00:00.000Z").toString()),
				"startTime", equalTo(DateTime.parse("2014-06-01T00:01:00.000Z").toString()),
				"endTime", equalTo(DateTime.parse("2014-06-01T00:02:00.000Z").toString()),
				"higherBoundary", equalTo(DateTime.parse("2014-06-01T00:03:00.000Z").toString()),
				"recurrent", equalTo(true))
			.statusCode(Status.OK.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + runId.toString());
	}
	
	@Ignore("To reintroduce when SIMULATION_RUN")
	@Test
	public void getShouldReturnNotFoundWhenBadRunId() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("2f096466-5a2a-463e-afad-4196c2952de3");
		expectations
			.expectTrustedLogin(domainId)
			.expectGetDomain(domainId);
		
		play(Operations.sequenceOf(cleanDB(), 
				insertDomainConfiguration(), 
				insertArchiveTreatment(ArchiveTreatmentRunId.from(TestImapArchiveModules.uuid), domainId)));
		
		control.replay();
		server.start();
		
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.NOT_FOUND.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/944e91fe-3cfc-422d-a3a3-0b0f8972edc8");
	}
	
	@Test
	public void getLogsShouldReturnTheLogsWhenArchiveTreatmentIsInTracking() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("2f096466-5a2a-463e-afad-4196c2952de3");
		expectations
			.expectTrustedLogin(domainId)
			.expectTrustedLogin(domainId)
			.expectGetDomain(domainId)
			.expectGetDomain(domainId)
			.expectGetDomain(domainId);
		
		play(Operations.sequenceOf(cleanDB(), 
				insertDomainConfiguration()));
		
		expect(userSystemDao.getByLogin("cyrus")).andReturn(ObmSystemUser.builder().login("cyrus").password("cyrus").id(12).build()).times(2);
		
		control.replay();
		server.start();
		
		UUID expectedRunId = TestImapArchiveModules.uuid;
		given()
			.config(RestAssuredConfig.config().redirect(RedirectConfig.redirectConfig().followRedirects(false)))
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken").
		expect()
			.header("Location", containsString("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + expectedRunId.toString()))
			.statusCode(Status.SEE_OTHER.getStatusCode()).
		when()
			.post("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments");
		Thread.sleep(1);
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.body(containsString("Starting IMAP Archive in REAL_RUN for domain mydomain"))
			.statusCode(Status.OK.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + expectedRunId.toString() + "/logs");
	}
	
	@Test
	public void getLogsShouldReturnNotFoundWhenBadRunId() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("2f096466-5a2a-463e-afad-4196c2952de3");
		expectations
			.expectTrustedLogin(domainId)
			.expectTrustedLogin(domainId)
			.expectTrustedLogin(domainId)
			.expectGetDomain(domainId)
			.expectGetDomain(domainId)
			.expectGetDomain(domainId)
			.expectGetDomain(domainId);
		
		play(Operations.sequenceOf(cleanDB(), 
				insertDomainConfiguration()));
		
		expect(userSystemDao.getByLogin("cyrus")).andReturn(ObmSystemUser.builder().login("cyrus").password("cyrus").id(12).build()).times(2);
		
		control.replay();
		server.start();
		
		UUID expectedRunId = TestImapArchiveModules.uuid;
		given()
			.config(RestAssuredConfig.config().redirect(RedirectConfig.redirectConfig().followRedirects(false)))
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken").
		expect()
			.header("Location", containsString("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + expectedRunId.toString()))
			.statusCode(Status.SEE_OTHER.getStatusCode()).
		when()
			.post("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments");
		Thread.sleep(1);
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.body(containsString("Starting IMAP Archive in REAL_RUN for domain mydomain"))
			.statusCode(Status.OK.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + expectedRunId.toString() + "/logs");
		
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.NOT_FOUND.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/b9a3c424-34f1-4f6d-86ab-c1025a48df9c/logs");
	}
	
	@Test
	public void getLogsShouldReturnTheLogsFromFileWhenArchiveTreatmentIsNotInTracking() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("2f096466-5a2a-463e-afad-4196c2952de3");
		expectations
			.expectTrustedLogin(domainId)
			.expectGetDomain(domainId);
		
		UUID runId = TestImapArchiveModules.uuid;
		play(Operations.sequenceOf(cleanDB(), 
				insertDomainConfiguration(), 
				insertArchiveTreatment(ArchiveTreatmentRunId.from(runId), domainId)));
		
		String expectedContent = "Old treatment file";
		temporaryFolder.create();
		File treatmentFile = temporaryFolder.newFile(runId.toString() + ".log");
		FileUtils.write(treatmentFile, expectedContent, Charsets.UTF_8);
		
		control.replay();
		server.start();
		
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.body(equalTo(expectedContent))
			.statusCode(Status.OK.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + runId.toString() + "/logs");
	}
	
	@Test
	public void getLogsShouldReturnNotFoundWhenArchiveTreatmentIsNotInTrackingAndNoFile() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("2f096466-5a2a-463e-afad-4196c2952de3");
		expectations
			.expectTrustedLogin(domainId)
			.expectGetDomain(domainId);
		
		UUID runId = TestImapArchiveModules.uuid;
		play(Operations.sequenceOf(cleanDB(), 
				insertDomainConfiguration(), 
				insertArchiveTreatment(ArchiveTreatmentRunId.from(runId), domainId)));
		
		control.replay();
		server.start();
		
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.statusCode(Status.NOT_FOUND.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + runId.toString() + "/logs");
	}
	
	@Test
	public void getLogsShouldWaitForTheLaunchWhenGettingLogsOnScheduleTreatment() throws Exception {
		ObmDomainUuid domainId = ObmDomainUuid.of("2f096466-5a2a-463e-afad-4196c2952de3");
		expectations
			.expectTrustedLogin(domainId)
			.expectGetDomain(domainId)
			.expectGetDomain(domainId);
		
		ArchiveTreatmentRunId runId = ArchiveTreatmentRunId.from(TestImapArchiveModules.uuid);
		final DateTime scheduled = TestImapArchiveModules.LOCAL_DATE_TIME.plusSeconds(2);
		play(Operations.sequenceOf(cleanDB(), 
				insertDomainConfiguration(), 
				Operations.insertInto(MailArchiveRun.NAME)
					.columns(MailArchiveRun.Fields.UUID,
							MailArchiveRun.Fields.DOMAIN_UUID,
							MailArchiveRun.Fields.STATUS, 
							MailArchiveRun.Fields.SCHEDULE,
							MailArchiveRun.Fields.HIGHER_BOUNDARY, 
							MailArchiveRun.Fields.RECURRENT)
					.values(runId.serialize(),
							domainId.get(),
							ArchiveStatus.SCHEDULED, 
							scheduled.toDate(), 
							DateTime.parse("2014-06-01T00:03:00.000Z").toDate(),
							true)
					.build()));
		
		expect(userSystemDao.getByLogin("cyrus")).andReturn(ObmSystemUser.builder().login("cyrus").password("cyrus").id(12).build()).times(2);
		
		control.replay();
		server.start();
		
		new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
				testDateProvider.setCurrent(scheduled);
			}
		}, TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
		
		Stopwatch stopwatch = Stopwatch.createStarted();
		given()
			.port(server.getHttpPort())
			.auth().basic("admin@mydomain.org", "trust3dToken")
			.contentType(ContentType.JSON).
		expect()
			.body(containsString("Starting IMAP Archive in REAL_RUN for domain mydomain"))
			.statusCode(Status.OK.getStatusCode()).
		when()
			.get("/imap-archive/service/v1/domains/2f096466-5a2a-463e-afad-4196c2952de3/treatments/" + runId.serialize() + "/logs");
		assertThat(stopwatch.elapsed(TimeUnit.SECONDS)).isGreaterThanOrEqualTo(2);
	}
}