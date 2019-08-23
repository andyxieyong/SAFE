package lu.uni.svv.StressTesting.search;

import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptEngine;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.READ;

public class Phase1Loader {
	TestingProblem problem = null;
	int[] targetTasks = null;
	
	public Phase1Loader(TestingProblem _problem, int[] _targetTasks){
		problem = _problem;
		targetTasks = _targetTasks;
	}
	
	/**
	 * loadSolutions from multiple tasks
	 * @param _path
	 * @param _bestRun
	 * @return
	 */
	public List<TimeListSolution> loadMultiTaskSolutions(String _path, int _bestRun){
		
		// load solutions for multiple tasks
		List<TimeListSolution> solutions = new ArrayList<TimeListSolution>();
		
		for (int taskID:targetTasks){
			
			String path = String.format("%s/Task%02d", _path, taskID);
			
			List<TimeListSolution> solutionsPart = this.loadSolutions(path, _bestRun);
			
			solutions.addAll(solutionsPart);
		}
		
		return solutions;
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
	
	public boolean makeInitialPoints(String _inputPath, String _outputPath, int _bestRun, String _workfile)	{
		
		File file = new File(String.format("%s/%s", _outputPath, _workfile));
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		// move inputs into output file
		Path outFile= Paths.get(file.getPath());
		try {
			FileChannel out=FileChannel.open(outFile, CREATE, TRUNCATE_EXISTING, WRITE);
			int titleLength = 0;
			for (int x=0; x<targetTasks.length;x++) {
				int taskID = targetTasks[x];
				String datafile = String.format("%s/Task%02d/samples/sampledata_run%02d.csv", _inputPath, taskID, _bestRun);
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
			return false;
		}
		
		return true;
	}
}
