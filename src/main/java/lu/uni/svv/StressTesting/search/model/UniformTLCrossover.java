package lu.uni.svv.StressTesting.search.model;

import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.utils.RandomGenerator;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.util.JMetalException;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("serial")
public class UniformTLCrossover implements CrossoverOperator<TimeListSolution>
{
	private double crossoverProbability;
	private double crossoverThrehold;
	private RandomGenerator randomGenerator;
	private TestingProblem problem=null;
	private List<Integer> PossibleTasks = null;

	/** Constructor */
	public UniformTLCrossover(TestingProblem _problem, double crossoverProbability, Double crossoverThrehold) {
		if (crossoverProbability < 0) {
			throw new JMetalException("Crossover probability is negative: " + crossoverProbability) ;
		}
		problem = _problem;
		RandomGenerator rand = new RandomGenerator();
		rand.nextInt();
		rand.nextDouble();
		
		this.crossoverProbability = crossoverProbability;
		this.randomGenerator = new RandomGenerator();
		
		if (crossoverThrehold==null){
			this.crossoverThrehold = -1;
		}
		
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
			// 1. Get crossover points
			double[] crossoverPoints = new double[PossibleTasks.size()];
			for(int x=0; x<PossibleTasks.size(); x++){
				crossoverPoints[x] = randomGenerator.nextFloat();
			}
			
			double threshold = this.crossoverThrehold;
			if (this.crossoverThrehold<0) {
				threshold = 1 / (double) PossibleTasks.size();
			}

			// 2. create offspring objects
			TimeList[] offspring1, offspring2;
			offspring1 = (TimeList[]) parent1.getVariables();
			offspring2 = (TimeList[]) parent2.getVariables();
			
			// 3. Exchange variables
			for (int x=0; x<PossibleTasks.size(); x++){
				if(crossoverPoints[x] > threshold) continue;
				int pID = PossibleTasks.get(x);
				offspring.get(0).setVariableValue(pID-1, (TimeList)offspring2[pID-1].clone());
				offspring.get(1).setVariableValue(pID-1, (TimeList)offspring1[pID-1].clone());
			}
		}
		
		return offspring;
	}

}
