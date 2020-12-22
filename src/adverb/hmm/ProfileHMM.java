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

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import adverb.util.*;


public class ProfileHMM extends HMM<String, Character>
{
	private static final long 				serialVersionUID = -6896773925044697813L;
	
	public final static String				START_STATE_NAME		= "START";
	public final static String				STOP_STATE_NAME			= "STOP";
	private final static float				DFLT_PSEUDOPROBABILITY	= 0.01f;
	private final static float				DFLT_P_MATCH_TO_INSERT	= 0.01f;
	private final static float				DFLT_P_INSERT_TO_SELF	= 0.01f;
	private final static float				SOFT_DELETE_TAX_RATE	= 0.01f;
	private final static Set<Character> 	ALL_EMISSIONS;
	
	private String							stateIndexFormatter;				// usually "%03d"
	
	
	static
	{
		ALL_EMISSIONS = new TreeSet<>();
		ALL_EMISSIONS.add('A');
		ALL_EMISSIONS.add('C');
		ALL_EMISSIONS.add('G');
		ALL_EMISSIONS.add('T');
	}
	
	
	private static boolean					verboseConstruction;
	
	private int								nTrainingSeqs;			
	private List<String>					orderedHardDeleteStates;		// forced by indels in training alignment
	
		
	public ProfileHMM(Alignment alignment)
	{
		init(alignment, DFLT_PSEUDOPROBABILITY);
	}
	
	
	// File must be fasta.
	public ProfileHMM(File file) throws IOException
	{
		init(new Alignment(file), DFLT_PSEUDOPROBABILITY);
	}
	
	
	public static void setVerboseConstruction(boolean b)
	{
		verboseConstruction = b;
	}
	
	
	private void init(Alignment alignment, float pseudoprob)
	{
		nTrainingSeqs = alignment.size();
		
		// Compute a string formatter for the index portion of state names. For a normal-size protein this
		// will generally be "%03d". The zero forces leading zeros, so alpha sorting equals numeric sorting.
		int nCols = alignment.nCols();
		int nDigits = ("" + nCols).length();
		stateIndexFormatter = "%0" + nDigits + "d";
		
		// Initial distribution. Always start in the START state.
		if (verboseConstruction)
			dsop("initial distribution");
		DiscreteProbabilityDistribution<String> localInitialProbs = new DiscreteProbabilityDistribution<>();
		super.setInitialDistribution(localInitialProbs);
		localInitialProbs.put(START_STATE_NAME, 1);
		
		// Build emission distributions for MATCH states.
		if (verboseConstruction)
			dsop("emission distributions for MATCH states");
		DualKeyProbabilityDistribution<String, Character> localEmissionProbs = new DualKeyProbabilityDistribution<>();
		super.setEmissionProbs(localEmissionProbs);
		for (int col=0; col<alignment.nCols(); col++)
		{
			TreeBinCounter<Character> charCountsForCol = alignment.binCountColumn(col);
			charCountsForCol.remove('-');
			float fCountTotal = charCountsForCol.getSumOfAllCounts();
			String matchName = colNumToMatchName(col);
			for (Character ch: charCountsForCol.keySet())
			{
				float prob = charCountsForCol.getCountForBin(ch) / fCountTotal;
				localEmissionProbs.put(matchName, ch, prob);
			}
			localEmissionProbs.get(matchName).setPseudocountProbabilities(ALL_EMISSIONS, pseudoprob);		// pseudocount tax
			String err = localEmissionProbs.checkIntegrity();
			assert err == null  :  
				"Integrity inconsistency for match state " + matchName + " (col " + col + "):\n" + err + "\n" + charCountsForCol;
		}
		
		// Build transition distributions.
		if (verboseConstruction)
			dsop("transition distributions");
		DualKeyProbabilityDistribution<String, String> localTransitionProbs = new DualKeyProbabilityDistribution<>();
		super.setTransitionProbs(localTransitionProbs);
		ArrayList<TreeBinCounter<Integer>> gapLengthCtrsByStartCol = alignment.getGapLengthCtrsByStartCol();  // for DELETE states
		orderedHardDeleteStates = new ArrayList<>();
		for (int fromCol=-1; fromCol<alignment.nCols(); fromCol++)
		{
			// Name the "from" and "to" states. These might include the START or STOP states.
			int toCol = fromCol + 1;
			String fromEmitState = (fromCol == -1)  ?  START_STATE_NAME  :  colNumToMatchName(fromCol);
			String toEmitState = (fromCol == alignment.nCols()-1)  ?  STOP_STATE_NAME  :  colNumToMatchName(toCol);

			// The from state is always followed by an INSERT state.
			String insertState = colNumToInsertName(toCol);		// INSERT_0 ... INSERT_N
			localTransitionProbs.put(fromEmitState, insertState, DFLT_P_MATCH_TO_INSERT);
			localTransitionProbs.put(insertState, insertState, DFLT_P_INSERT_TO_SELF);
			localTransitionProbs.put(insertState, toEmitState, 1f-DFLT_P_INSERT_TO_SELF);
			localEmissionProbs.ensureContainsMajorKey(insertState);
			localEmissionProbs.get(insertState).assignRemainingProbabilitiesEqually(ALL_EMISSIONS);
			
			// Set default MATCH->MATCH probabilities (also START->MATCH and MATCH->STOP). This will be 
			// updated if a DELETE state is involved.
			localTransitionProbs.put(fromEmitState, toEmitState, 1f-DFLT_P_MATCH_TO_INSERT);
			
			// The from state might be followed by a hard DELETE state. A DELETE state is hard if any indel
			// runs begin in the to state.
			if (fromCol == alignment.nCols()-1)
				continue;
			TreeBinCounter<Integer> gapLenCtr = gapLengthCtrsByStartCol.get(toCol);
			if (gapLenCtr.isEmpty())
				continue;
			String deleteState = colNumToDeleteName(toCol);
			orderedHardDeleteStates.add(deleteState);
			float nGapOpens = gapLenCtr.size();
			float pToDelete = nGapOpens / alignment.nRows();
			localTransitionProbs.put(fromEmitState, deleteState, pToDelete);
			float oldPEmitEmit = localTransitionProbs.get(fromEmitState, toEmitState);
			localTransitionProbs.put(fromEmitState, toEmitState, oldPEmitEmit-pToDelete);
			assert localTransitionProbs.get(fromEmitState).checkIntegrity() == null;
			float nIndelRunOpensInToCol = gapLenCtr.getSumOfAllCounts();
			for (Integer runLength: gapLenCtr.keySet())
			{
				assert runLength > 0;
				float pFromDelete = gapLenCtr.getCountForBin(runLength) / nIndelRunOpensInToCol;
				int destColNum = toCol + runLength;
				String destEmitName = (destColNum < alignment.nCols())  ?  colNumToMatchName(destColNum)  :  STOP_STATE_NAME;
				localTransitionProbs.put(deleteState, destEmitName, pFromDelete);
			}
		}
		
		// Add a "soft" DELETE state "above" every match state that doesn't already have a hard DELETE state. A soft DELETE
		// state provides low-probability shortcuts to all subsequent match states, for generating and evaluating sequences that
		// would otherwise be too short.
		if (verboseConstruction)
			sop("soft delete states");
		for (int col=0; col<alignment.nCols(); col++)
		{
			String softDelName = colNumToDeleteName(col);
			if (localTransitionProbs.containsKey(softDelName))
				continue;											// there's already a hard DELETE state here
			// Provide a low-probability shortcut from prevMatchName, around col's MATCH state.
			String prevMatchName = colNumToMatchName(col-1); 
			DiscreteProbabilityDistribution<String> transitionsFromPrevMatch = localTransitionProbs.get(prevMatchName);
			transitionsFromPrevMatch.tax(SOFT_DELETE_TAX_RATE);
			transitionsFromPrevMatch.put(softDelName, SOFT_DELETE_TAX_RATE);
			// Assign a transition probability from the new soft delete state to all subsequent MATCH states. Closer
			// MATCH states have higher probability.
			DiscreteProbabilityDistribution<String> transitionsFromNewSoftDel = new DiscreteProbabilityDistribution<>();
			List<String> remainingMatchStateNames = new ArrayList<>();
			for (int i=col+1; i<alignment.nCols(); i++)
				remainingMatchStateNames.add(colNumToMatchName(i));
			remainingMatchStateNames.add(STOP_STATE_NAME);
			transitionsFromNewSoftDel.assignRemainingProbabilitiesLinearDescent(remainingMatchStateNames);
			String integrityErr = transitionsFromNewSoftDel.checkIntegrity();
			assert integrityErr == null  :  integrityErr;
			localTransitionProbs.put(softDelName, transitionsFromNewSoftDel);
		}	
		
		// DELETE states don't emit, so they mess up the Viterbi, Forward, and Backward algorithms. Since they are really
		// adjustments to the the MATCH states that they connect, they can be modeled by adjusting transition probabilities
		// among MATCH states. The only hitch is that after this adjustment, the transition probabilities are no longer
		// faithful to the expected model. Not a major issue since we can store the original transition probabilities, e.g.
		// for a graphical view of the model. The alternative would be to adjust the algorithms to handle non-emitting states.
		// That wouldn't be especially hard, but the number of states would double, significantly slowing down the O(nStates^2)
		// computation of cells.
		if (verboseConstruction)
			sop("adjust delete");
		for (String srcMatchState: localTransitionProbs.keySet())
		{
			List<String> deleteDests = new ArrayList<>();
			for (String dest: localTransitionProbs.get(srcMatchState).keySet())
				if (isDeleteState(dest))
					deleteDests.add(dest);
			if (deleteDests.isEmpty())
				continue;
			// This source state has non-zero probability of transition to a DELETE state.
			assert isMatchState(srcMatchState)  ||  isStartState(srcMatchState);
			assert deleteDests.size() == 1;							// a MATCH state may only enter at most 1 DELETE state
			String middleDelState = deleteDests.get(0);
			float pMatchToDelete = localTransitionProbs.get(srcMatchState, middleDelState);
			DiscreteProbabilityDistribution<String> transitionsFromMiddleDelState = localTransitionProbs.get(middleDelState);
			for (String destMatchState: transitionsFromMiddleDelState.keySet())
			{
				// We have detected Mx -> D -> My. Change to Mx -> My, with probability p(Mx->D) * p(D->My).
				// DELETE state may only transition to MATCH state(s) or STOP.
				assert isMatchState(destMatchState)  ||  isStopState(destMatchState);	
				float pBypass = pMatchToDelete * transitionsFromMiddleDelState.get(destMatchState);
				localTransitionProbs.put(srcMatchState, destMatchState, pBypass);
			}
			// middleDelState is now bypassed. There should no longer be a possibility of transition from
			// srcMatchState to middleDelState.
			localTransitionProbs.unmap(srcMatchState, middleDelState);
			assert localTransitionProbs.get(srcMatchState).checkIntegrity() == null;
		}
		
		// Remove all transitions from DELETE states. This is not strictly necessary, since it's no longer possible to
		// get to any DELETE state, but it saves space.
		List<String> deleteStates = 
			localTransitionProbs.keySet()
			.stream()
			.filter(s -> isDeleteState(s))
			.collect(Collectors.toList());
		deleteStates
			.stream()
			.forEach(s -> localTransitionProbs.remove(s));

		// Convert START state to an initial state distribution.
		localInitialProbs = localTransitionProbs.get(START_STATE_NAME);
		localTransitionProbs.remove(START_STATE_NAME);
		
		// Set distributions in superclass. Allows calls to superclass method HMM.collectStates().
		setInitialDistribution(localInitialProbs);
		setTransitionProbs(localTransitionProbs);
		setEmissionProbs(localEmissionProbs);
		
		// Assert no DELETE states survive.
		for (String s: collectStates())
			assert !isDeleteState(s);
		
		// Assert no references to START state survive.
		assert localInitialProbs == initialStateProbs;
		assert !localInitialProbs.containsKey(START_STATE_NAME);
		assert !initialStateProbs.containsKey(START_STATE_NAME);
		assert !localEmissionProbs.containsKey(START_STATE_NAME);
		assert !localTransitionProbs.containsKey(START_STATE_NAME);
		for (DiscreteProbabilityDistribution<String> dist: localTransitionProbs.values())
			assert !dist.containsKey(START_STATE_NAME);
		for (String s: collectStates())
			assert !isStartState(s);

		// In case Viterbi needs them.
		if (verboseConstruction)
			sop("log(probs)");
		logOddsInitialProbs = new LogOddsDiscreteProbabilityDistribution<>(initialStateProbs);
		logOddsTransitionProbs = new LogOddsDualKeyDiscreteProbabilityDistribution<>(localTransitionProbs);
		logOddsEmissionProbs = new LogOddsDualKeyDiscreteProbabilityDistribution<>(localEmissionProbs);
		if (verboseConstruction)
			sop("finished construction");
	}	
	
	
	public static int stateNameToColNum(String s)
	{
		return s.equals(START_STATE_NAME)  ?  -1  :  Integer.parseInt(s.substring(2));
	}
	

