package lu.uni.svv.StressTesting.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import lu.uni.svv.StressTesting.datatype.FitnessList;
import lu.uni.svv.StressTesting.search.model.*;
import org.apache.commons.io.FileUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
import lu.uni.svv.StressTesting.datatype.SummaryItem;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.Settings;


public class SteadyStateGeneticAlgorithmRunner {
	
	public static SimpleFormatter formatter = new SimpleFormatter(){
		private static final String format = "[%1$tF %1$tT] %2$s: %3$s %n";
		
		@Override
		public synchronized String format(LogRecord lr) {
			return String.format(format,
					new Date(lr.getMillis()),
					lr.getLevel().getLocalizedName(),
					lr.getMessage()
			);
		}
	};
	
	public static void main( String[] args ) throws Exception
	{
		// Logger Setting
		JMetalLogger.logger.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(formatter);
		JMetalLogger.logger.addHandler(handler);

		// Environment Settings
		Settings.update(args);
		init();
		
		// problem load
		TestingProblem problem = new TestingProblem(Settings.INPUT_FILE, Settings.TIME_QUANTA, Settings.TIME_MAX, Settings.SCHEDULER);
		String changed = problem.increaseWCET(Settings.INC_TASK_TYPE, Settings.INC_RATE);
		printInput(changed, problem.getInputs());
		JMetalLogger.logger.info("Loaded problem");
		
		// experiment
		for (int run = 0; run < Settings.GA_RUN_MAX; run++) {
			experiment( run,
						problem,
						Settings.GA_POPULATION,
						Settings.GA_MAX_ITERATION,
						Settings.GA_CROSSOVER_PROB,
						Settings.GA_MUTATION_PROB);
		}
		
		// move final result to another location.
		if (Settings.EXPORT_PATH.compareTo("")!=0)
			moveResults();
	}
	
	public static void printInput(String changed, String inputs){
		GAWriter writer = new GAWriter("changed.txt", Level.INFO,  null, Settings.BASE_PATH);
		writer.print(changed);
		writer.close();
		writer = new GAWriter("input.csv", Level.INFO,  null, Settings.BASE_PATH);
		writer.info(inputs);
		writer.close();
	}
	 
	public static void experiment(int run, TestingProblem problem, int populationSize, int maxIterations, double crossoverProbability, double mutationProbability)
	{	 		 
		// Configuration of GA algorithm
		TimeListSolution.initUUID();
		CrossoverOperator<TimeListSolution> crossoverOperator = new SimpleTLCrossover(crossoverProbability);
		MutationOperator<TimeListSolution> mutationOperator = new SimpleTLMutation4(problem, mutationProbability);
		SelectionOperator<List<TimeListSolution>, TimeListSolution> selectionOperator = new BinaryTournamentSelection<TimeListSolution>();

		List<TimeListSolution> bestSolutions = new ArrayList<TimeListSolution>() ;
		
		JMetalLogger.logger.info("Started algorithem run "+(run+1));
		
		SteadyStateGeneticAlgorithm<TimeListSolution> algorithm =
				 new SteadyStateGeneticAlgorithm<TimeListSolution>(	Settings.BASE_PATH,
						                                            String.format("%02d", (run+1)),
						 											problem,
						                                            maxIterations,
																	populationSize, 
						 											crossoverOperator, 
						 											mutationOperator, 
						 											selectionOperator);
		
		AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

		// Get result
		long computingTime = algorithmRunner.getComputingTime() ;
		TimeListSolution solution = algorithm.getResult();
		
		// print results
		if (Settings.N_SAMPLE_WCET==0) {
			printDetails(solution, run);
			if (Settings.PRINT_RESULTS)
				printSummary(algorithm.getSummaries(), run, maxIterations);
		}
		else {
			printFullSolutions(algorithm.getPopulation(), run);
			if (Settings.PRINT_RESULTS)
				printSummaryList(algorithm.getSummaries(), run, maxIterations);
		}
		
		// logging some information
		JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
		JMetalLogger.logger.info("Size of Solution: " + getVariableSize(solution, run)) ;
		
		System.gc();
	}
	
