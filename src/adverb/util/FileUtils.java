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


package adverb.util;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.*;


public class FileUtils 
{
	private FileUtils() { } 
	
	
	public static Stream<File> stream(File dirf, Predicate<String> pred)
	{
		return 
			Stream.of(dirf.list())
			.filter(name -> pred.test(name))
			.map(name -> new File(dirf, name));
	}
	
	
	public static List<File> toList(File dirf, Predicate<String> pred)
	{
		return stream(dirf, pred).collect(Collectors.toList());
	}
	
	
	public static Stream<File> streamDepth2(File dirf, Predicate<String> pred)
	{
		return 
			stream(dirf, name->true)
			.filter(subdir -> subdir.isDirectory())
			.flatMap(subdir -> stream(subdir, pred));
	}
	
	
	public static List<File> toListDepth2(File dirf, Predicate<String> pred)
	{
		return streamDepth2(dirf, pred).collect(Collectors.toList());
	}	
	
	
	public static List<File> toListDepth2(File dirf, Predicate<String> pred1, Predicate<String> pred2)
	{
		List<File> ret = new ArrayList<>();
		
		for (File subdirf: toList(dirf, pred1))
		{
			assert subdirf.isDirectory();
			ret.addAll(toList(subdirf, pred2));
		}
		
		return ret;
	}	
	
	
	public static List<File> collectTsvs(File dirf)
	{
		return toList(dirf, name -> name.endsWith(".tsv"));
	}
	
	
	public static List<File> collectMusteredFamilies()
	{
		File dirf = new File("data/muster/families");
		List<File> ret = new ArrayList<>();
		for (File ch1: toList(dirf, name -> name.length()==1))
			for (File ch2: toList(ch1, name -> name.length()==1))
				ret.addAll(toList(ch2, name -> name.startsWith(ch1.getName().charAt(0) + "" + ch2.getName().charAt(0))));
		return ret;
	}
	
	
	public static void rmrf(File f)
	{
		if (f.isFile())
			f.delete();
		
		else
		{
			for (String kid: f.list())
			{
				if (kid.equals(".")  ||  kid.equals(".."))
					continue;
				File kidf = new File(f, kid);
				rmrf(kidf);
			}
			f.delete();
		}
	}
	
	
	public static List<File> collectSubdirs(File dirf)
	{
		return toList(dirf, name -> new File(dirf, name).isDirectory());
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args) throws Exception
	{
		sop("START");
		
		for (File f: collectMusteredFamilies())
			sop(f);
		
		sop("DONE");
	}
}
