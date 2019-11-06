package lu.uni.svv.StressTesting.search.model;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import com.sun.xml.internal.ws.api.config.management.policy.ManagementAssertion;
import lu.uni.svv.StressTesting.datatype.FitnessList;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.RandomGenerator;
import lu.uni.svv.StressTesting.utils.Settings;
import org.uma.jmetal.problem.impl.AbstractGenericProblem;

import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.search.model.TaskDescriptor.TaskType;
import org.uma.jmetal.util.JMetalLogger;


/**
 * Class Responsibility
 *  - Definition of the problem to solve
 *  - Basic environments (This is included the definition of the problem)
 *  - An interface to create a solution
 *  - A method to evaluate a solution
 * @author jaekwon.lee
 */
@SuppressWarnings("serial")
public class TestingProblem extends AbstractGenericProblem<TimeListSolution> {
	
	// Internal Values
	public double   TIME_QUANTA;	// This unit is ms;  /// 0.1
	public long     MAX_TIME;		// 1 hour (ms)
	public long     MAX_PHASETIME;	// maximum phase time;
	public long     QUANTA_LENGTH;	// MAX_TIME * (1/TIME_QUANTA) // to treat all time values as integer
	public int      RUN_ID =0;
	
	public TaskDescriptor[] Tasks = null;		// Task information
	
	Class schedulerClass = null;
	
