package lu.uni.svv.StressTesting.search;

import lu.uni.svv.StressTesting.search.update.*;
import lu.uni.svv.StressTesting.utils.Settings;
import org.uma.jmetal.util.JMetalLogger;
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
	
	
	public static void displaySettings(){
		JMetalLogger.logger.info("Settings.BEST_RUN in Phase 1: "+ Settings.BEST_RUN);
		JMetalLogger.logger.info("Settings.INPUT_FILE: "+ Settings.INPUT_FILE);
		
		JMetalLogger.logger.info("Settings.RUNID in Phase 2 : "+ Settings.GA_RUN);
		JMetalLogger.logger.info("Settings.LR_WORKPATH      : "+ Settings.LR_WORKPATH);
		JMetalLogger.logger.info("Settings.TEST_DATA        : "+ Settings.TEST_DATA);
		JMetalLogger.logger.info("Settings.TEST_NSAMPLES    : "+ Settings.TEST_NSAMPLES);
		
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
		int[] targetTasks = convertToIntArray(Settings.TARGET_TASKLIST);
		Settings.INPUT_FILE = Settings.BASE_PATH + String.format("/Task%02d/input.csv", targetTasks[0]);
		
		// Showing settings
		displaySettings();
		
		//Run phase 2
		ModelUpdate object = null;
		if (Settings.STOP_CONDITION) {
			if (Settings.STOP_DATA_TYPE.compareTo("initial") == 0) {
				object = new ModelUpdateTermInitial(targetTasks);
			}
			else if (Settings.STOP_DATA_TYPE.compareTo("training") == 0) {
				object = new ModelUpdateTermTraining(targetTasks);
			}
			else if (Settings.STOP_DATA_TYPE.compareTo("pool") == 0) {
				object = new ModelUpdateTermPool(targetTasks);
			}
			else if (Settings.STOP_DATA_TYPE.compareTo("new") == 0) {
				object = new ModelUpdateTermNew(targetTasks);
			}
			else if (Settings.STOP_DATA_TYPE.compareTo("line") == 0) {
				object = new ModelUpdateTermLine(targetTasks);
			}
			else{
				JMetalLogger.logger.fine("Error:: Unknown Stop data type");
				return ;
			}
		}
		else{
			object = new ModelUpdate(targetTasks);
		}
		
		object.run();
	}
}