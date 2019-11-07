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
		JMetalLogger.logger.info("INPUT_FILE         : "+ Settings.INPUT_FILE);
		JMetalLogger.logger.info("BASE_PATH          : "+ Settings.BASE_PATH);
		JMetalLogger.logger.info("EXTEND_PATH        : "+ Settings.EXTEND_PATH);
		JMetalLogger.logger.info("WORKNAME           : "+ Settings.WORKNAME);
		JMetalLogger.logger.info("RUN_NUM            : "+ Settings.RUN_NUM);
		JMetalLogger.logger.info("");
		// Scheduler variables
		JMetalLogger.logger.info("SCHEDULER          :"+  Settings.SCHEDULER);
		JMetalLogger.logger.info("TARGET_TASKS       :"+  Settings.TARGET_TASKLIST);
		JMetalLogger.logger.info("TIME_QUANTA        :"+  Settings.TIME_QUANTA);;
		JMetalLogger.logger.info("TIME_MAX           :"+  Settings.TIME_MAX);
		JMetalLogger.logger.info("EXTEND_SCHEDULER   :"+  Settings.SCHEDULER);
		JMetalLogger.logger.info("");
		JMetalLogger.logger.info("GA_POPULATION      : "+ Settings.GA_POPULATION);
		JMetalLogger.logger.info("GA_ITERATION       : "+ Settings.GA_ITERATION);
		JMetalLogger.logger.info("N_SAMPLE_WCET      : "+ Settings.N_SAMPLE_WCET);
		JMetalLogger.logger.info("");
		JMetalLogger.logger.info("N_MODEL_UPDATES    : "+ Settings.N_MODEL_UPDATES);
		JMetalLogger.logger.info("N_EXAMPLE_POINTS   : "+ Settings.N_EXAMPLE_POINTS);
		JMetalLogger.logger.info("");
		JMetalLogger.logger.info("TEST_DATA          : "+ Settings.TEST_DATA);
		JMetalLogger.logger.info("TEST_NSAMPLES      : "+ Settings.TEST_NSAMPLES);
		JMetalLogger.logger.info("TEST_NGROUP        : "+ Settings.TEST_NGROUP);
		JMetalLogger.logger.info("");
		JMetalLogger.logger.info("STOP_DATA_TYPE     : "+ Settings.STOP_DATA_TYPE);
		JMetalLogger.logger.info("STOP_FUNC_NAME     : "+ Settings.STOP_FUNCTION_NAME);
		JMetalLogger.logger.info("STOP_CONDITION     : "+ Settings.STOP_CONDITION);
		JMetalLogger.logger.info("STOP_ACCEPT_RATE   : " + Settings.STOP_ACCEPT_RATE);
		JMetalLogger.logger.info("");
		
		
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
		
		File inputFile = new File(Settings.BASE_PATH + String.format("/inputs/reduced_run%02d.csv", Settings.RUN_NUM));
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
			object = new ModelUpdateKFold();
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("refine")==0){
			object = new ModelUpdateRefine();
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("initial") == 0) {
			object = new ModelUpdateTermInitial();
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("training") == 0) {
			object = new ModelUpdateTermTraining();
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("pool") == 0) {
			object = new ModelUpdateTermPool();
		}
		else if (Settings.STOP_DATA_TYPE.compareTo("new") == 0) {
			object = new ModelUpdateTermNew();
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