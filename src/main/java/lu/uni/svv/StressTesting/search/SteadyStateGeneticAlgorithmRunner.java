package lu.uni.svv.StressTesting.search;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import lu.uni.svv.StressTesting.search.model.*;
import org.apache.commons.io.FileUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
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
		printInput(null, problem.getInputs());
		JMetalLogger.logger.info("Loaded problem");
		
		// experiment
		for (int run = 1; run <= Settings.GA_RUN_MAX; run++) {
			if (Settings.GA_RUN!=0 && run!=Settings.GA_RUN) continue;
			problem.RUN_ID = run;
			experiment( run,
						problem,
						Settings.GA_POPULATION,
						Settings.GA_MAX_ITERATION,
						Settings.GA_CROSSOVER_PROB,
						Settings.GA_MUTATION_PROB);
		}
	}
	
	public static void printInput(String changed, String inputs){
		if (Settings.GA_RUN<=1) {
			// multi run mode and single run mode with runID 1)
			GAWriter writer = new GAWriter("settings.txt", Level.FINE, null, Settings.BASE_PATH);
			writer.info(Settings.getString());
			writer.close();
			System.out.print(Settings.getString());
			
			if (changed != null) {
				writer = new GAWriter("changed.txt", Level.INFO, null, Settings.BASE_PATH);
				writer.print(changed);
				writer.close();
			}
			
			writer = new GAWriter("input.csv", Level.INFO, null, Settings.BASE_PATH);
			writer.info(inputs);
			writer.close();
		}
	}
	 
	public static void experiment(int run, TestingProblem problem, int populationSize, int maxIterations, double crossoverProbability, double mutationProbability)
	{	 		 
		// Configuration of GA algorithm
		TimeListSolution.initUUID();
		CrossoverOperator<TimeListSolution> crossoverOperator = new SimpleTLCrossover(crossoverProbability);
		MutationOperator<TimeListSolution> mutationOperator = new SimpleTLMutation4(problem, mutationProbability);
		SelectionOperator<List<TimeListSolution>, TimeListSolution> selectionOperator = new BinaryTournamentSelection<TimeListSolution>();

		
		JMetalLogger.logger.info("Started algorithem run "+run);
		
		SteadyStateGeneticAlgorithm<TimeListSolution> algorithm =
				 new SteadyStateGeneticAlgorithm<TimeListSolution>(	Settings.BASE_PATH,
						                                            String.format("%02d", run),
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
		}
		else {
			printFullSolutions(algorithm.getPopulation(), run);
		}
		
		// logging some information
		JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
		JMetalLogger.logger.info("Size of Solution: " + getVariableSize(solution, run)) ;
		
		System.gc();
	}
	
	public static void printDetails(TimeListSolution solution, int run)
	{
		GAWriter writer = new GAWriter(String.format("solutions/solutions_run%02d.json", run), Level.FINE, null, Settings.BASE_PATH);
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
			
			writer = new GAWriter(String.format("solutions/solutions_run%02d_%d.json", run, idx), Level.FINE, null, Settings.BASE_PATH);
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
		if (Settings.GA_RUN == 0) {
			// Only apply to multi run mode
			File dir = new File(Settings.BASE_PATH);
			if (dir.exists()) {
				try {
					FileUtils.deleteDirectory(dir);
				} catch (IOException e) {
					System.out.println("Failed to delete results");
					e.printStackTrace();
				}
			}
		}
	}
}
