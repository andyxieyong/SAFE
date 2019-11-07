package lu.uni.svv.StressTesting.search;

import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.search.update.Phase1Loader;
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

public class TestDataGenerator {
	/**********************************
	 * non-static methods
	 *********************************/
	TestingProblem problem;
	Constructor constructor = null;
	ScriptEngine engine = null;
	String filename = null;
	
	public TestDataGenerator() throws Exception{
		// Settings update
		// To vary WCET values N_SAMPLE_WCET should be over 0
		// And the data for test does not need to multiple fitness value
		Settings.N_SAMPLE_WCET=1;   // Scheduling option:
		
		//load Testing Problem
		File inputFile = new File(String.format("%s/inputs/reduced_run%02d.csv",Settings.BASE_PATH, Settings.RUN_NUM));
		if (inputFile.exists()){
			Settings.INPUT_FILE = inputFile.getPath();
		}
		else {
			Settings.INPUT_FILE = String.format("%s/input.csv",Settings.BASE_PATH);
		}
		
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
		
		// Initialize model
		this.setting_environment();
		
		// Make file name to save the points
		StringBuilder sb = new StringBuilder();
		if (!Settings.BASE_PATH.endsWith("/")) sb.append("/");
		if (Settings.WORKNAME.length()!=0){
			sb.append(Settings.WORKNAME);
			sb.append("/");
		}
		sb.append("testdata");
		sb.append(String.format("_run%02d", Settings.RUN_NUM));
		if (Settings.RUN_PARTITION !=0){
			sb.append(String.format("_part%d", Settings.RUN_PARTITION));
		}
		this.filename = sb.toString();
	}
	
	/**
	 * Run the second phase
	 * @throws IOException
	 */
	public void run() throws IOException {
		
		// Load Solutions
		Phase1Loader phase1 = new Phase1Loader(problem);
		List<TimeListSolution> solutions = phase1.loadSolutions(Settings.BASE_PATH, Settings.RUN_NUM);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + Settings.BASE_PATH);
			return;
		}
		
		// Preparing the file to save results
		GAWriter writer = new GAWriter(filename, Level.INFO, null, Settings.BASE_PATH, false);
		writer.info(getHeader("result"));
		
		int cnt = 0;
		int solID = 0;
		int cntNegative = 0;
		int cntPosivive = 0;
		while (cnt < Settings.N_EXAMPLE_POINTS) {
			
			List<long[]> samples = sampling_byRandom(1);
			
			int D = this.evaluate(solutions.get(solID), samples.get(0));
			merge_to_training_data(new int[]{D});
			
			cnt++;
			if (D==0) cntPosivive += 1;
			if (D==1) cntNegative += 1;
			
			// Save information
			String text = this.createSampleLine(D, samples.get(0));
			
			writer.info(text);
			JMetalLogger.logger.info(String.format("Evaluated data %d (P: %d, N: %d) with sol %d - %s", cnt, cntPosivive, cntNegative, solID, text));
			
			solID = (solID + 1) % solutions.size();
		}
		writer.close();
		JMetalLogger.logger.info(String.format("Finished to generate %d test points", cnt));
	}
	
	public boolean setting_environment() {
		try {
			engine.eval("library('org.renjin.cran:MASS')");
			engine.eval("library('org.renjin.cran:MLmetrics')");
			engine.eval("UNIT<- 1");
			engine.eval(String.format("TIME_QUANTA<- %.2f", Settings.TIME_QUANTA));
			engine.eval(String.format("RESOURCE_FILE<- \"%s\"", Settings.INPUT_FILE));
			engine.eval("TASK_INFO <- read.csv(file=RESOURCE_FILE, header = TRUE)");
			engine.eval("TASK_INFO<- data.frame(ID = c(1:nrow(TASK_INFO)), TASK_INFO)");
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
			engine.eval("source(\"R/libs/lib_quadratic.R\")");
			
			engine.eval("test_set <- data.frame()");
		}
		catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public String getHeader(String _label){
		StringBuilder sb = new StringBuilder();
		try{
			StringVector vector = (StringVector) engine.eval("get_uncertain_tasks()");
			String[] strs = vector.toArray();
			
			sb.append(_label);
			for (int i = 0; i < strs.length; i++) {
				sb.append(",");
				sb.append(strs[i]);
			}
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return null;
		}
		return sb.toString();
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
	public int evaluate(TimeListSolution _solution, long[] _sample){
		int result = 0;
		
		List<Integer> uncertainTasks = problem.getUncertainTasks();
		HashMap<Integer, Long> sampleMap = new HashMap<Integer, Long>();
		for(int x=0; x<uncertainTasks.size(); x++) {
			sampleMap.put(uncertainTasks.get(x), _sample[x]);
		}
		
		try {
			// Create Scheduler instance
			Object[] parameters = {problem, Settings.TARGET_TASKS};
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