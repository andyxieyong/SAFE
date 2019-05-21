package lu.uni.svv.StressTesting;

import junit.framework.TestCase;
import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.scheduler.RMSchedulerEx;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;

import java.math.BigDecimal;

public class EvaluationTest extends TestCase {
	
	public EvaluationTest( String testName )
	{
		super( testName );
	}
	
	/**
	 * Test with Periodic tasks
	 * No deadline misses 
	 */
	public void testPeriodic_1() throws Exception
	{
		System.out.println("-----------periodic Test1----------------");
		TestingProblem problem = new TestingProblem("res/sample_periodic_1.csv", 1, 60, "RMScheduler");
		
		RMScheduler scheduler = new RMSchedulerEx(problem);
		OneExecution(problem, scheduler, 5);
	}
	
	/**
	 * Test with Periodic tasks
	 * No deadline misses 
	 */
	public void testAperiodic_4() throws Exception
	{
		System.out.println("-----------aperiodic Test4----------------");
		TestingProblem problem = new TestingProblem("res/sample_aperiodic_4.csv", 1, 60, "RMScheduler");
		
		RMScheduler scheduler = new RMSchedulerEx(problem);
		OneExecution(problem, scheduler, 5);
		
	}
	
	private void OneExecution(TestingProblem problem, RMScheduler scheduler, int TestNum) {
		for (int x=0; x<TestNum; x++) {
			TimeListSolution solution = new TimeListSolution(problem);
			scheduler.run(solution);
			
			BigDecimal value = scheduler.getEvaluatedValue();
			
			System.out.println(String.format("%.32e - Chromosome: %s", value, solution.getVariableValueString()));
			
		}
	}
	
}
