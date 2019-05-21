package lu.uni.svv.StressTesting.scheduler;

import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.utils.Settings;


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
public class RMSchedulerNorm extends RMScheduler{
	
	public RMSchedulerNorm(TestingProblem _problem) {
		super(_problem);
	}
	
	@Override
	protected double evaluateDeadlineMiss(Task _T, int _missed) {
		if (!(Settings.TASK_FITNESS == 0 || _T.ID == Settings.TASK_FITNESS)) return 0.0;
		
		double normed;
		
		// if missed is positive:    2^( (e-d) )
		// else                 :    2^( -(abs(e-d)/(abs(e-d)+1)) ) = 0.5^( abs(e-d)/(abs(e-d)+1) )
		if (_missed > 0) {
			normed = _missed;
		} else {
			normed = -1 * (Math.abs(_missed) / (Math.abs(_missed) + 1.0));// pow function is not allowed negative values.
		}
		
		return Math.pow(2, normed);
	}
	
	public String getExecutedTasksString() {
		return "";
	}
}