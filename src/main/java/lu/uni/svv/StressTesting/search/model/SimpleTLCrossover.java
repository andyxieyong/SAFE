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
	private TestingProblem problem=null;
	private List<Integer> PossibleTasks = null;

	/** Constructor */
	public SimpleTLCrossover(TestingProblem _problem, double crossoverProbability) {
		if (crossoverProbability < 0) {
			throw new JMetalException("Crossover probability is negative: " + crossoverProbability) ;
		}
		problem = _problem;
		RandomGenerator rand = new RandomGenerator();
		rand.nextInt();
		rand.nextDouble();
		
		this.crossoverProbability = crossoverProbability;
		this.randomGenerator = new RandomGenerator();
		
		PossibleTasks = problem.getVaryingTasks();
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
			//System.out.println("[Debug] Executed crossover");
			
			// 1. Get the total number of bits
			int totalNumberOfVariables = parent1.getNumberOfVariables();
			  
			// 2. Get crossover point
			int crossoverPoint = randomGenerator.nextInt(1, PossibleTasks.size() - 1);
			crossoverPoint = PossibleTasks.get(crossoverPoint);
			
			//System.out.println(String.format("Crossover Point: Task %d", crossoverPoint));
			
			// 3. Exchange values
			TimeList[] offspring1, offspring2;
			offspring1 = (TimeList[]) parent1.getVariables();
			offspring2 = (TimeList[]) parent2.getVariables();
			  			  
			for (int x = crossoverPoint-1; x < totalNumberOfVariables; x++) {
				offspring.get(0).setVariableValue(x, (TimeList)offspring2[x].clone());
				offspring.get(1).setVariableValue(x, (TimeList)offspring1[x].clone());
			}
		}
		
		return offspring;
	}

}
