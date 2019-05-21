package lu.uni.svv.StressTesting;

import org.uma.jmetal.util.JMetalException;

import junit.framework.TestCase;
import lu.uni.svv.StressTesting.search.model.SimpleTLMutation4;
import lu.uni.svv.StressTesting.search.model.TaskDescriptor;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;

public class TestMutation  extends TestCase{
	public TestMutation( String testName )
	{
		super( testName );
	}
	
	/**
	 * Test with Periodic tasks
	 * No deadline misses 
	 */
	public void testTLMutation() throws Exception
	{
		TaskDescriptor.UNIQUE_ID = 1;
		System.out.println("--------Test SimpleTLMutation-------");
		
		TestingProblem problem = new TestingProblem("../res/samples/sample_mixed_1.csv", 1, 50, "RMScheduler");
		
		TimeListSolution solution = new TimeListSolution(problem);
		System.out.println(solution.getLineVariableValueString(100));
		System.out.println();

		SimpleTLMutation4 mutation = new SimpleTLMutation4(problem, 1);
		for(int x=0; x<100; x++) {
			TimeListSolution mutated = mutation.execute(solution);
			System.out.print("("+ mutation.taskID + ", " + mutation.position+")->" + mutation.newValue + "\t:: ");
			System.out.println(mutated.getLineVariableValueString(30));
		}
		
		System.out.print("Null test-->");
		try {
			TimeListSolution mutated = mutation.execute(null);
		}
		catch(JMetalException e){
			System.out.print("True");
			assertTrue(true);
		}
		
		 
	}
	
}
