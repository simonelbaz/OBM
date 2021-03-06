/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2011-2014  Linagora
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

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.dao.utils.DaoTestModule;
import org.obm.dao.utils.H2InMemoryDatabase;
import org.obm.dao.utils.H2InMemoryDatabaseRule;
import org.obm.dao.utils.H2TestClass;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import fr.aliacom.obm.ToolBox;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserEmails;
import fr.aliacom.obm.common.user.UserLogin;
import fr.aliacom.obm.common.user.UserIdentity;

@RunWith(GuiceRunner.class)
@GuiceModule(DaoTestModule.class)
public class UserPatternDaoJdbcImplTest implements H2TestClass {

	private final UserLogin jdoeLogin = UserLogin.valueOf("jdoe");
	
	@Rule public H2InMemoryDatabaseRule dbRule = new H2InMemoryDatabaseRule(this, "sql/initial.sql");
	@Inject H2InMemoryDatabase db;

	@Override
	public H2InMemoryDatabase getDb() {
		return db;
	}
	
	@Inject
	private UserPatternDaoJdbcImpl dao;
	
	@Test
	public void testGetUserPatterns() {
		ObmUser user = ObmUser
				.builder()
				.uid(1)
				.login(jdoeLogin)
				.identity(UserIdentity.builder()
					.lastName("Doe")
					.firstName("John")
					.commonName("J. Doe")
					.build())
				.emails(UserEmails.builder()
					.addAddress("jdoe")
					.addAddress("john.doe")
					.domain(ToolBox.getDefaultObmDomain())
					.build())
				.domain(ToolBox.getDefaultObmDomain())
				.build();
		Set<String> patterns = ImmutableSet.of(
				"jdoe", // Login + first email (single occurence in the set)
				"john.doe", // Second email
				"John", // Firstname
				"Doe"); // Lastname

		assertThat(dao.getUserPatterns(user)).isEqualTo(patterns);
	}

	@Test
	public void testGetUserPatternsWithLoginOnly() {
		ObmUser user = ObmUser
				.builder()
				.uid(1)
				.login(jdoeLogin)
				.domain(ToolBox.getDefaultObmDomain())
				.build();
		Set<String> patterns = ImmutableSet.of("jdoe");

		assertThat(dao.getUserPatterns(user)).isEqualTo(patterns);
	}

	@Test
	public void testUpdateUserIndex() throws Exception {
		ObmUser user = ObmUser
				.builder()
				.uid(1)
				.login(jdoeLogin)
				.identity(UserIdentity.builder()
					.lastName("Doe")
					.firstName("John")
					.commonName("J. Doe")
					.build())
				.emails(UserEmails.builder()
					.addAddress("jdoe")
					.addAddress("john.doe")
					.domain(ToolBox.getDefaultObmDomain())
					.build())
				.domain(ToolBox.getDefaultObmDomain())
				.build();

		dao.updateUserIndex(user);

		Set<String> patterns = Sets.newHashSet();
		Set<String> expectedPatterns = ImmutableSet.of("jdoe", "john.doe", "john", "doe");
		ResultSet rs = db.execute("SELECT pattern FROM _userpattern WHERE id = ?", user.getUid());

		while (rs.next()) {
			patterns.add(rs.getString(1));
		}

		assertThat(patterns).isEqualTo(expectedPatterns);
	}

	@Test
	public void testUpdateUserIndexClearsOldIndex() throws Exception {
		db.executeUpdate("INSERT INTO _userpattern (id, pattern) VALUES (1, 'p1'), (1, 'p2')");

		ObmUser user = ObmUser
				.builder()
				.uid(1)
				.login(jdoeLogin)
				.domain(ToolBox.getDefaultObmDomain())
				.build();

		dao.updateUserIndex(user);

		Set<String> patterns = Sets.newHashSet();
		Set<String> expectedPatterns = ImmutableSet.of("jdoe");
		ResultSet rs = db.execute("SELECT pattern FROM _userpattern WHERE id = ?", user.getUid());

		while (rs.next()) {
			patterns.add(rs.getString(1));
		}

		assertThat(patterns).isEqualTo(expectedPatterns);
	}

}
