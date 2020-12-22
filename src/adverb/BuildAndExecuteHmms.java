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



package adverb;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import adverb.util.*;
import adverb.util.taxo.*;
import adverb.hmm.*;

import static adverb.CollectTrainingSets.ALIGNED_TRAINING_DIR_NAME;


public class BuildAndExecuteHmms 
{
	private static String			theQuery;
	private static Reporter			theReporter = new Reporter();
	
	
	static void printUsageAndExit()
	{
		sop("Usage: java -cp Adverb_1.0.jar adverb.BuildAndExecuteHmms nuc-query-seq parallel");
		sop("Run this after running CollectTrainingSets and the script that it generates.");
		sop("\"parallel\" should be \"true\" or \"false\" to enable parallel computation.");
		sop("Only choose \"true\" on a large system with abundant memory.");
		
		System.exit(1);
	}
	
	
	private static class Reporter
	{
		private StrictTaxonomy		taxoOfBestHmm;
		private double				logViterbiOfBestHmm;
		private int					nReports;
		
		synchronized void report(File alignmentFasta, double logVit)
		{
			String s = " ";
			if (taxoOfBestHmm == null  ||  logVit > logViterbiOfBestHmm)
			{
				s = "*";
				logViterbiOfBestHmm = logVit;
				String stax = alignmentFasta.getName().replace(".fa", "");
				taxoOfBestHmm = new StrictTaxonomy(stax);
			}
			s += " After " + ++nReports + " HMMs, best match is " + taxoOfBestHmm + " ... log(Viterbi prob) = " + logViterbiOfBestHmm;
			sop(s);
		}
		
		
		synchronized void report(File alignmentFasta, Exception x)
		{
			report(alignmentFasta, x.getMessage());
		}
		
		
		synchronized void report(File alignmentFasta, String errorMessage)
		{
			sop(" After " + ++nReports + " HMMs, error in alignment file " + alignmentFasta.getName() + ": " + errorMessage);
		}
		
		
		StrictTaxonomy getTaxoOfBestHmm()		{ return taxoOfBestHmm; }
	} // Reporter
	
	
	private static void evaluate(File alignmentFasta)
	{
		try
		{
			// Make sure >= 1 training record.
			if (FastaReader.getRecords(alignmentFasta).size() == 0)
				theReporter.report(alignmentFasta, "Skipping empty training set.");
			
			// Build HMM.
			dsop(alignmentFasta.getName() + ": Building HMM");
			ProfileHMM hmm = new ProfileHMM(alignmentFasta);
			
			// Execute HMM.
			dsop(alignmentFasta.getName() + ": Computing log-Viterbi probability.");
			double logVit = hmm.logOddsViterbiScore(theQuery);
			
			// Report.
			dsop(alignmentFasta.getName() + ": Done.");
			theReporter.report(alignmentFasta, logVit);
		}
		catch (IOException | HMMException x)
		{
			theReporter.report(alignmentFasta, x);
			return;
		}
	}
	

	static void sop(Object x)		{ System.out.println(x); }
	static void dsop(Object x)		{ System.out.println(new Date() + ": " + x); }
	
	
	public static void main(String[] args)
	{	
		if (args.length != 2)
			printUsageAndExit();
		
		theQuery = args[0];
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<theQuery.length(); i++)
		{
			if ("ACGTacgt".indexOf(theQuery.charAt(i)) >= 0)
				sb.append(theQuery.charAt(i));
			else
			{
				sop("Dropping non-ACGT character '" + theQuery.charAt(i) + "' from input sequence");
			}
		}
		theQuery = sb.toString().toUpperCase();	
		
		boolean parallel = false;
		switch (args[1].toUpperCase())
		{
			case "TRUE":	
				parallel = true; 
				break;
			case "FALSE":	
				parallel = false; 
				break;
			default: 
				sop("3rd arg must be \"true\" or \"false\" for parallel or serial operation.\n");
				printUsageAndExit();
				break;
		}
		
		List<File> alignedFastas = FileUtils.toList(new File(ALIGNED_TRAINING_DIR_NAME), name -> name.endsWith(".fa"));
		
		if (parallel)
			alignedFastas.stream().parallel().forEach(fasta -> evaluate(fasta));
		else
			alignedFastas.stream().forEach(fasta -> evaluate(fasta));
		
		StrictTaxonomy taxoOfBest = theReporter.getTaxoOfBestHmm();
		if (taxoOfBest == null)
		{
			sop("No HMM computed any log-Viterbi probability for your sequence. One explanation is that");
			sop("your query is unusually short, unusually long, or not COI");
		}
		else
		{
			StrictTaxonomy famTax = new StrictTaxonomy(taxoOfBest);
			famTax.remove(Rank.SPECIES);
			sop("Adverb predicts that the family of the query is " + famTax);
		}
	}
}
