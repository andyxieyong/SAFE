package lu.uni.svv.StressTesting.utils;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;

import lu.uni.svv.StressTesting.utils.ArgumentParser.DataType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;


public class Settings {
	public static String  INPUT_FILE          = "";
	public static String  BASE_PATH           = "logs";
	public static String  EXTEND_PATH         = "";
	public static String  WORKNAME            = "";
	public static int     RUN_NUM             = 1;
	public static int     RUN_MAX             = 1;
	public static int     RUN_PARTITION       = 0;
	
	// Scheduler
	public static String  SCHEDULER           = "";
	public static String  TARGET_TASKLIST     = "";
	public static int[]   TARGET_TASKS        = null;
	public static double  TIME_QUANTA         = 0.1;
	public static int     TIME_MAX            = 3600000;
	public static boolean EXTEND_SCHEDULER    = true;

	// GA
	public static int     GA_POPULATION       = 10;
	public static int     GA_ITERATION        = 1000;
	public static double  GA_CROSSOVER_PROB   = 0.9;
	public static double  GA_MUTATION_PROB    = 0.5;
	public static boolean SIMPLE_SEARCH       = false;
	public static String  GA_REPR_FITNESS     = "average";
	public static int     N_SAMPLE_WCET       = 0;
	public static boolean UNIFORM_SAMPLE      = false;
	public static double  A12_THRESHOLD       = 0.5;
	//printing
	public static boolean PRINT_SAMPLES       = false;
	public static boolean PRINT_RESULTS       = false;
	
	//Second phase
	public static String  SECOND_PHASE_RUNTYPE= "distance";
	public static int     N_MODEL_UPDATES     = 20;
	public static int     N_EXAMPLE_POINTS    = 100;
	public static double  BORDER_PROBABILITY  = 0.5;
	public static int     SAMPLE_CANDIDATES   = 20;
	
	public static String  LR_FORMULA_PATH     = "";
	public static int     LR_INITIAL_SIZE     = 0;
	
	public static boolean STOP_CONDITION      = false;
	public static String  STOP_DATA_TYPE      = "training";
	public static String  STOP_FUNCTION_NAME  = "fpr";
	public static double  STOP_ACCEPT_RATE    = 0.01;
	
	public static String  TEST_DATA           = "testdata";
	public static int     TEST_NSAMPLES       = 100;
	public static int     TEST_NGROUP         = 10;

	
	public Settings()
	{
	}
	
