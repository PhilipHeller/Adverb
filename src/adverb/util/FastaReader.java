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
import java.util.stream.*;


public class FastaReader implements AutoCloseable
{
	private PushbackLineReader			pblr;
	
	
	public FastaReader(Reader src) throws IOException
	{
		pblr = new PushbackLineReader(src);
	}
	
	
	public void close() throws IOException
	{
		pblr.close();
	}
	
	
	private static boolean isDefline(String s)	{ return s.length() == 0  ||  s.charAt(0) == '>'; }

	
	public String[] readRecord() throws IOException
	{
		boolean skippingBlankLines = true;
		while (skippingBlankLines)
		{
			String line = pblr.readLine();
			if (line == null)
				return null;
			if (line.trim().length() > 0)
			{
				skippingBlankLines = false;
				pblr.push(line);
			}
		}
		String defline = pblr.readLine().trim();
		if (defline == null)
			return null;
		if (!isDefline(defline))
			throw new IllegalArgumentException("Expected defline, found:\n" + defline);
		String seq = "";
		String line = null;
		while ((line = pblr.readLine()) != null)
		{
			if (isDefline(line))
			{
				pblr.push(line);
				return new String[] { defline, seq };
			}
			else
				seq += line.trim();
		}
		
		return new String[] { defline, seq };
	}
	
	
	// Deflines must be unique or information will be lost. Deflines in map include leading ">".
	public LinkedHashMap<String, String> getDeflineToSequenceMap() throws IOException
	{
		LinkedHashMap<String, String> ret = new LinkedHashMap<String, String>();
		String[] rec;
		while ((rec = readRecord()) != null)
			ret.put(rec[0], rec[1]);
		return ret;
	}
	
	
	public List<String> getSequences() throws IOException
	{
		List<String> ret = new ArrayList<String>();
		String[] rec;
		while ((rec = readRecord()) != null)
			ret.add(rec[1]);
		return ret;
	}
	
	
	public List<String[]> getRecords() throws IOException
	{
		List<String[]> ret = new ArrayList<String[]>();
		String[] rec;
		while ((rec = readRecord()) != null)
			ret.add(rec);
		return ret;
	}
	
	
	public static List<String[]> getRecords(File fasta) throws IOException
	{
		try
		(
			FileReader fr = new FileReader(fasta);
			BufferedReader br = new BufferedReader(fr);
			FastaReader far = new FastaReader(br);
		)
		{
			return far.getRecords();
		}
	}
	
	
	public static List<String[]> getRecordsNoThrow(File fasta) 
	{
		try
		{
			return getRecords(fasta);
		}
		catch (IOException x)
		{
			return null;
		}
	}
	
	
	public static int countRecords(File fasta) throws IOException
	{
		try
		(
			FileReader fr = new FileReader(fasta);
			BufferedReader br = new BufferedReader(fr);
			FastaReader far = new FastaReader(br);
		)
		{
			int n = 0;
			while (far.readRecord() != null)
				n++;
			return n;
		}
	}
	
	
	public static TreeBinCounter<Integer> getLengthCensus(File fasta) throws IOException
	{
		try
		(
			FileReader fr = new FileReader(fasta);
			BufferedReader br = new BufferedReader(fr);
			FastaReader far = new FastaReader(br);
		)
		{
			TreeBinCounter<Integer> ctr = new TreeBinCounter<>();
			String[] rec;
			while ((rec = far.readRecord()) != null)
				ctr.bumpCountForBin(rec[1].length());
			
			return ctr;
		}
	}
	
	
	public static boolean isAligned(File fasta) throws IOException
	{
		try
		(
			FileReader fr = new FileReader(fasta);
			BufferedReader br = new BufferedReader(fr);
			FastaReader far = new FastaReader(br);
		)
		{
			int len = far.readRecord()[1].length();
			String[] rec;
			while ((rec = far.readRecord()) != null)
				if (rec[1].length() != len)
					return false;
			return true;
		}
	}
	
	
	public static boolean deflinesAreUnique(File fasta) throws IOException
	{
		List<String[]> recs = getRecords(fasta);
		Set<String> deflines = new HashSet<>();
		for (String[] rec: recs)
			if (deflines.contains(rec[0]))
				return false;
			else
				deflines.add(rec[0]);
		return true;
	}
	
	
	public static void stripEmptyRecords(File fasta) throws IOException
	{
		List<String[]> recs = getRecords(fasta);
		List<String[]> empties =
			recs.stream()
			.filter(rec -> rec[1].trim().isEmpty())
			.collect(Collectors.toList());
		if (empties.isEmpty())
			return;
		
		File temp = new File(fasta.getParentFile(), "TEMP_FOR_stripEmptyRecords");
		try (FileWriter fw = new FileWriter(temp))
		{
			for (String[] rec: recs)
				if (!empties.contains(rec))
					fw.write(rec[0] + "\n" + rec[1] + "\n");
		}
		fasta.delete();
		temp.renameTo(fasta);
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args) throws Exception
	{
		sop("START");
		File f = new File("data/ScaleUp/hmms/004__P_Arthropoda__C_Arachnida__O_Araneae__F_Titanoecidae/G_Titanoeca.fa");
		stripEmptyRecords(f);
		sop("DONE");
	}
}
