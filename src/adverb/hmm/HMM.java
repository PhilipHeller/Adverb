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
import java.text.*;


public class HMM<S, E> implements java.io.Serializable
{
	private static final long 								serialVersionUID = -5169538175476919766L;
	
	protected String										name;				// optional
	protected DiscreteProbabilityDistribution<S> 			initialStateProbs;
	protected DualKeyProbabilityDistribution<S, S> 			transitionProbs;
	protected DualKeyProbabilityDistribution<S, E> 			emissionProbs;
	protected LogOddsDiscreteProbabilityDistribution<S> 	logOddsInitialProbs;
	protected LogOddsDualKeyDiscreteProbabilityDistribution<S, S> 	
															logOddsTransitionProbs;
	protected LogOddsDualKeyDiscreteProbabilityDistribution<S, E> 
															logOddsEmissionProbs;
	protected boolean										verboseEvaluation;
	
	
	public HMM(DiscreteProbabilityDistribution<S> initialStateProbs, 
		       DualKeyProbabilityDistribution<S, S> transitionProbs,
		       DualKeyProbabilityDistribution<S, E> emissionProbs)
	{
		assert transitionProbs.checkIntegrity() == null;
		assert emissionProbs.checkIntegrity() == null;
		
		this.initialStateProbs = initialStateProbs;
		this.transitionProbs = transitionProbs;
		this.emissionProbs = emissionProbs;
		
		// In case Viterbi needs them.
		logOddsInitialProbs = new LogOddsDiscreteProbabilityDistribution<>(initialStateProbs);
		logOddsTransitionProbs = new LogOddsDualKeyDiscreteProbabilityDistribution<>(transitionProbs);
		logOddsEmissionProbs = new LogOddsDualKeyDiscreteProbabilityDistribution<>(emissionProbs);
	}
	
	
	public HMM(HMM<S, E> src)
	{
		this.initialStateProbs = new DiscreteProbabilityDistribution<>(src.initialStateProbs);
		this.transitionProbs = new DualKeyProbabilityDistribution<>(src.transitionProbs);
		this.emissionProbs = new DualKeyProbabilityDistribution<>(src.emissionProbs);
	}
	
	
	// Just for subclasses and HMMReader.
	protected HMM()						{ }
	
	
	public DiscreteProbabilityDistribution<S> getInitialDistribution()
	{
		return initialStateProbs;
	}
	

	public DualKeyProbabilityDistribution<S, S> getTransitionProbabilities()
	{
		return transitionProbs;
	}
	

	public DualKeyProbabilityDistribution<S, E> getEmissionProbabilities()
	{
		return emissionProbs;
	}
	
	
	// Just for subclasses.
	protected void setInitialDistribution(DiscreteProbabilityDistribution<S> initialStateProbs) 
	{
		this.initialStateProbs = initialStateProbs; 
	}
	
	
	// Just for subclasses.
	protected void setLogOddsInitialDistribution(DiscreteProbabilityDistribution<S> initialStateProbs) 
	{
		this.logOddsInitialProbs = new LogOddsDiscreteProbabilityDistribution<S>(initialStateProbs);
	}
	

	// Just for subclasses.
	protected void setTransitionProbs(DualKeyProbabilityDistribution<S, S> transitionProbs)
	{
		this.transitionProbs = transitionProbs;
	}
	

