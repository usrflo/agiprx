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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.agitos.agiprx.ConsoleWrapper;

@Ignore
public class ConsoleTableBufferTest {

	private ConsoleWrapper console;

	@Before
	public void setup() {
		this.console = ConsoleWrapper.getBean();
	}	

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
		
		row = new Row(11L, "Other label", "Ein Testprojekt mit einem etwas längeren Namen", "here a note as well, anything you can imagine", 3, null);
		tableBuf.addRow(row);

		tableBuf.printTable(console, "\t");
	}

}
