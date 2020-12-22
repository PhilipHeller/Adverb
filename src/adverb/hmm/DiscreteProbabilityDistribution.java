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


public class DiscreteProbabilityDistribution<S> extends LinkedHashMap<S, Float> implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;




	public DiscreteProbabilityDistribution()	{ } 
	
	
	// All states are equiprobable.
	public DiscreteProbabilityDistribution(Collection<S> states)
	{
		float prob = 1f / states.size();
		for (S state: states)
			put(state, prob);
		String integErr = checkIntegrity();
		assert integErr == null : integErr;
	}
	
	
	public DiscreteProbabilityDistribution(DiscreteProbabilityDistribution<S> src)
	{
		src.keySet()
			.stream()
			.forEach(k -> put(k, src.get(k)));
	}
	
	
	public String checkIntegrity()
	{
		double sumOfProbs = 
			values()
			.stream()
			.reduce(0f, (i, j) -> i+j);
		
		if (Math.abs(1 - sumOfProbs) < 1.0e-4f)
			return null;
		else
			return "Excessive delta: " + sumOfProbs + " should be closer to 1\n" + this;
	}
	
	
	public void put(S s, float f)
	{
		super.put(s, f);
	}
	
	
	public void assignRemainingProbabilitiesEqually(Collection<S> unrepresentedEmissions)
	{
		unrepresentedEmissions = new HashSet<S>(unrepresentedEmissions);
		unrepresentedEmissions.removeAll(keySet());		// just in case
		float remainingProb = 1f;
		for (float f: values())
			remainingProb -= f;
		assert remainingProb >= 0  &&  remainingProb <= 1;
		float probPerEvent = remainingProb / unrepresentedEmissions.size();
		for (S s: unrepresentedEmissions)
			put(s, probPerEvent);
	}
	
	
	// E.g. if there are 50 remaining emissions then the ratios of their probabilities are 50:49:48...:2:1. 
	public void assignRemainingProbabilitiesLinearDescent(List<S> unrepresentedEmissions)
	{
		float remainingProb = 1;
		for (float d: values())
			remainingProb -= d;
		assert remainingProb >= 0  &&  remainingProb <= 1;
		
		int nRemainingEmissions = unrepresentedEmissions.size();
		float denom = nRemainingEmissions * (nRemainingEmissions+1) / 2;		// = 1 + 2 + ...  + n
		float numer = nRemainingEmissions;
		for (S emission: unrepresentedEmissions)
		{
			float p = remainingProb * numer / denom;
			put(emission, p);
			numer--;
		}
	}
	
	
	// E.g. if tax rate is .1, then a probability of .5 becomes .45.
	public void tax(float taxRate)
	{
		keySet()
			.stream()
			.forEach(s -> put(s, get(s)*(1f-taxRate)));
	}
	
	
	public void setPseudocountProbabilities(Collection<S> allPossibleEmissions, float totalPseudoProb) throws IllegalArgumentException
	{
		// Collect unrepresented emissions.
		Set<S> unrepresentedEmissions = new HashSet<>(allPossibleEmissions);
		unrepresentedEmissions.removeAll(keySet());	// survivors get pseudoprobs
		if (unrepresentedEmissions.isEmpty())
			return;
	
		// Tax represented keys.
		tax(totalPseudoProb);
		
		// Add unrepresented keys.
		float individualPseudoProb = totalPseudoProb / unrepresentedEmissions.size();
		unrepresentedEmissions
			.stream()
			.forEach(s -> put(s, individualPseudoProb));
	}
	
	
	public void setPseudocountProbabilities(S[] allPossibleSs, float totalPseudoProb)
	{
		setPseudocountProbabilities(Arrays.asList(allPossibleSs), totalPseudoProb);
	}
	
	
	public S random()
	{
		return random(Math.random());
	}
	
	
	protected S random(double rand)
	{
		for (S s: keySet())
		{
			float probOfS = get(s);
			if (probOfS >= rand)
				return s;
			else
				rand -= get(s);
		}
		assert false : rand;
		return null;
	}
	
	
	public String toString()
	{
		String s = getClass().getName() + ":";
		for (S key: keySet())
			s += "\n  " + key + " = " + get(key);
		return s;
	}
	
	
	static void sop(Object x)  { System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		sop("START");
		
		Set<String> ALL_EMISSIONS = new TreeSet<>();
		ALL_EMISSIONS.add("A");
		ALL_EMISSIONS.add("C");
		ALL_EMISSIONS.add("G");
		ALL_EMISSIONS.add("T");
		
		DiscreteProbabilityDistribution<String> that = new DiscreteProbabilityDistribution<>();
		that.put("A", 1/6f);
		that.put("C", 1/2f);
		that.put("G", 1/6f);
		that.put("T", 1/6f);
		sop(that);
		that.setPseudocountProbabilities(ALL_EMISSIONS, 0.01f);
		sop("*****\n" + that);
		
		sop("\n\nDONE");
	}
	
}
