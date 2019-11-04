package lu.uni.svv.StressTesting.scheduler;

import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.utils.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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
public class RMSchedulerRange extends RMScheduler{
	
	public ArrayList[] selectedMisses;
	public int[] maximumMissed;
	public List<Task> bestExecutions;
	
	public RMSchedulerRange(TestingProblem _problem, int[] _targetTasks) {
		super(_problem, _targetTasks);
		
		initialize();
	}
	
	public void initialize(){
		super.initialize();
		
		selectedMisses = new ArrayList[problem.Tasks.length];
		for(int x=0; x<problem.Tasks.length; x++)
			selectedMisses[x] = new ArrayList<Integer>();
		
		maximumMissed = new int[problem.Tasks.length];
		Arrays.fill(maximumMissed, (int)problem.QUANTA_LENGTH * -1);
		
		bestExecutions = new ArrayList<Task>();
	}
	
	
	/**
	 *
	 * @param _T
	 * @param _missed
	 * @return
	 */
	@Override
	protected double evaluateDeadlineMiss(Task _T, int _missed) {
		if (_missed > maximumMissed[_T.ID-1]) maximumMissed[_T.ID-1] = _missed;
		
		return 0;
	}
	
	@Override
	public double getEvaluatedValue() {
		double fitness = 0.0;
		
		if (this.targetTasks.length==0){
			for (int id=0; id<maximumMissed.length; id++){
				fitness += maximumMissed[id];
			}
		}
		else{
			for (int id: this.targetTasks){
				// ID in the targetTasks are started number 1
				fitness += maximumMissed[id-1];
			}
		}
		
		return fitness;
	}
	
	@Override
	public String getByproduct() {
		/**
		 * Return best(e-d) of the solution
		 */
		StringBuilder sb = new StringBuilder();
		if (maximumMissed.length>=1) {
			int x=0;
			for (; x < maximumMissed.length - 1; x++) {
				sb.append(maximumMissed[x]);
				sb.append(',');
			}
			sb.append(maximumMissed[x]);
		}
		return sb.toString();
	}
	
	/**
	 * get Deadline Missed items
	 * @return
	 */
	@Override
	public String getExecutedTasksString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TaskID,ExecutionID,Arrival,Started,Finished,Deadline,Misses(finish-deadline)\n");
		
		for (int tid=1; tid<=problem.Tasks.length; tid++)
		{
			for (Task item:bestExecutions)
			{
				if (tid!=item.ID) continue;
				
				long deadline_tq = (item.ArrivedTime + item.Deadline);
				int missed = (int)(item.FinishedTime - deadline_tq);
				
				sb.append(String.format("%d,%d,%d,%d,%d,%d,%d\n",
						item.ID, item.ExecutionID, item.ArrivedTime, item.StartedTime, item.FinishedTime, deadline_tq, missed));
			}
		}
		return sb.toString();
	}
}