	// Just for subclasses.
	protected void setLogOddsTransitionProbs(DualKeyProbabilityDistribution<S, S> transitionProbs)
	{
		this.logOddsTransitionProbs = new LogOddsDualKeyDiscreteProbabilityDistribution<>(transitionProbs);
	}
	
	
	// Just for subclasses.
	protected void setEmissionProbs(DualKeyProbabilityDistribution<S, E> emissionProbs)
	{
		this.emissionProbs = emissionProbs;
	}
	
	
	// Just for subclasses.
	protected void setLogOddsEmissionProbs(DualKeyProbabilityDistribution<S, E> emissionProbs)
	{
		this.logOddsEmissionProbs = new LogOddsDualKeyDiscreteProbabilityDistribution<>(emissionProbs);
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public Set<E> getEmissionAlphabet()
	{
		return emissionProbs.subkeySet();		
	}
	
	
	protected Collection<S> collectStates()
	{
		Set<S> states = new HashSet<>();
		
		if (initialStateProbs != null)
			states.addAll(initialStateProbs.keySet());
		
		if (emissionProbs != null)
			states.addAll(emissionProbs.keySet());
		
		if (transitionProbs != null)
		{
			states.addAll(transitionProbs.keySet());
			states.addAll(transitionProbs.subkeySet());
		}
		
		return states;
	}
	
	
	// Order is arbitrary. Subclasses can override.
	protected String statesToString()
	{
		String s = "";
		for (S state: collectStates())
			s += " " + state;
		return s;
	}
	

	@Override
	// Caution: Slow for big (e.g. realistic protein) models.
	public String toString()
	{
		// Name.
		String s = "HMM " + name;
		
		// States and emissions.
		s += "\n  STATES:";
		s += statesToString();
		s += "\n\n  EMISSIONS: ";
		for (E e: getEmissionAlphabet())
			s += e + "/";
		s = s.substring(0, s.length()-1);
		
		// Initial probs.
		s += "\n\n  INITIAL PROBABILITIES:\n" + initialStateProbs;		
		
		// Transition probs.
		Format fmt = new DecimalFormat("#.######");
		s += "\n\n  TRANSITION PROBABILITIES:";
		Collection<S> states = collectStates();
		for (S state: states)
		{
			sop("state = " + state);
			DiscreteProbabilityDistribution<S> transitionProbsFromThisState = transitionProbs.get(state);
			if (transitionProbsFromThisState == null)
				continue;		// e.g. if state is STOP
			s += "\n    ";
			for (S stateDest: transitionProbsFromThisState.keySet())
			{
				sop("stateDest = " + stateDest);
				s += "P(" + state + "=>" + stateDest + ") = " + fmt.format(transitionProbsFromThisState.get(stateDest)) + ", ";		
			}
			s = s.substring(0, s.length()-2);
		}
		
		// Emission probs.
		s += "\n\n  EMISSION PROBABILITIES:";
		for (S state: states)
		{
			DiscreteProbabilityDistribution<E> emissionProbsFromThisState = emissionProbs.get(state);
			if (emissionProbsFromThisState == null)
				continue;		// e.g. if state is START, STOP, or DELETE
			s += "\n    ";
			for (E emission: emissionProbsFromThisState.keySet())
				s += "P(" + emission + "|" + state + ") = " + fmt.format(emissionProbsFromThisState.get(emission)) + ", ";	
			s = s.substring(0, s.length()-2);
		}
		
		return s;
	}
	
	
	public String stateToString(S state)
	{
		String s = "STATE = " + state + "\n  Transitions:";
		
		Format fmt = new DecimalFormat("#.######");
		DiscreteProbabilityDistribution<S> transitionDist = transitionProbs.get(state);
		for (S nextState: transitionDist.keySet())
			s += "\n    --> " + nextState + " = " + fmt.format(transitionDist.get(nextState));
		
		s += "\n  Emissions:";
		DiscreteProbabilityDistribution<E> emissionDist = emissionProbs.get(state);
		for (E emission: emissionDist.keySet())
			s += "\n    " + emission + ": " + fmt.format(emissionDist.get(emission));				
		
		return s;
	}
	
	
	
	
	
	
	
					
					///////////////////////////////////////////////////
					//                                               //
					//                    VITERBI                    //
					//                                               //
					///////////////////////////////////////////////////
					
					
	
	/********
	private Stack<DPStage<S>> buildViterbiStages(List<E> observations)
	{
		if (observations == null  ||  observations.isEmpty())
			return null;
		
		Stack<DPStage<S>> stages = new Stack<>(); 
		
		// Clone the observations list; the clone will be destroyed.
		observations = new ArrayList<>(observations);
		
		// First stage, from initial probabilities.
		DPStage<S> firstStage = new DPStage<>();
		E firstObservation = observations.remove(0);
		for (S state: initialStateProbs.keySet())
		{
			if (!emissionProbs.containsKeys(state, firstObservation))
				continue;
			float probOfState = initialStateProbs.get(state);
			float probOfEmission = emissionProbs.get(state, firstObservation);
			float prob = probOfState * probOfEmission;
			DPCell<S> cell = new DPCell<>();
			cell.put(null, prob);  // null as prev state means START state
			cell.setProbabilityToBest();
			firstStage.put(state, cell);
		}
		stages.push(firstStage);
		
		// Subsequent stages.
		int n = 0;
		for (E observation: observations)
		{
			if (verboseEvaluation)
				sop("Viterbi at stage " + ++n + " of " + observations.size());
			DPStage<S> prevStage = stages.peek();
			DPStage<S> nextStage = generateNextViterbiStage(prevStage, observation);
			stages.push(nextStage);
		}
		
		return stages;
	}
	
	************/

	/********	
	// Computes most probable path and its probability.
	public ViterbiReport<S> viterbi(List<E> observations)
	{
		// Clone the observations list; the clone will be destroyed.
		observations = new ArrayList<>(observations);

		// Build stages.
		Stack<DPStage<S>> stages = buildViterbiStages(observations);
		
		// Find best final state.
		DPStage<S> finalStage = stages.peek();	
		DPCell<S> bestFinalCell = null;	
		S bestFinalState = null;
		float pBestFinalState = -1f;
		for (S state: finalStage.keySet())
		{
			DPCell<S> cell = finalStage.get(state);
			float p = cell.getProbability();
			if (p > pBestFinalState)
			{
				bestFinalCell = cell;
				bestFinalState = state;
				pBestFinalState = p;
			}
		}
		assert bestFinalCell != null  :  "No best final cell";
		
		// Collect best states, tracing back from final.
		List<S> stateTraceback = new ArrayList<>();
		stateTraceback.add(bestFinalState);
		DPCell<S> tracebackCell = bestFinalCell;
		stages.pop();
		while (!stages.isEmpty())
		{
			assert tracebackCell != null  :  "Null traceback cell";
			S tracebackState = tracebackCell.getBestPrevState();
			assert tracebackState != null  :  "Null best-prev from traceback cell " + tracebackCell;
			stateTraceback.add(0, tracebackState);
			DPStage<S> tracebackStage = stages.pop();
			tracebackCell = tracebackStage.get(tracebackState);
		}
		
		return new ViterbiReport<>(pBestFinalState, stateTraceback);
	}
	*******/
	
	
	// Computes most probable path and its probability, using the log-odds domain.
	public ViterbiReport<S> viterbiWithLogOdds(List<E> observations) throws HMMException
	{
		assert observations.size() > 1;
		
		Stack<LogOddsDPStage<S>> stages = new Stack<>(); 
		
		// Clone the observations list; the clone will be destroyed.
		observations = new ArrayList<>(observations);
		
		// First stage, from initial probabilities.
		LogOddsDPStage<S> firstStage = new LogOddsDPStage<>();
		E firstObservation = observations.remove(0);
		for (S state: initialStateProbs.keySet())
		{
			LogOdds probOfState = logOddsInitialProbs.get(state);
			LogOdds probOfEmission = logOddsEmissionProbs.get(state, firstObservation);
			LogOdds prob = LogOdds.plus(probOfState, probOfEmission);
			LogOddsDPCell<S> cell = new LogOddsDPCell<>();
			cell.put(null, prob);  // null as prev state means START state
			cell.setProbabilityToBest();
			firstStage.put(state, cell);
		}
		stages.push(firstStage);
		
		// Subsequent stages.
		int n = 0;
		for (E observation: observations)
		{
			if (verboseEvaluation)
				sop("Viterbi(log) at stage " + ++n + " of " + observations.size());
			LogOddsDPStage<S> prevStage = stages.peek();
			LogOddsDPStage<S> nextStage = generateNextViterbiStageWithLogOdds(prevStage, observation);
			stages.push(nextStage);
		}
		
		// Find best final state.
		LogOddsDPStage<S> finalStage = stages.peek();	
		LogOddsDPCell<S> bestFinalCell = null;	
		S bestFinalState = null;
		LogOdds pBestFinalState = null;
		for (S state: finalStage.keySet())
		{
			LogOddsDPCell<S> cell = finalStage.get(state);
			LogOdds p = cell.getProbability();
			if (p.compareTo(pBestFinalState) > 0)
			{
				bestFinalCell = cell;
				bestFinalState = state;
				pBestFinalState = p;
			}
		}
		assert bestFinalCell != null  :  "No best final cell";
		
		// Collect best states, tracing back from final.
		List<S> stateTraceback = new ArrayList<>();
		stateTraceback.add(bestFinalState);
		LogOddsDPCell<S> tracebackCell = bestFinalCell;
		stages.pop();
		while (!stages.isEmpty())
		{
			S tracebackState = tracebackCell.getBestPrevState();
			stateTraceback.add(0, tracebackState);
			LogOddsDPStage<S> tracebackStage = stages.pop();
			tracebackCell = tracebackStage.get(tracebackState);
		}
		
		return new ViterbiReport<>(pBestFinalState, stateTraceback);
	}	
	
	/*******
	private DPStage<S> generateNextViterbiStage(DPStage<S> prevStage, E observation)
	{
		DPStage<S> nextStage = new DPStage<S>();
		for (S stateInNextStage: collectStates())
		{
			if (!emissionProbs.containsKeys(stateInNextStage, observation))
				continue;
			DPCell<S> cellInNextStage = new DPCell<>();
			nextStage.put(stateInNextStage, cellInNextStage);
			float pObservation = emissionProbs.get(stateInNextStage, observation);
			for (S stateInPrevStage: prevStage.keySet())
			{
				Float pTransition = transitionProbs.get(stateInPrevStage, stateInNextStage);
				if (pTransition == null)
					continue;
				float pPrevState = prevStage.get(stateInPrevStage).getProbability();
				float pPrevAndTransitionAndObservation = pPrevState * pTransition * pObservation;
				cellInNextStage.put(stateInPrevStage, pPrevAndTransitionAndObservation);
			}
			cellInNextStage.setProbabilityToBest();
		}
		
		return nextStage;
	}
	**********/
	
	
	private LogOddsDPStage<S> generateNextViterbiStageWithLogOdds(LogOddsDPStage<S> prevStage, E observation)
	{
		LogOddsDPStage<S> nextStage = new LogOddsDPStage<S>();
		
		for (S stateInNextStage: initialStateProbs.keySet())
		{
			LogOddsDPCell<S> cellInNextStage = new LogOddsDPCell<>();
			nextStage.put(stateInNextStage, cellInNextStage);
			LogOdds pObservation = logOddsEmissionProbs.get(stateInNextStage, observation);
			for (S stateInPrevStage: prevStage.keySet())
			{
				LogOdds pPrevState = prevStage.get(stateInPrevStage).getProbability();
				LogOdds pTransition = logOddsTransitionProbs.get(stateInPrevStage, stateInNextStage);
				assert pTransition != null  :  stateInPrevStage + " ?? " + stateInNextStage;
				LogOdds pPrevAndTransitionAndObservation = LogOdds.plus(pPrevState, pTransition, pObservation);
				cellInNextStage.put(stateInPrevStage, pPrevAndTransitionAndObservation);
			}
			cellInNextStage.setProbabilityToBest();
		}
		
		return nextStage;
	}
	
	
	
	

					

	

				
	
	
			
			
						
						
						//////////////////////////////////////////////////////
						//                                                  //
						//                    MISC & MAIN                   //
						//                                                  //
						//////////////////////////////////////////////////////
						
				
	
	public void setVerboseEvaluation(boolean b)
	{
		verboseEvaluation = b;
	}
	

	static void sop(Object x) 		{ System.out.println(x); }
	static void dsop(Object x) 		{ System.out.println(new Date() + ": " + x); }
}
