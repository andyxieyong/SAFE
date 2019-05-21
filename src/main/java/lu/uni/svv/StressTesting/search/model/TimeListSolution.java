package lu.uni.svv.StressTesting.search.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Formatter;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import lu.uni.svv.StressTesting.datatype.FitnessList;
import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.search.model.TaskDescriptor.TaskType;



/**
 * Class Responsibility
 *  - A method to create a solution
 *  - A method to copy a solution
 * So, this class need to reference Problem object
 * @author jaekwon.lee
 */

@SuppressWarnings("serial")
public class TimeListSolution implements Solution<TimeList>{

	private static long UUID = 1L;
	public long ID = 0L;
	
	private TimeList[] variables;   // list of variabls in chromosomes
	private double[] objectives;	// for saving the result of evaluation
	private FitnessList[] objectivesList;	// for saving the result of evaluation
	protected TestingProblem problem;
	protected JMetalRandom randomGenerator;
	
	private String deadlineExecution;		// to save deadline misses by String
	private String detailExecution;		// to save deadline misses by String
	private String byproduct;           // to save information anything to print out during evolution
	private String sampledWCET;         // to save sampled WCET results.
	
	public static void initUUID(){
		TimeListSolution.UUID = 1L;
	}
	
	private TimeListSolution()
	{
		// This function is for the copy function.
		// Do nothing.
		// Copy function will initialize all values.
	}
	
	private void initialize(TestingProblem _problem){
		ID = TimeListSolution.UUID++;
		
		this.problem = _problem;
		this.randomGenerator = JMetalRandom.getInstance() ;
		
		objectives = new double[problem.getNumberOfObjectives()] ;
		for(int x=0; x<objectives.length; x++) objectives[x] = 0.0;
		
		objectivesList = new FitnessList[problem.getNumberOfObjectives()];
		for(int x=0; x<objectivesList.length; x++) objectivesList[x] = new FitnessList();
	}
	/**
	 * Create solution following Testing problem
	 * @param _problem
	 */
	public TimeListSolution(TestingProblem _problem)
	{
		initialize(_problem);
		
		//Encoding chromosomes
		variables = new TimeList[problem.getNumberOfVariables()] ;
		for (int x = 0; x < problem.getNumberOfVariables(); x++) {
			variables[x] = this.createRandomList(x);
		}
		//System.out.println("Created a Solution");
	}
	
	public TimeListSolution(TestingProblem _problem, TimeList[] _variables)
	{
		initialize(_problem);
		
		//Encoding chromosomes
		variables = new TimeList[problem.getNumberOfVariables()] ;
		for (int x = 0; x < problem.getNumberOfVariables(); x++) {
			variables[x] = _variables[x];
		}
	}
	
	/**
	 * copy of this solution
	 * all values of objectives are initialized by 0 (This means the solution is not evaluated)
	 */
	@Override
	public Solution<TimeList> copy() {
		TimeListSolution solution = new TimeListSolution(this.problem);
		
		//Encoding chromosomes
		solution.variables = new TimeList[this.problem.getNumberOfVariables()] ;
		for (int x = 0; x < this.problem.getNumberOfVariables(); x++) {
			solution.variables[x] = (TimeList)this.variables[x].clone();
		}
		
		return solution;
	}
	
	/**
	 * Getter, Setter, and utils for Objectives member
	 */
	public void setObjectiveList(int index, FitnessList values) {
		this.objectivesList[index] = values;
	}
	public FitnessList getObjectiveList(int index) {
		return this.objectivesList[index];
	}
	
	@Override
	public void setObjective(int index, double value) {
		this.objectives[index] = value;
	}
	@Override
	public double getObjective(int index) {
		return this.objectives[index];
	}
	@Override
	public double[] getObjectives() {
		return this.objectives;
	}
	@Override
	public int getNumberOfObjectives() {
		return this.objectives.length;
	}

	/**
	 * Getter, Setter, and utils for Variables member
	 */
	public TimeList[] getVariables() {
		return this.variables;
	}
	@Override
	public TimeList getVariableValue(int index) {
		return this.variables[index];
	}
	@Override
	public void setVariableValue(int index, TimeList value) {
		this.variables[index] = value;
	}
	@Override
	public int getNumberOfVariables() {
		return this.variables.length;
	}
	@Override
	@SuppressWarnings("resource")
	public String getVariableValueString(int index) {
	
		TimeList aVariable = this.variables[index];
		
		StringBuilder sb = new StringBuilder();
		Formatter fmt = new Formatter(sb);
		
		fmt.format("[");
		for(int i=0; i< aVariable.size(); i++) {
			fmt.format("%d", aVariable.get(i));
			if ( aVariable.size() > (i+1) )
				sb.append(",");
		}
		fmt.format("]");
		
		return sb.toString();
		
	}
	
	public String getVariableValueString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		for (int x=0; x < this.variables.length; x++) {
			sb.append(this.getVariableValueString(x));
			if (x!=(this.variables.length-1))
				sb.append(", \n");
		}
		sb.append(" ]");
		
