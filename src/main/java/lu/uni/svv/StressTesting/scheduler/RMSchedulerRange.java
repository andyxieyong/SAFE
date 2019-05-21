package lu.uni.svv.StressTesting.scheduler;

import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.utils.Settings;

import java.util.ArrayList;
import java.util.Arrays;
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
	
	public RMSchedulerRange(TestingProblem _problem) {
		super(_problem);
		
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
		
		if (!(Settings.TASK_FITNESS == 0 || _T.ID == Settings.TASK_FITNESS)) return 0.0;
		
		int best = maximumMissed[_T.ID-1];
		if (best==_missed) {
			bestExecutions.add(_T);
			bestExecutions.removeIf(x->( (int)(x.FinishedTime - (x.ArrivedTime+x.Deadline)) < best ));
		}
		
		// P = (best - X ) / |best| ==> X = best - (best * P)
		int limit = best - (int)(Settings.FITNESS_RANGE * Math.abs(best));
		if (_missed >= limit)
			selectedMisses[_T.ID-1].add(_missed);
		selectedMisses[_T.ID-1].removeIf(n->( (int)n<limit ));
		
//		if (best<0)	{
//			// P = (best - X ) / |best| ==> X = best - (best * P)
//			int limit = best - (int)(Settings.FITNESS_RANGE * Math.abs(best));
//
//			((ArrayList<Integer>)selectedMisses[_T.ID-1]).removeIf(n -> (n < limit));
//			if (_missed >= limit) ((ArrayList<Integer>)selectedMisses[_T.ID-1]).add(_missed);
//		}
//		else{
//			;
//		}
		return 0;
	}
	
	@Override
	public double getEvaluatedValue() {
		
		double fitness = 0.0;
		for (int id=0; id<selectedMisses.length; id++){
			if (!(Settings.TASK_FITNESS == 0 || (id+1) == Settings.TASK_FITNESS)) continue;
			
			for (int x=0; x<selectedMisses[id].size(); x++) {
				int value = (Integer)selectedMisses[id].get(x);
				double norm = value / (problem.QUANTA_LENGTH*0.1);
				fitness = fitness + (Math.exp(norm) / (Math.exp(norm)+1));
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