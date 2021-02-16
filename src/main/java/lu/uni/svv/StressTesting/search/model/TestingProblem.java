package lu.uni.svv.StressTesting.search.model;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import lu.uni.svv.StressTesting.datatype.FitnessList;
import lu.uni.svv.StressTesting.datatype.TaskSeverity;
import lu.uni.svv.StressTesting.datatype.TaskType;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.RandomGenerator;
import lu.uni.svv.StressTesting.utils.Settings;
import org.uma.jmetal.problem.impl.AbstractGenericProblem;

import lu.uni.svv.StressTesting.scheduler.RMScheduler;
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
	public int      RUN_ID;         // RUN_NUM, we set the name to distinguish with Settings property
	
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
		this.Tasks = TaskDescriptor.loadFromCSV(_filename, this.MAX_TIME, this.TIME_QUANTA);
		
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
		String filename = (this.RUN_ID >0)? String.format("_samples/sampledata_run%02d.csv", this.RUN_ID):"samples/sampledata.csv";
		GAWriter sampledata = new GAWriter(filename, Level.INFO, uncertainHeader, Settings.BASE_PATH,true);
		
		// generate sample
		for (int sampleID = 0; sampleID < Settings.N_SAMPLE_WCET; sampleID++) {
			JMetalLogger.logger.info(String.format("   [%d/%d] sample evaluating", sampleID+1, Settings.N_SAMPLE_WCET));
			HashMap<Integer, Long> samples = this.getSampling(uncertainTasks);
			
			scheduler.initialize();
			scheduler.setSamples(samples);
			scheduler.run(solution);
			
			boolean titleFlag = (sampleID == 0);
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
		if (Settings.GA_REPR_FITNESS.compareTo("average")==0){
			solution.setObjective(0, AverageList(fitnessList));
		} else if (Settings.GA_REPR_FITNESS.compareTo("maximum")==0){
			solution.setObjective(0, MaximumList(fitnessList));
		} else {
			solution.setObjective(0, MinimumList(fitnessList));
		}
		
		
		solution.setObjectiveList(0, fitnessList);
		solution.setByproduct(byproduct.toString());
		
		sampledata.close();
	}
	
	public double AverageList(FitnessList list){
		double avg = 0.0;
		for (int x=0; x<list.size(); x++){
			avg = avg + list.get(x);
		}
		avg = avg / list.size();
		return avg;
	}
	
	public double MaximumList(FitnessList list){
		double max = list.get(0);
		for (int x=1; x<list.size(); x++){
			if (list.get(x)>max)
				max = list.get(x);
		}
		return max;
	}
	
	public double MinimumList(FitnessList list){
		double min = list.get(0);
		for (int x=1; x<list.size(); x++){
			if (list.get(x)<min)
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
	
	public List<Integer> getVaryingTasks(){
		List<Integer> list = new ArrayList<Integer>();
		
		for(TaskDescriptor task : Tasks){
			if (task.Type != TaskType.Periodic && task.MinIA != task.MaxIA)
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
	

}