	public static void update(String[] args) throws Exception {
		// Setting arguments
		ArgumentParser parser = new ArgumentParser();
		parser.addOption(false,"Help", DataType.BOOLEAN, "h", "help", "Show how to use this program");
		parser.addOption(false,"SettingFile", DataType.STRING, "f", null, "Base setting file.", "settings.json");
		parser.addOption(false,"INPUT_FILE", DataType.STRING, null, "data", "input data that including job information");
		parser.addOption(false,"BASE_PATH", DataType.STRING, "b", null, "Base path to save the result of experiments");
		parser.addOption(false,"EXTEND_PATH", DataType.STRING, "e", null, "Exported path to move the result of experiments");
		parser.addOption(false,"WORKNAME", DataType.STRING, "w", "workName", "the path for saving workdata in second phase");
		parser.addOption(false,"RUN_NUM", DataType.INTEGER, null, "runID", "Specific run ID when you execute run separately");
		parser.addOption(false,"RUN_MAX", DataType.INTEGER, "r", null, "Maximum run times for GA");
		parser.addOption(false,"RUN_PARTITION", DataType.INTEGER, null, "part", "partition for Test Generation");
		
		//scheduler
		parser.addOption(false,"SCHEDULER", DataType.STRING, "s", null, "Scheduler");
		parser.addOption(false,"TARGET_TASKLIST", DataType.STRING, "t", "targets","target tasks for search");
		parser.addOption(false,"TIME_QUANTA", DataType.DOUBLE, null, "quanta", "Scheduler time quanta");
		parser.addOption(false,"TIME_MAX", DataType.INTEGER, null, "max", "scheduler time max");
		parser.addOption(false,"EXTEND_SCHEDULER", DataType.BOOLEAN, null, "extendScheduler", "Scheduler extend when they finished simulation time, but the queue remains", true);
		
		// GA
		parser.addOption(false,"GA_POPULATION", DataType.INTEGER, "p", null, "Population for GA");
		parser.addOption(false,"GA_ITERATION", DataType.INTEGER, "i", null, "Maximum iterations for GA");
		parser.addOption(false,"GA_CROSSOVER_PROB", DataType.DOUBLE, "c", null, "Crossover rate for GA");
		parser.addOption(false,"GA_MUTATION_PROB", DataType.DOUBLE, "m", null, "Mutation rate for GA");
		parser.addOption(false,"SIMPLE_SEARCH", DataType.BOOLEAN, null, "simpleSearch", "Simple search mode, not using crossover and mutation just produce children randomly", false);
		parser.addOption(false,"GA_REPR_FITNESS", DataType.STRING, null, "reprFitness", "one type of fitness among average, maximum or minimum");
		
		parser.addOption(false,"PRINT_SAMPLES", DataType.BOOLEAN, null, "printSamples", "If you set this parameter, The program will produce sampling detail information", false);
		parser.addOption(false,"PRINT_RESULTS", DataType.BOOLEAN, null, "printResults", "If you set this parameter, The program will produce fitness detail information", false);

		parser.addOption(false,"N_SAMPLE_WCET", DataType.INTEGER, null, "nSamples", "The number of samples that will extracted between minWCET and maxWCET");
		parser.addOption(false,"UNIFORM_SAMPLE", DataType.BOOLEAN, null, "uniform", "uniform sample when we are sampling from a range of WCET", false);
		parser.addOption(false,"A12_THRESHOLD", DataType.DOUBLE, null, "A12Threshold", "Threshold for A12");
		
		//Second phase
		parser.addOption(false,"SECOND_PHASE_RUNTYPE", DataType.STRING, null, "secondRuntype", "Second phase run type {\"random\", \"distance\"}");
		parser.addOption(false,"N_MODEL_UPDATES", DataType.INTEGER, null, "modelUpdates", "The iteration number to finish second phase");
		parser.addOption(false,"N_EXAMPLE_POINTS", DataType.INTEGER, null, "exPoints", "The iteration number to update logsitic regression model");
		parser.addOption(false,"BORDER_PROBABILITY", DataType.DOUBLE, null, "borderProb", "Border Probability for second phase");
		parser.addOption(false,"SAMPLE_CANDIDATES", DataType.INTEGER, null, "sampleCandidates", "The number of sandidates to get one sample in second phase");
		
		parser.addOption(false,"LR_FORMULA_PATH", DataType.STRING, null, "formulaPath", "formula file path to use in second phase");
		parser.addOption(false,"LR_INITIAL_SIZE", DataType.INTEGER, null, "LRinitSize", "the number of initial training data size for the second phase");
		
		parser.addOption(false,"TEST_DATA", DataType.STRING, null, "testData", "test data file");
		parser.addOption(false,"TEST_NSAMPLES", DataType.INTEGER, null, "testSamples", "number of samples for testing of each model");
		parser.addOption(false,"TEST_NGROUP", DataType.INTEGER, null, "testGroups", "the number of test data set");
		
		parser.addOption(false,"STOP_ACCEPT_RATE", DataType.DOUBLE, null, "stopProbAccept", "acceptance probability of second phase model");
		parser.addOption(false,"STOP_DATA_TYPE", DataType.STRING, null, "stopDataType", "test data type: training, new, initial, pool");
		parser.addOption(false,"STOP_FUNCTION_NAME", DataType.STRING, null, "stopFuncName", "function name for testing to decide when we stop");
		parser.addOption(false,"STOP_CONDITION", DataType.BOOLEAN, "x", null, "Stop with stopping condition when this parameter set", false);
		
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
		Settings.updateSettings(filename);      //Update settings from the settings.json file.
		updateFromParser(parser);               //Update settings from the command parameters
		
		Settings.TARGET_TASKS = convertToIntArray(Settings.TARGET_TASKLIST);
		Arrays.sort(Settings.TARGET_TASKS);
		
		if (Settings.EXTEND_PATH.length()==0)
			Settings.EXTEND_PATH = Settings.BASE_PATH + "/updates";
			
		
	}
	
	public static int[] convertToIntArray(String commaSeparatedStr) {
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
	
	public static void updateFromParser(ArgumentParser _parser) throws Exception {
		Field[] fields = Settings.class.getDeclaredFields();
		for (Field field:fields){
			String fieldName = field.getName();
			Object param = _parser.getParam(fieldName);
			if (param == null) continue;
			if (fieldName.compareTo("TARGET_TASKS")==0) continue;
			
			try {
				field.setAccessible(true);
				
				if (_parser.getDataType(fieldName)==DataType.STRING)
					field.set(null, (String)param);
				else if (_parser.getDataType(fieldName)==DataType.INTEGER)
					field.setInt(null, (Integer)_parser.getParam(fieldName));
				else if (_parser.getDataType(fieldName)==DataType.BOOLEAN)
					field.setBoolean(null, (Boolean)_parser.getParam(fieldName));
				else if (_parser.getDataType(fieldName)==DataType.DOUBLE)
					field.setDouble(null, (Double)_parser.getParam(fieldName));
				else {
					throw new Exception("Undefined data type for " + fieldName);
				}
				
				field.setAccessible(false);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static int getCommentIdx(String s) {
		int idx = -1;
		
		if (s == null && s.length() <=1) return idx;
		
		boolean string = false;
		for(int x=0; x<s.length(); x++) {
			if (!string && s.charAt(x) == '\"'){string = true;	continue;}      // string start
			if (string)
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
			int idx = getCommentIdx(line);
			if (idx >= 0){
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
			if (value instanceof int[]){
				sb.append("[");
				for (int x=0; x<((int[]) value).length; x++){
					if (x!=0) sb.append(", ");
					sb.append(((int[])value)[x]);
				}
				sb.append("]");
			}
			
			sb.append("\n");
		}
		sb.append("---------------------------------------------------\n\n");
		
		return sb.toString();
	}
	
}
