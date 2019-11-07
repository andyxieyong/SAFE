package lu.uni.svv.StressTesting.search.model;

import lu.uni.svv.StressTesting.datatype.FitnessList;
import lu.uni.svv.StressTesting.datatype.SummaryItem;
import lu.uni.svv.StressTesting.datatype.TimeList;
import lu.uni.svv.StressTesting.utils.Settings;
import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.ObjectiveComparator.Ordering;

import lu.uni.svv.StressTesting.utils.GAWriter;

import java.util.*;
import java.util.logging.Level;

/**
 * @author Jaekwon Lee <jaekwon.lee@uni.lu>
 */
@SuppressWarnings("serial")
public class SteadyStateGeneticAlgorithm<S extends Solution<?>> extends AbstractGeneticAlgorithm<S, S> {
	private Comparator<S> comparator;
	private int maxIterations;
	private int iterations;
	private GAWriter fitness_writer;
	private GAWriter byproduct;
	
	/**
	 * Constructor
	 */
	public SteadyStateGeneticAlgorithm(String basepath,
	                                   String name,
	                                   Problem<S> problem,
	                                   int maxIterations,
	                                   int populationSize,
	                                   CrossoverOperator<S> crossoverOperator,
	                                   MutationOperator<S> mutationOperator,
	                                   SelectionOperator<List<S>, S> selectionOperator) {
		super(problem);
		setMaxPopulationSize(populationSize);
		this.maxIterations = maxIterations;
		this.crossoverOperator = crossoverOperator;
		this.mutationOperator = mutationOperator;
		this.selectionOperator = selectionOperator;
		
		if (Settings.N_SAMPLE_WCET==0)
			comparator = new SolutionComparator<S>(0, Ordering.DESCENDING);
		else
			comparator = new SolutionListComparatorAvg<S>(0, Ordering.DESCENDING);
		
		byproduct = new GAWriter(String.format("_minimums/minimumMissed_%s.csv", name), Level.FINE, null, basepath);
		printSummaryInit(name, basepath);
	}

	protected void close() {
		byproduct.close();
		if (fitness_writer != null)
			fitness_writer.close();
	}
	
	@Override
	public void run() {
		List<S> offspringPopulation;
		List<S> matingPopulation;
		
		population = createInitialPopulation();
		JMetalLogger.logger.info("created initial population");
		JMetalLogger.logger.info("evaluating initial population");
		population = evaluatePopulation(population);
		JMetalLogger.logger.info("evaluated initial population");
		initProgress();
		while (!isStoppingConditionReached()) {
			if (Settings.SIMPLE_SEARCH){
				offspringPopulation = simpleGenerateOffsprings();
			}
			else{
				matingPopulation = selection(population);
				offspringPopulation = reproduction(matingPopulation);
			}
			offspringPopulation = evaluatePopulation(offspringPopulation);
			population = replacement(population, offspringPopulation);
			updateProgress();
		}
		
		close();
	}
	
	@Override
	protected boolean isStoppingConditionReached() {
		
		return (iterations >= maxIterations);
	}
	
	@Override
	protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
		Collections.sort(population, comparator);
		int worstSolutionIndex = population.size() - 1;
		if (comparator.compare(population.get(worstSolutionIndex), offspringPopulation.get(0)) > 0) {
			population.remove(worstSolutionIndex);
			population.add(offspringPopulation.get(0));
		}
		
