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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileSystemOperation {

	public static List<Path> getMatchingPaths(Path baseDirectory, String pattern, List<String> warningMessages) {

		List<Path> result = new ArrayList<>();

		// pattern, e.g. "*.{java,class,jar}"
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDirectory, pattern)) {
			for (Path entry : stream) {
				result.add(entry);
			}

		} catch (IOException e) {
			warningMessages.add(String.format("Error reading directory %s: %s", baseDirectory, e.getMessage()));
		}

		return result;
	}
	
	public static void deleteFile(Path fileToBeDeleted, String pathStartsWith, List<String> warningMessages) {
		
		// deletion protection check
		checkPathPraefix(fileToBeDeleted, pathStartsWith);	

		try {
			Files.delete(fileToBeDeleted);
		} catch (Exception e) {
			warningMessages.add(fileToBeDeleted.toString() + " could not be deleted: " + e.getMessage());
		}
	}

	public static void deletePathRecursively(Path pathToBeDeleted, String pathStartsWith, List<String> warningMessages) {
		
		// deletion protection check
		checkPathPraefix(pathToBeDeleted, pathStartsWith);
		
	    try {
			Files.walk(pathToBeDeleted)
			  .sorted(Comparator.reverseOrder())
			  .map(Path::toFile)
			  .forEach(File::delete);
		} catch (IOException e) {
			warningMessages.add(String.format("Error deleting directory %s: %s", pathToBeDeleted, e.getMessage()));
		}
	}
	
	// security check before deletion
	private static void checkPathPraefix(Path pathToBeDeleted, String pathStartsWith) {
		if (!pathToBeDeleted.toAbsolutePath().toString().startsWith(pathStartsWith)) {
			throw new RuntimeException(pathToBeDeleted+" needs to start with "+pathStartsWith+", aborting deletion");
		}
	}
}
