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
package de.agitos.agiprx.rest;

import java.util.ArrayList;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.agitos.agiprx.dto.ProjectDto;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.Project;

public class ProjectServiceTest {

	private ProjectDto projectDto;

	@Before
	public void setupTests() {

		Project project = new Project();
		project.setId(1L);
		project.setLabel("test");

		List<Backend> backends = new ArrayList<>();

		Backend backend = new Backend();
		backend.setId(2L);
		backend.setLabel("testbackend");
		backend.setPort(8009);
		backend.setProject(project);

		backends.add(backend);
		project.setBackends(backends);

		projectDto = new ProjectDto(project);
	}

	@Test
	public void testProjectDtoSerialization() {

		Jsonb jsonb = JsonbBuilder.create();
		String defaultMarshalling = jsonb.toJson(projectDto);

		String expected = "{\"backends\":[{\"FQLabel\":\"test_2_testbackend\",\"backendContainers\":[],\"domainForwardings\":[],\"globalBackend\":false,\"label\":\"testbackend\",\"port\":8009,\"projectId\":1}],\"containers\":[],\"label\":\"test\"}";

		Assert.assertEquals(null, expected, defaultMarshalling);
	}

}
