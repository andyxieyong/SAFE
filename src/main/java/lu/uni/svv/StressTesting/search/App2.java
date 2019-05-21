package lu.uni.svv.StressTesting.search;

import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;

import java.io.IOException;

public class App2 {
	public static void main( String[] args ) throws IOException
	 {
		 TestingProblem problem = new TestingProblem("res/sample_mixed_1.csv", "RMScheduler");
		 
		 TimeListSolution s = new TimeListSolution(problem);
		 System.out.println(s.getVariableValueString());
	 }
}