		return sb.toString();
	}
	
	/**
	 * Make a string with Variable values, this show rep_size numbers.
	 * @param index
	 * @param rep_size
	 * @return
	 */
	@SuppressWarnings("resource")
	public String getCustomVariableValueString(int index, int rep_size) {
		TimeList aVariable = this.variables[index];
		
		StringBuilder sb = new StringBuilder();
		Formatter fmt = new Formatter(sb);
		
		fmt.format("P%02d: ", index);
		for(int i=0; i< MIN(aVariable.size(), rep_size); i++) {
			fmt.format("%d", aVariable.get(i));
			if ( aVariable.size() > (i+1) )
				sb.append(",");
		}
		
		if(aVariable.size() > rep_size )
			fmt.format("...(more %d)\n", (aVariable.size() - rep_size));
		else
			fmt.format("\n");

		return sb.toString();
	}
	public String getCustomVariableValueString(int rep_size) {
		StringBuilder sb = new StringBuilder();
		
		for (int x=0; x < this.variables.length; x++) {
			sb.append(this.getCustomVariableValueString(x, rep_size));
		}
		
		return sb.toString();
	}
	
	/**
	 * This function returns string of values in a specific Variable
	 * @param index
	 * @param rep_size
	 * @return
	 */
	public String getLineVariableValueString(int index, int rep_size) {
		TimeList aVariable = this.variables[index];
		
		StringBuilder sb = new StringBuilder();
		Formatter fmt = new Formatter(sb);
		
		fmt.format("[");
		for(int i=0; i< MIN(aVariable.size(), rep_size); i++) {
			fmt.format("%d", aVariable.get(i));
			if ( aVariable.size() > (i+1) )
				sb.append(",");
		}
		
		if(aVariable.size() > rep_size )
			fmt.format("...(more %d)]", (aVariable.size() - rep_size));
		else
			fmt.format("]");
		
		return sb.toString();
	}
	/**
	 * This function returns string of values in the Variables
	 * @param rep_size
	 * @return
	 */
	public String getLineVariableValueString(int rep_size) {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		for (int x=0; x < this.variables.length; x++) {
			sb.append(this.getLineVariableValueString(x, rep_size));
			if (x!=(this.variables.length-1))
				sb.append(", ");
		}
		sb.append(" ]");
		
		return sb.toString();
	}


	public void setByproduct(String values){
		this.byproduct = values;
	}
	public String getByproduct(){
		return this.byproduct;
	}
	
	private static final int MIN(int a, int b)
	{
		return (a>b)? b:a;
	}
	/**
	 * Getter, Setter, and utils for Attributes member
	 */
	@Override
	public void setAttribute(Object id, Object value) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Object getAttribute(Object id) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Create a Gene (a factor of Chromosomes) for each task
	 * @param _index
	 * @return
	 */
	private TimeList createRandomList(int _index) {

		TimeList list = new TimeList();
		TaskDescriptor T = problem.Tasks[_index];
		
		// this values are one of the value
		// between 0 to Environment.MAX_PHASETIME for periodic task;
		// between MinIA to MaxIA for other type of tasks;
		long arrival;
		if (T.Type == TaskType.Periodic)
			// we gives phase time to arrival time at the starting point
			arrival = randomGenerator.nextInt(0, (int)problem.MAX_PHASETIME);
		else
			arrival = randomGenerator.nextInt((int)T.MinIA, (int)T.MaxIA);
		
		// create arrival time table
		while(arrival < problem.QUANTA_LENGTH) {
			list.add(arrival); // Input first

			// create next value
			// randomGenerator.nextInt(0) occurs Error because there are no range.
			int inteval = 0;
			if (T.Type == TaskType.Periodic)
				inteval = (int)T.Period;
			
			// Aperiodic or Sporadic case
			else
				inteval = randomGenerator.nextInt((int)T.MinIA, (int)T.MaxIA);
			arrival += inteval;
		}
		
		return list;
	}
	
	public void setDeadlines(String _details) {
		deadlineExecution = _details;
	}
	public String getDeadlines(){return deadlineExecution;}
	
	public void setDetailExecution(String _details) {
		detailExecution = _details;
	}
	public String getDetailExecution(){return detailExecution;}
	
	
	public void setSampledWCET(String _sampledValues){
		sampledWCET = _sampledValues;
	}
	public String getSampledWCET(){
		return sampledWCET;
	}
	

	
	public static TimeListSolution loadFromJSON(TestingProblem _problem, String _filepath){
		TimeList[] variables = null;
		
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(new File(_filepath));
			JSONTokener tokener = new JSONTokener(stream);
			JSONArray json = new JSONArray(tokener);
			
			variables = new TimeList[json.length()];
			for (int i = 0; i < json.length(); i++) {
				JSONArray array = json.getJSONArray(i);
				
				variables[i] = new TimeList();
				for(Object item:array){
					variables[i].add(((Integer)item).longValue());
				}
			}
		}
		catch (IOException e){
			e.printStackTrace();
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return new TimeListSolution(_problem, variables);
	}
}
