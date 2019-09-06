package lu.uni.svv.StressTesting.datatype;


public class Task {
	public int		ID;				// Task Identification (Foreign key refers ID of TaskDescriptor)
	public int		ExecutionID;	// Task's execution ID
	public long		ExecutionTime;	// Worst-Case Execution Time
	public long		ArrivedTime;	// time at which a task arrived in the ready queue
	public long		StartedTime;	// time at which a task starts its execution
	public long		FinishedTime;	// time at which a task finishes its execution
	public long		RemainTime;		// remain time to execute
	public long		Deadline;		// ArrivedTime + Deadline == deadline for this task
	public long		Priority;		// Fixed Priority
	
	public Task(int _id, int _exID, long _execTime, long _arrivalTime, long _deadline, long _priority) {
		ID				= _id;
		ExecutionID     = _exID;
		ExecutionTime	= _execTime;		
		ArrivedTime 	= _arrivalTime;
		StartedTime		= 0;	
		FinishedTime	= 0;	
		RemainTime		= _execTime;
		Deadline 		= _deadline;
		Priority		= _priority;
	}
	
	public String str(){
		return String.format("{ID:%d (%d), exID:%d, arrival:%d, started:%d, ended:%d, remain:%d}", ID, Priority, ExecutionID, ArrivedTime, StartedTime, FinishedTime, RemainTime);
	}
}
