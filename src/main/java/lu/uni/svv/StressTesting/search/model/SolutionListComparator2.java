package lu.uni.svv.StressTesting.search.model;

import lu.uni.svv.StressTesting.datatype.FitnessList;
import lu.uni.svv.StressTesting.utils.Settings;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.comparator.ObjectiveComparator.Ordering;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

/**
 * This class implements a comparator based on a given objective
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class SolutionListComparator2<S extends Solution<?>> implements Comparator<S>, Serializable {
	private int objectiveId;
	
	private Ordering order;
	
	/**
	 * Constructor.
	 *
	 * @param objectiveId The index of the objective to compare
	 */
	public SolutionListComparator2(int objectiveId) {
		this.objectiveId = objectiveId;
		order = Ordering.ASCENDING;
	}
	
	/**
	 * Comparator.
	 * @param objectiveId The index of the objective to compare
	 * @param order Ascending or descending order
	 */
	public SolutionListComparator2(int objectiveId, Ordering order) {
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
			
			boolean diff = UtestSignificance(objective1, objective2);
			if (diff==true) {
				double value = 0.0;
				if (order == Ordering.ASCENDING) {
					value = A12(objective1, objective2);
				} else {
					value = A12(objective2, objective1);
				}
				if (value > Settings.A12_THRESHOLD) result = 1;
				else if (value < 0.5) result = -1;
				else result = 0;
			}
			else
				result = 0;
		}
		return result ;
	}
	
	public boolean UtestSignificance(FitnessList list1, FitnessList list2){
		// This function evaluates two-sided hypothesis
		// H0: The two populations are equal versus
		// H1: The two populations are not equal
		// if p-value < 0.05(a) : H0 is not supported == H1 is true
		// if this function's result is true, two populations are not equal.
		MannWhitneyUTest test = new MannWhitneyUTest();
		double[] double_list1 = new double[list1.size()];
		double[] double_list2 = new double[list2.size()];
		
		for (int x=0; x<list1.size(); x++)
			double_list1[x] = list1.get(x).doubleValue();
		for (int x=0; x<list1.size(); x++)
			double_list2[x] = list2.get(x).doubleValue();
		
		double pvalue = test.mannWhitneyUTest(double_list1, double_list2);
		
		if (pvalue < 0.05)
			return true;
		return false;
	}
	
	public double A12(FitnessList list1, FitnessList list2){
		double m = list1.size();
		double n = list2.size();
		double sum = rankSum1(list1, list2);
		
		return ((sum/m) - ((m+1)/2.0))/n;
	}
	
	public double rankSum1(FitnessList list1, FitnessList list2){
		List<BigDecimal> union = new ArrayList<BigDecimal>();
		List<BigDecimal> distinct = new ArrayList<BigDecimal>();
		Map<BigDecimal, Integer> map = new HashMap<BigDecimal, Integer>();
		
		union.addAll(list1);
		union.addAll(list2);
		Collections.sort(union);
		
		// create distinct list
		int idx = 0;
		for(int x=0; x<union.size(); x++) {
			if (distinct.size()>0 && union.get(x).compareTo(distinct.get(distinct.size() - 1)) == 0) continue;
			distinct.add(union.get(x));
			map.put(union.get(x), idx++);
		}
		
		// Count each items;
		int[] counts = new int[distinct.size()];
		BigDecimal item=null;
		for (int x=0; x<union.size(); x++){
			item = union.get(x);
			idx = map.get(item);
			counts[idx] += 1;
		}
		
		// accumulate
		for(int x=0; x<counts.length-1; x++){
			counts[x+1] += counts[x];
		}
		
		// calculate ranks
		int prev = 1;
		int current = 1;
		double[] ranks = new double[distinct.size()];
		for(int x=0; x<counts.length; x++){
			current = counts[x];
			ranks[x] = (prev + current)/2.0;
			prev = current+1;
		}
		
		// Calculate rankSum
		double sum = 0.0;
		for(int x=0; x<list1.size(); x++)
		{
			idx = map.get(list1.get(x));
			sum += ranks[idx];
		}
		
		return sum;
	}
	
}
