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

public class IntegerColumn extends AbstractColumn {

	public IntegerColumn(String label, int minLength) {
		super(label, minLength);
	}
	
	public Integer registerAndTransformValue(Object value) {
		
		if (value==null) {
			return 0;
		}
		
		String l = String.valueOf(value);

		if (l != null && l.length() > this.getActualLength()) {
			this.setActualLength(l.length());
		}
		
		return (Integer) value;
	}

	public String getFormat() {
		int length = this.getLength();
		return "%"+length+"d";
	}
}
