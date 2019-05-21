package lu.uni.svv.StressTesting.search;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.*;

import com.github.rcaller.rstuff.*;
import lu.uni.svv.StressTesting.datatype.FitnessList;
import org.uma.jmetal.util.JMetalLogger;
import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.Settings;
import lu.uni.svv.StressTesting.utils.RandomGenerator;
import lu.uni.svv.StressTesting.scheduler.*;


public class FinegrainedSearch {
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
	
	
	String basePath;
	TestingProblem problem;
	RMScheduler scheduler = null;
	
	/**
	 * Test method
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
		
		FinegrainedSearch secondPhase = new FinegrainedSearch(problem, Settings.SCHEDULER);
		secondPhase.run(Settings.UPDATE_ITERATION, Settings.MAX_ITERATION, Settings.BORDER_PROBABILITY);
	}
	
	public FinegrainedSearch(TestingProblem _problem, String _schedulerName) throws Exception{
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
	}
	
	public String getCoeffText(double[] _coeff){
		StringBuilder sb = new StringBuilder();
		for (int i=1; i<_coeff.length; i++){
			sb.append(String.format("%.4f*X%d + ", _coeff[i], i));
		}
		sb.append(String.format("%.4f", _coeff[0]));
		return sb.toString();
	}
	
	/**
	 * Run the second phase
	 * @param _updateIteration
	 * @param _maxIteration
	 * @param _probability
	 * @throws IOException
	 */
	public void run(int _updateIteration, int _maxIteration, double _probability) throws IOException {
		List<TimeListSolution> solutions = this.loadSolutions(this.basePath);
		if (solutions == null) {
			JMetalLogger.logger.info("There are no solutions in the path:" + this.basePath);
			return;
		}
		
		String datapath = this.basePath + "/sampledata.csv";
		String workfile = String.format("workdata_%d_%d_%.2f.csv", _maxIteration, _updateIteration, _probability);
		String workpath = this.basePath + "/" + workfile;
		
		Files.copy(Paths.get(datapath), Paths.get(workpath), StandardCopyOption.REPLACE_EXISTING);
		double[] coefficients = null;
		
		JMetalLogger.logger.info("initialized second phase");
		
		int count = 0;
		int UPDASTE_PERIOD = _updateIteration * solutions.size();
		int MAX_ITERATION = _maxIteration * solutions.size();
		while (count < MAX_ITERATION) {
			// evaluate all samples for each solution
			for(TimeListSolution s:solutions){
				// Learning model again with more data
				if (count % UPDASTE_PERIOD == 0) {
					coefficients = this.training_LRQuadratic(workpath);
					JMetalLogger.logger.info("updated logistic regression "+ count +"/" + MAX_ITERATION +": " + getCoeffText(coefficients));
				}
				
				long[] sampleWCET = this.sampling(_probability, coefficients);
				int D = this.evaluate(s, sampleWCET);
				this.appendNewDataset(workfile, D, sampleWCET);
				count+=1;
			}
		}
		
		//PDF ←FitPDF(Solutions,G)
		//return PDF
	}
	
	/**
	 * Load solution from the file
	 * @param _path
	 * @return
	 */
	public List<TimeListSolution> loadSolutions(String _path) {
		String path = _path+"/solutions/";
		ArrayList<TimeListSolution> solutions = new ArrayList<TimeListSolution>();
		
		File f = new File(path);
		File[] files = f.listFiles();
		if (files == null) return null;
		
		Arrays.sort(files);
		JMetalLogger.logger.info("Number of solutions to load: " + String.valueOf(files.length));
		for (File file : files) {
			if (file.isFile()==false) continue;
			if (file.getName().endsWith(".json")==false) continue;
			
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
			sampleMap.put(uncertainTasks.get(x), _sample[x+1]);
		}
		
		scheduler.initialize();
		scheduler.setSamples(sampleMap);
		scheduler.run(_solution);
		
	
		return scheduler.hasDeadlineMisses()==true?1:0;
	}
	
	/**
	 * append evaluated data into data file
	 * @param _result
	 * @param _sample
	 */
	public void appendNewDataset(String workPath, int _result, long[] _sample){
		GAWriter writer = new GAWriter( workPath, Level.INFO, null, this.basePath, true);
		
		StringBuilder sb = new StringBuilder();
		sb.append(_result);
		for (int i=1; i<_sample.length; i++){
			sb.append(",");
			sb.append(_sample[i]);
		}
		writer.info(sb.toString());
		writer.close();
		JMetalLogger.logger.info("New data set: "+sb.toString());
	}
	
	/**
	 * generate samples from the model with specific probability
	 * @param _probability probability to pick one line based on the model
	 * @param _coeff     coefficients of the model
	 * @return
	 */
	public long[] sampling(double _probability, double[] _coeff) {
		int nCandidates = Settings.SAMPLE_CANDIDATES;
		long[] bestW = null;
		double bestDist = Double.MAX_VALUE;
		
		// create candidates WCETs and select best one.
		for (int x = 0; x < nCandidates; x++) {
			// create candidate
			long[] W = this.getSampleWCETs();
			// for quadratic equation
			long[] Wq = new long[] {0, W[1]*W[1], W[2], W[2]*W[2]};
			
			// calculate distancc
			double dist = this.getDistance(_probability, Wq, _coeff);
			
			// update best one
			if (dist < bestDist) {
				bestW = W;
				bestDist = dist;
			}
		}
		return bestW;
	}
	