	public String colNumToMatchName(int n)		
	{ 
		return (n >= 0)  ?  "M_" + String.format(stateIndexFormatter, n)  :  START_STATE_NAME; 
	}
	
	
	public String colNumToInsertName(int n)				{ return "I_" + String.format(stateIndexFormatter, n); }	
	public String colNumToDeleteName(int n)				{ return "D_" + String.format(stateIndexFormatter, n); }
	public static boolean isMatchState(String s)		{ return s.startsWith("M_"); }
	public static boolean isInsertState(String s)		{ return s.startsWith("I_"); }
	public static boolean isDeleteState(String s)		{ return s.startsWith("D_"); }
	public static boolean isStartState(String s)		{ return s.equals(START_STATE_NAME); }
	public static boolean isStopState(String s)			{ return s.equals(STOP_STATE_NAME); }
	

	public String statesToString()
	{
		String s = "STATES:";
		for (String st: new TreeSet<String>(collectStates())) 
			s += " " + st;
		return s;
	}


	
	
	
	
					
					
					
					///////////////////////////////////////////////////
					//                                               //
					//                    VITERBI                    //
					//                                               //
					///////////////////////////////////////////////////
					
	
	
	
	private static List<Character> stringToCharList(String s)
	{
		return
			s.chars()
			.mapToObj(e -> (char)e)
			.collect(Collectors.toList());			
	}
	

