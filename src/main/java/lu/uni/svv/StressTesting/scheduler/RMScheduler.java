package lu.uni.svv.StressTesting.scheduler;

import java.io.PrintStream;
import java.util.*;

import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.search.model.TaskDescriptor;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.search.model.TaskDescriptor.TaskType;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.utils.RandomGenerator;
import lu.uni.svv.StressTesting.utils.Settings;


/**
 * Scheduling Policy
 * 	- We use Fixed Priority to schedule tasks.
 * 	- The two task has same priority, first arrived execution in the ready queue has higher priority to use CPU.
 *  -   In the other words, the execution that is arrived lately couldn't preempt the other executions already arrived in the ready queue.
 *
 * Deadline misses detection
 * 	- We count the number of deadline misses while the scheduling(by QUANTA_LENGTH)
 * 	- And, if the ready queue has tasks after being done scheduling(after QUANTA_LENGTH),
 * 			we will execute this scheduler by finishing all tasks in the ready queue.
 * @author jaekwon.lee
 *
 */
public class RMScheduler {
	
	protected TestingProblem problem;
	
	/* For Scheduling */
	private long        timeLapsed;		// current CPU time quanta
	private ReadyQueue  readyQueue;
	private Task        lastExecutedTask;
	private Task        previousTask;
	
	/* For evaluating */
	private List<Task>  missedDeadlines;
	private double  evaluatedValue;
	private long        CPUusages;
	
	
	/* For Debugging */
	public static boolean   DETAIL          = false;    // Show detail information (for debugging)
	public static boolean   PROOF           = false;    // Show proof of execution (for debugging)
	private long            LINEFEED        = 50;		// This value uses for showing CPU execution detail, This value gives line feed every LINEFEED time quanta
	private PrintStream     printer         = null;	    // print object to show progress during the scheduling.
	private List<int[]>     timelines       = null;     // Timelines for each task
	protected List<Task>    executedTasks   = null;     // Save all executions' details
	
	List<Task> sampledWCET = null;// for storing sampled WCET
	HashMap<Integer, Long> WCETSamples = null;// for storing sampled WCET
	
	
	public RMScheduler(TestingProblem _problem) {
		problem = _problem;
		
		initialize();
		
		printer = System.out;
	}
	
	public void initialize(){
		CPUusages = 0;
		missedDeadlines = new ArrayList<Task>();
		evaluatedValue = 0.0;
		
		timeLapsed = 0;
		lastExecutedTask = null;
		previousTask = null;
		readyQueue = new ReadyQueue();
		
		sampledWCET = new ArrayList<>();
	}
	
	public void setSamples(HashMap<Integer, Long> _samples){
		WCETSamples = _samples;
	}
	
	/**
	 * Execute the RateMonotonic Algorithm
	 */
	public void run(TimeListSolution _solution) {
		initilizeEvaluationTools();
				
		try {
			// get total number of jobs
			initializeRun(_solution.getNumberOfVariables());
			
			// Running Scheduler
			long time = 0;
			for (; time <= problem.QUANTA_LENGTH; time++)
			{
				// append a new task which is matched with this time
				appendNewTask(time, _solution.getVariables());
				
				//Execute Once!
				if(executeOneUnit() == -1)
					return;
			}
			
			//Check cycle complete or not  (It was before ExecuteOneUnit() originally)
			if (Settings.EXTEND_SCHEDULER && readyQueue.size() > 0) {
				if (RMScheduler.DETAIL == true)
				{
					printer.println("\nEnd of expected time quanta");
					printer.println("Here are extra execution because of ramaining tasks in queue");
					printer.println("------------------------------------------");
				}

				while(readyQueue.size() > 0) {
					int output = executeOneUnit();
					if (output == -1) return ;
				}
			}
		} catch (Exception e) {
			printer.println("Some error occurred. Program will now terminate: " + e);
			e.printStackTrace();
		}
	}
	
	
	
	int[] indexTable = null;
	private void initializeRun(int _nVariable){
		this.indexTable= new int[_nVariable];
		Arrays.fill(indexTable, 0);
	}
	
