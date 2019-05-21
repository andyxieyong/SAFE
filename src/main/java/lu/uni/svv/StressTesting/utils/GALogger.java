package lu.uni.svv.StressTesting.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GALogger{
	public Logger logger = null;
	
	public GALogger(String name, Level level) {
		logger = Logger.getLogger(name);  
		logger.setLevel(level);
	}
	
	public void setLevel(Level level) {
		logger.setLevel(level);
	}
	
	public void info(String msg) {
		logger.info(msg);
	}
	public void fine(String msg) {
		logger.fine(msg);
	}
	public void log(Level level, String msg) {
		logger.log(level, msg);
	}
	
	public void setFileLogger(String filename, Level level) {
		File logDir = new File("./logs/"); 
		if( !(logDir.exists()) )
			logDir.mkdir();
		
		FileHandler fh;
		try {
			fh = new FileHandler(filename);
			fh.setFormatter(new VerySimpleFormatter());
			fh.setLevel(level);
			logger.addHandler(fh);			
		} catch (SecurityException e){
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
	 }
	 
	 public void setConsoleLogger(Level level) {	
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(level);
		logger.addHandler(ch);
	}
}