	public ViterbiReport<String> viterbiWithLogOdds(String sObservations) throws HMMException
	{
		List<Character> observations = stringToCharList(sObservations);
		return viterbiWithLogOdds(observations);
	}
	

	public float logOddsViterbiScore(String sObservations) throws HMMException
	{
		List<Character> observations = stringToCharList(sObservations);
		return logOddsViterbiScore(observations);
	}
	
	
	// Computes most probable path and its probability, using the log-odds domain.
	public ViterbiReport<String> viterbiWithLogOdds(List<Character> observations) throws HMMException
	{
		return viterbiWithLogOdds(observations, true);
	}
	
	
	// Doesn't compute the Viterbi path. Saves memory because only most
	// recent stage is retained.
	public float logOddsViterbiScore(List<Character> observations) throws HMMException
	{
		ViterbiReport<String> rep = viterbiWithLogOdds(observations, false);
		return rep.getLogOdds().get();
	}
	
	
	public ViterbiReport<String> viterbiWithLogOdds(List<Character> observations, boolean retainPath) throws HMMException
	{
		observations.removeIf(ch -> ch.equals('-'));
		if (observations.isEmpty())
			return null;

		// Compute first stage.
		Stack<LogOddsDPStage<String>> stages = new Stack<>();
		LogOddsDPStage<String> firstStage = new LogOddsDPStage<>();
		Character firstObservation = observations.remove(0);
		for (String state: logOddsInitialProbs.keySet())
		{
			LogOdds probOfState = logOddsInitialProbs.get(state);
			LogOdds probOfEmission = logOddsEmissionProbs.get(state, firstObservation);
			if (probOfState == null  ||  probOfEmission == null)
				continue;
			LogOdds prob = LogOdds.plus(probOfState, probOfEmission);
			LogOddsDPCell<String> cell = new LogOddsDPCell<>();
			cell.put(null, prob);  // null as prev state means START state
			cell.setProbabilityToBest();
			firstStage.put(state, cell);
		}
		stages.push(firstStage);
		
		// Compute subsequent stages.
		int n = 0;
		for (Character observation: observations)
		{
			if (verboseEvaluation)
				sop("Viterbi(log) at stage " + ++n + " of " + observations.size());
			LogOddsDPStage<String> prevStage = stages.peek();
			LogOddsDPStage<String> nextStage = generateNextViterbiStageWithLogOdds(prevStage, observation);
			if (!retainPath)
			{
				// Saves memory but can't compute Viterbi path.
				stages.pop();
				assert stages.isEmpty();
			}
			stages.push(nextStage);
		}

		return logOddsStagesToViterbiReport(stages);
	}
	
	
	private ViterbiReport<String> logOddsStagesToViterbiReport(Stack<LogOddsDPStage<String>> stages) throws HMMException
	{
		// Find max-score path from final stage to STOP.
		LogOdds bestScore = null;
		String bestFinalEmittingState = null;
		LogOddsDPStage<String> finalStage = stages.peek();	
		
		for (String state: finalStage.keySet())
		{
			LogOddsDPCell<String> cell = finalStage.get(state);
			if (cell == null)
				continue;
			LogOdds score = cell.getProbability();
			LogOdds transitionScore = logOddsTransitionProbs.get(state, STOP_STATE_NAME);
			if (score == null  ||  transitionScore == null)
				continue;
			score = LogOdds.plus(score, transitionScore);
			if (score.compareTo(bestScore) > 0)
			{
				bestScore = score;
				bestFinalEmittingState = state;
			}
		}
		if (bestScore == null  ||  bestFinalEmittingState == null)
		{
			 throw new HMMException("Input too short, no Viterbi path");
		}
		
		// Collect best states, tracing back from final STOP state.
		StateTraceback<String> stateTraceback = new StateTraceback<>();
		stateTraceback.add(STOP_STATE_NAME);
		stateTraceback.add(0, bestFinalEmittingState);
		LogOddsDPCell<String> tracebackCell = finalStage.get(bestFinalEmittingState);
		stages.pop();
		while (!stages.isEmpty())
		{
			if (tracebackCell == null)
				return new ViterbiReport<>(LogOdds.getProbZeroInstance(), null);
			String tracebackState = tracebackCell.getBestPrevState();
			if (tracebackState == null)
				return new ViterbiReport<>(LogOdds.getProbZeroInstance(), null);
			stateTraceback.add(0, tracebackState);
			LogOddsDPStage<String> tracebackStage = stages.pop();
			tracebackCell = tracebackStage.get(tracebackState);
		}
		
		return new ViterbiReport<>(bestScore, stateTraceback);
	}	
	
	
	private LogOddsDPStage<String> 
	generateNextViterbiStageWithLogOdds(LogOddsDPStage<String> prevStage, Character observation)
	{
		LogOddsDPStage<String> nextStage = new LogOddsDPStage<String>();
		
		for (String stateInNextStage: collectStates())
		{
			if (stateInNextStage.equals(START_STATE_NAME)  ||  stateInNextStage.equalsIgnoreCase(STOP_STATE_NAME))
				continue;
			LogOddsDPCell<String> cellInNextStage = new LogOddsDPCell<>();
			LogOdds pObservation = logOddsEmissionProbs.get(stateInNextStage, observation);
			if (pObservation == null ||  pObservation.equals(LogOdds.getProbZeroInstance()))
				continue;
			for (String stateInPrevStage: prevStage.keySet())
			{
				LogOdds pPrevState = prevStage.get(stateInPrevStage).getProbability();
				LogOdds pTransition = logOddsTransitionProbs.get(stateInPrevStage, stateInNextStage);
				if (pPrevState == null  ||  pTransition == null)
					continue;
				LogOdds pPrevAndTransitionAndObservation = LogOdds.plus(pPrevState, pTransition, pObservation);
				cellInNextStage.put(stateInPrevStage, pPrevAndTransitionAndObservation);
			}
			if (cellInNextStage.isEmpty())
				continue;
			cellInNextStage.setProbabilityToBest();
			nextStage.put(stateInNextStage, cellInNextStage);
		}
		
		return nextStage;
	}
	
	
	
	
	
					
	
	
					
