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
package de.agitos.agiprx.output.table;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class ConsoleTableBufferTest {

	@Test
	public void checkTableOutput() throws IOException {

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(69);
		tableBuf.addColumn(new LongColumn("id", 5));
		tableBuf.addColumn(new StringColumn("label", 15));
		tableBuf.addColumn(new StringColumn("fullname", 40).setMinorImportance(true));
		tableBuf.addColumn(new StringColumn("note", 10).setMinorImportance(true).setMaxLength(20));
		tableBuf.addColumn(new IntegerColumn("#containers", 5));
		tableBuf.addColumn(new IntegerColumn("#backends", 5));

		Row row = new Row(10L, "myLabel", null, "some note on this project, can be a bit longer, however ...", 1, 2);
		tableBuf.addRow(row);

		row = new Row(11L, "Other label", "Ein Testprojekt mit einem etwas längeren Namen",
				"here a note as well, anything you can imagine", 3, null);
		tableBuf.addRow(row);

		StringBuilder buf = new StringBuilder();

		tableBuf.printTable(buf, "\t", null);

		StringBuilder check = new StringBuilder();

		check.append(
				"	| id    | label           | fullname                                 | note               | #cont | #back |\n");
		check.append(
				"	|    10 | myLabel         |                                          | some note on this  |     1 |     2 |\n");
		check.append(
				"	|    11 | Other label     | Ein Testprojekt mit einem etwas längeren | here a note as wel |     3 |     0 |\n");
		check.append("	--- 2 record(s) ---\n");

		Assert.assertEquals("table output needs to match", buf.toString(), check.toString());
	}

}
