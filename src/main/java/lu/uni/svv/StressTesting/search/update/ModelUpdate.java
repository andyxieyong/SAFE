package lu.uni.svv.StressTesting.search.update;

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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;


public class ModelUpdate {
	
	String basePath;
	TestingProblem problem;
	
	//	RMScheduler scheduler = null;
	Constructor constructor = null;
	ScriptEngine engine = null;
	int[] targetTasks = null;
	String filename = "";
	
	//Objects
	Phase1Loader phase1 = null;
	
	public ModelUpdate(int[] _targetTasks) throws Exception{
		basePath = Settings.BASE_PATH;
		targetTasks = _targetTasks;
		
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

		// load phase 1 loader
		JMetalLogger.logger.info("Preparing Phase-1 loader...");
		phase1 = new Phase1Loader(problem, targetTasks);
	}
	
	/**
	 * Run the second phase
	 * @throws IOException
	 */
	public void run() throws IOException {
		
		JMetalLogger.logger.info("Loading solutions from phase 1...");
		List<TimeListSolution> solutions = phase1.loadMultiTaskSolutions(this.basePath, Settings.BEST_RUN);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + this.basePath);
			return;
		}
		
		JMetalLogger.logger.info("Loading formula for phase 2...");
		String formula = loadFormula(Settings.LR_FORMULA_PATH);
		if (formula == null) {
			JMetalLogger.logger.info("Failed to load formula in " + Settings.LR_FORMULA_PATH);
			return;
		}
		
		JMetalLogger.logger.info("Initializing initial training data ...");
		String workfile = generateInitialTrainingData();
		if (workfile == null) {
			JMetalLogger.logger.info("Failed to load input data");
			return;
		}
		
		JMetalLogger.logger.info("Initializing R environment ...");
		if (!this.settingEnvironment()) {
			JMetalLogger.logger.info("Failed to load input data");
			return;
		}
		
		JMetalLogger.logger.info("Loading training data ...");
		if (!loadTrainingData(workfile, Settings.LR_INITIAL_SIZE)){
			JMetalLogger.logger.info("Error to load training data");
			return;
		}
		
		if (Settings.TEST_DATA.length() != 0){
			JMetalLogger.logger.info("Loading test data ...");
			if (!this.includeTestData())
			{
				JMetalLogger.logger.info("Error to including test data");
				return;
			}
		}
		
		if (Settings.STOP_CONDITION) {
			JMetalLogger.logger.info("Preparing termination data ...");
			if(!this.prepareTerminationData()) {
				JMetalLogger.logger.info("Failed to load input data");
				return;
			}
		}
		
		JMetalLogger.logger.info("Learning initial model ...");
		if (!this.initializeModel(formula)) {
			JMetalLogger.logger.info("Error to initialze model");
			return;
		}
		if (Settings.TEST_DATA.length() != 0 && !evaluateModel()){
			JMetalLogger.logger.info("Failed to evaluate model with test data");
			return;
		}
		
		JMetalLogger.logger.info("Update model process ...");
		if (!this.update(solutions, workfile)) {
			JMetalLogger.logger.info("Failed to update model process");
			return;
		}
	}
	
	public boolean update(List<TimeListSolution> solutions, String workfile){
		// Sampling new data point and evaluation
		int count = 0;
		int solID = 0;

		while (count <= Settings.MAX_ITERATION) {
			// Learning model again with more data
			if ((count != 0) && (count % Settings.UPDATE_ITERATION == 0)) {
				JMetalLogger.logger.info("update logistic regression " + count + "/" + Settings.MAX_ITERATION);
				this.updateModel();
				if (Settings.TEST_DATA.length() != 0 && !evaluateModel()){
					JMetalLogger.logger.info("Failed to evaluate model with test data in "+ count + "/" + Settings.MAX_ITERATION);
					return false;
				}
				boolean check = this.checkStopCondition();
				if (Settings.STOP_CONDITION && check) break;
			}
			
			//Sampling
			long[] sampleWCET = null;
			if (Settings.SECOND_PHASE_RUNTYPE.compareTo("random")==0){
				sampleWCET = samplingNewPointsRandom(1);
			}
			else{
				sampleWCET = this.samplingNewPoints(1, Settings.SAMPLE_CANDIDATES, Settings.BORDER_PROBABILITY);
			}
			
			//Evaluate using scheduler
			int taskID = targetTasks[solID/Settings.GA_POPULATION];
			int D = this.evaluate(solutions.get(solID), taskID, sampleWCET);
			this.mergeNewSample(new int[]{D});
			
			// Save information
			String text = this.createSampleLine(D, sampleWCET);
			this.appendNewDataset(workfile, text);
			JMetalLogger.logger.info(String.format("New data %d/%d: %s", (count % Settings.UPDATE_ITERATION) + 1, Settings.UPDATE_ITERATION, text));
			
			// update index
			solID = (solID + 1) % solutions.size();
			count += 1;
		} //while
		
		if (Settings.TEST_DATA.length()!=0 && !this.saveTestResults()) {
			JMetalLogger.logger.fine("Failed to save test results");
		}
		if (!Settings.STOP_CONDITION && !this.saveTerminationResults()) {
			JMetalLogger.logger.fine("Failed to save termination results");
		}
		JMetalLogger.logger.info("Finished to run Phase 2");
		return true;
	}
	
	public String generateInitialTrainingData(){
		
		// create file name
		String appendix = "";
		if (Settings.TEST_NSAMPLES != 0)
			appendix = String.format("_T%d", Settings.TEST_NSAMPLES);
		
		if (Settings.GA_RUN != 0)
			appendix = String.format("_P2run%d", Settings.GA_RUN);
		
		
		// Setting initial points to use for learning
		String formulaCode = "";
		if (Settings.LR_FORMULA_PATH.length()!=0){
			formulaCode = Settings.LR_FORMULA_PATH.replace("/", "_");
		}
		
		filename = String.format("workdata_%s_%d_%d_%.2f%s_run%02d%s",
				Settings.SECOND_PHASE_RUNTYPE,
				Settings.MAX_ITERATION,
				Settings.UPDATE_ITERATION,
				Settings.BORDER_PROBABILITY,
				formulaCode,
				Settings.BEST_RUN,
				appendix);
		String workfile = String.format("%s/%s.csv", Settings.LR_WORKPATH, filename);
		
		if (!phase1.makeInitialPoints(this.basePath, Settings.EXPORT_PATH, Settings.BEST_RUN, workfile)) {
			return null;
		}
		return workfile;
	}
	
	public String loadFormula(String formulaPath) {
		String formula = "";
		try {
			if (formulaPath.length() == 0)
				formulaPath = String.format("%s/formula/T%s_S%d_run%02d", Settings.BASE_PATH, Settings.TARGET_TASKLIST, Settings.N_SAMPLE_WCET, Settings.BEST_RUN);
			formula = new String(Files.readAllBytes(Paths.get(formulaPath)), StandardCharsets.UTF_8).trim();
			JMetalLogger.logger.info("Loaded formula from " + formulaPath);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return formula;
	}
	
	public boolean settingEnvironment() {
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
	 * @param inputpath   'results/20190722_P2_S20_GASearch/RQ2_N20000/workdata*.csv
	 * @param initialTrainingSize
	 */
	public boolean loadTrainingData(String inputpath, int initialTrainingSize){
		try {
			// load data
			engine.eval(String.format("datafile<- \"%s/%s\"", Settings.EXPORT_PATH, inputpath));
			engine.eval("training <- read.csv(datafile, header=TRUE)");
			
			if (initialTrainingSize != 0) {
				// sampling and save the result to the datafile
				engine.eval(String.format("training <- training[sample(nrow(training), %d), ]", initialTrainingSize));  //
				engine.eval("write.table(training, datafile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
			}
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 *
	 * @param formula
	 * @return
	 */
	public boolean initializeModel(String formula){
		try {
			// set formula
			engine.eval(String.format("formula_str<- \"%s\"",formula));
			
			// learning logistic regression with simple formula
			engine.eval("base_model <- glm(formula = formula_str, family = \"binomial\", data = training)");
			engine.eval("cntUpdate <- 0");
			
			
			JMetalLogger.logger.info("Initialized model: " + getModelText("base_model"));
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * Sample new points
	 * @param nSample
	 * @param nCandidate
	 * @param P
	 * @return
	 */
	public long[] samplingNewPoints(int nSample, int nCandidate, double P){
		
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
	
	public long[] samplingNewPointsRandom(int nSample){
		// sampling new points by random
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
	
	public boolean mergeNewSample(int[] answers){
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
	
	public boolean updateModel(){
		try {
			engine.eval("print(sprintf(\"Number of data set: %d\", nrow(training)))");
			engine.eval("prev_model<-base_model");
			engine.eval("base_model <- glm(formula = prev_model$formula, family = \"binomial\", data = training)");
			engine.eval("cntUpdate <- cntUpdate + 1");
			JMetalLogger.logger.info("Updated model: " + getModelText("base_model"));
			
			updateTerminationData();
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	
	////////////////////////////////////////////////////////////////////////
	// Related termination condition
	////////////////////////////////////////////////////////////////////////
	public boolean prepareTerminationData() {
		try{
			engine.eval("termination.results <- data.frame()");
			JMetalLogger.logger.info("Termination data is the same with test data");
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean updateTerminationData() {
		return true;
	}
	
	public boolean saveTerminationResults(){
		try{
			engine.eval(String.format("resultfile <- \"%s/%s/%s_termination_result.csv\"", Settings.EXPORT_PATH, Settings.LR_WORKPATH, filename));
			engine.eval("write.table(termination.results, resultfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean checkStopCondition(){
		boolean result=false;
		try {
			String code = "";
			code = String.format("terminate.value <- test.%s(prev_model, base_model, testData=termination_data, predictionLevel=%.2f)",
					Settings.STOP_FUNCTION_NAME, Settings.BORDER_PROBABILITY);
			engine.eval(code);
			engine.eval("termination.results <- rbind(termination.results,  data.frame(nUpdate=cntUpdate, Value=terminate.value))");
			
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
	
	////////////////////////////////////////////////////////////////////////
	// Related test model
	////////////////////////////////////////////////////////////////////////
	public boolean includeTestData(){
		try {
			engine.eval("test.results <- data.frame()");
			
			engine.eval(String.format("test_data <-read.csv(sprintf(\"%s/testdata/%s\"), header=TRUE)", basePath, Settings.TEST_DATA));
			engine.eval("positive <-test_data[test_data$result==0,]");
			engine.eval("negative <-test_data[test_data$result==1,]");
			engine.eval(String.format("positive <-positive[sample(nrow(positive), %d),]", Settings.TEST_NSAMPLES / 2));
			engine.eval(String.format("negative <-negative[sample(nrow(negative), %d),]", Settings.TEST_NSAMPLES / 2));
			engine.eval("test_data <-rbind(positive, negative)");
			Vector dataVector = (Vector) engine.eval("nrow(positive)");
			int nPositive = dataVector.getElementAsInt(0);
			dataVector = (Vector) engine.eval("nrow(negative)");
			int nNegative = dataVector.getElementAsInt(0);
			JMetalLogger.logger.info("Test Data (positive): " + nPositive);
			JMetalLogger.logger.info("Test Data (negative): " + nNegative);
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean evaluateModel(){
		try {
			engine.eval(String.format("test.results <- calculate_metrics(base_model, test_data, %.2f, cntUpdate)", Settings.BORDER_PROBABILITY));
			engine.eval("test.results <- rbind(test.results, test.result)");
			engine.eval("print(test.result)");
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
		
	}
	
	public boolean saveTestResults(){
		try {
			engine.eval(String.format("testfile<- \"%s/%s/%s_test_data.csv\"", Settings.EXPORT_PATH, Settings.LR_WORKPATH, filename));
			engine.eval(String.format("resultfile<- \"%s/%s/%s_test_result.csv\"", Settings.EXPORT_PATH, Settings.LR_WORKPATH, filename));
			engine.eval("write.table(test_data, testfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
			engine.eval("write.table(test.results, resultfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
	
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	
	////////////////////////////////////////////////////////////////////////
	// Util functions
	////////////////////////////////////////////////////////////////////////
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
}