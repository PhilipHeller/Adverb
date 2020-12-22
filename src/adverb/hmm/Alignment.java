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

import java.io.*;
import java.util.*;
import java.util.stream.*;

import adverb.util.*;


@SuppressWarnings("serial")
public class Alignment extends ArrayList<String>
{
	private String					name;
	private List<String>			deflines = new ArrayList<>();  		// generated if source isn't fasta
	
	
	public Alignment(Collection<String> src) 
	{
		addAll(src);
	}
	

	// Assumes all seqs are single lines and all seq lengths are identical.
	public Alignment(File fasta) throws IOException, IllegalArgumentException
	{
		this.name = fasta.getName();
					
		try
		(
			FileReader fr = new FileReader(fasta);
			BufferedReader br = new BufferedReader(fr);
			FastaReader far = new FastaReader(br);
		)
		{
			String[] rec;
			while ((rec = far.readRecord()) != null)
			{
				deflines.add(rec[0]);
				this.add(rec[1]);
			}		
		}
	}
	
	
	// For debugging.
	public Alignment() { }
	
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public Stream<Character> streamColumn(int colNum)
	{
		return stream().map(s -> s.charAt(colNum));
	}
	
	
	public TreeBinCounter<Character> binCountColumn(int colNum)
	{
		return
			streamColumn(colNum)
			.collect(MoreCollectors.toTreeBinCounter());
	}
	
	
	public String toString()
	{
		String s = "Alignment of " + nCols() + " columns:";
		for (String seq: this)
			s += "\n  " + seq;
		return s;
	}
	
	
	public List<String[]> toFastaRecords()
	{
		List<String[]> ret = new ArrayList<>();
		for (int i=0; i<size(); i++)
			ret.add(new String[] { deflines.get(i), get(i) });
		return ret;
	}
	
	
	private class IndelRun
	{
		int 	startCol;
		int		len;
		
		IndelRun(int startCol)
		{
			this.startCol = startCol;
			len = 1;
		}
		
		public String toString()
		{
			return "IndelRun at " + startCol + ", length = " + len;
		}
	}  // IndelRun
	
	
	private Stack<IndelRun> collectIndelRuns(String seq)
	{
		Stack<IndelRun> ret = new Stack<>();
		boolean inIndel = false;
		for (int i=0; i<seq.length(); i++)
		{
			if (seq.charAt(i) == '-')
			{
				// Saw an indel. Start or extend a run.
				if (inIndel)
					ret.peek().len++;
				else
				{
					inIndel = true;
					ret.push(new IndelRun(i));
				}
			}
			else
				inIndel = false;
		}
		
		return ret;
	}
	
	
	public ArrayList<TreeBinCounter<Integer>> getGapLengthCtrsByStartCol()
	{
		// Collect indel runs.
		ArrayList<IndelRun> runs = new ArrayList<>();
		this
			.stream()
			.filter(s -> !s.startsWith(">"))
			.map(seq -> collectIndelRuns(seq))
			.forEach(stack -> runs.addAll(stack));
	
		// For every column in the alignment, make a bin counter of lengths of
		// indel runs that start in that column.
		ArrayList<TreeBinCounter<Integer>>  gapLengthCtrsByStartCol = new ArrayList<>();
		for (int i=0; i<this.nCols(); i++)
			gapLengthCtrsByStartCol.add(new TreeBinCounter<Integer>());
		for (IndelRun run: runs)
		{
			TreeBinCounter<Integer> ctr = gapLengthCtrsByStartCol.get(run.startCol);
			ctr.bumpCountForBin(run.len);
		}
		
		return gapLengthCtrsByStartCol;
	}
	
	
	public int nRows()
	{
		return size();
	}
	
	
	public int nCols()
	{
		return get(0).length();
	}
	
	
	// Returns this alignment in case we're streaming.
	public Alignment trim(int nTrimFromStart, int nTrimFromEnd)
	{
		for (int i=0; i<size(); i++)
		{
			String s = get(i);
			s = s.substring(nTrimFromStart);
			s = s.substring(0, s.length()-nTrimFromEnd);
			set(i, s);
		}
		return this;
	}
	
	
	// Call this after trimming to remove all-gap records.
	public Alignment removeAllGapRecords()
	{
		Stack<Integer> indices = new Stack<>();
		for (int i=0; i<size(); i++)
		{
			String s = get(i);
			if (s.replace("-", "").isEmpty())
				indices.push(i);
		}
		while (!indices.isEmpty())
		{
			int i = indices.pop();
			remove(i);
			if (deflines != null)
				deflines.remove(i);
		}
		return this;
	}
	
	
	static void sop(Object x) 		{ System.out.println(x); }
	
	
	public static void main(String[] args) throws IOException
	{
		sop("START");
		
		File alignedFasta = new File("/Users/pheller/Research/Adverb/ArticleWorkspace/articleproj/data/SerializedHMMs/Metazoa/Acanthocephala/Archiacanthocephala/Gigantorhynchida/Gigantorhynchidae/Mediorhynchus/training_aligned.fa");
		sop(alignedFasta.getAbsolutePath());
		Alignment al = new Alignment(alignedFasta);
		sop(al);
	}
}
