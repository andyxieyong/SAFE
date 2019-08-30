package lu.uni.svv.StressTesting.search;

import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.StringVector;
import org.renjin.sexp.Vector;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import static java.nio.file.StandardOpenOption.*;


public class TestDataGenerator {
	/**********************************
	 * non-static methods
	 *********************************/
	String basePath;
	TestingProblem problem;
	Constructor constructor = null;
	ScriptEngine engine = null;
	int[] targetTasks = null;
	
	public TestDataGenerator() throws Exception{
		basePath = Settings.BASE_PATH;
		
		// Settings update
		if(Settings.N_SAMPLE_WCET==0) Settings.N_SAMPLE_WCET=1;   // Scheduling option:
		targetTasks = convertToIntArray(Settings.TARGET_TASKLIST);
		Settings.INPUT_FILE = Settings.BASE_PATH + String.format("/Task%02d/input.csv", targetTasks[0]);
		
		//load Testing Problem
		problem = new TestingProblem(Settings.INPUT_FILE, Settings.TIME_QUANTA, Settings.TIME_MAX, Settings.SCHEDULER);
		JMetalLogger.logger.info("Loaded problem");
		
		// Create Scheduler instance
		try {
			Class c = this.getClass();
			c.getPackage();
			Class schedulerClass = Class.forName("lu.uni.svv.StressTesting.scheduler." + Settings.SCHEDULER);
			
			this.constructor = schedulerClass.getConstructors()[0];
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		
		// create a Renjin engine:
		JMetalLogger.logger.info("Loading R module....");
		RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
		this.engine = factory.getScriptEngine();
		JMetalLogger.logger.info("Loading R module....Finished");
	}
	
	public int[] convertToIntArray(String commaSeparatedStr)	{
		
		if (commaSeparatedStr.startsWith("["))
			commaSeparatedStr = commaSeparatedStr.substring(1);
		if (commaSeparatedStr.endsWith("]"))
			commaSeparatedStr = commaSeparatedStr.substring(0,commaSeparatedStr.length()-1);
		
		String[] commaSeparatedArr = commaSeparatedStr.split("\\s*,\\s*");
		int[] result = new int[commaSeparatedArr.length];
		for(int x=0; x<commaSeparatedArr.length; x++){
			result[x] = Integer.parseInt(commaSeparatedArr[x]);
		}
		return result;
	}
	
	/**
	 * Run the second phase
	 * @throws IOException
	 */
	public void run() throws IOException {
		
		Phase1Loader phase1 = new Phase1Loader(problem, targetTasks);
		
		// Load Solutions
		List<TimeListSolution> solutions = phase1.loadMultiTaskSolutions(this.basePath, Settings.BEST_RUN);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + this.basePath);
			return;
		}
		
		// create evaluation data
		StringBuilder sb = new StringBuilder();
		int x=0;
		for(; x<targetTasks.length-1; x++){
			sb.append(targetTasks[x]);
			sb.append(",");
		}
		sb.append(targetTasks[x]);
		
		String appendix = "";
		if (Settings.GA_RUN !=0){
			appendix = String.format("_S%d", Settings.GA_RUN);
		}
		
		String evalfile = String.format("%s/testdata_T%s_N%d_run%02d%s.csv", Settings.LR_WORKPATH, sb.toString(), Settings.MAX_ITERATION, Settings.BEST_RUN, appendix);
		
		// Initialize model
		this.setting_environment();
		
		
		JMetalLogger.logger.info("Evaluating model ...");
		int solID = 0;
		int cntNegative = 0;
		int cntPosivive = 0;

		while (cntPosivive < Settings.MAX_ITERATION) {
			
			List<long[]> samples = sampling_byRandom(1);
			
			int taskID = targetTasks[solID/Settings.GA_POPULATION];
			int D = this.evaluate(solutions.get(solID), taskID, samples.get(0));
			merge_to_training_data(new int[]{D});
			
			if (D==0) cntPosivive += 1;
			if (D==1) cntNegative += 1;
			
			// Save information
			String text = this.createSampleLine(D, samples.get(0));
			JMetalLogger.logger.info(String.format("Evaluated data (P: %d/%d, N: %d/%d) - %s", cntPosivive, Settings.MAX_ITERATION, cntNegative, Settings.MAX_ITERATION, text));
			
			solID = (solID + 1) / Settings.GA_POPULATION;
		}
		
