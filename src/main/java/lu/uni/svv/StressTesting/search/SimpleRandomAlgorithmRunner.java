package lu.uni.svv.StressTesting.search;

import lu.uni.svv.StressTesting.datatype.SummaryItem;
import lu.uni.svv.StressTesting.search.model.*;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.Settings;
import org.apache.commons.io.FileUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class SimpleRandomAlgorithmRunner {
	
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
		JMetalLogger.logger.info("Loaded problem");
		
		// experiment
		for (int run = 0; run < Settings.GA_RUN_MAX; run++) {
			
			experiment( run, problem);
			
			unionSummary(problem.getNumberOfObjectives());
		}
		
		// move final result to another location.
		if (Settings.EXPORT_PATH.compareTo("")!=0)
			moveResults();
	}
	 
	public static void experiment(int run, TestingProblem problem)
	{
		JMetalLogger.logger.info("Started algorithem run "+(run+1));
		
		// Configuration of GA algorithm
		SimpleRandomAlgorithm<TimeListSolution> algorithm =
				 new SimpleRandomAlgorithm<TimeListSolution>(	Settings.BASE_PATH,
	                                            String.format("%02d", (run+1)),
	                                            problem,
												 Settings.GA_MAX_ITERATION,
												 Settings.GA_POPULATION);
		
		AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

		// Get result
		long computingTime = algorithmRunner.getComputingTime() ;
		TimeListSolution solution = algorithm.getResult();
		
		// print results
		printDetails(solution, run);
		printSummary(algorithm.getSummaries(), run, Settings.GA_MAX_ITERATION);
		
		// logging some information
		JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
		JMetalLogger.logger.info(String.format("Fitness: %.32e", solution.getObjectiveDecimal(0)));
		JMetalLogger.logger.info("Size of Solution: " + getVariableSize(solution, run)) ;
		
		System.gc();
	}
	
	public static void printDetails(TimeListSolution solution, int run)
	{
		String deadlineStr = solution.getDeadlines();
		GAWriter writer = new GAWriter(String.format("deadlines/deadlines_run%02d.csv", run+1), Level.FINE,  null, Settings.BASE_PATH);
		writer.info(deadlineStr);
		writer.close();
		
		writer = new GAWriter(String.format("solutions/solutions_run%02d.json", run+1), Level.FINE,  null, Settings.BASE_PATH);
		writer.info(solution.getVariableValueString());
		writer.close();
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
	
	public static void printSummary(List<Collection> summary, int run, int maxIterations)
	{
		for (int objIDX=0; objIDX<summary.size(); objIDX++) 
		{
			List<SummaryItem> items = (List<SummaryItem>)summary.get(objIDX);
			
			GAWriter writer = new GAWriter(String.format("results/result_obj%02d_run%02d.csv", objIDX, run+1), Level.INFO,  null, Settings.BASE_PATH);

			// Title
			StringBuilder sb = new StringBuilder("evaluation,");
			sb.append(String.format("Run %02d\n", run+1));
			
			// Data
			for (int iter=0; iter<maxIterations; iter++) {
				sb.append((iter+1)+",");
				sb.append(String.format("%.32e\n", items.get(iter).BestFitness));
			}
			writer.info(sb.toString());
			writer.close();
		} // for objIdx
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
		
		GAWriter writer = new GAWriter("settings.txt", Level.FINE,  null, Settings.BASE_PATH);
		writer.info(Settings.getString());
		writer.close();
		System.out.print(Settings.getString());
	}
	public static void unionSummary(int numObjectives) throws Exception
	{
		// open Directory
		File dir = new File(Settings.BASE_PATH + "/results");
		if (dir.exists() == false) {
			throw new IOException("Failed to create union results");
		}
		File[] results = dir.listFiles();
		
		for(int objIDX=0; objIDX<numObjectives; objIDX++) {

			// find target files.
			List<File> targets = new ArrayList<File>();
			for (File result:results)
			{
				String name = result.getName();
				int idx = name.indexOf("obj");
				String objStr = name.substring(idx+3,name.indexOf("_", idx)); 
				
				if (objIDX == Integer.parseInt(objStr)) targets.add(result);
			}
			Collections.sort(targets);
			
			List<Collection> valuesList = new ArrayList<Collection>();
			List<String> names = new ArrayList<String>();
			
			// reading Files
			for (File result:targets) {
				BufferedReader br = new BufferedReader(new FileReader(result));
				String line = br.readLine();
				
				String[] cols = line.split(",");
				names.add(cols[1]);
				List<BigDecimal> values = new ArrayList<BigDecimal>();
				while(true) {
					line = br.readLine();
					if (line == null || line.compareTo("")==0) break;
					cols = line.split(",");
					values.add(new BigDecimal(cols[1]));
				}
				valuesList.add(values);
			}
			
			//
			GAWriter writer = new GAWriter(String.format("result_runs_obj%02d.csv", objIDX), Level.INFO,  null, Settings.BASE_PATH);
			
			// print title
			StringBuilder sb = new StringBuilder();
			sb.append("Iteration,");
			for(int x=0; x<names.size(); x++) {
				sb.append(names.get(x));
				if (x != names.size()-1)
					sb.append(",");
			}
			//sb.append(",,Average");
			writer.info(sb.toString());
			
			
			for (int iter=0; iter<valuesList.get(0).size(); iter++)
			{
				sb = new StringBuilder((iter+1)+",");
				
				BigDecimal average = new BigDecimal("0.0");
				for (int r=0; r< valuesList.size(); r++) {
					BigDecimal value = ((List<BigDecimal>)valuesList.get(r)).get(iter);
					sb.append(String.format("%.32e", value));
					
					//average = average.add(value);
					if (r != valuesList.size())
						sb.append(",");
				}
				//average = average.divide(new BigDecimal(valuesList.size()));
				//sb.append(String.format(",%d,%.32e", ev, average));
				
				writer.info(sb.toString());
			}

		} // for objIDX
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
	
}
