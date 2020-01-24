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
	
	TestingProblem problem;
	List<TimeListSolution> solutions= null;
	
	// Objects
	Constructor constructor = null;
	ScriptEngine engine = null;
	Phase1Loader phase1 = null;
	
	//Variables
	String filename = "";
	
	public ModelUpdate() throws Exception{
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
		phase1 = new Phase1Loader(problem);
	}
	
	/**
	 * Run the second phase
	 * @throws IOException
	 */
	public void run() throws IOException, ScriptException, EvalException, Exception {
		
		// Loading solutions
		solutions = phase1.loadSolutions(Settings.BASE_PATH, Settings.RUN_NUM);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + Settings.BASE_PATH);
			return;
		}
		
		// Loading formula
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
		if (!loadTrainingData(workfile)){
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
		
		//if (Settings.STOP_CONDITION) {
		JMetalLogger.logger.info("Preparing termination data ...");
		if(!this.prepareTerminationData()) {
			JMetalLogger.logger.info("Failed to load input data");
			return;
		}
		//}

		// if the data is not imbalanced...
		if(!Settings.INPUT_FILE.endsWith("input.csv")) {
			JMetalLogger.logger.info("Adjusting training data in a balanced way...");
			if (!this.adjustingBalance(workfile, formula)) {
				JMetalLogger.logger.info("Error to adjust data balancing");
				return;
			}
		}
		
		JMetalLogger.logger.info("Learning initial model...");
		double prob = this.initializeModel(formula);
		
		if (Settings.TEST_DATA.length() != 0 && !evaluateModel(0, prob)){
			JMetalLogger.logger.info("Failed to evaluate model with test data");
			return;
		}
		
		JMetalLogger.logger.info("Update model process ...");
		if (!this.update(workfile, prob)) {
			JMetalLogger.logger.info("Failed to update model process");
			return;
		}
	}
	
	public boolean update(String workfile, double _initialProbability) throws ScriptException, EvalException, Exception{
		// Sampling new data point and evaluation
		int count = 0;
		int solID = 0;
		int nUpdates = 0;
		double borderProbability = _initialProbability;     // Set middle point
		
		while (nUpdates <= Settings.N_MODEL_UPDATES) {
			// Learning model again with more data
			if (count == Settings.N_EXAMPLE_POINTS) {
				//check stop condition
				boolean condition = this.checkStopCondition(nUpdates);
				if (Settings.STOP_CONDITION && condition) break;
				
				nUpdates += 1;
				
				JMetalLogger.logger.info("update logistic regression " + nUpdates + "/" + Settings.N_MODEL_UPDATES);
				borderProbability = this.updateModel(borderProbability);
				if (Settings.TEST_DATA.length() != 0 && !evaluateModel(nUpdates, borderProbability)){
					JMetalLogger.logger.info("Failed to evaluate model with test data in "+ nUpdates + "/" + Settings.N_MODEL_UPDATES);
					return false;
				}
				count = 0;
				
				// this line should be up there before changing uUpdate value
				// The reason why I put here is for check model evaluation with test data at the end of the Phase 2.
				if(nUpdates > Settings.N_MODEL_UPDATES) break;
			}
			
			//Sampling
			long[] sampleWCET = null;
			if (Settings.SECOND_PHASE_RUNTYPE.compareTo("random")==0){
				sampleWCET = samplingNewPointsRandom(1);
			}
			else{
				sampleWCET = this.samplingNewPoints(1, Settings.SAMPLE_CANDIDATES, borderProbability);
			}
			
			//Evaluate using scheduler
			int D = this.evaluate(solutions.get(solID), sampleWCET);
			this.mergeNewSample(new int[]{D});
			
			// Save information
			String text = this.createSampleLine(D, sampleWCET);
			this.appendNewDataset(workfile, text);
			JMetalLogger.logger.info(String.format("New data %d/%d: %s", count+1, Settings.N_EXAMPLE_POINTS, text));
			
			// update index
			solID = (solID + 1) % solutions.size();
			count += 1;
		} //while
		
		saveModelResults();
		
		if (Settings.TEST_DATA.length()!=0 && !this.saveTestResults()) {
			JMetalLogger.logger.fine("Failed to save test results");
		}
		if (this.saveTerminationResults()) {
			JMetalLogger.logger.fine("Failed to save termination results");
		}
		JMetalLogger.logger.info("Finished to run Phase 2");
		return true;
	}
	
	public String generateInitialTrainingData(){
		
		// Setting initial points to use for learning
		filename = String.format("workdata_%s_run%02d", Settings.SECOND_PHASE_RUNTYPE, Settings.RUN_NUM);
		String workfile = String.format("%s/%s.csv", Settings.WORKNAME, filename);
		
		if (!phase1.makeInitialPoints(Settings.BASE_PATH, Settings.EXTEND_PATH, Settings.RUN_NUM, workfile)) {
			return null;
		}
		return workfile;
	}
	
	public String loadFormula(String formulaPath) {
		String formula = "";
		try {
			if (formulaPath.length() == 0) {
				formulaPath = String.format("%s/formula/formula_run%02d", Settings.BASE_PATH, Settings.RUN_NUM);
			}
			formula = new String(Files.readAllBytes(Paths.get(formulaPath)), StandardCharsets.UTF_8).trim();
			JMetalLogger.logger.info("Loaded formula from " + formulaPath);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return formula;
	}
	
	public boolean settingEnvironment() throws ScriptException, EvalException{
		JMetalLogger.logger.info("Load input tasks info from " + Settings.INPUT_FILE);
		engine.eval("library('org.renjin.cran:MASS')");
		engine.eval("library('org.renjin.cran:MLmetrics')");
		engine.eval("library('org.renjin.cran:pracma')");
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
		engine.eval("source(\"R/libs/lib_data.R\")");           // dependency for lib_evaluate
		engine.eval("source(\"R/libs/lib_metrics.R\")");        // dependency for lib_evaluate (noFPR)
		engine.eval("source(\"R/libs/lib_evaluate.R\")");       // find_noFPR, integrateMC, calculate_metrics, kfoldCV
		
		engine.eval("source(\"R/libs/lib_pruning.R\")");        // pruning functions
		engine.eval("source(\"R/libs/lib_formula.R\")");        // formula functions (get_raw_names, get_base_names)
		engine.eval("source(\"R/libs/lib_model.R\")");          // dependency for sampling (generate_model_line)
		engine.eval("source(\"R/libs/lib_sampling.R\")");       // sampling functions
		engine.eval("get_uncertain_tasks<-function(){\n" +
				"    diffWCET <- TASK_INFO$WCET.MAX - TASK_INFO$WCET.MIN\n" +
				"    tasks <- c()\n" +
				"    for(x in 1:length(diffWCET)){\n" +
				"       if (diffWCET[x] <= 0) next\n" +
				"       tasks <- c(tasks, sprintf(\"T%d\",as.integer(x)))\n" +
				"    }\n" +
				"    return(tasks)\n" +
				"}\n");
		return true;
	}
	
	/**
	 *
	 * @param inputpath   'results/20190722_P2_S20_GASearch/RQ2_N20000/workdata*.csv
	 */
	public boolean loadTrainingData(String inputpath) throws ScriptException, EvalException{
		// load data
		engine.eval(String.format("datafile<- \"%s/%s\"", Settings.EXTEND_PATH, inputpath));
		engine.eval("training <- read.csv(datafile, header=TRUE)");
		
		return true;
	}
	
	/**
	 *
	 * @param formula
	 * @return
	 */
	public double initializeModel(String formula) throws ScriptException, EvalException{
		// set formula
		engine.eval(String.format("formula_str<- \"%s\"",formula));
		
		// learning logistic regression with simple formula
		engine.eval("base_model <- glm(formula = formula_str, family = \"binomial\", data = training)");
		engine.eval("cntUpdate <- 0");
		engine.eval("cntUpdate <- 0");

		// keep coefficients
		engine.eval("coef <- t(data.frame(base_model$coefficients))");
		engine.eval("colnames(coef) <- get_raw_names(names(base_model$coefficients))");
		engine.eval("coef.item <- data.frame(nUpdate=cntUpdate, TrainingSize=nrow(training), coef)");
		engine.eval("rownames(coef.item) <- c(cntUpdate)");
		engine.eval("coef.results <- coef.item");
		
		JMetalLogger.logger.info("Initialized model: " + getModelText("base_model"));

		return 0.5;
	}
	
	
	public boolean adjustingBalance(String inputpath, String formula) throws ScriptException, EvalException {
		// set formula
		engine.eval(String.format("formula_str<- \"%s\"",formula));
		
		// get uncertainIDs from formula
		engine.eval(".removedLabel <- strsplit(formula_str, \"~\")[[1]][2]");
		engine.eval(".terms <- strsplit(.removedLabel, \"\\\\+\")[[1]]");
		engine.eval(".terms <- trimws(.terms)");
		engine.eval("uncertainIDs <- get_base_names(.terms, isNum=TRUE)");
		
		// make intercept information from the TASK_INFO
		engine.eval("intercepts<-data.frame(t(TASK_INFO$WCET.MAX[uncertainIDs]))");
		engine.eval("colnames(intercepts) <- sprintf(\"T%d\", uncertainIDs)");
		
		// create blanceSide and
		engine.eval("positive <- nrow(training[training$result==0,])");
		engine.eval("negative <- nrow(training[training$result==1,])");
		engine.eval("balanceSide <- ifelse(positive > negative, \"positive\", \"negative\")");
		engine.eval("training <- pruning(training, balanceSide, intercepts, uncertainIDs)");
		
		// save training data
		engine.eval(String.format("datafile<- \"%s/%s\"", Settings.EXTEND_PATH, inputpath));
		engine.eval("write.table(training, datafile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
		return true;
	}
	
	/**
	 * Sample new points
	 * @param nSample
	 * @param nCandidate
	 * @param P
	 * @return
	 */
	public long[] samplingNewPoints(int nSample, int nCandidate, double P) throws ScriptException, EvalException{
		long[] samples = null;
		engine.eval("tnames <- get_task_names(training)");
		String codeText = String.format("sampled_data <- sample_based_euclid_distance(tnames, base_model, nSample=%d, nCandidate=%d, P=%.6f)", nSample, nCandidate, P);
		engine.eval(codeText);
		
		StringVector nameVector = (StringVector)engine.eval("colnames(sampled_data)");
		String[] names = nameVector.toArray();
		
		samples = get_row_longlist("sampled_data");

		return samples;
	}
	
	public long[] samplingNewPointsRandom(int nSample) throws ScriptException, EvalException{
		// sampling new points by random
		long[] samples = null;
		
		engine.eval("tnames <- get_task_names(training)");
		String codeText = String.format("sampled_data <- sample_by_random(tnames, nSample=%d)", nSample);
		engine.eval(codeText);
		
		StringVector nameVector = (StringVector)engine.eval("colnames(sampled_data)");
		String[] names = nameVector.toArray();
		
		samples = get_row_longlist("sampled_data");
		
	
		return samples;
	}
	
	public boolean mergeNewSample(int[] answers) throws ScriptException, EvalException{
		// generate list of evaluated result
		StringBuilder sb = new StringBuilder();
		for(int x=0; x<answers.length; x++){
			if (x==0) sb.append(answers[x]);
			else{
				sb.append(",");
				sb.append(answers[x]);
			}
		}
		
		engine.eval(String.format("training.item <- data.frame(result=c(%s), sampled_data)", sb.toString()));
		engine.eval("training <- rbind(training, training.item) ");

		return true;
	}
	
	public double updateModel(double _prob) throws ScriptException, EvalException{
		engine.eval("print(sprintf(\"Number of data set: %d\", nrow(training)))");
		engine.eval("prev_model<-base_model");
		engine.eval("base_model <- glm(formula = prev_model$formula, family = \"binomial\", data = training)");
		engine.eval("cntUpdate <- cntUpdate + 1");
		
		engine.eval("coef <- t(data.frame(base_model$coefficients))");
		engine.eval("colnames(coef) <- get_raw_names(names(base_model$coefficients))");
		engine.eval("coef.item <- data.frame(nUpdate=cntUpdate, TrainingSize=nrow(training), coef)");
		engine.eval("rownames(coef.item) <- c(cntUpdate)");
		engine.eval("coef.results <- rbind(coef.results, coef.item)");
		
		JMetalLogger.logger.info("Updated model: " + getModelText("base_model"));
		
		updateTerminationData();
		return _prob;
	}
	
	
	////////////////////////////////////////////////////////////////////////
	// Related termination condition
	////////////////////////////////////////////////////////////////////////
	public boolean prepareTerminationData()  throws ScriptException, EvalException{
		engine.eval("termination.results <- data.frame()");
		JMetalLogger.logger.info("Termination data is the same with test data");

		return true;
	}
	
	public boolean updateTerminationData()  throws ScriptException, EvalException{
		return true;
	}
	
	public boolean saveTerminationResults() throws ScriptException, EvalException{
		engine.eval(String.format("resultfile <- \"%s/%s/%s_termination_result.csv\"", Settings.EXTEND_PATH, Settings.WORKNAME, filename));
		engine.eval("write.table(termination.results, resultfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
		
		return true;
	}
	
	public boolean checkStopCondition(int _cntUpdate) throws ScriptException, EvalException, Exception {
		engine.eval("cv_error <- kfoldCV(base_model, training, 10)");
		engine.eval(String.format("termination.item <- data.frame(Iter=%d, cv_error)", _cntUpdate));
		engine.eval("termination.results <- rbind(termination.results,  termination.item)");
		
		Vector dataVector = (Vector)engine.eval("1 - cv_error$CV.Precision.Sum");
		double probability = dataVector.getElementAsDouble(0);
		
		if (probability <= Settings.STOP_ACCEPT_RATE)
			return true;
		return false;
	}
	
	////////////////////////////////////////////////////////////////////////
	// Related test model
	////////////////////////////////////////////////////////////////////////
	public boolean includeTestData()throws ScriptException, EvalException, Exception{
		if (Settings.TEST_DATA.length()==0)
		{
			JMetalLogger.logger.severe("You need to set TEST_NSAMPLES to get test values");
			throw new Exception("Setting Error for TEST_NSAMPLES");
		}
		
		engine.eval("test.results <- data.frame()");
		String cmd = String.format("test.samples <-read.csv(sprintf(\"%s/%s\"), header=TRUE)", Settings.BASE_PATH, Settings.TEST_DATA);
		engine.eval(cmd);

		engine.eval("positive <-test.samples[test.samples$result==0,]");
		engine.eval("negative <-test.samples[test.samples$result==1,]");
		
		Vector dataVector = (Vector) engine.eval("nrow(positive)");
		int nPositives = dataVector.getElementAsInt(0);
		dataVector = (Vector) engine.eval("nrow(negative)");
		int nNegatives = dataVector.getElementAsInt(0);
		JMetalLogger.logger.info("Test Data (positive): " + nPositives);
		JMetalLogger.logger.info("Test Data (negative): " + nNegatives);
		
		return true;
	}
	
	public boolean evaluateModel(int _cntUpdate, double _prob)throws ScriptException, EvalException{
		engine.eval(String.format("result.item <- calculate_metrics(base_model, test.samples, %.6f, %d)", _prob, _cntUpdate));
		engine.eval(String.format("result.item <- data.frame(Probability=%f, result.item)", _prob));
		engine.eval("print(result.item)");
		engine.eval("test.results <- rbind(test.results, result.item)");
		return true;
	}
	
	public boolean saveTestResults()throws ScriptException, EvalException{
		engine.eval(String.format("resultfile <- \"%s/%s/%s_test_result.csv\"", Settings.EXTEND_PATH, Settings.WORKNAME, filename));
		engine.eval("write.table(test.results, resultfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");

		return true;
	}
	
	public boolean saveModelResults()throws ScriptException, EvalException{
		engine.eval(String.format("modelfile <- \"%s/%s/%s_model_result.csv\"", Settings.EXTEND_PATH, Settings.WORKNAME, filename));
		engine.eval("write.table(coef.results, modelfile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
		engine.eval("print(coef.results)");
		engine.eval("print(format(coef.results, digits=16,scientific=T))");
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
	
	/**
	 * append evaluated data into data file
	 * @param _filename
	 * @param _datatext
	 */
	public void appendNewDataset(String _filename, String _datatext){
		GAWriter writer = new GAWriter(_filename, Level.INFO, null, Settings.EXTEND_PATH, true);
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