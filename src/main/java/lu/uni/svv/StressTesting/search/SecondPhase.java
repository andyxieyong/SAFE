package lu.uni.svv.StressTesting.search;

import lu.uni.svv.StressTesting.search.update.*;
import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptException;
import java.io.File;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;



public class SecondPhase {
	
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
	
	
	public static void displaySettings(){
		JMetalLogger.logger.info("Settings.RUN_NUM            : "+ Settings.RUN_NUM);
		JMetalLogger.logger.info("Settings.INPUT_FILE         : "+ Settings.INPUT_FILE);
		JMetalLogger.logger.info("Settings.MAX_ITERATION      : "+ Settings.MAX_ITERATION);
		JMetalLogger.logger.info("Settings.UPDATE_ITERATION   : "+ Settings.UPDATE_ITERATION);
		JMetalLogger.logger.info("Settings.TARGET_TASKLIST    : "+ Settings.TARGET_TASKLIST);
		JMetalLogger.logger.info("Settings.GA_POPULATION      : "+ Settings.GA_POPULATION);
		JMetalLogger.logger.info("Settings.N_SAMPLE_WCET      : "+ Settings.N_SAMPLE_WCET);
		JMetalLogger.logger.info("");
		JMetalLogger.logger.info("Settings.MAX_ITERATION      : "+ Settings.MAX_ITERATION);
		JMetalLogger.logger.info("Settings.UPDATE_ITERATION   : "+ Settings.UPDATE_ITERATION);
		JMetalLogger.logger.info("Settings.LR_WORKPATH        : "+ Settings.LR_WORKPATH);
		JMetalLogger.logger.info("Settings.TEST_DATA          : "+ Settings.TEST_DATA);
		JMetalLogger.logger.info("Settings.TEST_NSAMPLES      : "+ Settings.TEST_NSAMPLES);
		JMetalLogger.logger.info("Settings.TEST_NGROUP        : "+ Settings.TEST_NGROUP);
		
		JMetalLogger.logger.info("");
		JMetalLogger.logger.info("Settings.STOP_DATA_TYPE     : "+ Settings.STOP_DATA_TYPE);
		JMetalLogger.logger.info("Settings.STOP_FUNC_NAME     : "+ Settings.STOP_FUNCTION_NAME);
		JMetalLogger.logger.info("Settings.STOP_CONDITION     : "+ Settings.STOP_CONDITION);
		JMetalLogger.logger.info("");
		JMetalLogger.logger.info("Settings.STOP_ACCEPT_RATE  : " + Settings.STOP_ACCEPT_RATE);
		
		JMetalLogger.logger.info("Settings.STOP_DATA_TYPE  : "+ Settings.STOP_DATA_TYPE);
		JMetalLogger.logger.info("Settings.STOP_FUNC_NAME  : "+ Settings.STOP_FUNCTION_NAME);
		JMetalLogger.logger.info("Settings.STOP_CONDITION  : "+ Settings.STOP_CONDITION);
	}
	
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
		
		File inputFile = new File(Settings.BASE_PATH + String.format("/input_reduced_run%02d.csv", Settings.RUN_NUM));
		if (inputFile.exists()){
			Settings.INPUT_FILE = inputFile.getPath();
		}
		else {
			Settings.INPUT_FILE = Settings.BASE_PATH + "/input.csv";
		}
		
		// Showing settings
		displaySettings();
		
		//Run phase 2
		ModelUpdate object = null;
		//if (Settings.STOP_CONDITION) {
		if (Settings.TEST_DATA.compareTo("kfold")==0){
			object = new ModelUpdateKFold(Settings.TARGET_TASKS);
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("refine")==0){
			object = new ModelUpdateRefine(Settings.TARGET_TASKS);
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("initial") == 0) {
			object = new ModelUpdateTermInitial(Settings.TARGET_TASKS);
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("training") == 0) {
			object = new ModelUpdateTermTraining(Settings.TARGET_TASKS);
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("pool") == 0) {
			object = new ModelUpdateTermPool(Settings.TARGET_TASKS);
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("new") == 0) {
			object = new ModelUpdateTermNew(Settings.TARGET_TASKS);
		}
		else{
			JMetalLogger.logger.fine("Error:: Unknown Stop data type");
			return ;
		}
//		}
//		else{
//			object = new ModelUpdate(targetTasks);
//		}
		try {
			long initTime = System.currentTimeMillis();
			object.run();
			long computingTime = System.currentTimeMillis() - initTime ;
			JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
			
		} catch(ScriptException | EvalException e){
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			e.printStackTrace();
		}
		catch(Exception e){
			JMetalLogger.logger.info("Error:: " + e.getMessage());
			e.printStackTrace();
		}
	}
}