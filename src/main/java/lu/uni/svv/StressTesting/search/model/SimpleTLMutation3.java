package lu.uni.svv.StressTesting.search.model;

import lu.uni.svv.StressTesting.datatype.TaskType;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.util.JMetalException;

import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.utils.RandomGenerator;

@SuppressWarnings("serial")
public class SimpleTLMutation3 implements MutationOperator<TimeListSolution>
{
	//This class changes only Aperiodic or Sporadic tasks that can be changeable
	private double mutationProbability;
	private double mutationImpact;
	private RandomGenerator randomGenerator;

	public long newValue = 0;
	public int taskID = 0;
	public long position = 0;

	/**  Constructor */
	public SimpleTLMutation3(double probability, double impact) throws JMetalException{
		
		if (probability < 0) {
			throw new JMetalException("Mutation probability is negative: " + probability) ;
		}
		if (probability > 1) {
			throw new JMetalException("Mutation probability is over 1.0: " + probability) ;
		}

		this.mutationProbability = probability ;
		this.randomGenerator = new RandomGenerator() ;
		this.mutationImpact = impact;
	}
	  

	/* Getters and Setters */
	public double getMutationProbability() {
		return mutationProbability;
	}
	public void setMutationProbability(double mutationProbability) {
		this.mutationProbability = mutationProbability;
	}
	public double getMutationImpact() {
		return mutationImpact;
	}
	public void setMutationImpact(double mutationImpact) {
		this.mutationImpact = mutationImpact;
	}


	/** Execute() method */
	@Override
	public TimeListSolution execute(TimeListSolution solution) throws JMetalException {
		if (null == solution) {
			throw new JMetalException("Executed SimpleTLMutation with Null parameter");
		}
		
		this.newValue = -1;
		this.position = -1;
		this.taskID = -1;
		
		if (randomGenerator.nextDouble() < mutationProbability)
			doMutation(solution);

		return solution;
	}
	
	

	/** Implements the mutation operation */
	private void doMutation(TimeListSolution solution) {
		
		//JMetalRandom random = JMetalRandom.getInstance();   // range random picker
		RandomGenerator random = new RandomGenerator();
		
		// calculate sum of the number of arrival times
		long range = 0;
		for(TimeList gene:solution.getVariables()) {
			range += gene.size();
		}
		
		// decide how many positions to mutate
		long changes = (long)(range * this.mutationImpact);
		for(int c=0; c<changes; c++) {
			// pick a position to mutate 
			int taskIdx = random.nextInt(0, solution.getNumberOfVariables()-1);
			TimeList variable = solution.getVariableValue(taskIdx);
			int position = random.nextInt(0, variable.size()-1);
			
			// execute mutation
			long curValue = variable.get(position);
			long minIA = solution.problem.Tasks[taskIdx].MinIA;
			long maxIA = solution.problem.Tasks[taskIdx].MaxIA;
			if (solution.problem.Tasks[taskIdx].Type==TaskType.Periodic) {
				minIA = solution.problem.Tasks[taskIdx].Period;
				maxIA = solution.problem.Tasks[taskIdx].Period;
			}
			
			// make a new value // if the position is 0, pick a new value between 0 to MAX_PHASETIME
			long newValue = 0;
			if (position == 0)
				newValue = random.nextLong(0, solution.problem.MAX_PHASETIME);
			else {
				long lastValue = variable.get(position-1);
				newValue = random.nextLong(lastValue+minIA, lastValue+maxIA);
			}
				
			long delta = newValue - curValue;
			curValue += delta;
			variable.set(position, curValue);
			
			// modify nextValues following range constraint			
			for (int x=position+1; x< variable.size(); x++) {
				long nextValue = variable.get(x);
				if ( (nextValue >= minIA + curValue) && (nextValue <= maxIA + curValue) )	break;
				variable.set(x, nextValue+delta);
				curValue = nextValue+delta;
			}
			
			// Maximum Constraint
			// if the current value is over Maximum time quanta, remove the value
			// otherwise, save the result at the same place
			for (int x=variable.size()-1; x>=0; x--) {
				if (variable.get(x) <= solution.problem.QUANTA_LENGTH) 
					break; 
				variable.remove(x);
			}
			
			this.newValue = newValue;
			this.position = position;
			this.taskID = taskIdx;
		}
	} // doMutation
	
	
	
}