	/**
	 * Constructor
	 * Load input data and setting environment of experiment	
	 * @param _filename
	 */
	public TestingProblem(String _filename,  String _schedulerName) throws NumberFormatException, IOException
	{
		this(_filename, 0.1, 3600000, _schedulerName);
	}
	
	
	public long find_max_deadline(){
		long max_dealine=0;
		for (TaskDescriptor task:this.Tasks){
			if (task.Deadline> max_dealine)
				max_dealine = task.Deadline;
		}
		return max_dealine;
	}
	public TestingProblem(String _filename, double _time_quanta, int _max_time, String _schedulerName) throws NumberFormatException, IOException{
		
		// Set environment of this problem.
		this.TIME_QUANTA = _time_quanta;
		this.MAX_TIME = _max_time;
		this.RUN_ID =0;
		this.QUANTA_LENGTH = (int) (this.MAX_TIME * (1/this.TIME_QUANTA));
		
		// This function updates this.Tasks value.
		this.loadFromCSV(_filename);
		
		// Increase Quanta_length
		long max_deadline = find_max_deadline();
		this.QUANTA_LENGTH += max_deadline;
		
		this.setName("StressTesting");
		this.setNumberOfVariables(this.Tasks.length);
		this.setNumberOfObjectives(1);	//Single Objective Problem
		
		// Create Scheduler instance
		try {
			Class c = this.getClass();
			c.getPackage();
			this.schedulerClass = Class.forName("lu.uni.svv.StressTesting.scheduler." + _schedulerName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Class Responsibility :create solution interface
	 * Delegate this responsibility to Solution Class.
	 */
	@Override
	public TimeListSolution createSolution() {
		return new TimeListSolution(this);
	}
	
	
	/**
	 * Class Responsibility :evaluate solution interface
	 */
	@Override
	public void evaluate(TimeListSolution solution) {
		// get scheduler object
//		RMScheduler.DETAIL = true;
//		RMScheduler.PROOF = true;
		RMScheduler scheduler = null;
		try {
			Constructor constructor = schedulerClass.getConstructors()[0];
			Object[] parameters = {this, Settings.TARGET_TASKS};
			scheduler = (RMScheduler)constructor.newInstance(parameters);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (Settings.N_SAMPLE_WCET == 0) {
			evaluateDirect(solution, scheduler);
		}
		else{// Sampling Works
			evaluateWithSampling(solution, scheduler);
		}
		
	}
	
	private void evaluateDirect(TimeListSolution solution, RMScheduler scheduler) {
		scheduler.run(solution);
		double value = scheduler.getEvaluatedValue();
		double cpu = scheduler.getCPUusages();
		
		solution.setObjective(0, value);
		solution.setDeadlines(scheduler.getMissedDeadlineString());
		solution.setByproduct(scheduler.getByproduct());
		
		solution.setDetailExecution(scheduler.getExecutedTasksString());
	}
	
	private void evaluateWithSampling(TimeListSolution solution, RMScheduler scheduler) {
		double value = 0.0;
		FitnessList fitnessList = new FitnessList();
		StringBuilder byproduct = new StringBuilder();
		String header = taskHeader("", this.getNumberOfVariables());
		
		// Sample
		List<Integer> uncertainTasks = this.getUncertainTasks();
		String uncertainHeader = this.getUncertainTasksString("result", uncertainTasks);
		String filename = (this.RUN_ID >0)? String.format("samples/sampledata_run%02d.csv", this.RUN_ID):"samples/sampledata.csv";
		GAWriter sampledata = new GAWriter(filename, Level.INFO, uncertainHeader, Settings.BASE_PATH,true);
		
		// generate sample
		for (int sampleID = 0; sampleID < Settings.N_SAMPLE_WCET; sampleID++) {
			JMetalLogger.logger.info(String.format("   [%d/%d] sample evaluating", sampleID+1, Settings.N_SAMPLE_WCET));
			HashMap<Integer, Long> samples = this.getSampling(uncertainTasks);
			
			scheduler.initialize();
			scheduler.setSamples(samples);
			scheduler.run(solution);
			
			boolean titleFlag = ((sampleID==0)? true:false);
			String pretitle = "SampleID";
			String prefix = String.format("%d", sampleID);
			
			fitnessList.add(scheduler.getEvaluatedValue());
			byproduct.append(addPrefix(header + scheduler.getByproduct(), pretitle, prefix, titleFlag));
			
			// print a sample data for one copied chromosome
			StringBuilder sb = new StringBuilder();
			sb.append((scheduler.hasDeadlineMisses())?1:0);
			for (int taskID : uncertainTasks){
				sb.append(',');
				sb.append(samples.get(taskID));
			}
			sampledata.info(sb.toString());
		}
		solution.setObjective(0, MinimumList(fitnessList));
		solution.setObjectiveList(0, fitnessList);
		solution.setByproduct(byproduct.toString());
		
		sampledata.close();
	}
	
	public double MinimumList(FitnessList list){
		double min = list.get(0);
		for (int x=1; x<list.size(); x++){
			if (list.get(x)<=min)
				min = list.get(x);
		}
		return min;
	}
	
	private String getUncertainTasksString(String header, List<Integer> uncertainTasks){
		StringBuilder sb = new StringBuilder();
		sb.append(header);
		for (int taskID: uncertainTasks){
			sb.append(",T");
			sb.append(Integer.toString(taskID));
		}
		return sb.toString();
	}
	
	public List<Integer> getUncertainTasks(){
		List<Integer> list = new ArrayList<Integer>();
		
		for(TaskDescriptor task : Tasks){
			if (task.MinWCET != task.MaxWCET)
				list.add(task.ID);
		}
		return list;
	}
	
	private HashMap<Integer, Long> getSampling(List<Integer> _uncertainTasks){
		HashMap<Integer, Long> sampling = new HashMap<Integer, Long>();
	
		RandomGenerator randomGenerator = new RandomGenerator();
		for (int taskID : _uncertainTasks) {
			long sampleWCET = 0;
			sampleWCET = randomGenerator.nextLong(Tasks[taskID-1].MinWCET, Tasks[taskID-1].MaxWCET);
			sampling.put(taskID, sampleWCET);
		}
		
		return sampling;
	}

	private String addPrefix(String _origin, String _prfix_title, String _prefix, boolean _title){
		StringBuilder sb = new StringBuilder();
		
		String[] lines = _origin.split("\n");
		
		if (_title==true) {
			if (_prefix != null) {
				sb.append(_prfix_title);
				sb.append(",");
			}
			sb.append(lines[0]);
			sb.append("\n");
		}
		for (int x=1; x<lines.length; x++){
			if (_prefix != null) {
				sb.append(_prefix);
				sb.append(",");
			}
			sb.append(lines[x]);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private boolean checkPrintHeader(String _filename){
		File file = new File(_filename);
		if (file.exists() == false)
			return true;
		return false;
	}
	
	private String taskHeader(String _init, int _nums){
		StringBuilder sb = new StringBuilder();
		sb.append(_init);
		for(int tID=0; tID<_nums; tID++){
			sb.append(String.format("Task%2s",tID+1));
			if (tID != _nums-1)
				sb.append(",");
		}
		sb.append("\n");
		return sb.toString();
	}
	
	/* **********************************************************
	 *  Utilities
	 */
	public void loadFromCSV(String _filepath) throws NumberFormatException, IOException {
		List<TaskDescriptor> listJobs = new ArrayList<TaskDescriptor>();
		
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
			aJob.Name 		= cols[0].trim();	// Name
			aJob.Type 		= getTypeFromString(cols[1].trim());
			aJob.Priority 	= getValueFromString(cols[2].trim(), this.QUANTA_LENGTH);
			aJob.MinWCET 	= getTimeFromString(cols[3].trim(), 0);
			aJob.MaxWCET 	= getTimeFromString(cols[4].trim(), 0);
			aJob.Period 	= getTimeFromString(cols[5].trim(), this.QUANTA_LENGTH);
			aJob.MinIA 		= getTimeFromString(cols[6].trim(), 0);
			aJob.MaxIA		= getTimeFromString(cols[7].trim(), this.QUANTA_LENGTH);
			aJob.Deadline 	= getTimeFromString(cols[8].trim(), this.QUANTA_LENGTH);
			aJob.Severity 	= cols[9].trim();	// Severity type
			
			listJobs.add(aJob);
		}
		// after loop, close reader
		br.close();
		
		// Register in the Environment.
		this.Tasks = new TaskDescriptor[listJobs.size()];
		listJobs.toArray(this.Tasks);
	}
	
	public String increaseWCET(String targetType, double increase) throws Exception {
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
		for(TaskDescriptor task:this.Tasks)
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
	
	public String getInputs(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("Task Name,Task Type,Task Priority,WCET min,WCET max,Task Period (ms),Minimum interarrival-time (ms),Maximum Interarrival time,Task Deadline, Deadline Type\n");
		for(TaskDescriptor task:this.Tasks)
		{
			sb.append(String.format("%s,%s,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%s\n",
									task.Name, task.Type.toString(), task.Priority,
									task.MinWCET * TIME_QUANTA, task.MaxWCET* TIME_QUANTA,
									task.Period * TIME_QUANTA,
									task.MinIA* TIME_QUANTA, task.MaxIA* TIME_QUANTA, task.Deadline* TIME_QUANTA, task.Severity));
		}
		return sb.toString();
	}
	
	public TaskType getTypeFromString(String _text) {
		
		if (_text.compareTo("Sporadic")==0)					
			return TaskType.Sporadic;
		else if (_text.compareTo("Aperiodic")==0)
			return TaskType.Aperiodic;
		else
			return TaskType.Periodic;
	}
	
	public long getValueFromString(String _text, long _default) {
		
		if (_text.compareTo("")==0 || _text.compareTo("N/A")==0) 
			return _default;
		else
			return (long)(Double.parseDouble(_text));
	}
	
	public long getTimeFromString(String _text, long _default) {
		return getTimeFromString(_text, _default, this.QUANTA_LENGTH);
	}
	
	
	public long getTimeFromString(String _text, long _default, long _max) {
		
		if (_text.compareTo("")==0 || _text.compareTo("N/A")==0) 
			return _default;
		else {
			long value = (long)(Double.parseDouble(_text) * (1 / this.TIME_QUANTA));
			if (value > _max)
				return _max;
			return value;
		}
	}

}
