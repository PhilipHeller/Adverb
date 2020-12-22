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


public class DualKeyProbabilityDistribution<S, T> extends LinkedHashMap<S, DiscreteProbabilityDistribution<T>>
{
	public DualKeyProbabilityDistribution()		{ }
	
	
	public DualKeyProbabilityDistribution(DualKeyProbabilityDistribution<S, T> src)
	{
		src.keySet()
			.stream()
			.forEach(key -> put(key, new DiscreteProbabilityDistribution<T>(src.get(key))));
	}
	
	
	public void put(S s, T t, Float prob)
	{
		DiscreteProbabilityDistribution<T> subDist = get(s);
		if (subDist == null)
		{
			subDist = new DiscreteProbabilityDistribution<T>();
			put(s, subDist);
		}
		subDist.put(t, prob);
	}
	
	
	public boolean containsKeys(S key1, T key2)
	{
		return containsKey(key1)  &&  get(key1).containsKey(key2);
	}
	
	
	public void put(S s, T t, float dprob)
	{
		put(s, t, new Float(dprob));
	}
	
	
	public Float get(S s, T t)
	{
		DiscreteProbabilityDistribution<T> subDist = get(s);
		if (subDist != null)
			return subDist.get(t);
		else
			return null;
	}
	
	
	// Returns null if this map has integrity, otherwise returns a message.
	public String checkIntegrity()
	{
		String ret = null;
		for (DiscreteProbabilityDistribution<T> dist: values())
			if ((ret = dist.checkIntegrity()) != null)
				return ret;
		
		return null;
	}
	
	
	public Set<T> subkeySet()
	{
		Set<T> subkeys = new HashSet<>();
		values()
			.stream()
			.forEach(dist -> subkeys.addAll(dist.keySet()));
		return subkeys;		
	}
	
	
	public void ensureContainsMajorKey(S s)
	{
		if (!containsKey(s))
			put(s, new DiscreteProbabilityDistribution<T>());
	}
	
	
	public void unmap(S s, T t)
	{
		DiscreteProbabilityDistribution<T> submap = get(s);
		if (submap != null)
			submap.remove(t);
	}
	
	
	public String toString()
	{
		String s = "DualKeyProbabilityDistribution:";
		for (S key: keySet())
			s += "\n  for major key " + key + ": " + get(key);
		return s;
	}
}
