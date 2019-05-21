package lu.uni.svv.StressTesting.scheduler;

import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.search.model.TestingProblem;


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
public class RMSchedulerEx extends RMScheduler{
		
	public RMSchedulerEx(TestingProblem _problem) {
		super(_problem);
	}
	
	@Override
	protected double evaluateDeadlineMiss(Task _T, int _missed){
		return Math.pow(2, _missed);
	}
}