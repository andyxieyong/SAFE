package lu.uni.svv.StressTesting.search.model;

import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.util.JMetalException;

import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.datatype.TaskType;
import lu.uni.svv.StressTesting.utils.RandomGenerator;

@SuppressWarnings("serial")
public class SimpleTLMutation implements MutationOperator<TimeListSolution>
{
	private double mutationProbability;
	private RandomGenerator randomGenerator;

	public long newValue = 0;
	public int taskID = 0;
	public long position = 0;

	/**  Constructor */
	public SimpleTLMutation(double probability) throws JMetalException{
		
		if (probability < 0) {
			throw new JMetalException("Mutation probability is negative: " + probability) ;
		}
		if (probability > 1) {
			throw new JMetalException("Mutation probability is over 1.0: " + probability) ;
		}

		this.mutationProbability = probability ;
		this.randomGenerator = new RandomGenerator() ;
	}
	  

	/* Getters */
	public double getMutationProbability() {
		return mutationProbability;
	}

	/* Setters */
	public void setMutationProbability(double mutationProbability) {
		this.mutationProbability = mutationProbability;
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

		// ---------------------------------------------------
		// pick a mutation position
		//JMetalRandom random = JMetalRandom.getInstance();   // range random picker
		RandomGenerator random = new RandomGenerator();
		
		// calculate sum of the number of arrival times
		long range = 0;
		for(TimeList gene:solution.getVariables()) {
			range += gene.size();
		}
		
		// find a position to mutate in the middle of genes
		long positionMutate = random.nextLong(0, range-1);
		TimeList variable = null;
		int taskID = 0;
		for (; taskID<solution.getNumberOfVariables(); taskID++)
		{
			variable = solution.getVariableValue(taskID);
			if (positionMutate-variable.size() < 0)	break;
			positionMutate -= variable.size();
		}
		int position = (int)positionMutate;

		// ---------------------------------------------------
		// execute mutation
		long curValue = variable.get(position);
		long minIA = solution.problem.Tasks[taskID].MinIA;
		long maxIA = solution.problem.Tasks[taskID].MaxIA;
		if (solution.problem.Tasks[taskID].Type==TaskType.Periodic) {
			minIA = solution.problem.Tasks[taskID].Period;
			maxIA = solution.problem.Tasks[taskID].Period;
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
		this.taskID = taskID;
	} // doMutation
	
	
	
}