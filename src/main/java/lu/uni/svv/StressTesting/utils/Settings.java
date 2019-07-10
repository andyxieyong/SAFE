package lu.uni.svv.StressTesting.utils;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;

import lu.uni.svv.StressTesting.utils.ArgumentParser.DataType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;

public class Settings {
	
	public static String  INPUT_FILE          = "";
	
	// Scheduler
	public static String  SCHEDULER           = "";
	public static double  TIME_QUANTA         = 0.1;
	public static int     TIME_MAX            = 3600000;
	public static int     TASK_FITNESS        = 0;
	public static double  FITNESS_RANGE       = 0.5;
	public static int     N_SAMPLE_WCET       = 0;
	public static boolean UNIFORM_SAMPLE      = false;
	
	// GA
	public static int     GA_RUN              = 1;
	public static int     GA_RUN_MAX          = 0;
	public static int     GA_POPULATION       = 10;
	public static int     GA_MAX_ITERATION    = 1000;
	public static double  GA_CROSSOVER_PROB   = 0.9;
	public static double  GA_MUTATION_PROB    = 0.5;
	public static double  A12_THRESHOLD       = 0.5;
	public static boolean SIMPLE_SEARCH       = false;
	
	// Experiment control
	public static String  BASE_PATH           = "logs";
	public static String  EXPORT_PATH         = "";
	
	// Experiment control
	public static String  INC_TASK_TYPE       = "Periodic";
	public static double  INC_RATE            = 0.05;
	
	//Second phase
	public static int     UPDATE_ITERATION    = 10;
	public static int     MAX_ITERATION       = 200;
	public static double  BORDER_PROBABILITY  = 0.5;
	public static int     SAMPLE_CANDIDATES   = 20;
	
	//printing
	public static boolean PRINT_SAMPLES       = false;
	public static boolean PRINT_RESULTS       = false;
	
	
	
	public Settings()
	{
	}
	
