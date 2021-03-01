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
package de.agitos.agiprx.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileSystemOperationTest {

	private Path parentPath;
	
	@Before
	public void setup() {
		try {
			this.parentPath = Files.createTempDirectory("tmp-");
		} catch (IOException e) {
			throw new RuntimeException("Cannot create temporary parentPath", e);
		}
	}

	@Test
	public void checkDomainTest() throws IOException {

		List<String> warningMessages = new ArrayList<>();
		
		Path path1 = this.parentPath.resolve("test.de.conf");
		Path path2 = this.parentPath.resolve("test.de-0001.conf");
		Path path3 = this.parentPath.resolve("test2.de.conf");
		
		Files.createFile(path1);
		Files.createFile(path2);
		Files.createFile(path3);

		List<Path> result = FileSystemOperation.getMatchingPaths(this.parentPath, "test.de"+"{.conf,-*.conf}", warningMessages);
		
		Assert.isTrue(warningMessages.isEmpty(), "warnings available");
		Assert.isTrue(result.size()==2, "2 files need to match");
		Assert.isTrue(result.get(0).getFileName().toString().equals("test.de.conf") || result.get(1).getFileName().toString().equals("test.de.conf"), "test.de.conf is not available");
		Assert.isTrue(result.get(0).getFileName().toString().equals("test.de-0001.conf") || result.get(1).getFileName().toString().equals("test.de-0001.conf"), "test.de-0001.conf is not available");
		
		Files.delete(path1);
		Files.delete(path2);
		Files.delete(path3);
	}
	
	@Test
	public void deletePathRecursivelyTest() throws IOException {
		
		List<String> warningMessages = new ArrayList<>();
		
		Path path1 = this.parentPath.resolve("test.de");
		Path path2 = this.parentPath.resolve("test.de-0001");
		Path path3 = this.parentPath.resolve("test2.de");
		
		Files.createDirectory(path1);
		Files.createFile(path1.resolve("subFile1"));
		Files.createFile(path1.resolve("subFile2"));
		Files.createDirectory(path2);
		Files.createFile(path2.resolve("subFile3"));
		Files.createFile(path2.resolve("subFile4"));
		Files.createDirectory(path3);

		List<Path> pathsToBeDeleted = FileSystemOperation.getMatchingPaths(this.parentPath, "test.de"+"{,-*}", warningMessages);
		
		Assert.isTrue(warningMessages.isEmpty(), "warnings available");
		Assert.isTrue(pathsToBeDeleted.size()==2, "2 files need to match");
		Assert.isTrue(pathsToBeDeleted.get(0).getFileName().toString().equals("test.de") || pathsToBeDeleted.get(1).getFileName().toString().equals("test.de"), "test.de is not available");
		Assert.isTrue(pathsToBeDeleted.get(0).getFileName().toString().equals("test.de-0001") || pathsToBeDeleted.get(1).getFileName().toString().equals("test.de-0001"), "test.de-0001 is not available");		

		for (Path pathToBeDeleted : pathsToBeDeleted) {
			FileSystemOperation.deletePathRecursively(pathToBeDeleted, Paths.get(this.parentPath.toAbsolutePath().toString(), "test.de").toAbsolutePath().toString(), warningMessages);
		}
		
		Assert.isTrue(warningMessages.isEmpty(), "warnings available");
		Assert.isTrue(Files.notExists(path1), "test.de needs not to exist");
		Assert.isTrue(Files.notExists(path2), "test.de-0001 needs not to exist");
		Assert.isTrue(Files.exists(path3), "test2.de needs to exist");

		Files.delete(path3);
	}

	@After
	public void tearDown() throws IOException {
		Files.deleteIfExists(parentPath);
	}	
	
}
