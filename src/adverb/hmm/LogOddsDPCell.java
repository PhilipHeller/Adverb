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

import java.util.LinkedHashMap;


// Keys are previous states, values are path probabilities from prev state to current state.

public class LogOddsDPCell<S> extends LinkedHashMap<S, LogOdds>
{

	private	LogOdds				probability;
	private S					bestPrevState;		// for Viterbi
	
	
	public LogOdds getProbability()
	{
		return probability;
	}
	
	
	// Also sets bestPrevState.
	public LogOdds setProbabilityToBest()
	{
		probability = new LogOdds(0);
		for (S state: keySet())
		{
			if (get(state).compareTo(probability) > 0)
			{
				bestPrevState = state;
				probability = get(state);
			}
		}
		
		return probability;
	}
	
	
	public S getBestPrevState()
	{
		return bestPrevState;
	}
	
	
	public String toString()
	{
		String s = "DPCell:";
		
		if (bestPrevState != null)
			s += " P = " + probability + " FROM " + bestPrevState;
		
		else
		{
			for (S state: keySet())
				s += "  from " + state + " = " + get(state);
		}
		
		return s;
	}
	
}
