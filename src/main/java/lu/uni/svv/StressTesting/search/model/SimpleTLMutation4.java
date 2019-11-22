package lu.uni.svv.StressTesting.search.model;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.util.JMetalException;

import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.datatype.TaskType;
import lu.uni.svv.StressTesting.utils.RandomGenerator;

@SuppressWarnings("serial")
public class SimpleTLMutation4 implements MutationOperator<TimeListSolution>
{
	//This class changes only Aperiodic or Sporadic tasks that can be changeable
	private double mutationProbability;
	List<Integer> PossibleTasks = null;
	RandomGenerator randomGenerator = null;

	public long newValue = 0;
	public int taskID = 0;
	public long position = 0;
	
	/**  Constructor */
	public SimpleTLMutation4(TestingProblem problem, double probability) throws JMetalException {

		if (probability < 0) {
			throw new JMetalException("Mutation probability is negative: " + probability) ;
		}
		if (probability > 1) {
			throw new JMetalException("Mutation probability is over 1.0: " + probability) ;
		}

		this.mutationProbability = probability ;
		this.randomGenerator = new RandomGenerator() ;
		
		PossibleTasks = problem.getVaryingTasks();
	}
	  

	/* Getters and Setters */
	public double getMutationProbability() {
		return mutationProbability;
	}
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
		
		
		for (int t:PossibleTasks)
		{
			TimeList variable = solution.getVariableValue(t);
			for (int a=0; a<variable.size(); a++) {
				if (randomGenerator.nextDouble() >= mutationProbability) continue;
				doMutation(solution, t, a);
			}
		}

		return solution;
	}
	
	/** Implements the mutation operation */
	private void doMutation(TimeListSolution solution, int _tIDX, int _position)
	{
		TimeList variable = solution.getVariableValue(_tIDX);
		
		// execute mutation
		long curValue = variable.get(_position);
		long lastValue = 0;
		if ( _position>=1 ) lastValue = variable.get(_position-1);
		
		TaskDescriptor T = solution.problem.Tasks[_tIDX];
		
		// make a new value
		// only non-periodic tasks changes because we filtered in early stage
		long newValue = lastValue + randomGenerator.nextLong(T.MinIA, T.MaxIA);;

		// propagate changed values
		long delta = newValue - curValue;
		curValue += delta;
		variable.set(_position, curValue);
		
		// modify nextValues following range constraint			
		for (int x=_position+1; x< variable.size(); x++) {
			long nextValue = variable.get(x);
			if ( (nextValue >= T.MinIA + curValue) && (nextValue <= T.MaxIA + curValue) )	break;
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
		
		// Maximum Constraint
		lastValue = variable.get(variable.size()-1);
		if (lastValue + T.MaxIA < solution.problem.QUANTA_LENGTH)
		{
			variable.add(lastValue + randomGenerator.nextLong(T.MinIA, T.MaxIA));
		}
		
		return;
	}
}