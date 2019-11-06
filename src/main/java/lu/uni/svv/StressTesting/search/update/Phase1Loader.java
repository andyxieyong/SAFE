package lu.uni.svv.StressTesting.search.update;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.READ;

public class Phase1Loader {
	TestingProblem problem = null;
	
	public Phase1Loader(TestingProblem _problem){
		problem = _problem;
	}
	
	/**
	 * Load solution from the file
	 * @param _path
	 * @return
	 */
	public List<TimeListSolution> loadSolutions(String _path, int _runID) {
		String path = _path + "/solutions/";
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
			String runID = name.substring(name.indexOf('_') + 4, name.lastIndexOf('_'));
			if (Integer.parseInt(runID) != _runID) continue;
			
			TimeListSolution s = TimeListSolution.loadFromJSON(problem, file.getAbsolutePath());
			solutions.add(s);
//			JMetalLogger.logger.info("loaded solution from " + file.getName());
		}
		return solutions;
	}
	
	public boolean makeInitialPoints(String _inputPath, String _outputPath, int _bestRun, String _workfile)	{
		
		Path targetFile = Paths.get(String.format("%s/%s", _outputPath, _workfile));
		File file = targetFile.toFile();
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		Path sourceFile = Paths.get(String.format("%s/samples/sampledata_run%02d.csv", _inputPath, _bestRun));
		try {
			Files.copy(sourceFile, targetFile);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}
