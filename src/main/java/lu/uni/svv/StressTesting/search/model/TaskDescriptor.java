package lu.uni.svv.StressTesting.search.model;

import com.sun.scenario.effect.Offset;
import lu.uni.svv.StressTesting.datatype.TaskSeverity;
import lu.uni.svv.StressTesting.datatype.TaskType;
import java.util.Comparator;


public class TaskDescriptor implements Comparable<TaskDescriptor>{
	
	
	public static int UNIQUE_ID = 1;
	public int		ID;			// The sequence of input data (main key for ordering)
	public String	Name;		// Task Name
	public TaskType Type;		// Task type {Periodic, Aperiodic, Sporadic}
	public long		MinWCET;	// Minimum WCET
	public long		MaxWCET;	// Maximum WCET
	public long		Period;		// Time period which a task occurs, This variable is for the Periodic task
	public long 	MinIA;		// Minimum inter-arrival time,This variable is for Aperiodic or Sporadic Task
	public long		MaxIA;		// Maximum inter-arrival time, This variable is for Aperiodic or Sporadic Task
	public long		Deadline;	// Time period which a task should be finished
	public long		Priority;	// Fixed Priority read from input data
	public long		Offset;	    // Fixed Priority read from input data
	public TaskSeverity	Severity;	// {Hard, Soft}
	
	
	public TaskDescriptor() {
		ID = TaskDescriptor.UNIQUE_ID++;
		Name 		= "";
		Type 		= TaskType.Periodic;
		MinWCET 	= 0;
		MaxWCET 	= 0;
		Period  	= 0;
		MinIA		= 0;
		MaxIA  		= 0;
		Deadline 	= 0;
		Priority 	= 0;
		Offset      = 0;
		Severity    = TaskSeverity.HARD;
	}
	
	public TaskDescriptor(String _name, TaskType _type, long _minWCET, long _maxWCET, long _period, long _minIA, long _maxIA, long _deadline, TaskSeverity _severity) {
		ID 		    = TaskDescriptor.UNIQUE_ID++;
		Name 		= _name;
		Type 		= _type;
		MinWCET 	= _minWCET;
		MaxWCET 	= _maxWCET;
		Period  	= _period;
		MinIA		= _minIA;
		MaxIA  		= _maxIA;
		Deadline 	= _deadline;
		Priority 	= 0;
		Severity    = _severity;
	}

	@Override
	public int compareTo(TaskDescriptor _o) {
		if ((this.Period - _o.Period) > 0)
			return 1;
		else 
			return -1;
	}
	
	public static Comparator<TaskDescriptor> PriorityComparator = new Comparator<TaskDescriptor>() {

		@Override
		public int compare(TaskDescriptor _o1, TaskDescriptor _o2) {
			if ((_o1.Priority - _o2.Priority) > 0)
				return 1;
			else 
				return -1;
		}
		
	};
	
	public static Comparator<TaskDescriptor> PeriodComparator = new Comparator<TaskDescriptor>() {

		@Override
		public int compare(TaskDescriptor _o1, TaskDescriptor _o2) {
			if ((_o1.Period - _o2.Period) > 0)
				return 1;
			else 
				return -1;
		}
		
	};
	

	public static Comparator<TaskDescriptor> OrderComparator = new Comparator<TaskDescriptor>() {
		@Override
		public int compare(TaskDescriptor _o1, TaskDescriptor _o2) {
			if ((_o1.ID - _o2.ID) > 0)
				return 1;
			else 
				return -1;
		}
		
	};
}