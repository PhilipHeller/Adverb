/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 *    Copyright (C) 2021 Philip Heller.
 *    
 */


package adverb.hmm;

import java.util.*;


class LogOddsDPStage<S> extends LinkedHashMap<S, LogOddsDPCell<S>>
{
	public String toString()
	{
		String s = "LogOddsDPStage:";
		for (S state: new TreeSet<>(keySet()))
		{
			LogOddsDPCell<S> cell = get(state);
			if (cell == null  ||  cell.isEmpty())
				continue;
			s += "\n  For state " + state + " ... " + get(state);
		}
		return s;
	}

}