	public static void update(String[] args) throws Exception {
		// Setting arguments
		ArgumentParser parser = new ArgumentParser();
		parser.addOption(false,"Help", DataType.BOOLEAN, "h", "help", "Show how to use this program");
		parser.addOption(false,"SettingFile", DataType.STRING, "f", null, "Base setting file.", "settings.json");
		parser.addOption(false,"RunMax", DataType.INTEGER, "r", null, "Maximum run times for GA");
		parser.addOption(false,"Run", DataType.INTEGER, null, "runID", "Specific run ID when you execute run separately");
		parser.addOption(false,"Populations", DataType.INTEGER, "p", null, "Population for GA");
		parser.addOption(false,"Iterations", DataType.INTEGER, "i", null, "Maximum iterations for GA");
		parser.addOption(false,"CrossoverRate", DataType.DOUBLE, "c", null, "Crossover rate for GA");
		parser.addOption(false,"MutationRate", DataType.DOUBLE, "m", null, "Mutation rate for GA");
		parser.addOption(false,"BasePath", DataType.STRING, "b", null, "Base path to save the result of experiments");
		parser.addOption(false,"ExportPath", DataType.STRING, "e", null, "Exported path to move the result of experiments");
		parser.addOption(false,"TaskFitness", DataType.INTEGER, "t", null, "Task number to use for calculating fitness. If this is unset or sets 0, We will calculate fitness for all tasks.");
		parser.addOption(false,"Scheduler", DataType.STRING, "s", null, "Scheduler");
		parser.addOption(false,"FitnessRange", DataType.DOUBLE, null, "range", "fitness rage");
		parser.addOption(false,"TimeQuanta", DataType.DOUBLE, null, "quanta", "Scheduler time quanta");
		parser.addOption(false,"TimeMax", DataType.INTEGER, null, "max", "scheduler time max");
		parser.addOption(false,"InputFile", DataType.STRING, null, "data", "input data that including job information");
		parser.addOption(false,"IncType", DataType.STRING, null, "incType", "increase WCET, select type");
		parser.addOption(false,"IncRate", DataType.DOUBLE, null, "incRate", "increase WCET, select rate 1.05 means inc 5%");
		parser.addOption(false,"nSampleWCET", DataType.INTEGER, null, "nSamples", "The number of samples that will extracted between minWCET and maxWCET");
		parser.addOption(false,"A12Threshold", DataType.DOUBLE, null, "A12Threshold", "Threshold for A12");
		parser.addOption(false,"uniformSample", DataType.BOOLEAN, null, "uniform", "uniform sample when we are sampling from a range of WCET", false);
		parser.addOption(false,"iterMax", DataType.INTEGER, null, "iterMax", "The iteration number to finish second phase");
		parser.addOption(false,"iterUpdate", DataType.INTEGER, null, "iterUpdate", "The iteration number to update logsitic regression model");
		parser.addOption(false,"borderProb", DataType.DOUBLE, null, "borderProb", "Border Probability for second phase");
		parser.addOption(false,"sampleCandidates", DataType.INTEGER, null, "sampleCandidates", "The number of sandidates to get one sample in second phase");
		parser.addOption(false,"printSamples", DataType.BOOLEAN, null, "printSamples", "If you set this parameter, The program will produce sampling detail information", false);
		parser.addOption(false,"printResults", DataType.BOOLEAN, null, "printResults", "If you set this parameter, The program will produce fitness detail information", false);
		parser.addOption(false,"simpleSearch", DataType.BOOLEAN, null, "simpleSearch", "Simple search mode, not using crossover and mutation just produce children randomly", false);
		
		// parsing args;
		try{
			parser.parseArgs(args);
		}
		catch(Exception e)
		{
			System.out.println("Error: "+e.getMessage());
			System.out.println("");
			System.out.println(parser.getHelpMsg());
			System.exit(0);
		}
		
		if((Boolean)parser.getParam("Help")){
			System.out.println(parser.getHelpMsg());
			System.exit(1);
		}
		
		// Load settings from file
		String filename = (String)parser.getParam("SettingFile");
		Settings.updateSettings(filename);
		
		
		// update settings from command parameters
		if (parser.containsParam("RunMax"))         GA_RUN_MAX = (Integer) parser.getParam("RunMax");
		if (parser.containsParam("Run"))            GA_RUN = (Integer) parser.getParam("Run");
		if (parser.containsParam("Populations"))    GA_POPULATION = (Integer) parser.getParam("Populations");
		if (parser.containsParam("Iterations"))     GA_MAX_ITERATION = (Integer) parser.getParam("Iterations");
		if (parser.containsParam("CrossoverRate"))  GA_CROSSOVER_PROB = (Double) parser.getParam("CrossoverRate");
		if (parser.containsParam("MutationRate"))   GA_MUTATION_PROB = (Double) parser.getParam("MutationRate");
		if (parser.containsParam("BasePath"))       BASE_PATH = (String) parser.getParam("BasePath");
		if (parser.containsParam("ExportPath"))     EXPORT_PATH = (String) parser.getParam("ExportPath");
		if (parser.containsParam("TaskFitness"))    TASK_FITNESS = (Integer) parser.getParam("TaskFitness");
		if (parser.containsParam("Scheduler"))      SCHEDULER = (String) parser.getParam("Scheduler");
		if (parser.containsParam("FitnessRange"))   FITNESS_RANGE = (Double) parser.getParam("FitnessRange");
		if (parser.containsParam("TimeQuanta"))     TIME_QUANTA = (Double) parser.getParam("TimeQuanta");
		if (parser.containsParam("TimeMax"))        TIME_MAX = (Integer) parser.getParam("TimeMax");
		if (parser.containsParam("InputFile"))      INPUT_FILE = (String) parser.getParam("InputFile");
		if (parser.containsParam("IncType"))        INC_TASK_TYPE = (String) parser.getParam("IncType");
		if (parser.containsParam("IncRate"))        INC_RATE = (Double) parser.getParam("IncRate");
		if (parser.containsParam("nSampleWCET"))    N_SAMPLE_WCET = (Integer) parser.getParam("nSampleWCET");
		if (parser.containsParam("A12Threshold"))   A12_THRESHOLD = (Double) parser.getParam("A12Threshold");
		if (parser.containsParam("uniformSample"))  UNIFORM_SAMPLE = (Boolean) parser.getParam("uniformSample");
		if (parser.containsParam("iterMax"))        MAX_ITERATION = (Integer) parser.getParam("iterMax");
		if (parser.containsParam("iterUpdate"))     UPDATE_ITERATION = (Integer) parser.getParam("iterUpdate");
		if (parser.containsParam("borderProb"))     BORDER_PROBABILITY = (Double) parser.getParam("borderProb");
		if (parser.containsParam("sampleCandidates"))SAMPLE_CANDIDATES = (Integer) parser.getParam("sampleCandidates");
		if (parser.containsParam("printSamples"))   PRINT_SAMPLES = (Boolean) parser.getParam("printSamples");
		if (parser.containsParam("printResults"))   PRINT_RESULTS = (Boolean) parser.getParam("printResults");
		if (parser.containsParam("simpleSearch"))   SIMPLE_SEARCH = (Boolean) parser.getParam("simpleSearch");
		
	}
	
