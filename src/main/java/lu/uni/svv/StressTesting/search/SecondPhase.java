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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;



public class SecondPhase {
	
	/**********************************
	 * non-static methods
	 *********************************/
	String basePath;
	TestingProblem problem;
//	RMScheduler scheduler = null;
	Constructor constructor = null;
	ScriptEngine engine = null;
	int[] targetTasks = null;
	String filename = "";
	
	public SecondPhase() throws Exception{
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
	
	public int[] convertToIntArray(String commaSeparatedStr)
	{
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
	 * @param _run_type: "random" or "distance"
	 * @param _updateIteration
	 * @param _maxIteration
	 * @param _probability
	 * @throws IOException
	 */
	public void run(String _run_type, int _updateIteration, int _maxIteration, double _probability) throws IOException {
		// Print settings
		JMetalLogger.logger.info("Settings.BEST_RUN in Phase 1: "+ Settings.BEST_RUN);
		JMetalLogger.logger.info("Settings.RUNID in Phase 2 : "+ Settings.GA_RUN);
		JMetalLogger.logger.info("Settings.LR_WORKPATH      : "+ Settings.LR_WORKPATH);
		JMetalLogger.logger.info("Settings.TEST_DATA        : "+ Settings.TEST_DATA);
		JMetalLogger.logger.info("Settings.TEST_NSAMPLES    : "+ Settings.TEST_NSAMPLES);
		
		JMetalLogger.logger.info("Settings.STOP_DATA_TYPE  : "+ Settings.STOP_DATA_TYPE);
		JMetalLogger.logger.info("Settings.STOP_FUNC_NAME  : "+ Settings.STOP_FUNCTION_NAME);
		JMetalLogger.logger.info("Settings.STOP_CONDITION  : "+ Settings.STOP_CONDITION);
		
		
		
		Phase1Loader phase1 = new Phase1Loader(problem, targetTasks);
		
		// Load Solutions
		List<TimeListSolution> solutions = phase1.loadMultiTaskSolutions(this.basePath, Settings.BEST_RUN);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + this.basePath);
			return;
		}
		
		String appendix = "";
		if (Settings.TEST_NSAMPLES != 0)
			appendix = String.format("_T%d", Settings.TEST_NSAMPLES);
		
		if (Settings.GA_RUN != 0)
			appendix = String.format("_P2run%d", Settings.GA_RUN);
		
		
		// Setting initial points to use for learning
		String formulaCode = Settings.LR_FORMULA_PATH.replace("/", "_");
		filename = String.format("workdata_%s_%d_%d_%.2f_%s_run%02d%s", _run_type, _maxIteration, _updateIteration, _probability, formulaCode, Settings.BEST_RUN, appendix);
		String workfile = String.format("%s/%s.csv", Settings.LR_WORKPATH, filename);
		
		if (!phase1.makeInitialPoints(this.basePath, Settings.EXPORT_PATH, Settings.BEST_RUN, workfile)) {
			JMetalLogger.logger.info("Failed to load input data");
			return;
		}
		JMetalLogger.logger.info("Started second phase");

		// Initialize model
		this.setting_environment();
		
		if (!this.initialize_model(workfile, Settings.LR_FORMULA_PATH, Settings.LR_INITIAL_SIZE)){
			JMetalLogger.logger.info("Error to initialze model");
			return;
		}
		JMetalLogger.logger.info("Generated initial model");
		
		// Sampling new data point and evaluation
		int count = 0;
		int solID = 0;
		boolean meetCondition = false;
		while (count < _maxIteration) {
			// Learning model again with more data
			if ((count != 0) && (count % _updateIteration == 0)) {
				JMetalLogger.logger.info("update logistic regression " + count + "/" + _maxIteration);
				this.update_model();
				boolean check = this.check_stop_update();
				if (Settings.STOP_CONDITION && check){
					meetCondition = check;
					break;
				}
			}
			
			//Sampling
			long[] sampleWCET = null;
			if (_run_type.compareTo("random") == 0) {
				sampleWCET = this.sampling_byRandom(1);
			} else {
				sampleWCET = this.sampling_byDistance(1, Settings.SAMPLE_CANDIDATES, _probability);
			}
			//Evaluate using scheduler
			int taskID = targetTasks[solID/Settings.GA_POPULATION];
			int D = this.evaluate(solutions.get(solID), taskID, sampleWCET);
			merge_to_training_data(new int[]{D});
			
			
			// Save information
			String text = this.createSampleLine(D, sampleWCET);
			this.appendNewDataset(workfile, text);
			JMetalLogger.logger.info(String.format("New data %d/%d: %s", (count % _updateIteration) + 1, _updateIteration, text));
			
			// update index
			solID = (solID + 1) % solutions.size();
			count += 1;
		} //while
		
		if (meetCondition == false) {
			JMetalLogger.logger.info("update logistic regression " + count + "/" + _maxIteration);
			this.update_model();
			this.check_stop_update();
		}
		this.finalize_phase2();
		JMetalLogger.logger.info("Finished to run");
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
			engine.eval("source(\"R/LogisticRegressionN/lib_quadratic.R\")");
			engine.eval("source(\"R/LogisticRegressionN/lib_RQ3.R\")");
			
			engine.eval("get_uncertain_tasks<-function(){\n" +
							"    diffWCET <- TASK_INFO$WCET.MAX - TASK_INFO$WCET.MIN\n" +
							"    tasks <- c()\n" +
							"    for(x in 1:length(diffWCET)){\n" +
							"       if (diffWCET[x] <= 0) next\n" +
							"       tasks <- c(tasks, sprintf(\"T%d\",as.integer(x)))\n" +
							"    }\n" +
							"    return(tasks)\n" +
							"}\n");
		}
		catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	
	/**
	 *
	 * @param filepath   'results/20190722_FirstPhase_Ex30_s20_GASearch/Task23/samples/
	 * @param formulaPath
	 * @return
	 */
	public boolean initialize_model(String filepath, String formulaPath, int initialTrainingSize){
		
		//Load formula from formulaPath
		String formula="";
		try{
			if (formulaPath.length()==0)
				formulaPath = String.format("%s/formula/T%s_S%d_run%02d", Settings.BASE_PATH, Settings.TARGET_TASKLIST, Settings.N_SAMPLE_WCET, Settings.BEST_RUN);
			formula = new String(Files.readAllBytes(Paths.get(formulaPath)), StandardCharsets.UTF_8).trim();
			JMetalLogger.logger.info("Loaded formula from " + formulaPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		Object[] samples = null;
		try {
			// load data
			engine.eval(String.format("datafile<- \"%s/%s\"", Settings.EXPORT_PATH, filepath));
			engine.eval("training <- read.csv(datafile, header=TRUE)");
			
			if (Settings.TEST_DATA.length()!=0){
				engine.eval(String.format("test_data <-read.csv(sprintf(\"%s/testdata/%s\"), header=TRUE)", basePath, Settings.TEST_DATA));
				engine.eval("positive <-test_data[test_data$result==0,]");
				engine.eval("negative <-test_data[test_data$result==1,]");
				engine.eval(String.format("positive <-positive[sample(nrow(positive), %d),]",Settings.TEST_NSAMPLES/2));
				engine.eval(String.format("negative <-negative[sample(nrow(negative), %d),]",Settings.TEST_NSAMPLES/2));
				engine.eval("test_data <-rbind(positive, negative)");
				Vector dataVector = (Vector)engine.eval("nrow(positive)");
				int nPositive = dataVector.getElementAsInt(0);
				dataVector = (Vector)engine.eval("nrow(negative)");
				int nNegative = dataVector.getElementAsInt(0);
				JMetalLogger.logger.info("Test Data (positive): " + nPositive);
				JMetalLogger.logger.info("Test Data (negative): " + nNegative);
			}
			
			if (Settings.STOP_DATA_TYPE.compareTo("initial")==0){
				engine.eval("half_size <-nrow(training)/2");
				engine.eval("testPool <-training[(half_size+1):nrow(training),]");
				engine.eval("training <-training[1:half_size,]");
				
				engine.eval("positive <-testPool[testPool$result==0,]");
				engine.eval("negative <-testPool[testPool$result==1,]");
				engine.eval("negative <-negative[sample(nrow(negative),nrow(positive)),]");
				engine.eval("termination_data <- rbind(positive, negative)");
				Vector dataVector = (Vector)engine.eval("nrow(positive)");
				int nPositive = dataVector.getElementAsInt(0);
				dataVector = (Vector)engine.eval("nrow(negative)");
				int nNegative = dataVector.getElementAsInt(0);
				JMetalLogger.logger.info("Test Data (positive): " + nPositive);
				JMetalLogger.logger.info("Test Data (negative): " + nNegative);
			}
			else if (Settings.STOP_DATA_TYPE.compareTo("pool")==0){
				engine.eval("termination_data <- test_data");
			}
			
			if (initialTrainingSize!=0){
				// sampling and save the result to the datafile
				engine.eval(String.format("training <- training[sample(nrow(training), %d), ]",initialTrainingSize));  //
				engine.eval("write.table(training, datafile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
			}
			
			// learning logistic regression with simple formula
			JMetalLogger.logger.info("Started logistic regression...");
			
			engine.eval(String.format("formula_str<- \"%s\"",formula));
			engine.eval("base_model <- glm(formula = formula_str, family = \"binomial\", data = training)");
			engine.eval("cntUpdate <- 0");

			if (Settings.TEST_DATA.length()!=0) {
				engine.eval(String.format("test.results <- calculate_metrics(base_model, test_data, %.2f, cntUpdate)", Settings.BORDER_PROBABILITY));
			}
			
			
			JMetalLogger.logger.info("Initialized model: " + getModelText("base_model"));
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
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
			engine.eval(String.format("training.item <- data.frame(result=c(%s), sampled_data)", sb.toString()));
			engine.eval("training <- rbind(training, training.item) ");
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean update_model(){
		boolean result=true;
		try {
			engine.eval("print(sprintf(\"Number of data set: %d\", nrow(training)))");
			engine.eval("prev_model<-base_model");
			engine.eval("base_model <- glm(formula = prev_model$formula, family = \"binomial\", data = training)");
			engine.eval("cntUpdate <- cntUpdate + 1");
			JMetalLogger.logger.info("Updated model: " + getModelText("base_model"));
			
			
			if (Settings.STOP_DATA_TYPE.compareTo("training")==0)
				engine.eval("termination_data<-training");
			else if (Settings.STOP_DATA_TYPE.compareTo("new")==0) {
				engine.eval("tSize<-nrow(training)");
				engine.eval(String.format("termination_data<-training[(tSize-%d):tSize,]", Settings.UPDATE_ITERATION));
			}
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return result;
	}
	
	public boolean check_stop_update(){
		boolean result=false;
		try {
			if (Settings.TEST_DATA.length()!=0) {
				engine.eval(String.format("test.result <- calculate_metrics(base_model, test_data, %.2f, cntUpdate)", Settings.BORDER_PROBABILITY));
				engine.eval("test.results <- rbind(test.results, test.result)");
				engine.eval("print(test.result)");
			}
			
			String test = "";
			test = String.format("terminate.value <- test.%s(prev_model, base_model, testData=termination_data, predictionLevel=%.2f)",
					Settings.STOP_FUNCTION_NAME, Settings.BORDER_PROBABILITY);
			engine.eval(test);
			

			Vector dataVector = (Vector)engine.eval("terminate.value");
			double test_result = dataVector.getElementAsDouble(0);
			JMetalLogger.logger.info("TEST_FUNC_RESULT: " + test_result);
			if (test_result <= Settings.STOP_ACCEPT_RATE)
				result = true;
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return result;
	}
	
	public boolean finalize_phase2(){
		boolean result=false;
		try {
			if (Settings.TEST_DATA.length()!=0) {
				engine.eval(String.format("testfile<- \"%s/%s/%s_test_data.csv\"", Settings.EXPORT_PATH, Settings.LR_WORKPATH, filename));
				engine.eval(String.format("resultfile<- \"%s/%s/%s_test_result.csv\"", Settings.EXPORT_PATH, Settings.LR_WORKPATH, filename));
				engine.eval("write.table(test_data, testfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
				engine.eval("write.table(test.results, resultfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
			}
			result = true;
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return result;
	}
	
	/*********************
	 *
	 */
	
	/**
	 * convert R variable to Java long[]
	 * @param varName
	 * @return
	 * @throws ScriptException
	 * @throws EvalException
	 */
	public long[] get_row_longlist(String varName) throws ScriptException, EvalException {
		long[] items = null;
		
		Vector dataVector = (Vector)engine.eval(varName);
		items = new long[dataVector.length()];
		for(int x=0; x<dataVector.length(); x++){
			items[x] = (long)dataVector.getElementAsInt(x); //dataVector.getElementAsInt(x);
		}
		return items;
	}
	
	public long[] sampling_byDistance(int nSample, int nCandidate, double P){
		
		// sampled_data <- get_random_sampling(training, nSample=1)
		long[] samples = null;
		try {
			engine.eval("tnames <- get_task_names(training)");
			String codeText = String.format("sampled_data <- get_distance_sampling(tnames, base_model, nSample=%d, nCandidate=%d, P=%.2f)", nSample, nCandidate, P);
			engine.eval(codeText);
			
			StringVector nameVector = (StringVector)engine.eval("colnames(sampled_data)");
			String[] names = nameVector.toArray();
			
			samples = get_row_longlist("sampled_data");
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return null;
		}
		
		return samples;
	}
	
	public long[] sampling_byRandom(int nSample){
		
		// sampled_data <- get_random_sampling(training, nSample=1)
		long[] samples = null;
		try {
			engine.eval("tnames <- get_task_names(training)");
			String codeText = String.format("sampled_data <- get_random_sampling(tnames, nSample=%d)", nSample);
			engine.eval(codeText);
			
			StringVector nameVector = (StringVector)engine.eval("colnames(sampled_data)");
			String[] names = nameVector.toArray();
			
			samples = get_row_longlist("sampled_data");
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return null;
		}
		
		return samples;
	}
	
	
	public String getModelText(String modelname) throws ScriptException, EvalException {
		StringBuilder sb = new StringBuilder();
		
		StringVector nameVector = (StringVector)engine.eval(String.format("names(%s$coefficients)", modelname));
		String[] names = nameVector.toArray();
		
		Vector dataVector = (Vector)engine.eval(modelname+"$coefficients");
		double[] coeff = new double[dataVector.length()];
		for(int x=0; x<dataVector.length(); x++){
			coeff[x] = dataVector.getElementAsDouble(x);
		}

		sb.append("Y = ");
		sb.append(coeff[0]);
		for(int x=1; x<names.length; x++) {
			sb.append(" + ");
			sb.append(coeff[x]);
			sb.append("*");
			sb.append(names[x]);
		}
		return sb.toString();
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
	
	/**
	 * append evaluated data into data file
	 * @param _filename
	 * @param _datatext
	 */
	public void appendNewDataset(String _filename, String _datatext){
		GAWriter writer = new GAWriter(_filename, Level.INFO, null, Settings.EXPORT_PATH, true);
		writer.info(_datatext);
		writer.close();
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
		
		SecondPhase secondPhase = new SecondPhase();
		secondPhase.run(Settings.SECOND_PHASE_RUNTYPE, Settings.UPDATE_ITERATION, Settings.MAX_ITERATION, Settings.BORDER_PROBABILITY);
	}
	
}