		//Save all points
		savePoints(evalfile);
		
	}
	
	public boolean setting_environment() {
		try {
			engine.eval("library('org.renjin.cran:MASS')");
			engine.eval("library('org.renjin.cran:MLmetrics')");
			engine.eval("UNIT<- 1");
			engine.eval(String.format("TIME_QUANTA<- %.2f", Settings.TIME_QUANTA));
			engine.eval(String.format("RESOURCE_FILE<- \"%s\"", Settings.INPUT_FILE));
			engine.eval("TASK_INFO <- read.csv(file=RESOURCE_FILE, header = TRUE)");
			engine.eval("TASK_INFO<- data.frame(ID = c(1:34), TASK_INFO)");
			engine.eval("colnames(TASK_INFO)<- c(\"ID\", \"NAME\", \"TYPE\", \"PRIORITY\", \"WCET.MIN\", \"WCET.MAX\", \"PERIOD\", \"INTER.MIN\", \"INTER.MAX\", \"DEADLINE\")");
			String update_time_str =
					"TASK_INFO$WCET.MIN = as.integer(round(TASK_INFO$WCET.MIN/TIME_QUANTA))\n" +
					"TASK_INFO$WCET.MAX = as.integer(round(TASK_INFO$WCET.MAX/TIME_QUANTA))\n" +
					"TASK_INFO$PERIOD = as.integer(round(TASK_INFO$PERIOD/TIME_QUANTA))\n" +
					"TASK_INFO$INTER.MIN = as.integer(round(TASK_INFO$INTER.MIN/TIME_QUANTA))\n" +
					"TASK_INFO$INTER.MAX = as.integer(round(TASK_INFO$INTER.MAX/TIME_QUANTA))\n" +
					"TASK_INFO$DEADLINE = as.integer(round(TASK_INFO$DEADLINE/TIME_QUANTA))";
			engine.eval(update_time_str);
			String func = "get_uncertain_tasks<-function(){\n" +
					"    diffWCET <- TASK_INFO$WCET.MAX - TASK_INFO$WCET.MIN\n" +
					"    tasks <- c()\n" +
					"    for(x in 1:length(diffWCET)){\n" +
					"       if (diffWCET[x] <= 0) next\n" +
					"       tasks <- c(tasks, sprintf(\"T%d\", x))\n" +
					"    }\n" +
					"    return(tasks)\n" +
					"}\n";
			engine.eval(func);
			engine.eval("source(\"R/LogisticRegressionN/lib_quadratic.R\")");
			
			engine.eval("test_set <- data.frame()");
			
		}
		catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	
	public long[] get_row_longlist(String varName) throws ScriptException, EvalException {
		long[] items = null;
		
		Vector dataVector = (Vector)engine.eval(varName);
		items = new long[dataVector.length()];
		for(int x=0; x<dataVector.length(); x++){
			items[x] = (long)dataVector.getElementAsInt(x); //dataVector.getElementAsInt(x);
		}
		return items;
	}
	
	public List<long[]> sampling_byRandom(int nSample){
		
		// sampled_data <- get_random_sampling(training, nSample=1)
		List<long[]> samples = new ArrayList<long[]>();
		try {
			engine.eval("tnames <- get_uncertain_tasks()");
			engine.eval(String.format("sampled_data <- get_random_sampling(tnames, nSample=%d)", nSample));
			for(int x=1; x<=nSample; x++){
				samples.add(get_row_longlist(String.format("sampled_data[%d,]",x)));
			}
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return null;
		}
		return samples;
	}


	/**
	 * Evaluation a sample with a TimeListSolution in Scheduler
	 * @param _solution
	 * @param _sample
	 * @return
	 */
	public int evaluate(TimeListSolution _solution, int _taskFitness, long[] _sample){
		int result = 0;
		
		List<Integer> uncertainTasks = problem.getUncertainTasks();
		HashMap<Integer, Long> sampleMap = new HashMap<Integer, Long>();
		for(int x=0; x<uncertainTasks.size(); x++) {
			sampleMap.put(uncertainTasks.get(x), _sample[x]);
		}
		
		try {
			// Create Scheduler instance
			Object[] parameters = {problem, _taskFitness};
			RMScheduler scheduler = (RMScheduler)constructor.newInstance(parameters);
			
			scheduler.initialize();
			scheduler.setSamples(sampleMap);
			scheduler.run(_solution);
			
			result = scheduler.hasDeadlineMisses()?1:0;
			
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public boolean merge_to_training_data(int[] answers){
		// generate list of evaluated result
		StringBuilder sb = new StringBuilder();
		for(int x=0; x<answers.length; x++){
			if (x==0) sb.append(answers[x]);
			else{
				sb.append(",");
				sb.append(answers[x]);
			}
		}
		
		try {
			engine.eval(String.format("test_set.item <- data.frame(result=c(%s), sampled_data)", sb.toString()));
			engine.eval("test_set <- rbind(test_set, test_set.item) ");
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public void savePoints(String _filename){
		String filepath = String.format("%s/%s", basePath, _filename);
		File file = new File(filepath);
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		String savecode = String.format("write.table(samples, \"%s\", append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)",filepath);
		try {
			engine.eval("positive<-test_set[test_set$result==0,]");
			engine.eval("negative<-test_set[test_set$result==1,]");
			engine.eval(String.format("negative<-negative[sample(nrow(negative),%d),]",Settings.MAX_ITERATION));
			engine.eval("samples<-rbind(positive, negative)");
			engine.eval(savecode);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}
	
	public String createSampleLine(int _result, long[] _sample){
		StringBuilder sb = new StringBuilder();
		sb.append(_result);
		for (int i=0; i<_sample.length; i++){
			sb.append(",");
			sb.append(_sample[i]);
		}
		return sb.toString();

	}
	
	/**********************************
	 * static methods
	 *********************************/
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
	
	/**
	 * Start function of second phase
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// Logger Setting
		JMetalLogger.logger.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(formatter);
		JMetalLogger.logger.addHandler(handler);
		Settings.update(args);
		
		
		TestDataGenerator generator = new TestDataGenerator();
		generator.run();
	}
}