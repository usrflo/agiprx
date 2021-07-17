package de.agitos.agiprx.util;

import java.util.List;

public class ListUtils {

	public static <T> boolean replace(List<T> list, T o) {
		int idx = list.indexOf(o);
		if (idx >= 0) {
			list.set(idx, o);
			return true;
		}
		return false;
	}

}