	public static void printDetails(TimeListSolution solution, int run)
	{
		GAWriter writer = new GAWriter(String.format("solutions/solutions_run%02d.json", run+1), Level.FINE, null, Settings.BASE_PATH);
		writer.info(solution.getVariableValueString());
		writer.close();
	}
	
	public static void printFullSolutions(List<TimeListSolution> solutions, int run)
	{
		JMetalLogger.logger.info("Saving populations...");
		
		GAWriter writer = null;
		int idx = 0;
		for(TimeListSolution solution: solutions) {
			idx += 1;
			
			writer = new GAWriter(String.format("solutions/solutions_run%02d_%d.json", run + 1, idx), Level.FINE, null, Settings.BASE_PATH);
			writer.info(solution.getVariableValueString());
			writer.close();
			
			JMetalLogger.logger.info("\t["+idx+"/"+solutions.size()+"] saved");
		}
		JMetalLogger.logger.info("Saving populations...Done");
	}
	
	public static String getVariableSize(TimeListSolution solution, int run) 
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int x=0; x<solution.getNumberOfVariables(); x++) {
			sb.append(solution.getVariableValue(x).size());
			if (x< solution.getNumberOfVariables()-1)
				sb.append(",");
		}
		sb.append("]");
		
		return sb.toString();
	}

	public static void init() {
		File dir = new File(Settings.BASE_PATH);
		if (dir.exists() == true) {
			try {
				FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
				System.out.println("Failed to delete results");
				e.printStackTrace();
			}
		}
		
		GAWriter writer = new GAWriter("settings.txt", Level.FINE, null, Settings.BASE_PATH);
		writer.info(Settings.getString());
		writer.close();
		System.out.print(Settings.getString());
	}
	
	public static void moveResults(){
		File srcDir = new File(Settings.BASE_PATH);
		createParentsDirs(Settings.EXPORT_PATH);
		File destDir = new File(Settings.EXPORT_PATH);
		
		try {
			FileUtils.copyDirectory(srcDir, destDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void createParentsDirs(String path){
		String pathstr = "";
		String[] paths = path.substring(0, path.lastIndexOf("/")).split("/");
		if (paths.length > 1) {
			for(int t=0; t<paths.length-1; t++) {
				pathstr += paths[t]+"/";
				File dir = new File(pathstr);
				if( !(dir.exists()) )
					dir.mkdir();
			}
		}
	}
	
	/*******************************************
	 * Related logging results
	 *******************************************/
	public static void printSummary(List<Collection> summary, int run, int maxIterations)
	{
		for (int objIDX=0; objIDX<summary.size(); objIDX++)
		{
			List<SummaryItem> items = (List<SummaryItem>)summary.get(objIDX);
			
			GAWriter writer = new GAWriter(String.format("results/result_obj%02d_run%02d.csv", objIDX, run+1), Level.INFO, null, Settings.BASE_PATH);
			
			// Title
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("Iteration,Run %02d\n", run+1));
			
			// Data
			for (int iter=0; iter<maxIterations; iter++) {
				sb.append((iter+1));
				sb.append(",");
				sb.append(String.format("%.0f\n", items.get(iter).BestFitness));
			}
			writer.info(sb.toString());
			writer.close();
		} // for objIdx
	}
	
	public static void printSummaryList(List<Collection> summary, int run, int maxIterations)
	{
		for (int objIDX=0; objIDX<summary.size(); objIDX++)
		{
			List<FitnessList> items = (List<FitnessList>)summary.get(objIDX);
			
			GAWriter writer = new GAWriter(String.format("results/result_obj%02d_run%02d.csv", objIDX, run+1), Level.INFO, null, Settings.BASE_PATH);
			
			// Title
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("Iterations,SampleID,Run %02d\n", run+1));
			
			// Data
			for (int iter=0; iter<maxIterations; iter++) {
				FitnessList samples = items.get(iter);
				for (int x=0; x<samples.size(); x++){
					sb.append((iter + 1));
					sb.append(",");
					sb.append(x);
					sb.append(",");
					sb.append(String.format("%.0f\n", samples.get(x) ));
				}
			}
			writer.info(sb.toString());
			writer.close();
		} // for objIdx
	}
}
