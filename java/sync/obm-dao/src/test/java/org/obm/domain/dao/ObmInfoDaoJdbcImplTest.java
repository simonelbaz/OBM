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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.dao.utils.DaoTestModule;
import org.obm.dao.utils.H2InMemoryDatabase;
import org.obm.dao.utils.H2InMemoryDatabaseRule;
import org.obm.dao.utils.H2TestClass;
import org.obm.guice.GuiceModule;
import org.obm.guice.GuiceRunner;
import org.obm.provisioning.dao.exceptions.DaoException;

import com.google.inject.Inject;

@RunWith(GuiceRunner.class)
@GuiceModule(DaoTestModule.class)
public class ObmInfoDaoJdbcImplTest implements H2TestClass {

	@Rule public H2InMemoryDatabaseRule dbRule = new H2InMemoryDatabaseRule(this, "sql/initial.sql");
	@Inject H2InMemoryDatabase db;

	@Override
	public H2InMemoryDatabase getDb() {
		return db;
	}

	@Inject
	private ObmInfoDaoJdbcImpl dao;

	@Test
	public void testGetUidMaxUsedWhenNoValueInDb() throws Exception {
		assertThat(dao.getUidMaxUsed()).isNull();
	}

	@Test
	public void testGetUidMaxUsed() throws Exception {
		db.executeUpdate("INSERT INTO ObmInfo (obminfo_name, obminfo_value) VALUES ('uid_max_used', 123)");

		assertThat(dao.getUidMaxUsed()).isEqualTo(123);
	}

	@Test(expected = DaoException.class)
	public void testUpdateUidMaxUsedWhenNoValueInDb() throws Exception {
		dao.updateUidMaxUsed(1000);
	}

	@Test
	public void testGetAfterUpdateUidMaxUsed() throws Exception {
		dao.insertUidMaxUsed(123);
		dao.updateUidMaxUsed(1000);

		assertThat(dao.getUidMaxUsed()).isEqualTo(1000);
	}

	@Test
	public void testGetAfterInsertUidMaxUsed() throws Exception {
		dao.insertUidMaxUsed(123);

		assertThat(dao.getUidMaxUsed()).isEqualTo(123);
	}

	@Test
	public void testInsertUidMaxUsed() throws Exception {
		assertThat(dao.insertUidMaxUsed(123)).isEqualTo(123);
	}

	@Test
	public void testUpdateUidMaxUsed() throws Exception {
		db.executeUpdate("INSERT INTO ObmInfo (obminfo_name, obminfo_value) VALUES ('uid_max_used', 123)");

		assertThat(dao.updateUidMaxUsed(1000)).isEqualTo(1000);
	}
}