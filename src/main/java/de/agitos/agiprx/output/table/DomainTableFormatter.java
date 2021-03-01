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

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.model.Domain;

public class DomainTableFormatter {

	private ConsoleTableBuffer tableBuf;

	public DomainTableFormatter(int maxTotalWidth) {

		this.tableBuf = new ConsoleTableBuffer(maxTotalWidth);
		tableBuf.addColumn(new LongColumn("id", 4));
		tableBuf.addColumn(new StringColumn("domainname", 20));
		tableBuf.addColumn(new StringColumn("backend", 10));
		tableBuf.addColumn(new StringColumn("cert", 5));
		tableBuf.addColumn(new StringColumn("redirectToUrl", 15).setMinorImportance(true));
	}

	public DomainTableFormatter(ConsoleWrapper console) {
		this(console.getTerminalColumns());
	}

	public void addDomain(Domain model) {
		String certShort = "";
		if (model.getLetsEncrypt()) {
			certShort = "LESSL";
		} else if (model.getCertProvided()) {
			certShort = "SSL";
		}

		Row row = new Row(model.getId(), model.getDomain(), model.getBackend().getLabel(), certShort,
				model.getRedirectToUrl());
		tableBuf.addRow(row);
	}

	public void printTable(ConsoleWrapper console, String fmtPrefix) {
		tableBuf.printTable(console, fmtPrefix);
	}

	public void printTable(StringBuilder buf, String fmtPrefix, String overrideFooter) {
		tableBuf.printTable(buf, fmtPrefix, overrideFooter);
	}
}