	private void appendNewTask(long _time, TimeList[] _variables){
		
		//compare arrivalTime and add a new execution into ReadyQueue for each task
		for (int taskIDX=0; taskIDX<_variables.length; taskIDX++)
		{
			// Check whether there is more executions
			if (_variables[taskIDX].size() <= indexTable[taskIDX]) continue;
			if (_time != _variables[taskIDX].get(indexTable[taskIDX])) continue;
			
			// Add Tasks
			TaskDescriptor task = problem.Tasks[taskIDX];
			long WCET = getSampleWCET(task);
			Task t = new Task(task.ID, indexTable[taskIDX], WCET, _time, task.Deadline, task.Priority);
			readyQueue.addNewTask(t);
			indexTable[taskIDX]++;
			if (Settings.N_SAMPLE_WCET!=0 && task.MinWCET != task.MaxWCET)
				sampledWCET.add(t);
		}
	}
	
	private long getSampleWCET(TaskDescriptor task){
		if (Settings.N_SAMPLE_WCET==0) return task.MinWCET;
		if (task.MinWCET == task.MaxWCET) return task.MinWCET;
		
		// use samples from outside when it set
		if (WCETSamples != null){
			if (WCETSamples.containsKey(task.ID)==true){
				return WCETSamples.get(task.ID);
			}
		}
		
		RandomGenerator randomGenerator = new RandomGenerator();
			
		// make a random WCET
		long sampleWCET = 0;
		if (Settings.UNIFORM_SAMPLE==false) {
			if (randomGenerator.nextDouble() < 0.75)
				sampleWCET = randomGenerator.nextLong(1, task.MinWCET);
			else
				sampleWCET = randomGenerator.nextLong(task.MinWCET + 1, task.MaxWCET);
		}
		else {
			long min = (task.MinWCET == 0) ? task.MinWCET + 1 : task.MinWCET;
			sampleWCET = randomGenerator.nextLong(min, task.MaxWCET);
		}
		return sampleWCET;
	}
	
