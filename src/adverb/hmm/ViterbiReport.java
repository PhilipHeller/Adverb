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


//
// S is the type of the states.
//


public class ViterbiReport<S>
{
	private double				probability;		// Only one of
	private LogOdds				logOdds;			// these is valid
	private List<S>				statePath;
	
	
	ViterbiReport(double probability, List<S> statePath)
	{
		this.probability = probability;
		this.statePath = statePath;
	}
	
	
	ViterbiReport(LogOdds logOdds, List<S> statePath)
	{
		this.logOdds = logOdds;
		this.statePath = statePath;
	}
	
	
	public String toString()
	{
		String s =  (logOdds == null)  ?
			"Viterbi analysis: probability = " + probability + "\n  "  :
			"Viterbi analysis: log odds = " + logOdds + "\n  ";
		if (statePath == null)
			return s + "No state path";
		for (int i=0; i<statePath.size(); i++)
		{
			if (i > 0)
				s += " => ";
			s += statePath.get(i);
		}
		return s;
	}
	
	
	public double getProbability()
	{
		return probability;
	}
	
	
	public LogOdds getLogOdds()
	{
		return logOdds;
	}
	
	
	public List<S> getStatePath()
	{
		return statePath;
	}
}
