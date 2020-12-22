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


//
// Log base 10 of a probability.
//


public class LogOdds implements Comparable<LogOdds>, java.io.Serializable
{
	private static final long 	serialVersionUID = 7439966786904368794L;
	
	private float				logOfProb;
	private boolean				probIsZero;
	
	
	private static LogOdds		THE_PROBZERO_INSTANCE = new LogOdds(0);
	
	
	public LogOdds(float prob)
	{
		if (prob == 0)
			probIsZero = true;
		else
			logOfProb = (float)Math.log10(prob);
	}
	
	
	private LogOdds() {	}
	
	
	public static LogOdds getProbZeroInstance()
	{
		return THE_PROBZERO_INSTANCE;
	}
	
	
	public float get()
	{
		return probIsZero ? 0 : logOfProb;
	}
	
	
	public static LogOdds plus(LogOdds l1, LogOdds l2)
	{		
		if (l1.probIsZero  ||  l2.probIsZero)
			return THE_PROBZERO_INSTANCE;
		
		else
		{
			LogOdds sum = new LogOdds();
			sum.logOfProb = l1.logOfProb + l2.logOfProb;
			return sum;
		}
	}
	
	
	public static LogOdds plus(LogOdds l1, LogOdds l2, LogOdds l3)
	{
		if (l1.probIsZero  ||  l2.probIsZero  ||  l3.probIsZero)
			return THE_PROBZERO_INSTANCE;
		
		else
		{
			LogOdds sum = new LogOdds();
			sum.logOfProb = l1.logOfProb + l2.logOfProb + l3.logOfProb;
			return sum;
		}
	}


	@Override
	public int compareTo(LogOdds that) 
	{
		if (that == null)
			return 12345;
		
		if (this.probIsZero  &&  that.probIsZero)
			return 0;
		else if (this.probIsZero)
			return -1;
		else if (that.probIsZero)
			return 1;
		else
			return (int)Math.signum(this.logOfProb - that.logOfProb);
	}
	
	
	@Override
	public boolean equals(Object x)
	{
		LogOdds that = (LogOdds)x;
		return this.compareTo(that) == 0;
	}
	
	
	@Override
	public String toString()
	{
		return probIsZero  ?  "{0}"  :  "" + logOfProb;
	}
}
