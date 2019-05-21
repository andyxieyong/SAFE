package lu.uni.svv.StressTesting.search.model;

import lu.uni.svv.StressTesting.datatype.FitnessList;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.comparator.ObjectiveComparator.Ordering;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * This class implements a comparator based on a given objective
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class SolutionListComparatorAvg<S extends Solution<?>> implements Comparator<S>, Serializable {
	private int objectiveId;
	
	private Ordering order;
	
	/**
	 * Constructor.
	 *
	 * @param objectiveId The index of the objective to compare
	 */
	public SolutionListComparatorAvg(int objectiveId) {
		this.objectiveId = objectiveId;
		order = Ordering.ASCENDING;
	}
	
	/**
	 * Comparator.
	 * @param objectiveId The index of the objective to compare
	 * @param order Ascending or descending order
	 */
	public SolutionListComparatorAvg(int objectiveId, Ordering order) {
		this.objectiveId = objectiveId;
		this.order = order ;
	}
	
	/**
	 * Compares two solutions according to a given objective.
	 *
	 * @param solution1 The first solution
	 * @param solution2 The second solution
	 * @return -1, or 0, or 1 if solution1 is less than, equal, or greater than solution2,
	 * respectively, according to the established order
	 */
	@Override
	public int compare(S solution1, S solution2) {
		int result;
		if (solution1 == null) {
			if (solution2 == null) {
				result = 0;
			} else {
				result = 1;
			}
		} else if (solution2 == null) {
			result =  -1;
		} else if (solution1.getNumberOfObjectives() <= objectiveId) {
			throw new JMetalException("The solution1 has " + solution1.getNumberOfObjectives()+ " objectives "
					+ "and the objective to sort is " + objectiveId) ;
		} else if (solution2.getNumberOfObjectives() <= objectiveId) {
			throw new JMetalException("The solution2 has " + solution2.getNumberOfObjectives()+ " objectives "
					+ "and the objective to sort is " + objectiveId) ;
		} else {
			FitnessList objective1 = ((TimeListSolution)solution1).getObjectiveDecimalList(this.objectiveId);
			FitnessList objective2 = ((TimeListSolution)solution2).getObjectiveDecimalList(this.objectiveId);

			BigDecimal avgSolution1 = AverageList(objective1);
			BigDecimal avgSolution2 = AverageList(objective2);
			if (order == Ordering.ASCENDING) {
				result = avgSolution1.compareTo(avgSolution2);
			} else {
				result = avgSolution2.compareTo(avgSolution1);
			}
			
		}
		return result ;
	}
	
	public BigDecimal AverageList(FitnessList list){
		BigDecimal avg = new BigDecimal("0.0");
		for (int x=0; x<list.size(); x++){
			avg = avg.add(list.get(x));
		}
		avg = avg.divide(new BigDecimal(list.size()));
		return avg;
	}
}
