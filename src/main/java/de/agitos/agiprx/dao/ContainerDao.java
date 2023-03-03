/*******************************************************************************
 * Copyright (C) 2021 Florian Sager, www.agitos.de
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.agitos.agiprx.dao;

import java.sql.Types;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.dao.mapper.ContainerRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.Host;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.util.Assert;

public class ContainerDao extends AbstractDao implements DependencyInjector {

	private static ContainerDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`label`,"
			+ "`fullname`,"
			+ "`host_id`,"
			+ "`project_id`,"
			+ "`ipv6`"
			+ " FROM `container`";

	private static final String INSERT_STMT =
			"INSERT INTO `container` ("
			+ "`version`,"
			+ "`label`,"
			+ "`fullname`,"
			+ "`host_id`,"
			+ "`project_id`,"
			+ "`ipv6`"					
			+ ") VALUES (0, :label, :fullname, :host_id, :project_id, :ipv6)";
	
	private static final String UPDATE_STMT =
			"UPDATE `container` SET "
			+ "`version` = :version,"
			+ "`label` = :label,"
			+ "`fullname` = :fullname,"
			+ "`host_id` = :host_id,"
			+ "`project_id` = :project_id,"
			+ "`ipv6` = :ipv6"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `container` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	private HostDao hostDao;

	private ProjectDao projectDao;

	private ContainerPermissionDao containerPermissionDao;

	public ContainerDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		this.hostDao = HostDao.getBean();
		this.projectDao = ProjectDao.getBean();
		this.containerPermissionDao = ContainerPermissionDao.getBean();
	}

	public static ContainerDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(Container model) {

		try {

			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("label", model.getLabel());
			parameters.addValue("fullname", model.getFullname());
			parameters.addValue("host_id", model.getHostId());
			parameters.addValue("project_id", model.getProjectId());
			parameters.addValue("ipv6", model.getIpv6());

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());

			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}

	}

	public Container find(Long id) {
		try {
			Container container = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new ContainerRowMapper());

			initRelations(container, EnumSet.of(RelationType.ALL));

			return container;

		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Container find(Project project, String label) {
		try {
			Container container = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE project_id = ? AND label = ?",
					new Object[] { project.getId(), label }, new int[] { Types.NUMERIC, Types.VARCHAR },
					new ContainerRowMapper());

			initRelations(container, EnumSet.of(RelationType.ALL));

			return container;

		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<Container> findAll(EnumSet<RelationType> relationTypes) {
		List<Container> result = jdbcTemplate.query(SELECT_ALL_STMT, new ContainerRowMapper());

		for (Container container : result) {
			initRelations(container, relationTypes);
		}

		return result;
	}

	public List<Container> findAllByProject(Project project, EnumSet<RelationType> relationTypes) {
		List<Container> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE project_id = ?",
				new Object[] { project.getId() }, new int[] { Types.NUMERIC }, new ContainerRowMapper());

		for (Container container : result) {
			container.setProject(project);
			initRelations(container, relationTypes);
		}

		return result;
	}

	public Set<Long> findAllIdsByProject(Long projectId) {
		Set<Long> result = new HashSet<Long>();
		List<Container> containers = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE project_id = ?",
				new Object[] { projectId }, new int[] { Types.NUMERIC }, new ContainerRowMapper());
		for (Container container : containers) {
			result.add(container.getId());
		}
		return result;
	}

	public List<Container> findAllByHost(Host host, EnumSet<RelationType> relationTypes) {
		List<Container> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE host_id = ?",
				new Object[] { host.getId() }, new int[] { Types.NUMERIC }, new ContainerRowMapper());

		for (Container container : result) {
			container.setHost(host);
			initRelations(container, relationTypes);
		}

		Collections.sort(result, new Comparator<Container>() {

			@Override
			public int compare(Container o1, Container o2) {
				int cmp = o1.getProject().getLabel().compareTo(o2.getProject().getLabel());
				if (cmp != 0) {
					return cmp;
				}
				return o1.getLabel().compareTo(o2.getLabel());
			}

		});

		return result;
	}

	public List<Container> findAllWithFilterIPv6(String filter) {
		List<Container> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE ipv6 LIKE ?", new Object[] { filter },
				new int[] { Types.VARCHAR }, new ContainerRowMapper());

		for (Container container : result) {
			initRelations(container, EnumSet.of(RelationType.ALL));
		}

		return result;
	}

	private void initRelations(Container container, EnumSet<RelationType> relationTypes) {

		if (relationTypes == null) {
			return;
		}

		boolean updateAll = relationTypes.contains(RelationType.ALL);

		if ((updateAll || relationTypes.contains(RelationType.PERMISSION))
				&& container.getContainerPermissions() == null) {
			container.setContainerPermissions(containerPermissionDao.findAllByContainer(container.getId()));
		}

		if ((updateAll || relationTypes.contains(RelationType.HOST)) && container.getHost() == null) {
			container.setHost(hostDao.find(container.getHostId()));
		}

		if ((updateAll || relationTypes.contains(RelationType.PROJECT)) && container.getProject() == null) {
			container.setProject(projectDao.find(container.getProjectId(), null));
		}
	}

	// @Transactional
	public void update(Container model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("label", model.getLabel());
			parameters.addValue("fullname", model.getFullname());
			parameters.addValue("host_id", model.getHostId());
			parameters.addValue("project_id", model.getProjectId());
			parameters.addValue("ipv6", model.getIpv6());

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {

			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(Container model) {

		try {
			dataSourceUtils.startTransaction();

			initRelations(model, EnumSet.of(RelationType.PERMISSION));

			for (ContainerPermission permission : model.getContainerPermissions()) {
				containerPermissionDao.delete(permission);
			}

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}
}
