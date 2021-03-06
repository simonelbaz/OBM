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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.obm.provisioning.dao.exceptions.DaoException;
import org.obm.sync.Right;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.dao.EntityId;
import org.obm.utils.LinkedEntity;
import org.obm.utils.ObmHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import fr.aliacom.obm.common.resource.Resource;


@Singleton
public class ResourceDao {

	private static final int ONE_QUANTITY = 1;
	private static final EntityId PUBLIC_CONSUMER_ID = null;
	
	private static final String MY_GROUPS_QUERY = "SELECT groupentity_entity_id FROM of_usergroup "
			+ "INNER JOIN GroupEntity ON of_usergroup_group_id=groupentity_group_id WHERE of_usergroup_user_id=?";

	private static final String INSERT_QUERY = "INSERT INTO resource "
			+ "(resource_domain_id, resource_name, resource_qty, resource_userupdate, resource_usercreate, resource_timeupdate) "
			+ "VALUES (?, ?, ?, ?, ?, now())";
	
	private static final String UPDATE_EMAIL_QUERY = "UPDATE resource SET resource_email=? WHERE resource_id=?";
	private static final String EMAIL_FORMAT = "res-%d@%s";
	
	private final ObmHelper obmHelper;
	private final EntityRightDao entityRightDao;
	
	@Inject
	@VisibleForTesting
	ResourceDao(ObmHelper obmHelper, EntityRightDao entityRightDao) {
		this.obmHelper = obmHelper;
		this.entityRightDao = entityRightDao;
	}
	
	public void createWithPublicRights(AccessToken token, Resource resource, Set<Right> rights) throws DaoException {
		Connection con = null;
		try {
			con = obmHelper.getConnection();
			int resourceId = createResource(con, token, resource);
			defineResourceEmail(con, token, resourceId);
			LinkedEntity link = obmHelper.linkEntity(con, "ResourceEntity", "resource_id", resourceId);
			entityRightDao.grantRights(con, link.getEntityId(), PUBLIC_CONSUMER_ID, rights);
		} catch (SQLException e) {
			throw new DaoException(e);
		} finally {
			obmHelper.cleanup(con, null, null);
		}
	}

	private int createResource(Connection con, AccessToken token, Resource resource) throws SQLException {
		PreparedStatement ps = null;

		try {
			ps = con.prepareStatement(INSERT_QUERY);

			int index = 1;
			ps.setInt(index++, token.getDomain().getId());
			ps.setString(index++, resource.getName());
			ps.setInt(index++, ONE_QUANTITY);
			ps.setInt(index++, token.getObmId());
			ps.setInt(index++, token.getObmId());
			ps.executeUpdate();

			return obmHelper.lastInsertId(con);
		} finally {
			obmHelper.cleanup(null, ps, null);
		}
	}
	
	private void defineResourceEmail(Connection con, AccessToken token, int resourceId) throws SQLException {
		PreparedStatement ps = null;

		try {
			ps = con.prepareStatement(UPDATE_EMAIL_QUERY);
			ps.setString(1, String.format(EMAIL_FORMAT, resourceId, token.getDomain().getName()));
			ps.setInt(2, resourceId);
			ps.executeUpdate();
		} finally {
			obmHelper.cleanup(null, ps, null);
		}
	}

	public Resource findAttendeeResourceFromEmailForUser(String email, Integer userId) throws SQLException {
		String q = "SELECT resource_id, resource_name, resourceentity_entity_id, resource_email "
				+ "FROM Resource "
				+ "INNER JOIN ResourceEntity ON resourceentity_resource_id = resource_id "
				+ "INNER JOIN UserEntity ON userentity_user_id = ? "
				+ "LEFT JOIN EntityRight urights ON (urights.entityright_entity_id = resourceentity_entity_id AND urights.entityright_consumer_id = userentity_entity_id) "
				+ "LEFT JOIN EntityRight grights ON (grights.entityright_entity_id = resourceentity_entity_id AND grights.entityright_consumer_id IN (" + MY_GROUPS_QUERY + ")) "
				+ "LEFT JOIN EntityRight prights ON (prights.entityright_entity_id = resourceentity_entity_id AND prights.entityright_consumer_id IS NULL) "
				+ "WHERE resource_email = ? AND (resource_usercreate = ? OR urights.entityright_access = 1 or grights.entityright_access = 1 or prights.entityright_access = 1)";
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = obmHelper.getConnection();
			ps = con.prepareStatement(q);

			ps.setInt(1, userId);
			ps.setInt(2, userId);
			ps.setString(3, email);
			ps.setInt(4, userId);
			rs = ps.executeQuery();

			if (rs.next()) {
				return resourceFromCursor(rs);
			}
		} finally {
			obmHelper.cleanup(con, ps, rs);
		}
		
		return null;
	}
	
	public Resource findAttendeeResourceFromNameForUser(String name, Integer userId) throws SQLException {
		String q = "SELECT resource_id, resource_name, resourceentity_entity_id, resource_email "
				+ "FROM Resource "
				+ "INNER JOIN ResourceEntity ON resourceentity_resource_id = resource_id "
				+ "INNER JOIN UserEntity ON userentity_user_id = ? "
				+ "LEFT JOIN EntityRight urights ON (urights.entityright_entity_id = resourceentity_entity_id AND urights.entityright_consumer_id = userentity_entity_id) "
				+ "LEFT JOIN EntityRight grights ON (grights.entityright_entity_id = resourceentity_entity_id AND grights.entityright_consumer_id IN (" + MY_GROUPS_QUERY + ")) "
				+ "LEFT JOIN EntityRight prights ON (prights.entityright_entity_id = resourceentity_entity_id AND prights.entityright_consumer_id IS NULL) "
				+ "WHERE resource_name = ? AND (resource_usercreate = ? OR urights.entityright_access = 1 or grights.entityright_access = 1 or prights.entityright_access = 1)";
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = obmHelper.getConnection();
			ps = con.prepareStatement(q);

			ps.setInt(1, userId);
			ps.setInt(2, userId);
			ps.setString(3, name);
			ps.setInt(4, userId);
			rs = ps.executeQuery();

			if (rs.next()) {
				return resourceFromCursor(rs);
			}
		} finally {
			obmHelper.cleanup(con, ps, rs);
		}
		
		return null;
	}

	private Resource resourceFromCursor(ResultSet rs) throws SQLException {
		return Resource
				.builder()
				.id(rs.getInt("resource_id"))
				.name(rs.getString("resource_name"))
				.entityId(EntityId.valueOf(rs.getInt("resourceentity_entity_id")))
				.mail(rs.getString("resource_email"))
				.build();
	}
	
}
