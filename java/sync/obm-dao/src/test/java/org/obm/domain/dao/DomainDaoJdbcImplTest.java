/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.domain.dao;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.dao.utils.H2ConnectionProvider;
import org.obm.dao.utils.H2InMemoryDatabase;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.guice.GuiceModule;
import org.obm.guice.SlowGuiceRunner;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomain.Builder;
import fr.aliacom.obm.common.domain.ObmDomainUuid;

@RunWith(SlowGuiceRunner.class)
@GuiceModule(DomainDaoJdbcImplTest.Env.class)
public class DomainDaoJdbcImplTest {

	public static class Env extends AbstractModule {

		@Override
		protected void configure() {
			bindConstant().annotatedWith(Names.named("initialSchema")).to("sql/initial.sql");

			bind(DatabaseConnectionProvider.class).to(H2ConnectionProvider.class);
			bind(DomainDao.class);
		}

	}

	@Inject
	private DomainDao dao;

	@Rule
	@Inject
	public H2InMemoryDatabase db;
	
	@Test
	public void testCreateThenGet() {
		Builder domainBuilder = ObmDomain.builder()
			.uuid(ObmDomainUuid.of("dcf3a388-6dc4-4ac1-bf4f-88c5e4457a66"))
			.name("mydomain")
			.label("my domain");
		dao.create(domainBuilder.build());
		assertThat(dao.findDomainByName("mydomain")).isEqualTo(domainBuilder.id(3).build());
	}

	@Test
	public void testGetByUuidWhenDomainDoesntExist() {
		assertThat(dao.findDomainByUuid(ObmDomainUuid.of("dcf3a388-6dc4-4ac1-bf4f-88c5e4457a66"))).isNull();
	}

	@Test
	public void testCreateThenGetByUuid() {
		ObmDomainUuid uuid = ObmDomainUuid.of("dcf3a388-6dc4-4ac1-bf4f-88c5e4457a66");
		Builder domainBuilder = ObmDomain.builder()
			.uuid(uuid)
			.name("mydomain")
			.label("my domain");

		dao.create(domainBuilder.build());

		assertThat(dao.findDomainByUuid(uuid)).isEqualTo(domainBuilder.id(3).build());
	}
	
	@Test
	public void testCreateThenList() {
		Builder domainBuilder = ObmDomain.builder()
			.uuid(ObmDomainUuid.of("dcf3a388-6dc4-4ac1-bf4f-88c5e4457a66"))
			.name("mydomain")
			.label("my domain");
		dao.create(domainBuilder.build());
		assertThat(dao.list()).containsOnly(
				domainBuilder.id(3).build(), 
				ObmDomain.builder()
					.uuid(ObmDomainUuid.of("3a2ba641-4ae0-4b40-aa5e-c3fd3acb78bf"))
					.label("test2.tlse.lng")
					.name("test2.tlse.lng")
					.id(2)
					.build(),
				ObmDomain.builder()
					.uuid(ObmDomainUuid.of("ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6"))
					.label("test.tlse.lng")
					.name("test.tlse.lng")
					.id(1)
					.build()
					);
	}
	
	@Test
	public void testCreateWithAliasThenList() {
		Builder domainBuilder = ObmDomain.builder()
			.uuid(ObmDomainUuid.of("dcf3a388-6dc4-4ac1-bf4f-88c5e4457a66"))
			.name("mydomain")
			.label("my domain")
			.alias("myalias");
		dao.create(domainBuilder.build());
		assertThat(dao.list()).containsOnly(
				domainBuilder.id(3).alias("myalias").build(), 
				ObmDomain.builder()
					.uuid(ObmDomainUuid.of("3a2ba641-4ae0-4b40-aa5e-c3fd3acb78bf"))
					.label("test2.tlse.lng")
					.name("test2.tlse.lng")
					.id(2)
					.build(),
				ObmDomain.builder()
					.uuid(ObmDomainUuid.of("ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6"))
					.label("test.tlse.lng")
					.name("test.tlse.lng")
					.id(1)
					.build()
					);
	}

	@Test
	public void testCreateWithAliasesThenList() {
		Builder domainBuilder = ObmDomain.builder()
			.uuid(ObmDomainUuid.of("dcf3a388-6dc4-4ac1-bf4f-88c5e4457a66"))
			.name("mydomain")
			.label("my domain")
			.aliases(ImmutableList.of("myalias1", "myalias2"));
		dao.create(domainBuilder.build());
		assertThat(dao.list()).containsOnly(
				domainBuilder.id(3).aliases(ImmutableList.of("myalias2", "myalias1")).build(), 
				ObmDomain.builder()
					.uuid(ObmDomainUuid.of("3a2ba641-4ae0-4b40-aa5e-c3fd3acb78bf"))
					.label("test2.tlse.lng")
					.name("test2.tlse.lng")
					.id(2)
					.build(),
				ObmDomain.builder()
					.uuid(ObmDomainUuid.of("ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6"))
					.label("test.tlse.lng")
					.name("test.tlse.lng")
					.id(1)
					.build()
				);
	}
}
