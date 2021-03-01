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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.agitos.agiprx.ConsoleWrapper;

public class ConsoleTableBuffer {

	private int maxTotalWidth = 0;
	private List<AbstractColumn> columns = new ArrayList<>();
	private List<Row> rows = new ArrayList<>();

	public ConsoleTableBuffer(int maxTotalWidth) {
		this.maxTotalWidth = maxTotalWidth;
	}

	public void addColumn(AbstractColumn col) {
		this.columns.add(col);
	}

	public Object[] getColumnLabels() {

		String[] labels = new String[this.columns.size()];

		for (int i = 0; i < this.columns.size(); i++) {
			labels[i] = this.columns.get(i).getLabel();
		}

		return labels;
	}

	public void addRow(Row row) {

		if (row.getValues().length != this.columns.size()) {
			throw new RuntimeException("Table has " + this.columns.size() + " columns, row differs with "
					+ row.getValues().length + " columns");
		}

		for (int i = 0; i < this.columns.size(); i++) {

			AbstractColumn col = columns.get(i);
			Object val = row.getColumn(i);
			row.setValue(i, col.registerAndTransformValue(val));
		}

		this.rows.add(row);
	}

	public void printTable(ConsoleWrapper console, String fmtPrefix) {

		StringBuilder columnHeaderFormat = new StringBuilder();
		StringBuilder columnRowFormat = new StringBuilder();
		generateTableFormat(columnHeaderFormat, columnRowFormat);

		// TODO: remove this reset
		fmtPrefix = "";

		// output table, print header
		console.printlnfStress(columnHeaderFormat.toString(), this.getColumnLabels());

		// output table, print body
		for (Row row : this.rows) {
			console.printlnf(columnRowFormat.toString(), row.getValues());
		}

		// output footer
		console.printlnf("--- " + this.rows.size() + " record(s) ---");
	}

	public void printTable(StringBuilder buf, String fmtPrefix, String overrideFooter) {

		StringBuilder columnHeaderFormat = new StringBuilder();
		StringBuilder columnRowFormat = new StringBuilder();
		generateTableFormat(columnHeaderFormat, columnRowFormat, fmtPrefix);

		// output table, print header
		buf.append(String.format(columnHeaderFormat.toString(), this.getColumnLabels())).append("\n");

		// output table, print body
		for (Row row : this.rows) {
			buf.append(String.format(columnRowFormat.toString(), row.getValues())).append("\n");
		}

		// output footer
		if (overrideFooter != null) {
			buf.append(String.format(fmtPrefix + "--- " + overrideFooter + " ---\n"));
		} else {
			buf.append(String.format(fmtPrefix + "--- " + this.rows.size() + " record(s) ---\n"));
		}
	}

	private void generateTableFormat(StringBuilder headerFmt, StringBuilder rowFmt) {
		generateTableFormat(headerFmt, rowFmt, "");
	}

	private void generateTableFormat(StringBuilder headerFmt, StringBuilder rowFmt, String fmtPrefix) {
		// calculate column width and return format string
		// | %-10.10s |

		String prefix = String.format(fmtPrefix);

		int sumLength = prefix.length() + 1; // prefix and starting '|'
		Set<Integer> minorImportantCols = new HashSet<Integer>();

		for (int i = 0; i < this.columns.size(); i++) {
			AbstractColumn col = this.columns.get(i);
			sumLength += col.getLength();
			sumLength += 3; // separator ' | '
			if (col.isMinorImportance()) {
				minorImportantCols.add(i);
			}
		}

		// reduce length of minor important columns if required
		while (sumLength > maxTotalWidth && minorImportantCols.size() > 0) {
			int reducePerCol = (sumLength - maxTotalWidth) / minorImportantCols.size();

			Set<Integer> minifiedColIds = new HashSet<Integer>();
			for (Integer colId : minorImportantCols) {
				AbstractColumn col = this.columns.get(colId);
				if (col.getActualLength() == col.getMinLength()) {
					minifiedColIds.add(colId);
				} else if (col.getActualLength() - reducePerCol < col.getMinLength()) {
					int diff = col.getActualLength() - col.getMinLength();
					col.setActualLength(col.getMinLength());
					sumLength -= diff;
					minifiedColIds.add(colId);
				} else {
					col.setActualLength(col.getActualLength() - reducePerCol);
					sumLength -= reducePerCol;
				}
			}

			minorImportantCols.removeAll(minifiedColIds);
		}

		headerFmt.append(fmtPrefix).append("|");
		rowFmt.append(fmtPrefix).append("|");

		for (int i = 0; i < this.columns.size(); i++) {
			AbstractColumn col = this.columns.get(i);
			headerFmt.append(" ").append(col.getHeaderFormat()).append(" |");
			rowFmt.append(" ").append(col.getFormat()).append(" |");
		}
	}
}
