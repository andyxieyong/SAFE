package lu.uni.svv.StressTesting.search.model;

import lu.uni.svv.StressTesting.datatype.SummaryItem;
import org.uma.jmetal.util.comparator.ObjectiveComparator.Ordering;
import lu.uni.svv.StressTesting.utils.GAWriter;
import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalLogger;
import java.util.*;
import java.util.logging.Level;

/**
 * @author Jaekwon Lee <jaekwon.lee@uni.lu>
 */
@SuppressWarnings("serial")
public class SimpleRandomAlgorithm<S extends Solution<?>>  extends AbstractGeneticAlgorithm<S, S> {
	private Comparator<S> comparator;
	private int maxIterations;
	private int iterations;
	private GAWriter writer;
	private GAWriter candidatesWriter;
	private GAWriter byproduct;
	private GAWriter detailWriter;
	
	private String name;
	private String basepath;
	
	/**
	 * Constructor
	 */
	public SimpleRandomAlgorithm(String basepath,
	                             String name,
	                             Problem<S> problem,
	                             int maxIterations,
	                             int populationSize) {
		super(problem);
		setMaxPopulationSize(populationSize);
		this.maxIterations = maxIterations;
		this.basepath = basepath;
		this.name = name;
		
		comparator = new SolutionComparator<S>(0, Ordering.DESCENDING);
		
		writer = new GAWriter(String.format("iterations/iterations_%s.log", name), Level.FINE, null, basepath);
		candidatesWriter = new GAWriter(String.format("candidates/candidates_%s.csv", name), Level.FINE, null, basepath);
		byproduct = new GAWriter(String.format("minimums/minimumMissed_%s.csv", name), Level.FINE, null, basepath);
		detailWriter = new GAWriter(String.format("executions/run%s.csv", name), Level.FINE, null, basepath);
	}
	
	protected void close() {
		writer.close();
		candidatesWriter.close();
		byproduct.close();
		detailWriter.close();
	}
	
	@Override
	public String getName() {
		return "SimpleRandom";
	}
	
	@Override
	public String getDescription() {
		return "Steady-State Genetic Algorithm";
	}
	
	@Override
	public void run() {
		List<S> offspringPopulation;
		
		population = createInitialPopulation();
		JMetalLogger.logger.info("created " + maxPopulationSize + " solutions");
		population = evaluatePopulation(population);
		printCandidates(population);
		JMetalLogger.logger.info("evaluated initial population");
		initProgress();
		while (!isStoppingConditionReached()) {
			offspringPopulation = createInitialPopulation();
			offspringPopulation = evaluatePopulation(offspringPopulation);
			printCandidates(offspringPopulation);
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
		getPopulation().sort(comparator);
		int worstSolutionIndex = population.size() - 1;
		if (comparator.compare(population.get(worstSolutionIndex), offspringPopulation.get(0)) > 0) {
			population.remove(worstSolutionIndex);
			population.add(offspringPopulation.get(0));
		}
		
		return population;
	}
	
	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		for (S solution : population) {
			getProblem().evaluate(solution);
		}
		return population;
	}
	
	@Override
	public S getResult() {
		getPopulation().sort(comparator);
		return getPopulation().get(0);
	}
	
	@Override
	public void initProgress() {
		iterations = 1;
		JMetalLogger.logger.info("initialized Progress");
		initSummary();
		loggingSumary();
		printPopulation();
		initByproduct();
		printByproduct();
		printExecutions(true);
	}
	
	@Override
	public void updateProgress() {
		iterations++;
		JMetalLogger.logger.info("move to next evaluation: " + iterations);
		loggingSumary();
		printPopulation();
		printByproduct();
		printExecutions(false);
	}
	
	public void printPopulation() {
		writer.fine("--------- Iteration: " + iterations + "----------------");
		for (S solution : population) {
			String pattern = "{%2d} fitness: %.32e, Arrival Times: %s";
			String str = String.format(pattern, ((TimeListSolution) solution).ID, ((TimeListSolution) solution).getObjective(0), ((TimeListSolution) solution).getLineVariableValueString(20));
			writer.fine(str);
			
		}
	}
	
	public void printCandidates(List<S> solutions) {
		// Data
		for (S solution: solutions) {
			double value = ((TimeListSolution)solution).getObjective(0);
			candidatesWriter.print(String.format("%d,%.32e\n", iterations+1, value));
		}
	}
	
	public void initByproduct(){
		StringBuilder sb = new StringBuilder();
		sb.append("Iteration,");
		for(int num=0; num<problem.getNumberOfVariables(); num++){
			sb.append(String.format("Task%2s,",num+1));
		}
		byproduct.info(sb.toString());
	}
	
	public void printByproduct() {
		StringBuilder sb = new StringBuilder();
		sb.append(iterations);
		sb.append(",");
		
		String text = ((TimeListSolution)population.get(0)).getByproduct();
		sb.append(text);
		byproduct.info(sb.toString());
	}
	
	public void printExecutions(boolean _init){
		TimeListSolution solution = (TimeListSolution)population.get(0);
		String executionsStr = solution.getDetailExecution();
		String[] lines = executionsStr.split("\n");
		
		for(int x=0; x<lines.length; x++){
			if (lines[x].length()==0) continue;
			if (x==0){
				if (_init==false) continue;
				detailWriter.info("Iteration," + lines[x]);
			}
			else{
				detailWriter.info(iterations + "," + lines[x]);
			}
		}
	}
	
	/*******************************************
	 * Related showing results
	 *******************************************/
	List<Collection> summaries = null;
	
	private void initSummary() {
		summaries = new ArrayList<Collection>();
		for (int x = 0; x < problem.getNumberOfObjectives(); x++) {
			summaries.add(new ArrayList<SummaryItem>());
		}
		
	}
	
	private void loggingSumary() {
		Collections.sort(population, comparator);
		for (int objIdx = 0; objIdx < problem.getNumberOfObjectives(); objIdx++) {
			double avgFitness = 0.0;
			for (S solution : population) {
				avgFitness = avgFitness + ((TimeListSolution)solution).getObjective(objIdx);
			}
			avgFitness = avgFitness / population.size();
			summaries.get(objIdx).add(new SummaryItem(((TimeListSolution)population.get(0)).getObjective(objIdx), avgFitness));
		}
	}
	
	public List<Collection> getSummaries() {
		return summaries;
	}
}
