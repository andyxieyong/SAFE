package lu.uni.svv.StressTesting;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import lu.uni.svv.StressTesting.search.model.SimpleTLCrossover;
import lu.uni.svv.StressTesting.search.model.TaskDescriptor;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;

public class TestCrossover  extends TestCase{
	public TestCrossover( String testName )
	{
		super( testName );
		/* Apply Common Environment */
	}
	
	/**
	 * Test with Periodic tasks
	 * No deadline misses 
	 */
	public void testTLCrossover() throws Exception
	{
		TaskDescriptor.UNIQUE_ID = 1;
		System.out.println("--------Test SimpleTLCrossover-------");
		
		TestingProblem problem = new TestingProblem("res/sample_aperiodic_2.csv", 1, 60, "RMScheduler");
		for(int x=0; x<10; x++) {
			List<TimeListSolution> parents = new ArrayList<TimeListSolution>();
			parents.add(new TimeListSolution(problem));
			parents.add(new TimeListSolution(problem));
			
			System.out.println("P1: " + parents.get(0).getVariableValueString());
			System.out.println("P2: " + parents.get(1).getVariableValueString());	
			
			SimpleTLCrossover crossover = new SimpleTLCrossover(0.8);
			
			List<TimeListSolution> children = crossover.execute(parents);
			
			System.out.println("C1: " + children.get(0).getVariableValueString());
			System.out.println("C2: " + children.get(1).getVariableValueString());
			System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		}
		 
	}	
	
}
