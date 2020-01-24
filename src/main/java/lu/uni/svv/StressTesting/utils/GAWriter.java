package lu.uni.svv.StressTesting.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

public class GAWriter {
	BufferedWriter logger = null;
	Level level = Level.INFO;
	
	public GAWriter(String _filename, Level _level, String _title) {
		this(_filename, _level, _title,"logs");
	}
	
	public GAWriter(String _filename, Level _level,  String _title, String _basepath){
		this(_filename, _level, _title, _basepath, false);
	}
	
	public GAWriter(String _filename, Level _level, String _title, String _basepath, boolean _append) {
		this.level = _level;

		File fileDir = new File(_basepath+"/"+_filename);
		File parent = fileDir.getParentFile();
		int count=0;
		while (!parent.exists()){
			if (!fileDir.getParentFile().mkdirs()) {
				System.out.print("Creating error");
				System.out.println(fileDir.getAbsolutePath());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				}
				if (count > 3){
					System.out.println("Failed to create folder");
					System.exit(1);
				}
				count += 1;
			}
		}
		
		boolean flagTitle = false;
		if (fileDir.exists() == false && _title != null) flagTitle = true;
		
		FileWriter fw = null;
		try {
			fw = new FileWriter(fileDir.getAbsolutePath(), _append);
			logger = new BufferedWriter(fw);
			if (flagTitle == true){
				this.info(_title);
			}
		} catch (IOException e) {
			e.printStackTrace();

		} finally {
		}
	}
	
	public void close()
	{
		try {
			logger.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void print(String msg) {
		if (!(level==Level.INFO || level==Level.FINE)) return;
		try { 
			System.out.print(msg);
			logger.write(msg);
			logger.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void info(String msg) {
		if (!(level==Level.INFO || level==Level.FINE)) return;
		try { 
			logger.write(msg+"\n");
			logger.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	public void fine(String msg) {
		if (!(level==Level.FINE)) return;
		try {
			logger.write(msg+"\n");
			logger.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	


}
