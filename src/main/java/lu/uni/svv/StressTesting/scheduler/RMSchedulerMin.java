package lu.uni.svv.StressTesting.scheduler;

import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.utils.Settings;

import java.math.BigDecimal;
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
	
	public RMSchedulerMin(TestingProblem _problem) {
		super(_problem);
		
		minimumMissed = new int[_problem.Tasks.length];
		Arrays.fill(minimumMissed, (int)_problem.QUANTA_LENGTH);
	}
	
	@Override
	protected double evaluateDeadlineMiss(Task _T, int _missed) {
		//save minimum of missed value
		if (Math.abs(_missed) < minimumMissed[_T.ID-1]) minimumMissed[_T.ID-1] = Math.abs(_missed);
		
		if (!(Settings.TASK_FITNESS == 0 || _T.ID == Settings.TASK_FITNESS)) return 0.0;
		
		return _missed;
	}
	
	/*
	 * Result getters
	 * This functions can give a result after 'run()' function executed
	 */
	@Override
	public BigDecimal getEvaluatedValue() {
		BigDecimal result = new BigDecimal("0.0");
		BigDecimal a = new BigDecimal("2");
		for (int x=0; x<minimumMissed.length; x ++) {
			int ID = x + 1;
			if (Settings.TASK_FITNESS == 0 || ID == Settings.TASK_FITNESS) {
				result = result.add(a.pow(minimumMissed[x]));
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