	/**
	 * update setting information from json file
	 * @param filename
	 * @throws Exception
	 */
	public static void updateSettings(String filename) throws Exception {
		// Parse Json
		String jsontext = readFile(filename);
		JSONParser json = new JSONParser();
		JSONObject obj = (JSONObject)json.parse(jsontext);
		
		Field[] fields = Settings.class.getFields();
		
		for (Field field:fields){
			if (!obj.containsKey(field.getName())) {
				throw new Exception("Cannot find variable \""+ field.getName() + " \" in settings File.");
			}
			
			field.setAccessible(true);
			Object value = obj.get(field.getName());
			if (value instanceof Long) {
				field.set(Settings.class, Integer.parseInt(value.toString()));
			}
			else if (value instanceof Float) {
				field.set(Settings.class, Double.parseDouble(value.toString()));
			}
			else
				field.set(Settings.class, value);
		}
	}
	
	public static int getCommentIdx(String s) {
		int idx = -1;
		
		if (s == null && s.length() <=1) return idx;
		
		boolean string = false;
		for(int x=0; x<s.length(); x++) {
			if (string == false && s.charAt(x) == '\"'){string = true;	continue;}      // string start
			if (string == true)
			{
				if (s.charAt(x) == '\\') {x++; continue;}                 // escape
				if (s.charAt(x) == '\"') {string = false;	continue;}      // string end
				continue;
			}
			
			if (s.charAt(x) == '/' && s.charAt(x+1) == '/'){
				idx = x;
				break;
			}
		}
		
		return idx;
	}
	
	
	public static String readFile(String filename) throws IOException, Exception{
		StringBuilder content = new StringBuilder();
		
	
		BufferedReader br = new BufferedReader(new FileReader(filename));
		while (true) {
			String line = br.readLine();
			if (line == null) break;
			
			// remove comment
			//int idx = 0;
			int idx = getCommentIdx(line);
			if (idx >= 0){
//			int idx = line.lastIndexOf("//");
//			int idx2 = line.lastIndexOf("\"");
//			if (idx>=0 && idx2<idx){
				line = line.substring(0, idx);
			}
			
			// append them into content
			content.append(line);
			content.append(System.lineSeparator());
		}
		
		return content.toString();
	}
	
	public static String getString(){
		Field[] fields = Settings.class.getFields();
		
		StringBuilder sb = new StringBuilder();
		sb.append("---------------------Settings----------------------\n");
		for (Field field:fields){
			sb.append(String.format("%-20s: ",field.getName()));
			
			field.setAccessible(true);
			Object value;
			try {
				value = field.get(Settings.class);
			}catch(IllegalAccessException e){
				value = "";
			}
			if (value instanceof Integer) sb.append((Integer)value);
			if (value instanceof Double) sb.append((Double)value);
			if (value instanceof Boolean) sb.append((Boolean)value);
			if (value instanceof String){
				sb.append("\"");
				sb.append((String)value);
				sb.append("\"");
			}
			
			sb.append("\n");
		}
		sb.append("---------------------------------------------------\n\n");
		
		return sb.toString();
	}
	
}
