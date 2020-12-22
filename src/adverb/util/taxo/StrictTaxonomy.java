package adverb.util.taxo;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.util.function.Predicate;

import adverb.util.*;

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


//
// A strict taxonomy only provides domain, kingdom ... through species. Intermediate ranks such
// as infraorder are not modeled. Holes are allowed, more in sorrow than in anger.
//


public class StrictTaxonomy extends TreeMap<Rank, String> implements Comparable<StrictTaxonomy>
{	
	public StrictTaxonomy()		{ }
	
	
	// Format of s is same as toString(), e.g. K_Metazoa__P_Annelida__C_Polychaeta__O_Flabelligerida__F_Flabelligeridae__G_Agenus__S_Aspecies
	public StrictTaxonomy(String s)
	{
		// Special case: bold_full_taxo has one value containing "__": Polychaeta__order_incertae_sedis.
		// TODO: Cleaner parsing.
		s = s.replace("Polychaeta__order_incertae_sedis", "XYZZY");
		
		String[] pieces = s.split("__");
		for (String piece: pieces)
		{
			String initial = piece.substring(0, 1);
			String val = piece.substring(2);
			for (Rank r: Rank.values())
			{
				if (r.name().startsWith(initial))
				{
					put(r, val);
					break;
				}
			}
		}
		
		if (getOrder() != null  &&  getOrder().equals("XYZZY"))
			put(Rank.ORDER, "Polychaeta__order_incertae_sedis");
	}
	
	
	// Column names must exactly match r.toSqlColumnName()
	public StrictTaxonomy(ResultSet rs) throws SQLException
	{
		Set<String> colNames = new HashSet<>();
		for (int i=1; i<=rs.getMetaData().getColumnCount(); i++)		// metadata column #s start at 1
			colNames.add(rs.getMetaData().getColumnName(i));
		
		for (Rank r: Rank.values())
		{
			String col = r.toSqlColumnName();
			if (colNames.contains(col))
			{
				String val = rs.getString(col);
				if (val != null)
					val = val.trim();
				if (val == null  ||  val.isEmpty())
					continue;
				this.put(r, val);
			}
		}
	}
	
	
	public StrictTaxonomy(ResultSet rs, Map<String, Rank> colNameToRank) throws SQLException
	{
		for (String col: colNameToRank.keySet())
			this.put(colNameToRank.get(col), rs.getString(col));
	}
	
	
	public StrictTaxonomy(StrictTaxonomy src)
	{
		for (Rank r: src.keySet())
			put(r, src.get(r));
	}
	
	
	// File is e.g. /a/b/c/K_Metazoa/P_Annelida...S_sapiens
	public StrictTaxonomy(File file)
	{
		String path = file.getAbsolutePath();
		String[] pieces = path.split("/");
		for (String piece: pieces)
			for (Rank r: Rank.values())
				if (piece.length() >= 3  &&  piece.charAt(0) == r.initial()  &&  piece.charAt(1) == '_')
					put(r, piece.substring(2));
	}
	
	
	@Override
	public String toString()
	{
		String s = "";
		if (isEmpty())
			return "";
		for (Rank r: keySet())
		{
			if (get(r) == null  ||  get(r).isEmpty()  ||  get(r).equalsIgnoreCase("NULL"))
				continue;
			s += "__" + r.name().charAt(0) + "_" + get(r);
		}
		return s.substring(2);
	}
	
	
	@Override
	public int compareTo(StrictTaxonomy that)
	{
		return this.toString().compareTo(that.toString());
	}
	
	
	public String toMultiLineString()
	{
		String s = "";
		for (Rank r: keySet())
			s += r.name().charAt(0) + "=" + get(r) + "\n";
		return s.trim();
	}
	
	
	public String toLongString()
	{
		String s = "";
		for (Rank r: keySet())
			s += r + " = " + get(r) + "\n";
		return s;
	}
	
	
	public boolean equals(StrictTaxonomy that, Rank startRank, Rank endRank)
	{
		assert startRank.ordinal() <= endRank.ordinal();
		
		for (int ord=startRank.ordinal(); ord<=endRank.ordinal(); ord++)
		{
			Rank r = Rank.values()[ord];
			if ((this.get(r) == null)  !=  (that.get(r) == null))
				return false;		// 1 val is null, 1 isn't
			if (this.get(r) != null  &&  !(this.get(r).equals(that.get(r))))
				return false;		// neither null, and not equal
		}
		
		return true;
	}
	
	
	public Rank lowestRank()
	{
		if (isEmpty())
			return null;
		List<Rank> keys = new ArrayList<Rank>(keySet());
		return keys.get(keys.size()-1);
	}
	
	
	public boolean isStrictlyContainedBy(StrictTaxonomy that)
	{
		if (this.size() <= that.size())
			return false;
		
		for (Rank rank: that.keySet())
			if (!this.containsKey(rank)  ||  !this.get(rank).equals(that.get(rank)))
				return false;
		
		return true;
	}
	
	
	// Assumes this binomial really is binomial, e.g. from CO-ARBitrator.
	public boolean strictlyContains(String binomial)
	{
		if (isEmpty())
			return false;
		
		Rank topRank = keySet().iterator().next();
		if (!topRank.outranks(Rank.GENUS))
			return false;

		binomial = binomial.replace("_", " ");
		String[] pieces = binomial.split("\\s");
		return containsKey(Rank.GENUS)  &&  getGenus().equals(pieces[0])  &&
			   containsKey(Rank.SPECIES)  &&  getSpecies().equals(pieces[1]);
	}
	
	
	public StrictTaxonomy forLowestRank(Rank lowestRank)
	{
		StrictTaxonomy ret = new StrictTaxonomy();
		for (Rank r: this.keySet())
		{
			ret.put(r, get(r));
			if (r == lowestRank)
				break;
		}
		return ret;
	}
	
	
	public String toBinomialOrBestPossible()
	{
		if (isEmpty())
			return null;
		
		else if (isBinomial())
			return get(Rank.GENUS) + " " + get(Rank.SPECIES);
		
		else
		{
			Stack<Rank> rankStack = new Stack<Rank>();
			for (Rank r: keySet())
				rankStack.push(r);
			Rank lowestRank = rankStack.pop();
			if (rankStack.isEmpty()  ||  lowestRank.outranks(Rank.SPECIES))
				return get(lowestRank);
			Rank nextLowest = rankStack.pop();
			return get(nextLowest) + " " + get(lowestRank);
		}
	}
	
	
	public String toSetSql()
	{
		String s = "";
		for (Rank rank: keySet())
			s += "," + rank.toSqlColumnName() + "='" + get(rank) + "'";
		s = s.substring(1);
		return s;
	}
	
	
	public boolean containsRange(Rank highRank, Rank lowRank)
	{
		assert highRank.ordinal() <= lowRank.ordinal();
		
		for (int ord=highRank.ordinal(); ord<=lowRank.ordinal(); ord++)
			if (!containsKey(Rank.values()[ord]))
				return false;
		
		return true;
	}
	
	
	public File toDirf(File aboveKingdomDirf)
	{
		File f = aboveKingdomDirf;
		for (Rank r: keySet())
		{
			String s = r.name().charAt(0) + "_" + get(r);
			f = new File(f, s);
		}
		return f;
	}
	
	
	private final static Map<Rank, Integer> RANK_TO_BOLD_CSV_COL;
	static
	{
		RANK_TO_BOLD_CSV_COL = new LinkedHashMap<>();
		RANK_TO_BOLD_CSV_COL.put(Rank.PHYLUM, 8);
		RANK_TO_BOLD_CSV_COL.put(Rank.CLASS, 10);
		RANK_TO_BOLD_CSV_COL.put(Rank.ORDER, 12);
		RANK_TO_BOLD_CSV_COL.put(Rank.FAMILY, 14);
		RANK_TO_BOLD_CSV_COL.put(Rank.GENUS, 18);
		RANK_TO_BOLD_CSV_COL.put(Rank.SPECIES, 20);
	}
	
	
	public static StrictTaxonomy forBoldTsvLine(String tsv)
	{
		String[] pieces = tsv.split("\\t");
		StrictTaxonomy that = new StrictTaxonomy();
		that.put(Rank.KINGDOM, "Metazoa");
		for (Rank r: RANK_TO_BOLD_CSV_COL.keySet())
		{
			int col = RANK_TO_BOLD_CSV_COL.get(r);
			String sval = StringUtils.removeNonCharAscii(pieces[col]);
			sval = pieces[col].trim();
			that.put(r, sval);
		}
		
		String genus = that.get(Rank.GENUS);
		String species = that.get(Rank.SPECIES);
		if (genus != null  &&  !genus.isEmpty()  &&  species != null  &&  !species.isEmpty())
		{
			if (species.startsWith(genus))
			{
				species = species.substring(genus.length()+1);
				species = species.trim();			// paranoia
			}
			species = species.replace(" ", ".");
			that.put(Rank.SPECIES, species);
		}
		
		return that;
	}
	
	
	public Rank lowestCommonRank(StrictTaxonomy that)
	{
		for (int ord=Rank.values().length-1; ord>=0; ord--)
		{
			Rank r = Rank.values()[ord];
			String thisVal = this.get(r);
			String thatVal = that.get(r);
			if (thisVal == null  ||  thatVal == null)
				continue;
			if (thisVal.equals(thatVal))
				return r;
		}
		return null;
	}
	
	
	// Returns a taxonomy that most closely represents the consensus of the
	// collection. To be used when there are multiple blast hits with identical E-value.
	// Ranks above startRank are ignored; for Adverb, startRank is probably PHYLUM.
	public static StrictTaxonomy consensus(Collection<StrictTaxonomy> taxos, Rank startRank)
	{		
		assert !taxos.isEmpty();
		
		StrictTaxonomy ret = new StrictTaxonomy();
		
		// Only consider most specific taxonomies.
		int maxRankOrdinal = -1;
		for (StrictTaxonomy st: taxos)
			for (Rank r: st.keySet())
				if (st.get(r) != null)
					maxRankOrdinal = Math.max(maxRankOrdinal, r.ordinal());
		Rank mostSpecificRank = Rank.values()[maxRankOrdinal];
		taxos = taxos.stream().filter(tax -> tax.get(mostSpecificRank) != null).collect(Collectors.toList());
		
		Collection<StrictTaxonomy> taxosForMostPopulated = taxos;
		for (Rank rank: Rank.values(startRank, Rank.SPECIES))
		{
			// Group by value of current rank. The stream only works if there is 
			// at least 1 item in taxosForMostPopulated that has a value for rank.
			boolean haveValueForRank = false;
			for (StrictTaxonomy st: taxosForMostPopulated)
			{
				if (st.get(rank) != null)
				{
					haveValueForRank = true;
					break;
				}
			}
			if (!haveValueForRank)
				continue;
			Map<String, List<StrictTaxonomy>> valueToOwners =
				taxosForMostPopulated.stream()
				.filter(tax -> tax.get(rank) != null)
				.collect(Collectors.groupingBy(tax -> tax.get(rank)));
			// Done if current rank is not represented.
			if (valueToOwners.isEmpty())
				return ret;
			// Find most populated value. Choose randomly if > 1.
			int maxPopulation = 
				valueToOwners.values().stream()
				.map(list -> list.size())
				.max(Integer::compare)
				.get();
			List<String> mostPopulatedValues = 
				valueToOwners.keySet().stream()
				.filter(val -> valueToOwners.get(val).size() == maxPopulation)
				.collect(Collectors.toList());			// if tie for most populated, choose 1 randomly
			Collections.shuffle(mostPopulatedValues);
			String chosenConsensusValue = mostPopulatedValues.get(0);
			ret.put(rank, chosenConsensusValue);
			taxosForMostPopulated = valueToOwners.get(chosenConsensusValue);
		}
		
		return ret;
	}
	
	
	public static boolean allAreCompatible(Collection<StrictTaxonomy> checkUs)
	{
		StrictTaxonomy checker = new StrictTaxonomy();
		for (StrictTaxonomy checkMe: checkUs)
		{
			for (Rank rank: checkMe.keySet())
			{
				if (!checker.containsKey(rank))
					checker.put(rank, checkMe.get(rank));
				else if (!checker.get(rank).equals(checkMe.get(rank)))
					return false;
			}	
		}
		return true;
	}
	
	
	public boolean strictlyContains(StrictTaxonomy that)	{ return that.isStrictlyContainedBy(this); }
	public boolean isBinomial()								{ return containsKey(Rank.GENUS)  &&  containsKey(Rank.SPECIES); }
	public boolean isBroad()								{ return !isBinomial(); }
	public boolean isHighResolution()						{ return containsKey(Rank.SPECIES); }
	public List<String> getPieces()							{ return new ArrayList<String>(values()); }
	public String getPhylum()								{ return get(Rank.PHYLUM); }
	public String getClazz()								{ return get(Rank.CLASS); }
	public String getOrder()								{ return get(Rank.ORDER); }
	public String getFamily()								{ return get(Rank.FAMILY); }
	public String getGenus()								{ return get(Rank.GENUS); }
	public String getSpecies()								{ return get(Rank.SPECIES); }
	static void sop(Object x)								{ System.out.println(x); }
	
	
	public static void main(String[] args) throws Exception
	{
		sop("START");

		String s = "P_Nemertea__C_Enopla__O_Enopla_order_incertae_sedis__F_Valenciniidae__G_Baseodiscus__S_Baseodiscus mexicanus";
		StrictTaxonomy that = new StrictTaxonomy(s);
		for (Rank r: that.keySet())
			sop(r + " = " + that.get(r));
		sop(that);
		
		sop("DONE");
	}
}
