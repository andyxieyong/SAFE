package lu.uni.svv.StressTesting.search.model;

import com.sun.scenario.effect.Offset;
import lu.uni.svv.StressTesting.datatype.TaskSeverity;
import lu.uni.svv.StressTesting.datatype.TaskType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


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
	
	
	/* **********************************************************
	 *  Load data from CSV file
	 */
	
	/**
	 * load from CSV file  (Time unit is TIME_QUANTA )
	 * @param _filepath
	 * @param _maximumTime
	 * @param _timeQuanta
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static TaskDescriptor[] loadFromCSV(String _filepath, double _maximumTime, double _timeQuanta) throws NumberFormatException, IOException {
		List<TaskDescriptor> listJobs = new ArrayList<>();
		
		File file = new File(_filepath);
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		long lineCnt=0;
		String data;
		while ((data = br.readLine()) != null) {
			lineCnt++;
			if (lineCnt==1) continue;   // Remove CSV file header
			if (data.trim().length()==0) continue;
			
			String[] cols = data.split(",");
			
			TaskDescriptor aJob = new TaskDescriptor();
			aJob.Name 		= cols[1].trim();	// Name
			aJob.Type 		= getTypeFromString(cols[2].trim());
			aJob.Priority 	= getValueFromString(cols[3].trim(), 10000);
			aJob.Offset     = getTimeFromString(cols[4].trim(), 0, _maximumTime, _timeQuanta);
			aJob.MinWCET 	= getTimeFromString(cols[5].trim(), 0, _maximumTime, _timeQuanta);
			aJob.MaxWCET 	= getTimeFromString(cols[6].trim(), 0, _maximumTime,_timeQuanta);
			aJob.Period 	= getTimeFromString(cols[7].trim(), _maximumTime, _maximumTime, _timeQuanta);
			if (aJob.Type == TaskType.Periodic){
				aJob.MinIA  = 0;
				aJob.MaxIA  = 0;
			}else{
				aJob.MinIA  = getTimeFromString(cols[8].trim(), 0, _maximumTime,_timeQuanta);
				aJob.MaxIA	= getTimeFromString(cols[9].trim(), _maximumTime, _maximumTime, _timeQuanta);
			}
			aJob.Deadline 	= getTimeFromString(cols[10].trim(), _maximumTime, _maximumTime, _timeQuanta);
			aJob.Severity 	= getSeverityFromString(cols[11].trim());	// Severity type
			
			listJobs.add(aJob);
		}
		// after loop, close reader
		br.close();
		
		// Return loaded data
		TaskDescriptor[] tasks = new TaskDescriptor[listJobs.size()];
		listJobs.toArray(tasks);
		return tasks;
	}
	
	/**
	 * converting to string to store an array of task descriptors
	 * @param _tasks
	 * @param _timeQuanta
	 * @return
	 */
	public static String toString(TaskDescriptor[] _tasks, double _timeQuanta){
		StringBuilder sb = new StringBuilder();
		
		sb.append("Task ID, Task Name,Task Type,Task Priority,Offset,WCET min,WCET max,Task Period (ms),Minimum interarrival-time (ms),Maximum Interarrival time,Task Deadline, Deadline Type, Dependencies, Triggers\n");
		for(TaskDescriptor task:_tasks)
		{
			sb.append(String.format("%d,%s,%s,%d,%f,%f,%f,%f",
					task.ID,
					task.Name, task.Type.toString(), task.Priority, task.Offset* _timeQuanta,
					task.MinWCET * _timeQuanta, task.MaxWCET* _timeQuanta,
					task.Period * _timeQuanta));
			
			if (task.Type==TaskType.Periodic) {
				sb.append(",,");
			}
			else{
				sb.append(String.format(",%f,%f", task.MinIA* _timeQuanta, task.MaxIA* _timeQuanta));
			}
			sb.append(String.format(",%f,%s\n",
					task.Deadline* _timeQuanta, task.Severity));
		}
		return sb.toString();
	}
	
	/////////
	// sub functions for Loading from CSV
	/////////
	public static TaskType getTypeFromString(String _text) {
		
		if (_text.toLowerCase().compareTo("sporadic")==0)
			return TaskType.Sporadic;
		else if (_text.toLowerCase().compareTo("aperiodic")==0)
			return TaskType.Aperiodic;
		else
			return TaskType.Periodic;
	}
	
	public static TaskSeverity getSeverityFromString(String _text) {
		
		if (_text.toLowerCase().compareTo("soft")==0)
			return TaskSeverity.SOFT;
		else
			return TaskSeverity.HARD;
	}
	
	public static int getValueFromString(String _text, int _default) {
		
		if (_text.compareTo("")==0 || _text.compareTo("N/A")==0)
			return _default;
		else
			return (int)(Double.parseDouble(_text));
	}
	
	public static int getTimeFromString(String _text, double _default, double _max, double _timeQuanta) {
		double value = 0.0;
		if (_text.compareTo("")==0 || _text.compareTo("N/A")==0)
			value = _default;
		else {
			value = Double.parseDouble(_text);
			if (_max !=0 && value > _max) value =  _max;
		}
		return (int)(value * (1 / _timeQuanta));
	}
	
	public static int[] getListFromString(String _text) {
		String[] texts = _text.split(";");
		int[] items = new int[texts.length];
		int cnt=0;
		for ( int i=0; i< texts.length; i++){
			texts[i] = texts[i].trim();
			if(texts[i].length()==0) continue;
			items[i] =  Integer.parseInt(texts[i]);
			cnt++;
		}
		
		if (cnt==0){
			return new int[0];
		}
		return items;
	}
	
	
	public static String increaseWCET(TaskDescriptor[] tasks, String targetType, double increase) throws Exception {
		StringBuilder sb = new StringBuilder();
		
		TaskType type = TaskType.Periodic;
		if (targetType.compareTo(TaskType.Aperiodic.toString())==0)
			type = TaskType.Aperiodic;
		if (targetType.compareTo(TaskType.Sporadic.toString())==0)
			type = TaskType.Sporadic;
		
		long previous=0;
		sb.append("---------------------------< Used data set >------------------------------\n");
		if (increase == 1.0)
			sb.append("[         TaskName        ]  Task Type, MinWCET, Deadline\n");
		else
			sb.append("[         TaskName        ]  Task Type, MinWCET, Deadline, Changed MinWCET\n");
		for(TaskDescriptor task:tasks)
		{
			if (task.Type != type) {
				sb.append(String.format("[%25s] %10s, %7d, %8d\n", task.Name, task.Type.toString(), task.MinWCET, task.Deadline));
				continue;
			}
			previous = task.MinWCET;
			task.MinWCET = (int)Math.ceil((double)task.MinWCET*increase);
			task.MaxWCET = (int)Math.ceil((double)task.MaxWCET*increase);
			if (increase == 1.0)
				sb.append(String.format("[%25s] %10s, %7d, %8d\n", task.Name, task.Type.toString(), task.MinWCET, task.Deadline));
			else
				sb.append(String.format("[%25s] %10s, %7d, %8d, %15d\n", task.Name, task.Type.toString(), previous, task.Deadline, task.MinWCET));
			
			if (task.MinWCET > task.Deadline){
				sb.append(task.Name + ": Not proper increase\n");
				throw new Exception(sb.toString());
			}
		}
		return sb.toString();
	}
}