		return population;
	}
	
	protected int findIndex(List<S> population, long id){
		int target = 0;
		for(int x=0; x<population.size(); x++){
			if ( ((TimeListSolution)population.get(x)).ID == id ){
				target = x;
				break;
			}
		}
		return target;
	}
	
	@Override
	protected List<S> reproduction(List<S> matingPopulation) {
		List<S> parents = new ArrayList<S>(2);
		parents.add(matingPopulation.get(0));
		parents.add(matingPopulation.get(1));
		
		List<S> offspring = crossoverOperator.execute(parents);
		mutationOperator.execute(offspring.get(0));
		
		List<S> offspringPopulation = new ArrayList<S>(1);
		offspringPopulation.add(offspring.get(0));
		return offspringPopulation;
	}
	
	public List<S> simpleGenerateOffsprings() {
		List<S> offsprings = new ArrayList<S>(1);
		S solution = problem.createSolution();
		offsprings.add(solution);
		return offsprings;
	}
	
	@Override
	protected List<S> selection(List<S> population) {
		List<S> matingPopulation = new ArrayList<S>(2);
		for (int i = 0; i < 2; i++) {
			S solution = selectionOperator.execute(population);
			matingPopulation.add(solution);
		}
		
		return matingPopulation;
	}
	
	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		TestingProblem problem = (TestingProblem)getProblem();
		int count =0;
		for (S solution : population) {
			count += 1;
			problem.evaluate((TimeListSolution)solution);
			if (Settings.N_SAMPLE_WCET!=0 && Settings.PRINT_SAMPLES)
				saveExpendedInfo((TimeListSolution)solution);
			JMetalLogger.logger.info("" + count + "/" + population.size()+ " evaluated population");
		}
		
		return population;
	}
	
	private void saveExpendedInfo(TimeListSolution _solution)
	{
		// Save Results -->
		//     samples/solution/solutionID.txt
		//     samples/best(e-d)/solutionID.csv
		//     samples/deadlines/solutionID.csv
		//     samples/executions/solutionID.csv
		//     samples/WCET/solutionID.csv
		// print out a solution info.
		String filename = String.format("/samples/solution/%d.json", _solution.ID);
		GAWriter writer = new GAWriter(filename, Level.INFO, null, Settings.BASE_PATH);
		writer.info(_solution.getVariableValueString());
		writer.close();
		
		// best (e-d)
		filename = String.format("/samples/best(e-d)/%d.csv", _solution.ID);
		writer = new GAWriter(filename, Level.INFO, null, Settings.BASE_PATH);
		writer.info(_solution.getByproduct());
		writer.close();
		
		// deadline missed
		filename = String.format("/samples/deadlines/%d.csv", _solution.ID);
		writer = new GAWriter(filename, Level.INFO, null, Settings.BASE_PATH);
		writer.info(_solution.getDeadlines());
		writer.close();
		
		// executions which are best(e-d)
		filename = String.format("/samples/executions/%d.csv", _solution.ID);
		writer = new GAWriter(filename, Level.INFO, null, Settings.BASE_PATH);
		writer.info(_solution.getDetailExecution());
		writer.close();
		
		// sampleing information
		filename = String.format("/samples/WCET/%d.csv", _solution.ID);
		writer = new GAWriter(filename, Level.INFO, null,  Settings.BASE_PATH);
		writer.info(_solution.getSampledWCET());
		writer.close();
		
		// sampleing information
		filename = String.format("/samples/arrivals/%d.csv", _solution.ID);
		writer = new GAWriter(filename, Level.INFO, null, Settings.BASE_PATH);
		writer.info(makeArrivals(_solution));
		writer.close();
		
		// counting deadline missed
		String str = _solution.getDeadlines();
		String[] deadlines = str.split("\n");
		int[] counts = new int[Settings.N_SAMPLE_WCET];
		boolean title = true;
		for(String item:deadlines){ // int x=0; x<deadlines.length; x++){
			if (title) {
				title = false;
				continue;
			}
			int solID = Integer.parseInt(item.substring(0, item.indexOf(",")));
			counts[solID] += 1;
		}
		
		// print fitness
		filename = String.format("/samples/fitness/%d.csv", _solution.ID);
		
		FitnessList fitnessList = _solution.getObjectiveList(0);
		StringBuilder sb = new StringBuilder();
		sb.append("SampleID,Fitness,MissingDeadlines\n");
		for (int x=0; x<fitnessList.size(); x++) {
			sb.append(String.format("%d,%.32e,%d\n",x, fitnessList.get(x), counts[x]));
		}
		writer = new GAWriter(filename, Level.INFO, null, Settings.BASE_PATH);
		writer.info(sb.toString());
		writer.close();
	}
	
	private String makeArrivals(TimeListSolution _solution){
		StringBuilder sb = new StringBuilder();
		sb.append("TaskID,Arrivals\n");
		TimeList[] timelist = _solution.getVariables();
		int tid = 1;
		for (TimeList item : timelist){
			sb.append(tid);
			sb.append(",");
			sb.append(item.size());
			sb.append("\n");
			tid+=1;
		}
		return sb.toString();
	}
	
	@Override
	public S getResult() {
		Collections.sort(getPopulation(), comparator);
		return getPopulation().get(0);
	}
	
	@Override
	public void initProgress() {
		iterations = 0;
		JMetalLogger.logger.info("initialized Progress");
		if (Settings.PRINT_RESULTS) {
			printSummary();
		}
		initByproduct();
		printByproduct();

	}
	
	@Override
	public void updateProgress() {
		iterations++;
		JMetalLogger.logger.info("move to next evaluation: " + iterations);
		if (Settings.PRINT_RESULTS) {
			printSummary();
		}
		printByproduct();
	}
	
	@Override
	public String getName() {
		return "ssGA";
	}
	
	@Override
	public String getDescription() {
		return "Steady-State Genetic Algorithm";
	}
	
	public void printSummaryInit(String run, String basepath){
		fitness_writer = new GAWriter(String.format("_results/result_run%s.csv", run), Level.INFO, null, basepath);
		// Title
		StringBuilder sb = new StringBuilder();
		if (Settings.N_SAMPLE_WCET==0)
			sb.append(String.format("Iterations,Run %s", run));
		else
			sb.append(String.format("Iterations,SampleID,Run %s", run));
		
		fitness_writer.info(sb.toString());
	}
	
	public void printSummary(){
		Collections.sort(population, comparator);
		TimeListSolution bestSolution = (TimeListSolution)population.get(0);
		
		StringBuilder sb = new StringBuilder();
		
		if (Settings.N_SAMPLE_WCET==0){
			double fitness = bestSolution.getObjective(0);
			sb.append((iterations));
			sb.append(",");
			sb.append(String.format("%.0f", fitness));
		}
		else{
			FitnessList list = bestSolution.getObjectiveList(0);
			for (int x=0; x<list.size(); x++){
				sb.append((iterations));
				sb.append(",");
				sb.append(x);
				sb.append(",");
				sb.append(String.format("%.0f", list.get(x) ));
				if (x!=list.size()-1)
					sb.append("\n");
			}
		}
		fitness_writer.info(sb.toString());
	}
	
	public void initByproduct(){
		StringBuilder sb = new StringBuilder();
		sb.append("Iteration,");
		if (Settings.N_SAMPLE_WCET!=0)
			sb.append("SampleID,");
		for(int num=0; num<problem.getNumberOfVariables(); num++){
			sb.append(String.format("Task%2s",num+1));
			if (num+1 < problem.getNumberOfVariables())
				sb.append(",");
		}
		byproduct.info(sb.toString());
	}
	
	public void printByproduct() {
		StringBuilder sb = new StringBuilder();
		String text = ((TimeListSolution) population.get(0)).getByproduct();
		
		if (Settings.N_SAMPLE_WCET == 0) {
			sb.append(iterations);
			sb.append(",");
			sb.append(text);
		} else {
			String[] lines = text.split("\n");
			for (int x = 1; x < lines.length; x++) {
				sb.append(iterations);
				sb.append(",");
				sb.append(lines[x]);
				if (x != lines.length - 1)
					sb.append("\n");
			}
		}
		
		byproduct.info(sb.toString());
	}
}
