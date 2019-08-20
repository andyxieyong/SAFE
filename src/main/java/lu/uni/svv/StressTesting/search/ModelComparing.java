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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.*;


public class ModelComparing {
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
		if(Settings.N_SAMPLE_WCET==0) Settings.N_SAMPLE_WCET=1;
		Settings.INPUT_FILE = Settings.BASE_PATH + "/input.csv";
		
		
		TestingProblem problem = new TestingProblem(Settings.INPUT_FILE, Settings.TIME_QUANTA, Settings.TIME_MAX, Settings.SCHEDULER);
		JMetalLogger.logger.info("Loaded problem");
		
		ModelComparing secondPhase = new ModelComparing(problem, Settings.SCHEDULER);
		secondPhase.run(Settings.SECOND_PHASE_RUNTYPE, Settings.UPDATE_ITERATION, Settings.MAX_ITERATION, Settings.BORDER_PROBABILITY);
	}
	
	/**********************************
	 * non-static methods
	 *********************************/
	String basePath;
	TestingProblem problem;
	RMScheduler scheduler = null;
	ScriptEngine engine = null;
	
	
	public ModelComparing(TestingProblem _problem, String _schedulerName) throws Exception{
		basePath = Settings.BASE_PATH;
		problem = _problem;
		System.out.println(basePath);
		
		// Create Scheduler instance
		try {
			Class c = this.getClass();
			c.getPackage();
			Class schedulerClass = Class.forName("lu.uni.svv.StressTesting.scheduler." + _schedulerName);
			
			Constructor constructor = schedulerClass.getConstructors()[0];
			Object[] parameters = {_problem};
			scheduler = (RMScheduler)constructor.newInstance(parameters);
			
		} catch (ClassNotFoundException| InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// create a Renjin engine:
		JMetalLogger.logger.info("Loading R module....");
		RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
		this.engine = factory.getScriptEngine();
		JMetalLogger.logger.info("Loading R module....Finished");
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
		List<TimeListSolution> solutions = this.loadSolutions(this.basePath, Settings.BEST_RUN);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + this.basePath);
			return;
		}
		
		// Setting initial points to use for learning
		String datapath = String.format("%s/sampledata_%s.csv", this.basePath, Settings.LR_SAMPLEDATA_CODE);
		
		String formulaCode = Settings.LR_FORMULA_PATH.replace("/", "_");
		String run = "";
		if (Settings.BEST_RUN != 0)
			run = String.format("_run%02d", Settings.BEST_RUN);
		
		// to execute multiple test (because we use subset of first phase data)
		String testID = "";
		if (Settings.GA_RUN != 0)
			run = String.format("_test%02d", Settings.GA_RUN);
		
		String workfile = String.format("/%s/workdata_%s_%d_%d_%.2f_%s%s%s.csv", Settings.LR_WORKPATH, _run_type, _maxIteration, _updateIteration, _probability, formulaCode, run, testID);
		
		String workpath = this.basePath + workfile;
		File file = new File(workpath);
		if (!file.getParentFile().exists())
			file.getParentFile().mkdir();
		
		Files.copy(Paths.get(datapath), Paths.get(workpath), StandardCopyOption.REPLACE_EXISTING);
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
		while (count < _maxIteration) {
			// evaluate all samples for each solution
			for (TimeListSolution s : solutions) {
				long[] sampleWCET = null;
				// Learning model again with more data
				if ((count != 0) && (count % _updateIteration == 0)) {
					JMetalLogger.logger.info("update logistic regression " + count + "/" + _maxIteration);
					this.update_model();
				}
				
				if (_run_type.compareTo("random") == 0) {
					sampleWCET = this.sampling_byRandom(1);
				} else {
					sampleWCET = this.sampling_byDistance(1, Settings.SAMPLE_CANDIDATES, _probability);
				}
				
				
				int D = this.evaluate(s, sampleWCET);
				String text = this.createSampleLine(D, sampleWCET);
				this.appendNewDataset(workfile, text);
				JMetalLogger.logger.info(String.format("New data %d/%d: %s", (count % _updateIteration) + 1, _updateIteration, text));
				
				merge_to_training_data(new int[]{D});
				count += 1;
			}
		} // while

		this.update_model();
		JMetalLogger.logger.info("Finished to run");
	}

	public boolean setting_environment() {
		try {
			engine.eval("library('org.renjin.cran:MASS')");
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
	
	public boolean update_model(){
		try {
			engine.eval("print(sprintf(\"Number of data set: %d\", nrow(training)))");
			engine.eval("base_model <- glm(formula = base_model$formula, family = \"binomial\", data = training)");
			JMetalLogger.logger.info("Updated model: " + getModelText("base_model"));
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
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
		JMetalLogger.logger.info("Number of solutions to load: " + String.valueOf(files.length));
		for (File file : files) {
			if (!file.isFile()) continue;
			String name = file.getName();
			if (!name.endsWith(".json")) continue;
			String runID = name.substring(name.indexOf('_')+4, name.lastIndexOf('_'));
			if (Integer.parseInt(runID) != _bestRunID) continue;
			
			TimeListSolution s = TimeListSolution.loadFromJSON(problem, file.getAbsolutePath());
			solutions.add(s);
			JMetalLogger.logger.info("loaded solution from " + file.getName());
		}
		return solutions;
	}
	
	/**
	 * Evaluation a sample with a TimeListSolution in Scheduler
	 * @param _solution
	 * @param _sample
	 * @return
	 */
	public int evaluate(TimeListSolution _solution, long[] _sample){
		
		List<Integer> uncertainTasks = problem.getUncertainTasks();
		HashMap<Integer, Long> sampleMap = new HashMap<Integer, Long>();
		for(int x=0; x<uncertainTasks.size(); x++) {
			sampleMap.put(uncertainTasks.get(x), _sample[x]);
		}
		
		scheduler.initialize();
		scheduler.setSamples(sampleMap);
		scheduler.run(_solution);
		
		
		return scheduler.hasDeadlineMisses()?1:0;
	}
	
	/**
	 * append evaluated data into data file
	 * @param workPath
	 * @param datatext
	 */
	public void appendNewDataset(String workPath, String datatext){
		GAWriter writer = new GAWriter( workPath, Level.INFO, null, this.basePath, true);
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