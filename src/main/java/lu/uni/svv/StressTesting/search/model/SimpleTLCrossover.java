package lu.uni.svv.StressTesting.search.model;

import java.util.List;
import java.util.ArrayList;

import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.util.JMetalException;

import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.utils.RandomGenerator;


@SuppressWarnings("serial")
public class SimpleTLCrossover implements CrossoverOperator<TimeListSolution>
{
	private double crossoverProbability;
	private RandomGenerator randomGenerator;

	/** Constructor */
	public SimpleTLCrossover(double crossoverProbability) {
		if (crossoverProbability < 0) {
			throw new JMetalException("Crossover probability is negative: " + crossoverProbability) ;
		}
		RandomGenerator rand = new RandomGenerator();
		rand.nextInt();
		rand.nextDouble();
		
		this.crossoverProbability = crossoverProbability;
		this.randomGenerator = new RandomGenerator();
	}
	
	/* Getter and Setter */
	public double getCrossoverProbability() {
		return crossoverProbability;
	}
	public void setCrossoverProbability(double crossoverProbability) {
		this.crossoverProbability = crossoverProbability;
	}
	@Override
	public int getNumberOfRequiredParents() {
		return 2;
	}
	@Override
	public int getNumberOfGeneratedChildren() {
		return 2;
	}

	
	/* Executing */
	@Override
	public List<TimeListSolution> execute(List<TimeListSolution> solutions)
	{
		if (solutions == null) {
			throw new JMetalException("Null parameter") ;
		} else if (solutions.size() != 2) {
			throw new JMetalException("There must be two parents instead of " + solutions.size()) ;
		}
		
		return doCrossover(crossoverProbability, solutions.get(0), solutions.get(1)) ;
	}


	/** doCrossover method */
	public List<TimeListSolution> doCrossover(double probability, TimeListSolution parent1, TimeListSolution parent2) 
	{
		List<TimeListSolution> offspring = new ArrayList<TimeListSolution>(2);

		offspring.add((TimeListSolution) parent1.copy()) ;
		offspring.add((TimeListSolution) parent2.copy()) ;

		if (randomGenerator.nextDouble() < probability) {
			  // 1. Get the total number of bits
			  int totalNumberOfVariables = parent1.getNumberOfVariables();
			  
			  // 2. Get crossover point
			  int crossoverPoint = randomGenerator.nextInt(0, totalNumberOfVariables - 1);
			  
			  // 3. Exchange values
			  TimeList[] offspring1, offspring2;
			  offspring1 = (TimeList[]) parent1.getVariables();
			  offspring2 = (TimeList[]) parent2.getVariables();
			  			  
			  for (int x = crossoverPoint; x < totalNumberOfVariables; x++) {
				  offspring.get(0).setVariableValue(x, (TimeList)offspring2[x].clone());
				  offspring.get(1).setVariableValue(x, (TimeList)offspring1[x].clone());
			  }
		}
		
		return offspring;
	}

}
