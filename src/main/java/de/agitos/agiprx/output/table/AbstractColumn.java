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

public abstract class AbstractColumn {

	private final String label;
	private final int minLength;
	private int maxLength = -1;
	private int actualLength;
	private boolean minorImportance = false;

	public AbstractColumn(String label, int minLength) {
		this.label = label;
		this.minLength = minLength;
		this.actualLength = minLength;
	}
	
	public String getLabel() {
		return label;
	}

	public int getMinLength() {
		return minLength;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public AbstractColumn setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	public int getActualLength() {
		return actualLength;
	}

	public void setActualLength(int actualLength) {
		this.actualLength = actualLength;
	}

	public boolean isMinorImportance() {
		return minorImportance;
	}

	public AbstractColumn setMinorImportance(boolean minorImportance) {
		this.minorImportance = minorImportance;
		return this;
	}

	public int getLength() {
		if (maxLength>0 && maxLength<actualLength) {
			return maxLength;
		}
		return actualLength;
	}

	public abstract Object registerAndTransformValue(Object value);

	public String getHeaderFormat() {
		int length = this.getLength();
		return "%-"+length+"."+length+"s";
	}
	
	public abstract String getFormat();
}