	public String getSampledWCET(){
		StringBuilder sb = new StringBuilder();
		sb.append("TaskID,ExecutionID,SampledWCET,Arrival,Started,Finished\n");
		
		for(Task item :sampledWCET){
			sb.append(item.ID);
			sb.append(",");
			sb.append(item.ExecutionID);
			sb.append(",");
			sb.append(item.ExecutionTime);
			sb.append(",");
			sb.append(item.ArrivedTime);
			sb.append(",");
			sb.append(item.StartedTime);
			sb.append(",");
			sb.append(item.FinishedTime);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	/**
	 *
	 * @return 0: Nothing to do
	 *		 1: Everything went normally
	 *		 2: Deadline Missed!
	 *		 3: Process completely executed without missing the deadline
	 * @throws Exception
	 */
	int executeOneUnit() throws Exception {
		double missedDeadline=0.0;
		int result = 0;
		boolean preemption = false;
		Task T = null;
		
		// CPU time increased!
		timeLapsed++;
		
		if (readyQueue.isEmpty()) {
			previousTask = lastExecutedTask;
			lastExecutedTask = null;
		}
		else {
			// Get one task and execute!
			T = readyQueue.getFirst();
			if (T.RemainTime <= 0) 	throw new Exception(); //Somehow remaining time became negative, or is 0 from first
			
			// preemption check;
			if ( (lastExecutedTask != null) && (lastExecutedTask.RemainTime != 0) && (lastExecutedTask != T) )
				preemption = true;
			
			// Log StartedTime
			if (T.RemainTime==T.ExecutionTime)
				T.StartedTime = timeLapsed-1;
			
			//Execute!
			T.RemainTime--;
			CPUusages++;
			
			// updated status
			previousTask = lastExecutedTask;
			lastExecutedTask = T;
			result = 1;
			
			// Check task finished and deadline misses
			if (T.RemainTime == 0) {
				readyQueue.pollFirst();  // Task finished, poll the Task out.
				
				// Log a time the task ended
				T.FinishedTime = timeLapsed;
				
				missedDeadline = checkDeadlineMisses(T);//T.ID, T.ArrivedTime, T.StartedTime, (T.ArrivedTime + T.Deadline), timeLapsed);
				
				if (missedDeadline > 0) result = 3;
				else result = 2;
			}
		}
		
		// Represent working status ========================
		if (RMScheduler.DETAIL == true) {
			String started = " ";
			String terminated = " ";
			
			// Notification for a Task execution started.
			if ((lastExecutedTask != null) &&
					((lastExecutedTask.RemainTime + 1 == T.ExecutionTime) || (lastExecutedTask != previousTask))) {
				if (preemption == true)	started = "*";
				else					started = "+";
			}
			//After running if remaining time becomes zero, i.e. process is completely executed
			if ((lastExecutedTask != null) && (lastExecutedTask.RemainTime == 0)) {
				
				if (missedDeadline == 0)
					terminated = "/";	// Finished Normal.
				else
					terminated = "!";	//finished after Deadline.
			}
			
			if (RMScheduler.PROOF == true) {
				if (((timeLapsed - 1) % LINEFEED) == 0)
					printer.format("\nCPU[%010d]: ", (timeLapsed - 1));
				
				printer.format("%s%02d%s ", started, (T == null ? 0 : T.ID), terminated);
			}
			
			if (T!=null && RMScheduler.PROOF == true) {
				int type = (terminated.compareTo(" ")!=0)? 4: (started.compareTo(" ")!=0)?2:3;
				timelines.get(T.ID -1)[(int)timeLapsed-1] = type;
			}
		}
		
		// End of this function
		return result;
		
	}
	
	protected double checkDeadlineMisses(Task _T) { //int _tid, long _arrival_tq, long _started_tq, long _deadline_tq, long _finished_tq) {
		int missed = (int)(_T.FinishedTime - (_T.ArrivedTime + _T.Deadline));
		
		// calculate deadline misses for evaluating
		double factor = evaluateDeadlineMiss(_T, missed);
		evaluatedValue = evaluatedValue + factor;
		
		// For Debugging
		if (RMScheduler.DETAIL == true) executedTasks.add(_T);
		
		if ( missed > 0 ){
			missedDeadlines.add(_T);
			return 1;
		}
		return 0;
	}
	
	protected double evaluateDeadlineMiss(Task _T, int _missed){
		if (Settings.TASK_FITNESS == 0 || _T.ID == Settings.TASK_FITNESS){
			return 0.0;
		}
		return (_missed>0) ? 1 : 0;
	}
	
	/*
	 * setter
	 */
	public void setPrinter(PrintStream _stream) {
		printer = _stream;
	}
	
	/*
	 * Result getters
	 * This functions can give a result after 'run()' function executed
	 */
	public double getEvaluatedValue() {
		return evaluatedValue;
	}
	
	public boolean hasDeadlineMisses()
	{
		return missedDeadlines.size()>0;
	}
	
	public double getCPUusages()
	{
		return (double)CPUusages / problem.QUANTA_LENGTH;
	}
	
	public double calculateUtilization() {
		
		double result = 0;
		
		for (TaskDescriptor task:problem.Tasks)
		{
			if (task.Type == TaskType.Periodic)
				result += (task.MinWCET / (double)task.Period);
			else
				result += (task.MinWCET / (double)task.MinIA);
		}
		return result;
	}
	
	
	/////////////////////////////////////////////////////////////////
	// Utilities
	/////////////////////////////////////////////////////////////////
	
	/**
	 * check input data's feasibility
	 * @return
	 */
	public boolean checkFeasibility() {
		double feasible = muSigma(problem.Tasks.length);
		
		if (feasible >= calculateUtilization())
			return true;
		return false;
	}
	
	/**
	 * Greatest Common Divisor for two int values
	 * @param _result
	 * @param _periodArray
	 * @return
	 */
	private long gcd(long _result, long _periodArray) {
		while (_periodArray > 0) {
			long temp = _periodArray;
			_periodArray = _result % _periodArray; // % is remainder
			_result = temp;
		}
		return _result;
	}
	
	/**
	 * Least Common Multiple for two int numbers
	 * @param _result
	 * @param _periodArray
	 * @return
	 */
	private long lcm(long _result, long _periodArray) {
		return _result * (_periodArray / gcd(_result, _periodArray));
	}
	
	/**
	 * Least Common Multiple for int arrays
	 *
	 * @param _periodArray
	 * @return
	 */
	private long lcm(long[] _periodArray) {
		long result = _periodArray[0];
		for (int i = 1; i < _periodArray.length; i++) result = lcm(result, _periodArray[i]);
		return result;
	}
	
	/**
	 * calculate feasible values
	 * @param _n
	 * @return
	 */
	public double muSigma(int _n) {
		return ((double) _n) * ((Math.pow((double) 2, ((1 / ((double) _n)))) - (double) 1));
	}
	
	
	/**
	 * Ready queue for the Scheduler
	 *   This uses fixed priority for adding new task
	 * @author jaekwon.lee
	 *
	 */
	public class ReadyQueue {
		Task lastExecutedTask;
		LinkedList<Task> TheQueue;
		
		ReadyQueue() {
			TheQueue = new LinkedList<Task>();
			lastExecutedTask = null;
		}
		
		public Task get(int index) {
			return TheQueue.get(index);
		}
		public int size() {
			return TheQueue.size();
		}
		
		public boolean isEmpty(){
			return TheQueue.isEmpty();
		}
		
		public Task getFirst() {
			return TheQueue.getFirst();
		}
		
		public Task getLast() {
			return TheQueue.getLast();
		}
		
		
		public Task pollFirst() {
			return TheQueue.pollFirst();
		}
		
		public Task pollLast() {
			return TheQueue.pollLast();
		}
		
		/**
		 *
		 * @param T
		 * @return
		 */
		public int addNewTask(Task T) {
			for (int i = 0; i < TheQueue.size(); i++) {
				if (T.Priority < TheQueue.get(i).Priority) {
					TheQueue.add(i, T);
					T.ArrivedTime = timeLapsed;
					return 1;
				}
			}
			TheQueue.addLast(T);
			return 1;
		}
	}
	
	/////////////////////////////////////////////////////////////////
	//  Evaluation functions
	/////////////////////////////////////////////////////////////////
	public void initilizeEvaluationTools()
	{
		if (RMScheduler.DETAIL == false) return;
		
		if (RMScheduler.PROOF == true) {
			timelines = new ArrayList<int[]>();
			for (TaskDescriptor task : this.problem.Tasks)
				timelines.add(new int[(int) this.problem.QUANTA_LENGTH]);
		}
		
		executedTasks = new ArrayList<Task>();
	}
	
	class SortbyPriority implements Comparator<TaskDescriptor>
	{
		@Override
		public int compare(TaskDescriptor o1, TaskDescriptor o2) {
			return (int)(o1.Priority - o2.Priority);
		}
	}
	
	private boolean isHigherTasksActive(Task t) {
		// find higher priority tasks
		List<Integer> IDXs = this.getHigherPriorityTasks(t);
		
		// find active states for all IDXs
		for(int idx:IDXs) {
			if (timelines.get(idx)[(int)(t.ArrivedTime)] != 0)
				return true;
		}
		return false;
	}
	
	private boolean isLowerTasksActive(Task t) {
		// find lower priority tasks
		List<Integer> IDXs = this.getLowerPriorityTasks(t);
		
		//find lower tasks active
		for(int idx:IDXs) {
			int[] timeline = timelines.get(idx);
			for(int x=(int)t.StartedTime; x<t.FinishedTime; x++) {
				if (timeline[x]!=0) return true;
			}
		}
		return false;
	}
	
	private List<Integer> getHigherPriorityTasks(Task t)
	{
		ArrayList<Integer> IDXs = new ArrayList<Integer>();
		for(TaskDescriptor item:problem.Tasks) {
			if (item.Priority < t.Priority)
				IDXs.add(item.ID-1);
			if (item.Priority == t.Priority && item.ID<t.ID)
				IDXs.add(item.ID-1);
		}
		return IDXs;
	}
	
	private List<Integer> getLowerPriorityTasks(Task t)
	{
		// find lower priority tasks
		ArrayList<Integer> IDXs = new ArrayList<Integer>();
		for(TaskDescriptor item:problem.Tasks) {
			if (item.Priority > t.Priority)
				IDXs.add(item.ID-1);
			if (item.Priority == t.Priority && item.ID<t.ID)
				IDXs.add(item.ID-1);
		}
		return IDXs;
	}
	
	private int getMinimumInactiveTimeDelta(Task t) {
		List<Integer> IDXs = this.getHigherPriorityTasks(t);
		
		int tq = (int)t.ArrivedTime;
		for (; tq<problem.QUANTA_LENGTH; tq++)
		{
			boolean isActive = false;
			for(int idx:IDXs) {
				if(timelines.get(idx)[tq] != 0) {
					isActive = true;
					break;
				}
			}
			if (isActive == false)
				break;
		}
		
		return (int)(tq - t.ArrivedTime);
	}
	
	public int getMaximumDelayTime(Task t) {
		// find higher priority tasks
		List<Integer> IDXs = this.getHigherPriorityTasks(t);
		
		//calculate sum of delay for all higher priority tasks
		int delay =0;
		for(int tID:IDXs) {
			delay += problem.Tasks[tID].MaxWCET;
		}
		
		// calculate WCET of tasks which the tasks' periods are longer than 'delay'.
		int[] multi = new int[problem.Tasks.length];
		Arrays.fill(multi, 1);
		while(true) {
			boolean flag = false;
			for(int tID:IDXs) {
				TaskDescriptor item = problem.Tasks[tID];
				int period = (int)( (item.Type==TaskType.Periodic) ? item.Period : item.MinIA);
				if (delay < period*multi[tID]) break;
				delay += item.MaxWCET;
				multi[tID]+=1;
				flag = true;
			}
			if (flag == false) break;
		}
		return delay;
	}
	
	public boolean assertScheduler(GAWriter writer) {
		if (RMScheduler.DETAIL == false || RMScheduler.PROOF == false) return true;
		
		for (Task task:executedTasks)
		{
			String str = String.format("{ID:%d,Arrived:%03d, Started:%03d, Finished:%03d, Deadline:%03d}: ", task.ID, task.ArrivedTime, task.StartedTime, task.FinishedTime, task.Deadline+task.ArrivedTime);
			writer.print(str);
			try {
				//first assert
				if (!isHigherTasksActive(task))	{
					assert task.StartedTime == task.ArrivedTime: "Failed to assert higher_tasks_non_active";
				}
				else {
					assert task.StartedTime <= (task.ArrivedTime+getMinimumInactiveTimeDelta(task)): "Failed to assert non exceed WCET";
				}
				//second assert
				assert !isLowerTasksActive(task): "Failed to assert lower_tasks_active";
				
				writer.print("Success\n");
			}
			catch(AssertionError e) {
				writer.print(e.getMessage()+"\n");
			}//			
		}// for
		
		return true;
	}
	
	public  String getTimelinesStr()
	{
		if (RMScheduler.DETAIL == false || RMScheduler.PROOF == false) return "";
		
		StringBuilder sb = new StringBuilder();
		for(int tID=0; tID<this.problem.Tasks.length; tID++)
		{
			sb.append(String.format("Task %02d: ", tID+1));
			for (int x=0; x<this.problem.QUANTA_LENGTH; x++) {
				switch(timelines.get(tID)[x]) {
					case 0:	sb.append("0 "); break;
					case 1:	sb.append("A "); break;
					case 2:	sb.append("S "); break;
					case 3:	sb.append("W "); break;
					case 4:	sb.append("E "); break;
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * get Deadline Missed items
	 * @return
	 */
	public String getExecutedTasksString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TaskID,ExecutionID,Arrival,Started,Finished,Deadline,Misses(finish-deadline),Pow\n");
		
		for (int tid=1; tid<=problem.Tasks.length; tid++) {
			for (Task item:executedTasks) {
				if (tid!=item.ID) continue;
				
				long deadline_tq = (item.ArrivedTime + item.Deadline);
				int missed = (int)(item.FinishedTime - deadline_tq);
				double fitness_item = evaluateDeadlineMiss(item, missed);
				
				sb.append(String.format("%d,%d,%d,%d,%d,%d,%d,%.32e\n",
						item.ID, item.ExecutionID,item.ArrivedTime, item.StartedTime, item.FinishedTime, deadline_tq, missed, fitness_item));
			}
		}
		return sb.toString();
	}
	
	public String getMissedDeadlineString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TaskID,ExecutionID,Arrival,Started,Finished,Deadline,Misses(finish-deadline)\n");
		
		for (int tid=1; tid<=problem.Tasks.length; tid++) {
			for (Task item:missedDeadlines) {
				if (tid!=item.ID) continue;
				long deadline_tq = (item.ArrivedTime+item.Deadline);
				sb.append(String.format("%d,%d,%d,%d,%d,%d,%d\n",
						item.ID, item.ExecutionID, item.ArrivedTime, item.StartedTime, item.FinishedTime, deadline_tq, item.FinishedTime - deadline_tq));
			}
		}
		return sb.toString();
	}
	
	public String getByproduct() {
		return "";
	}
	
}