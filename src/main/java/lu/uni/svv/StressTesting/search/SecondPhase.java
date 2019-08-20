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
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;


public class SecondPhase {
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
		
		// Settings update
		if(Settings.N_SAMPLE_WCET==0) Settings.N_SAMPLE_WCET=1;   // Scheduling option:
		int[] targetTasks = convertToIntArray(Settings.TARGET_TASKLIST);
		Settings.INPUT_FILE = Settings.BASE_PATH + String.format("/Task%02d/input.csv", targetTasks[0]);
		
		TestingProblem problem = new TestingProblem(Settings.INPUT_FILE, Settings.TIME_QUANTA, Settings.TIME_MAX, Settings.SCHEDULER);
		JMetalLogger.logger.info("Loaded problem");
		
		SecondPhase secondPhase = new SecondPhase(problem, Settings.SCHEDULER, targetTasks);
		secondPhase.run(Settings.SECOND_PHASE_RUNTYPE, Settings.UPDATE_ITERATION, Settings.MAX_ITERATION, Settings.BORDER_PROBABILITY);
	}
	
	public static int[] convertToIntArray(String commaSeparatedStr)
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
	
	/**********************************
	 * non-static methods
	 *********************************/
	String basePath;
	TestingProblem problem;
//	RMScheduler scheduler = null;
	Constructor constructor = null;
	ScriptEngine engine = null;
	int[] targetTasks = null;
	
	public SecondPhase(TestingProblem _problem, String _schedulerName, int[] _targetTasks) throws Exception{
		basePath = Settings.BASE_PATH;
		problem = _problem;
		targetTasks = _targetTasks;
		System.out.println(basePath);
		
		// Create Scheduler instance
		try {
			Class c = this.getClass();
			c.getPackage();
			Class schedulerClass = Class.forName("lu.uni.svv.StressTesting.scheduler." + _schedulerName);
			
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
	
	public List<TimeListSolution> loadMultiTaskSolutions(String basepath, int bestRun){
		// load solutions for multiple tasks
		List<TimeListSolution> solutions = new ArrayList<TimeListSolution>();
		for (int taskID:targetTasks){
			String path = String.format("%s/Task%02d", basepath, taskID);
			List<TimeListSolution> solutionsPart = this.loadSolutions(path, bestRun);
			solutions.addAll(solutionsPart);
		}
		return solutions;
	}
	
	public String loadInitialPoints(String _basePath, String _outputPath, int _bestRun, String _run_type, int _updateIteration, int _maxIteration, double _probability)	{
		String formulaCode = Settings.LR_FORMULA_PATH.replace("/", "_");
		
		String workfile = String.format("%s/%s/workdata_%s_%d_%d_%.2f_%s_run%02d.csv",
										_outputPath, Settings.LR_WORKPATH,
										_run_type,
										_maxIteration,
										_updateIteration,
										_probability, formulaCode, _bestRun);
		File file = new File(workfile);
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		// move inputs into output file
		Path outFile=Paths.get(workfile);
		try {
			FileChannel out=FileChannel.open(outFile, CREATE, TRUNCATE_EXISTING, WRITE);
			int titleLength = 0;
			for (int x=0; x<targetTasks.length;x++) {
				int taskID = targetTasks[x];
				String datafile = String.format("%s/Task%02d/samples/sampledata_run%02d.csv", _basePath, taskID, _bestRun);
				Path inFile=Paths.get(datafile);
				if(x==0)
				{
					BufferedReader br = new BufferedReader(new FileReader(datafile));
					String line = br.readLine();
					br.close();
					titleLength = line.length()+1;
				}
				
				JMetalLogger.logger.info("loading "+inFile+"...");
				FileChannel in=FileChannel.open(inFile, READ);
				for(long p=(x==0?0:titleLength), l=in.size(); p<l; )
					p+=in.transferTo(p, l-p, out);
			}
			out.close();
		}
		catch (IOException e){
			e.printStackTrace();
			workfile = null;
		}
		
		return workfile;
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
		// Load Solutions
		List<TimeListSolution> solutions = loadMultiTaskSolutions(this.basePath, Settings.BEST_RUN);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + this.basePath);
			return;
		}
		
		// Setting initial points to use for learning
		String workpath = loadInitialPoints(this.basePath, Settings.EXPORT_PATH, Settings.BEST_RUN, _run_type, _updateIteration, _maxIteration, _probability);
		if (workpath == null) {
			JMetalLogger.logger.info("Failed to load input data");
			return;
		}
		JMetalLogger.logger.info("Started second phase");
		
		// Initialize model
		this.setting_environment();
		
		if (!this.initialize_model(workpath, Settings.LR_FORMULA_PATH, Settings.LR_INITIAL_SIZE)){
			JMetalLogger.logger.info("Error to initialze model");
			return;
		}
		JMetalLogger.logger.info("Generated initial model");
		
		
		// Sampling new data point and evaluation
		int count = 0;
		boolean END = false;
		while (count < _maxIteration && !END) {
			// evaluate all samples for each solution
			for (int solID=0; solID < solutions.size(); solID++){
				long[] sampleWCET = null;
				// Learning model again with more data
				if ((count != 0) && (count % _updateIteration == 0)) {
					JMetalLogger.logger.info("update logistic regression " + count + "/" + _maxIteration);
					if (!this.update_model(count/_updateIteration)){
						END=true; break;
					}
				}
				
				if (_run_type.compareTo("random") == 0) {
					sampleWCET = this.sampling_byRandom(1);
				} else {
					sampleWCET = this.sampling_byDistance(1, Settings.SAMPLE_CANDIDATES, _probability);
				}
				
				int taskID = solID/Settings.GA_POPULATION;
				int D = this.evaluate(solutions.get(solID), taskID, sampleWCET);
				String text = this.createSampleLine(D, sampleWCET);
				this.appendNewDataset(workpath, text);
				JMetalLogger.logger.info(String.format("New data %d/%d: %s", (count % _updateIteration) + 1, _updateIteration, text));
				
				merge_to_training_data(new int[]{D});
				count += 1;
			}
		} // while

		this.update_model(count/_updateIteration);
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
			engine.eval("TASK_INFO<- data.frame(ID = c(1:34), TASK_INFO)");
			engine.eval("colnames(TASK_INFO)<- c(\"ID\", \"NAME\", \"TYPE\", \"PRIORITY\", \"WCET.MIN\", \"WCET.MAX\", \"PERIOD\", \"INTER.MIN\", \"INTER.MAX\", \"DEADLINE\")");
			String update_time_str =
				"TASK_INFO$WCET.MIN = as.integer(TASK_INFO$WCET.MIN/TIME_QUANTA)\n" +
				"TASK_INFO$WCET.MAX = as.integer(TASK_INFO$WCET.MAX/TIME_QUANTA)\n" +
				"TASK_INFO$PERIOD = as.integer(TASK_INFO$PERIOD/TIME_QUANTA)\n" +
				"TASK_INFO$INTER.MIN = as.integer(TASK_INFO$INTER.MIN/TIME_QUANTA)\n" +
				"TASK_INFO$INTER.MAX = as.integer(TASK_INFO$INTER.MAX/TIME_QUANTA)\n" +
				"TASK_INFO$DEADLINE = as.integer(TASK_INFO$DEADLINE/TIME_QUANTA)";
			engine.eval(update_time_str);
			engine.eval("source(\"R/LogisticRegressionN/lib_quadratic.R\")");
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
			formula = new String(Files.readAllBytes(Paths.get(formulaPath)), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		
		Object[] samples = null;
		try {
			// load data
			engine.eval(String.format("datafile<- \"%s\"", filepath));
			engine.eval("training <- read.csv(datafile, header=TRUE)");
			engine.eval(String.format("testDataPool <-get_evaluate_data_pool(\"%s/testdata\")", basePath));
			
			if (initialTrainingSize!=0){
//				engine.eval(String.format("training <- training[1:%d,]",initialTrainingSize));  //sample_n(training, %d)
				// sampling and save the result to the datafile
				engine.eval(String.format("training <- training[sample(nrow(training), %d), ]",initialTrainingSize));  //
				engine.eval("write.table(training, datafile, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
			}
			
			// learning logistic regression with simple formula
			JMetalLogger.logger.info("Started logistic regression...");
			
//			engine.eval(String.format("formula <- read.delim(\"%s\", header=FALSE)",formula));
			engine.eval(String.format("formula_str<- \"%s\"",formula));
			engine.eval("base_model <- glm(formula = formula_str, family = \"binomial\", data = training)");
			
			JMetalLogger.logger.info("Initialized model: " + getModelText("base_model"));
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public ArrayList<long[]> sampling_byRange(int nSample, double minP, double maxP, int distance){

		ArrayList<long[]> samples = new ArrayList<long[]>();
		try {
			engine.eval("tnames <- get_task_names(training)");
			String codeText = String.format("sampled_data<-get_range_sampling(tnames, base_model, nSample=%d, Pmin=%.5f, Pmax=%.5f, overBound=%d)", nSample, minP, maxP, distance);
			engine.eval(codeText);
			
			for(int x=1; x<=nSample; x++) {
				samples.add(get_row_longlist(String.format("sampled_data[%d,]",x)));
			}
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return null;
		}
		
		return samples;
	}
	
	public long[] get_row_longlist(String varName) throws ScriptException {
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
	
	public boolean update_model(int count){
		boolean result=true;
		try {
			engine.eval("print(sprintf(\"Number of data set: %d\", nrow(training)))");
			engine.eval("prev_model<-base_model");
			engine.eval("base_model <- glm(formula = prev_model$formula, family = \"binomial\", data = training)");
			JMetalLogger.logger.info("Updated model: " + getModelText("base_model"));
			
			engine.eval("testData <-sample_subset_data_even(testDataPool, 20000)"); //training[20201:nrow(training),]
			
			engine.eval("fitPrev <-predict(prev_model, newdata=testData, type=\"response\")");
			engine.eval("fitNew <- predict(base_model, newdata=testData, type=\"response\")");
			
			engine.eval(String.format("predPrev <- ifelse(fitPrev<= %.3f, 0, 1)", Settings.BORDER_PROBABILITY));
			engine.eval(String.format("predNew <- ifelse(fitNew<= %.3f, 0, 1)", Settings.BORDER_PROBABILITY));
			
			engine.eval("pFPR <- FPRate(y_true=testData$result, y_pred=predPrev, positive = \"0\")");
			engine.eval("nFPR <- FPRate(y_true=testData$result, y_pred=predNew, positive = \"0\")");
			engine.eval("rate <- abs(pFPR-nFPR)");
			Vector dataVector = (Vector)engine.eval("rate");
			double rate = dataVector.getElementAsDouble(0);
			JMetalLogger.logger.info(String.format("FPR compare: %.3f", rate));
			
			engine.eval("comp <-data.frame(Prev=predPrev, New=predNew)");
			engine.eval("nT <- nrow(comp[(comp$Prev==comp$New),])");
			engine.eval("nF <- nrow(comp[(comp$Prev!=comp$New),])");
			engine.eval("rate <- nF/(nT+nF)");
			
			dataVector = (Vector)engine.eval("rate");
			rate = dataVector.getElementAsDouble(0);
			JMetalLogger.logger.info(String.format("rate: %.3f", rate));
			
			engine.eval(String.format("printCDF('prev_model', prev_model, testData, %.3f)", Settings.BORDER_PROBABILITY));
			engine.eval(String.format("printCDF('current_model', base_model, testData, %.3f)", Settings.BORDER_PROBABILITY));
			
			
			if (rate <= 0.001 && count>=2) result = false;
			
			
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return result;
	}
	
	public String getModelText(String modelname){
		StringBuilder sb = new StringBuilder();
		try {
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
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return "";
		}
		return sb.toString();
	}
	
	/**
	 * Load solution from the file
	 * @param _path
	 * @return
	 */
	public List<TimeListSolution> loadSolutions(String _path, int _bestRunID) {
		String path = _path+"/solutions/";
		ArrayList<TimeListSolution> solutions = new ArrayList<TimeListSolution>();
		
		File f = new File(path);
		File[] files = f.listFiles();
		if (files == null) return null;
		
		Arrays.sort(files);
		JMetalLogger.logger.info("loading solution from " + path);
//		JMetalLogger.logger.info("Number of solutions to load: " + String.valueOf(files.length));
		for (File file : files) {
			if (!file.isFile()) continue;
			String name = file.getName();
			if (!name.endsWith(".json")) continue;
			String runID = name.substring(name.indexOf('_')+4, name.lastIndexOf('_'));
			if (Integer.parseInt(runID) != _bestRunID) continue;
			
			TimeListSolution s = TimeListSolution.loadFromJSON(problem, file.getAbsolutePath());
			solutions.add(s);
//			JMetalLogger.logger.info("loaded solution from " + file.getName());
		}
		return solutions;
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
	 * @param workPath
	 * @param datatext
	 */
	public void appendNewDataset(String workPath, String datatext){
		String filename = workPath.substring(Settings.EXPORT_PATH.length()+1);
		GAWriter writer = new GAWriter(filename, Level.INFO, null, Settings.EXPORT_PATH, true);
		writer.info(datatext);
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
	
}