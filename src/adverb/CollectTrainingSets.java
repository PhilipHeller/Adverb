package adverb;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import adverb.util.*;
import adverb.util.taxo.*;

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

public class CollectTrainingSets 
{
	private final static String		FULL_GENUS_DIR_NAME				= "full_genus_fastas";
	private final static String		UNALIGNED_TRAINING_DIR_NAME		= "unaligned_training_fastas";
	        final static String		ALIGNED_TRAINING_DIR_NAME		= "aligned_training_fastas";
	private final static String		SCRIPT_NAME						= "align_all.sh";
	
	
	static void printUsageAndExit()
	{
		sop("Usage: java -cp Adverb_1.0.jar adverb.CollectTrainingSets BOLDfasta clustal-path");
			
		sop("\nThe BOLDfasta arg is the name of a fasta-format file containing 1 nucleotide record for each fully classified");
		sop("BOLD sequence in the class or order. Deflines must provide the phylum, class, order, family, genus, and species of each sequence");
		sop("in the following format:");
		sop("              >P_thephylum__C_theclass__O_theOrder__F_theFamily__G_theGenus__S_theSpecies");
		sop("Example:      >P_Porifera__C_Demospongiae__O_Verongida__F_Aplysinellidae__G_Suberea__S_Suberea clavata");
		sop("Sequences shorter than 480 nt or longer than 782 nt will be ignored.");
		sop("Adverb_1.0.jar and the fasta file must both be in the same directory, and this app must have write/execute permission in that directory.");
		
		sop("\nThe clustal-path is the path to a local copy of the Clustal Omega executable. It's probably named \"clustalo\".");
		
		sop("\nThis program creates 3 subdirectories and 1 file in the directory containing the fasta file:");
		sop("    File " + SCRIPT_NAME + " is a bash script that invokes ClustalOmega to align all the training fastas.");
		sop("    Subdirectory " + FULL_GENUS_DIR_NAME + " contains a fasta file for each genus in the input fasta.");
		sop("    Subdirectory " + UNALIGNED_TRAINING_DIR_NAME + " contains a fasta file of up to 25 records for each genus in the input fasta.");
		sop("    Subdirectory " + ALIGNED_TRAINING_DIR_NAME + " will be empty until populated by the bash script.");
		
		sop("\nAfter running the bash script, run");
		sop("    java -cp Adverb_1.0.jar adverb.BuildAndExecuteHmms nuc-query-seq parallel");
		sop("where \"parallel\" is \"true\" or \"false\" to enable or disable parallel execution.");
		
		System.exit(1);
	}
	
	

	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{	
		if (args.length != 2)
			printUsageAndExit();
		
		File classOrOrderFasta = new File(args[0]);
		if (!classOrOrderFasta.exists())
		{
			sop("Fasta file not found: " + args[0] + "\n\n");
			printUsageAndExit();
		}
		
		File clustalf = new File(args[1]);
		if (!clustalf.exists())
		{
			sop("Clustal executable " + args[1] + " doesn't exist.\n\n");
			printUsageAndExit();
		}
		
		// Split big fasta into 1 fasta for each genus.
		sop("Will write per-genus fastas...");
		Set<File> allFullSplitFastas = new TreeSet<>();
		File fullSplitsDirf = new File(classOrOrderFasta.getAbsoluteFile().getParentFile(), FULL_GENUS_DIR_NAME);
		fullSplitsDirf.mkdirs();
		try (FileReader fr = new FileReader(classOrOrderFasta); BufferedReader br = new BufferedReader(fr); FastaReader far = new FastaReader(br))
		{
			String[] rec;
			while  ((rec=far.readRecord()) != null)
			{
				String stax = rec[0].substring(1);
				StrictTaxonomy taxo = new StrictTaxonomy(stax);
				taxo.remove(Rank.SPECIES);
				File subFasta = new File(fullSplitsDirf, taxo + ".fa");
				int len = rec[1].length();
				if (len >= 480  &&  len <= 782)
				{
					allFullSplitFastas.add(subFasta);
					try (FileWriter fw = new FileWriter(subFasta, true))
					{
						fw.write(rec[0] + "\n" + rec[1] + "\n");
					}
				}
			}
		}
		catch (IOException x)
		{
			sop("Trouble writing per-genus fastas: " + x.getMessage());
			x.printStackTrace();
			System.exit(2);
		}
		sop("... done.");
		
		// Reduce each genus fasta to a training set.
		sop("\nWill select training sequences...");
		File trainingDirf = new File(fullSplitsDirf.getParentFile(), UNALIGNED_TRAINING_DIR_NAME);
		trainingDirf.mkdirs();
		int nthFasta = 1;
		for (File fullFasta: allFullSplitFastas)
		{
			StrictTaxonomy genusTaxo = new StrictTaxonomy(fullFasta.getName().replace(".fa", ""));
			try
			{
				List<String[]> allRecs = FastaReader.getRecords(fullFasta);
				if (allRecs.isEmpty())
					continue;
				allRecs.stream().forEach(rec -> rec[1] = rec[1].toUpperCase());
				Map<String, List<String[]>> speciesToRecs =
					allRecs.stream()
					.collect(Collectors.groupingBy(rec -> new StrictTaxonomy(rec[0].substring(1)).getSpecies()));
				speciesToRecs.values()
					.stream()
					.forEach(list -> Collections.shuffle(list));
				List<List<String[]>> recListList = 				
					speciesToRecs.values()
					.stream()
					.collect(Collectors.toList());
				List<String[]> training = new ArrayList<>();
				Set<String> usedSeqs = new HashSet<>();
				int nEmptySpeciesLists = 0;
				int index = 0;
				while (nEmptySpeciesLists < recListList.size()  &&  training.size() < 25  &&  index < recListList.size())
				{
					List<String[]> spList = recListList.get(index);
					if (!spList.isEmpty())
					{
						while (!spList.isEmpty())
						{
							String[] rec = spList.remove(0);
							if (!usedSeqs.contains(rec[1]))
							{
								training.add(rec);
								usedSeqs.add(rec[1]);
								break;
							}
						}
						if (spList.isEmpty())
							nEmptySpeciesLists++;
					}
					index = ++index % recListList.size();
				}
				sop("   Chose " + training.size() + " training record(s) for genus " + nthFasta++ + " of " + allFullSplitFastas.size() + " = " + genusTaxo);
				try (FileWriter fw = new FileWriter(new File(trainingDirf, fullFasta.getName())))
				{
					for (String[] rec: training)
						fw.write(rec[0] + "\n" + rec[1] + "\n");
				}
			}
			catch (IOException x)
			{
				sop("Trouble writing per-genus training fastas: " + x.getMessage());
				x.printStackTrace();
				System.exit(2);
			}
		}
		
		// Write the aligner script.
		File alignedDirf = new File(trainingDirf.getParentFile(), ALIGNED_TRAINING_DIR_NAME);
		alignedDirf.mkdirs();
		List<File> trains = FileUtils.toList(trainingDirf, name -> name.endsWith(".fa"));
		File scriptf = new File(trainingDirf.getParentFile(), "align_all.sh");
		int nth = 0;
		try (FileWriter fw = new FileWriter(scriptf))
		{
			fw.write("#!/bin/bash\n");
			for (File train: trains)
			{
				List<String[]> recs = FastaReader.getRecords(train);
				if (recs.isEmpty())
					continue;
				else if (recs.size() == 1)
				{
					try (FileWriter size1Fw = new FileWriter(new File(alignedDirf, train.getName())))
					{
						size1Fw.write(recs.get(0)[0] + "\n");
						size1Fw.write(recs.get(0)[1] + "\n");
					}
				}
				else
				{
					File ofile = new File(alignedDirf, train.getName());
					fw.write("echo \"Will align " + ++nth + " of " + trains.size() + ": " + train.getName() + "\"\n");
					String cl = clustalf.getAbsolutePath() + " -i " + train.getAbsolutePath() + " -o " + ofile.getAbsolutePath() + " --force";
					fw.write(cl + "\n");
				}
			}
			scriptf.setExecutable(true, false);
			sop("\nWrote aligner script " + scriptf.getAbsolutePath());
			sop("Run the script. Then execute");
			sop("     java -cp Adverb_1.0.jar adverb.BuildAndExecuteHmms nuc-query-seq parallel");
			sop("where \"parallel\" is \"true\" or \"false\" to enable or disable parallel execution.");
		}
		catch (IOException x)
		{
			sop("Trouble writing alignment script: " + x.getMessage());
			x.printStackTrace();
			System.exit(2);
		}
		
		sop("\nDONE");
	}
}
