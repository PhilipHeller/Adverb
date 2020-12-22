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


import java.util.*;
import java.io.*;


public class StringUtils
{
	private final static Map<Character, Character>		REV_COMP_MAP;
	private final static Set<Character>					EXTENDED_NUC_CHARS;
	
	
	static
	{
		REV_COMP_MAP = new HashMap<Character, Character>();
		REV_COMP_MAP.put('A', 'T');
		REV_COMP_MAP.put('T', 'A');
		REV_COMP_MAP.put('C', 'G');
		REV_COMP_MAP.put('G', 'C');
		REV_COMP_MAP.put('R', 'Y');
		REV_COMP_MAP.put('Y', 'R');
		REV_COMP_MAP.put('S', 'S');
		REV_COMP_MAP.put('W', 'W');
		REV_COMP_MAP.put('R', 'Y');
		REV_COMP_MAP.put('Y', 'R');
		REV_COMP_MAP.put('K', 'M');
		REV_COMP_MAP.put('M', 'K');
		REV_COMP_MAP.put('R', 'Y');
		REV_COMP_MAP.put('N', 'N');
		
		Set<Character> clonedKeys = new HashSet<Character>(REV_COMP_MAP.keySet());
		for (Character ch: clonedKeys)
			REV_COMP_MAP.put(Character.toLowerCase(ch), Character.toLowerCase(REV_COMP_MAP.get(ch)));
		
		EXTENDED_NUC_CHARS = new HashSet<>(REV_COMP_MAP.keySet());
	}
	
	
	private StringUtils()		{ }
	
	
	public static boolean isValidWildcardNucSeq(String seq)
	{
		for (char ch: seq.toCharArray())
			if (!EXTENDED_NUC_CHARS.contains(ch))
				return false;
		return true;
	}
	
	
	public static String reverseComplement(String src)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=src.length()-1; i>=0; i--)
			sb.append(REV_COMP_MAP.get(src.charAt(i)));
		return sb.toString();
	}
	
	
	public static String forwardComplement(String src)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<src.length(); i++)
			sb.append(REV_COMP_MAP.get(src.charAt(i)));
		return sb.toString();
	}
	
	
	// See http://www.bioinformatics.org/sms/iupac.html
	private final static Map<Character, HashSet<Character>> 		NUC_CHAR_TO_MATCHES;
	
	
	private static void addMatchesForNucChar(Character ch, String matches)
	{
		HashSet<Character> set = new HashSet<Character>();
		for (char c1: matches.toCharArray())
			set.add(c1);
		NUC_CHAR_TO_MATCHES.put(ch, set);
	}
	
	
	static
	{
		NUC_CHAR_TO_MATCHES = new HashMap<Character, HashSet<Character>>();
		addMatchesForNucChar('-', "-");
		addMatchesForNucChar('A', "A");
		addMatchesForNucChar('C', "C");
		addMatchesForNucChar('G', "G");
		addMatchesForNucChar('T', "T");
		addMatchesForNucChar('T', "T");
		addMatchesForNucChar('R', "AG");
		addMatchesForNucChar('Y', "CT");
		addMatchesForNucChar('S', "GC");
		addMatchesForNucChar('W', "AT");
		addMatchesForNucChar('K', "GT");
		addMatchesForNucChar('M', "AC");
		addMatchesForNucChar('B', "CGT");
		addMatchesForNucChar('D', "AGT");
		addMatchesForNucChar('H', "ACT");
		addMatchesForNucChar('V', "ACG");
		addMatchesForNucChar('N', "ACGT");
	}
	
	
	public static boolean nucleotidesMatch(char n1, char n2)
	{
		Set<Character> matchers = NUC_CHAR_TO_MATCHES.get(n1);
		if (matchers == null)
			return false;
		else
			return matchers.contains(n2);
	}
	
	
	// Removes every char in removeUS from crunchMe.
	public static String crunch(String crunchMe, String removeUs)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<crunchMe.length(); i++)
		{
			char ch = crunchMe.charAt(i);
			if (removeUs.indexOf(ch) < 0)
				sb.append(ch);
		}
		return sb.toString();
	}
	
	
	public static int count(char ch, String s)
	{
		int n = 0;
		for (int i=0; i<s.length(); i++)
			if (s.charAt(i) == ch)
				n++;
		return n;
	}
	
	
	public static List<Integer> getRunStartIndices(String s, int minRunLen)
	{
		List<Integer> ret = new ArrayList<Integer>();
		if (s.length() < minRunLen)
			return ret;
		
		// Collect starting indices for all runs.
		List<Integer> allRunStarts = new ArrayList<Integer>();
		allRunStarts.add(0);
		for (int i=1; i<s.length(); i++)
			if (s.charAt(i) != s.charAt(i-1))
				allRunStarts.add(i);
		allRunStarts.add(s.length());
		
		// Collect starting indices for runs that meet length criterion.
		for (int i=0; i<allRunStarts.size()-1; i++)
		{
			int len = allRunStarts.get(i+1) - allRunStarts.get(i);
			if (len >= minRunLen)
				ret.add(allRunStarts.get(i));
		}
		return ret;
	}
	
	
	public static boolean nucleotideStringContainsWildcards(String s)
	{
		for (int i=0; i<s.length(); i++)
			if ("ACGT".indexOf(s.charAt(i)) < 0)
				return true;
		return false;
	}
	
	
	public static String markDiffs(String s1, String s2)
	{
		assert s1.length() == s2.length();
		
		String ret = "";
		int nDiffs = 0;
		for (int i=0; i<s1.length(); i++)
		{
			if (s1.charAt(i) == s2.charAt(i)) 
				ret += ' ';
			else
			{
				ret += '@';
				nDiffs++;
			}
		}
		return ret + "\n" + nDiffs;
	}
	
	
	public static String prependBeforeEachLine(String s, String prefix)
	{
		StringReader sr = new StringReader(s);
		BufferedReader br = new BufferedReader(sr);
		StringBuilder sb = new StringBuilder();
		String line;
		try
		{
			while ((line = br.readLine()) != null)
			{
				sb.append(prefix);
				sb.append(line);
				sb.append("\n");
			}
			return sb.toString();
		}
		catch (IOException x)
		{
			return "Very unusual IOException";
		}
	}
	
	
	public static void stringToFile(String s, File f) throws IOException
	{
		FileWriter fw = new FileWriter(f);
		fw.write(s);
		fw.flush();
		fw.close();
	}
	
	
	public static String fileToString(File f) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		try
		(
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
		)
		{
			String line;
			while ((line = br.readLine()) != null)
				sb.append(line + "\n");
			return sb.toString();
		}
	}
	
	
	public static String fileToStringNoThrow(File f)
	{
		try
		{
			return fileToString(f);
		}
		catch (IOException x)
		{
			return null;
		}
	}
	
	
	public static List<String> fileToListOfLines(File f) throws IOException
	{
		List<String> ret = new ArrayList<>();
		
		try
		(
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
		)
		{
			String line;
			while ((line = br.readLine()) != null)
				ret.add(line);
			return ret;
		}
	}
	
	
	public static String setMaxLength(String s, int maxLen)
	{
		if (s.length() <= maxLen)
			return s;
		else
			return s.substring(0, maxLen);
	}
	
	
	public static String listToCSV(List<String> pieces)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<pieces.size(); i++)
		{
			sb.append(pieces.get(i));
			if (i < pieces.size()-1)
				sb.append(",");
		}
		return sb.toString();
	}
	
	
	//
	// For Jukes-Cantor correction, see http://www.ch.embnet.org/CoursEMBnet/PHYL03/Slides/Distance_membley.pdf.
	//
	// dxy = corrected distance = -.75 ln(1 - D*4/3)     where D = fractional dissimilarity
	//
	public static double jukesCantorDistance(String seq1, String seq2)
	{
		assert seq1.length() == seq2.length();
		
		int nDiffs = 0;
		for (int i=0; i<seq1.length(); i++)
			if (seq1.charAt(i) != seq2.charAt(i))
				nDiffs++;
		double fractionalDissimilarity = nDiffs / (double)seq1.length();
		return -.75 * Math.log(1 - fractionalDissimilarity*4/3);
	}
	
	
	public static String compressRepeats(String src)
	{
		StringBuilder sb = new StringBuilder();
		char lastChar = 0;
		for (int i=0; i<src.length(); i++)
		{
			if (src.charAt(i) != lastChar)
				sb.append(src.charAt(i));
			lastChar = src.charAt(i);
		}
		return sb.toString();
	}

	
	public static String toColumns(List<String> svals, int[] colStarts, boolean replaceCharsWithDashes)
	{
		assert svals.size() <= colStarts.length;
		
		if (replaceCharsWithDashes)
		{
			List<String> dashes = new ArrayList<String>();
			for (String sval: svals)
			{
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<sval.length(); i++)
					sb.append('-');
				dashes.add(sb.toString());
			}
			svals = dashes;
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<svals.size(); i++)
		{
			while (sb.length() < colStarts[i])
				sb.append(" ");
			sb.append(svals.get(i));
		}
		return sb.toString();
	}
	
	
	// Can handle initial minus sign.
	public static int parseFirstInt(String s)
	{
		// Find 1st digit.
		int indexFirstDigit = -1;
		for (int i=0; i<s.length(); i++)
		{
			if (Character.isDigit(s.charAt(i)))
			{
				indexFirstDigit = i;
				break;
			}
		}
		if (indexFirstDigit == -1)
			throw new IllegalArgumentException("No digits in " + s);
		
		// Parse.
		int ival = s.charAt(indexFirstDigit) - '0';
		int i = indexFirstDigit + 1;
		while (i<s.length())
		{
			char ch = s.charAt(i);
			if (Character.isDigit(ch))
			{
				ival *= 10;
				ival += ch - '0';
				i++;
			}
			else
				break;
		}
		
		// Check for - sign.
		if (indexFirstDigit > 0  &&  s.charAt(indexFirstDigit-1) == '-')
			ival = -ival;
		
		return ival;
	}
		
	
	public static String extractDefinitionField(String gpPage)
	{		
		String definition = "";
		
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine()).startsWith("DEFINITION"))
				;
			definition += line.substring("DEFINITION".length()).trim();
			line = br.readLine();
			while (Character.isWhitespace(line.charAt(0)))
			{
				definition += " " + line.trim();
				line = br.readLine();
			}
			// cytochrome c oxidase subunit I, partial (mitochondrion) [Haemadipsa zeylanica agilis].
		}
		catch (IOException x) { }
		
		if (definition.endsWith("."))
			definition = definition.substring(0,  definition.length()-1);
		definition = definition.trim();
		if (definition.endsWith(","))
			definition = definition.substring(0,  definition.length()-1).trim();
		if (definition.startsWith("RecName:"))
		{
			definition = definition.substring(8).trim();
			if (definition.contains(";"))
				definition = definition.substring(0,  definition.indexOf(';'));
		}
		
		return definition;
	}
	
	
	public static String extractAuthorsField(String gpPage)
	{		
		String authors = "";
		
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine()).trim().startsWith("AUTHORS"))
				;
			authors += line.trim().substring(7).trim();
		}
		catch (NullPointerException x)
		{
			return null;
		}
		catch (IOException x) { }

		return authors;
	}
	
	
	// [0] is the binomial, [1] is the organism field (which doesn't include species).
	public static String[] extractBinomialAndOrganismField(String gpPage)
	{		
		String organism = "";
		String binomial = "";
		
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine().trim()).startsWith("ORGANISM"))
				;
			binomial = line.trim().substring("ORGANISM".length()).trim();	//   ORGANISM  Poliopastea sp. Janzen02
			binomial = binomial.replace("'", "");
			binomial = binomial.replace("\"", "");
			binomial = binomial.trim();
			line = br.readLine();
			while (Character.isWhitespace(line.charAt(0)))
			{
				organism += line.trim();
				line = br.readLine();
			}
			organism = organism.replace("'", "");
			organism = organism.replace("\"", "");
			organism = organism.trim();
		}
		catch (IOException x) { }
		
		return new String[] { binomial, organism };
	}
	
	
	public static String extractSequence(String gpPage)
	{
		if (!gpPage.contains("ORIGIN"))
			return null;
		if (!gpPage.contains("//"))
			return null;
		
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			StringBuilder sb = new StringBuilder();
			while (!(line = br.readLine()).startsWith("ORIGIN"));
			while (!(line = br.readLine()).startsWith("//"))
			{
				line.toUpperCase().chars()
					.mapToObj(i -> (char)i)
					.filter(ch -> ch >= 'A' && ch <= 'Z')
					.forEach(ch -> sb.append(ch));
			}
			
			return sb.toString();
		}
		catch (IOException|NullPointerException x)
		{
			return null;
		}
	}
	
	
	public static String extractTranslation(String gpPage)
	{
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine()).contains("/translation=\""));
			int nQuotes = count('\"', line);
			String ret = line.trim();
			while (nQuotes < 2) {
				ret += br.readLine().trim();
				nQuotes = count('\"', ret);
			}
			ret = ret.substring(1 + ret.indexOf('"'));
			ret = ret.substring(0, ret.indexOf('"'));
			return ret;
		}
		catch (IOException x)
		{
			return null;
		}
	}
	
	
	public static int extractSequenceLength(String gpPage)
	{
		try
		(
			StringReader sr = new StringReader(gpPage);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line;
			while (!(line = br.readLine()).startsWith("LOCUS"));
			int nEnd = line.indexOf(" aa");
			int n = nEnd - 1;
			while (Character.isDigit(line.charAt(n)))
				n--;
			n++;
			return Integer.parseInt(line.substring(n, nEnd).trim());
		}
		catch (IOException x)
		{
			return -12345;
		}
	}
	
	
	// gi|325301010|gb|ADZ05492.1|gi|325301011|gb|ADZ05493.1
	// With possible multiple "gi" fields.
	public static List<String> extractGIsFromBlastSubject(String subj)
	{
		List<String> ret = new ArrayList<String>();
		String[] pieces = subj.split("\\|");
		for (int i=0; i<pieces.length-1; i++)
			if (pieces[i].equals("gi"))
				ret.add(pieces[i+1]);
		return ret;			
	}
	
	
	// 1st line is e.g.
	// LOCUS       AB002225                 985 bp    DNA     linear   INV 23-JUL-2016
	public static String extractSubmissionDateString(String page)
	{
		try
		(
			StringReader sr = new StringReader(page);
			BufferedReader br = new BufferedReader(sr);
		)
		{
			String line = br.readLine();
			String[] pieces = line.split("\\s");
			return pieces[pieces.length-1];
		}
		catch (IOException x)
		{
			return null;
		}
	}
	
	
	public static String removeLast(String s)
	{
		return s.substring(0, s.length());
	}
	
	
	public static String replaceFirst(String s, char cOld, char cNew)
	{
		int n = s.indexOf(cOld);
		if (n < 0)
			return s;
		else if (n == 0)
			return cNew + s.substring(1);
		else if (n == s.length()-1)
			return removeLast(s) + cNew;
		else
			return s.substring(0, n) + cNew + s.substring(n+1);
	}
	
	
	public static String showNucImpurities(String seq)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<seq.length(); i++)
		{
			char ch = seq.charAt(i);
			if ("ACGT".indexOf(ch) < 0)
				sb.append("*");
			else
				sb.append(" ");
		}
		return seq + "\n" + sb;
	}
	
	
	public static String removeNonCharAscii(String s)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<s.length(); i++)
			if (s.charAt(i) < 0x7f)
				sb.append(s.charAt(i));
		return sb.toString();
	}
	
	

	//
	// Commas in binomials cause wrong # of fields in tsv lines. E.g.
	// 10751681___K_Metazoa__P_Arthropoda__C_Insecta__O_Coleoptera__F_Rentoniidae__G_Rentonellum__S_sp..G0299,9838852___K_Metazoa__P_Arthropoda__C_Insecta__O_Coleoptera__F_Prionoceridae__G_Idgia__S_sp..n..1.Yunnan,.Laos,84.110,730,102,10,1,723,23,745,0.0,693
	//
	public static String repairBoldCsv(String csv)
	{		
		// Remove commas from query.
		int indexTripleUscoreInSubject = csv.lastIndexOf("___");
		int indexBoldIdOfSubject = indexTripleUscoreInSubject - 1;
		while (Character.isDigit(csv.charAt(indexBoldIdOfSubject-1)))
			indexBoldIdOfSubject--;
		int indexCommaBetweenQueryAndSubject = indexBoldIdOfSubject - 1;
		String query = csv.substring(0, indexCommaBetweenQueryAndSubject);
		query = query.replace(",", "");
		String ret = query + ",";
		
		// Remove commas from subject. Subject ends at 10th comma from end.
		int nCommasFromEnd = 0;
		int indexOfCommaJustPastSubject = csv.length();
		while (nCommasFromEnd != 10)
			if (csv.charAt(--indexOfCommaJustPastSubject) == ',')
				nCommasFromEnd++;
		String subject = csv.substring(indexBoldIdOfSubject, indexOfCommaJustPastSubject);
		ret += subject.replace(",", "");
		ret += csv.substring(indexOfCommaJustPastSubject);
		return ret;
	}
	
	
	public static String retainOnlyACGT(String seq)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<seq.length(); i++)
		{
			char ch = seq.charAt(i);
			if (ch == 'A'  ||  ch == 'C'  ||  ch == 'G'  ||  ch == 'T')
				sb.append(ch);
		}
		return sb.toString();
	}
	
	
	public static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			String s = "\"abcde\"";
			sop(count('"', s) + "   " + s);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally 
		{
			sop("DONE");
		}
	}
}
