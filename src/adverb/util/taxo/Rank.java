package adverb.util.taxo;

import java.util.*;
import java.util.stream.*;

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

public enum Rank
{
	KINGDOM, PHYLUM, CLASS, ORDER, FAMILY, GENUS, SPECIES;

	
	public Rank higher()					
	{ 
		return (this == Rank.KINGDOM)   ?  null  :  values()[ordinal()-1]; 
	}
	
	
	public Rank lower()					
	{
		return (this == Rank.SPECIES)  ?  null  :  values()[ordinal()+1]; 
	}
	
	
	public boolean outranks(Rank that)
	{
		return this.ordinal() < that.ordinal();
	}
	
	
	public static Rank forInitial(char ch)
	{
		for (Rank r: values())
			if (r.name().startsWith(""+ch))
				return r;
		return null;
	}
	
	
	public char initial()
	{
		return name().charAt(0);
	}
	
	
	public static char nextInitial(char initial)
	{
		Rank r = forInitial(initial);
		if (r == null  ||  r.lower() == null)
			throw new IllegalArgumentException();
		return r.lower().name().charAt(0);
	}
	
	
	// Can't have a MySQL column named "order".
	public String toSqlColumnName()
	{
		return (this != ORDER)  ?  name().toLowerCase()  :  "ordr";
	}
	
	
	public static Rank forSqlColumnName(String s)
	{
		if (s.equals("ordr"))
			return Rank.ORDER;
		else return valueOf(s.toUpperCase());
	}
	
	
	// Highest means highest rank, not highest ordinal.
	public static Stream<Rank> streamRange(Rank highest, Rank lowest)
	{
		return
			Stream.of(values())
			.filter(r -> r.ordinal() >= highest.ordinal())
			.filter(r -> r.ordinal() <= lowest.ordinal());
	}
	
	
	public static String toMysqlEnumPhrase()
	{
		return 
			Stream.of(values())
			.map(r -> r.toSqlColumnName())
			.map(name -> "'" + name + "'")
			.collect(Collectors.joining(",", "ENUM(", ")"));
	}
	
	
	// Highest means highest rank, not highest ordinal.
	public static List<Rank> values(Rank highest, Rank lowest)
	{
		assert highest.ordinal() <= lowest.ordinal();
		
		return streamRange(highest, lowest).collect(Collectors.toList());
	}
	
	
	public static void main(String[] args)
	{
		System.out.println(toMysqlEnumPhrase());
	}
}