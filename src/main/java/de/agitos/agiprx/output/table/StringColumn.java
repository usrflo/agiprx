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

public class StringColumn extends AbstractColumn {

	public StringColumn(String label, int minLength) {
		super(label, minLength);
	}

	public String registerAndTransformValue(Object value) {
		
		if (value==null) {
			return "";
		}

		String s = (String) value;

		if (s != null && s.length() > this.getActualLength()) {
			this.setActualLength(s.length());
		}
		
		return s;
	}

	public String getFormat() {
		int length = this.getLength();
		return "%-"+length+"."+length+"s";
	}
}