	/**
	 * Create n-dimension sample based on TaskDescription
	 * @return
	 */
	public long[] getSampleWCETs() {
		List<Integer> uncertains = problem.getUncertainTasks();  // return Task IDs
		RandomGenerator random = new RandomGenerator();
		long[] W = new long[uncertains.size() + 1];
		
		for (int i = 0; i < uncertains.size(); i++) {
			// for each range from
			long min = problem.Tasks[uncertains.get(i)-1].MinWCET;
			long max = problem.Tasks[uncertains.get(i)-1].MaxWCET;
			W[i+1] = random.nextLong(min, max);
		}
		return W;
	}
	
	/**
	 * training logistic regression with data
	 * @param _filePath
	 * @return
	 */
	public double[] training_LR(String _filePath) {
		double[] coefficients = null;
		RCaller caller=this.get_rCaller();
		
		try {
			/**
			 * Creating an instance of RCaller class
			 */
			RCode code = RCode.create();
			code.addRCode("training <- read.csv(file=\"" + _filePath + "\", header = TRUE)");
			code.addRCode("md <- glm(formula = result~., family = \"binomial\", data = training)");
			
			caller.setRCode(code);
			caller.runAndReturnResult("md");
			coefficients = caller.getParser().getAsDoubleArray("coefficients");
			
		} catch (Exception e) {
			/**
			 * Note that, RCaller does some OS based works such as creating an external process and
			 * reading files from temporary directories or creating images for plots. Those operations
			 * may cause exceptions for those that user must handle the potential errors.
			 */
			Logger.getLogger(lu.uni.svv.StressTesting.search.FinegrainedSearch.class.getName()).log(Level.SEVERE, e.getMessage());
		}
		return coefficients;
	}
	
	/**
	 * training logistic regression with data
	 * @param _filePath
	 * @return
	 */
	public double[] training_LRQuadratic(String _filePath) {
		double[] coefficients = null;
		RCaller caller=this.get_rCaller();
		
		try {
			/**
			 * Creating an instance of RCaller class
			 */
			RCode code = RCode.create();
			code.addRCode("training <- read.csv(file=\"" + _filePath + "\", header = TRUE)");
			code.addRCode("md <- glm(formula = result~I(T30^2) + T33 + I(T33^2), family = \"binomial\", data = training)");
			
			caller.setRCode(code);
			caller.runAndReturnResult("md");
			coefficients = caller.getParser().getAsDoubleArray("coefficients");
			
		} catch (Exception e) {
			/**
			 * Note that, RCaller does some OS based works such as creating an external process and
			 * reading files from temporary directories or creating images for plots. Those operations
			 * may cause exceptions for those that user must handle the potential errors.
			 */
			Logger.getLogger(lu.uni.svv.StressTesting.search.FinegrainedSearch.class.getName()).log(Level.SEVERE, e.getMessage());
		}
		return coefficients;
	}
	
	/**
	 * calculate distance between sample(_W) and model line(_coeff) of probability(_p)
	 * @param _p
	 * @param _W
	 * @param _coeff
	 * @return
	 */
	public double getDistance(double _p, long[] _W, double[] _coeff) {
		// c - loglink(probability)
		double addition = _coeff[0] - Math.log(_p / (1 - _p));
		
		// a*W1 + b*W2 + ...
		double numerator = 0.0;
		for (int i = 1; i < _W.length; i++) {
			numerator += _W[i] * _coeff[i];
		}
		
		// a^2 + b^2 + ....
		double denominator = 0.0;
		for (int i = 1; i < _W.length; i++) {
			denominator += _coeff[i] * _coeff[i];
		}
		return Math.abs(numerator + addition) / Math.sqrt(denominator);
	}
	
	/**
	 * get Rcaller object based on OS type
	 * @return
	 */
	public RCaller get_rCaller(){
		RCaller caller = null;
		if (detectOS().startsWith("Mac")){
			String RPath = "/Library/Frameworks/R.framework/Resources/bin/R";
			String RScriptPath = "/Library/Frameworks/R.framework/Resources/bin/Rscript";
			RCallerOptions options =
					RCallerOptions.create(RScriptPath, RPath,
							FailurePolicy.RETRY_1,
							3000L,
							100L,
							RProcessStartUpOptions.create());
			caller = RCaller.create(options);
		}
		else{
			caller = RCaller.create();
		}
		return caller;
	}

	
	/**
	 * Detect OS
	 */
	public String detectOS(){
		String OS = System.getProperty("os.name").toLowerCase();
		
		if (OS.contains("win")) {
			return "Windows";
		} else if (OS.contains("mac")) {
			return "Mac";
		} else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix") ) {
			return "Linux";
		} else if (OS.contains("sunos")) {
			return "Solaris";
		} else {
			return OS;
		}
	}
}