package lu.uni.svv.StressTesting.scheduler;

import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.utils.Settings;

import java.util.Arrays;


/**
 * Scheduling Policy
 * 	- We use Fixed Priority to schedule tasks.
 * 	- The two task has same priority, first arrived task in the ready queue has more high priority.
 * 
 * Deadline misses detection
 * 	- We count the number of deadline misses while the scheduling(by MAX_TIMEQUANTA)
 * 	- And, if the ready queue has tasks after being done scheduling(after MAX_TIMEQUANTA),
 * 			we will execute this scheduler by finishing all tasks in the ready queue.
 * @author jaekwon.lee
 *
 */
public class RMSchedulerMin extends RMScheduler{
	
	protected int[]     minimumMissed;
	
	public RMSchedulerMin(TestingProblem _problem, int _taskFitness) {
		super(_problem, _taskFitness);
		
		minimumMissed = new int[_problem.Tasks.length];
		Arrays.fill(minimumMissed, (int)_problem.QUANTA_LENGTH);
	}
	
	@Override
	protected double evaluateDeadlineMiss(Task _T, int _missed) {
		//save minimum of missed value
		if (Math.abs(_missed) < minimumMissed[_T.ID-1]) minimumMissed[_T.ID-1] = Math.abs(_missed);
		
		if (!(this.taskFitness == 0 || _T.ID == this.taskFitness)) return 0.0;
		
		return _missed;
	}
	
	/*
	 * Result getters
	 * This functions can give a result after 'run()' function executed
	 */
	@Override
	public double getEvaluatedValue() {
		double result = 0.0;
		double a = 2;
		for (int x=0; x<minimumMissed.length; x ++) {
			int ID = x + 1;
			if (this.taskFitness == 0 || ID == this.taskFitness) {
				result = result + Math.pow(a, Math.abs(minimumMissed[x]));
			}
		}
		return result;
	}

	@Override
	public String getByproduct() {
		StringBuilder sb = new StringBuilder();
		if (minimumMissed.length>=1) {
			int x=0;
			for (; x < minimumMissed.length - 1; x++) {
				sb.append(minimumMissed[x]);
				sb.append(',');
			}
			sb.append(minimumMissed[x]);
		}
		return sb.toString();
	}
}