					///////////////////////////////////////////////////////////////
					//                                                           //
					//                       MISC AND MAIN                       //
					//                                                           //
					///////////////////////////////////////////////////////////////



	
	
	
	public int getNStates(Predicate<String> nameFilter)
	{
		return 
			(int)
			collectStates()
			.stream()
			.filter(nameFilter)
			.count();
	}
	
	
	public int getNMatchStates()
	{
		return getNStates(s -> s.startsWith("M_"));
	}
	
	
	public int getNHardDeleteStates()
	{
		return orderedHardDeleteStates.size();
	}
	
	
	public int getNInsertStates()
	{
		return getNStates(s -> s.startsWith("I_"));
	}
	
	
	public List<String> getHardDeleteStates()
	{
		return orderedHardDeleteStates;
	}
	
	
	public boolean isHardDeleteState(String stateName)
	{
		return orderedHardDeleteStates.contains(stateName);
	}
	
	
	public int getNTrainingSeqs()
	{
		return nTrainingSeqs;
	}
	
	
	public static void main(String[] args) throws IOException, HMMException
	{
		sop("START");
		
		File alignmentFile = new File("data/aligned.fa");
		try
		{
			ProfileHMM phmm = new ProfileHMM(alignmentFile);
		}
		catch (Exception x)
		{
			sop("STRESS!\n" + x.getMessage() + "\n" + x.getStackTrace());
		}
		
		sop("DONE");
	}
}
