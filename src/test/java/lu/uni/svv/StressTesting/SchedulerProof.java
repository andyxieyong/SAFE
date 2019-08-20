package lu.uni.svv.StressTesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.logging.Level;

import junit.framework.TestCase;
import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.scheduler.RMSchedulerNorm;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.Settings;

/**
 * Unit test for simple App.
 */
public class SchedulerProof extends TestCase
{
	public static String BASE_PATH = "logs_test";
	
	public SchedulerProof( String testName )
	{
		super( testName );
		
		// open Directory
		File dir = new File(BASE_PATH);
		if (dir.exists() == false) {
			dir.mkdir();
		}
	}

	/**
	 * Test with Periodic tasks
	 * No deadline misses
	 */
	public void testProof() throws Exception
	{
		TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 3000, "RMScheduler"); // append last item
		//TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 100); // full data set
		TimeListSolution solution = problem.createSolution();

		RMScheduler.DETAIL = true;
		RMScheduler.PROOF = true;
		//RMScheduler scheduler = new RMScheduler(this);
		//RMScheduler scheduler = new RMSchedulerEx(problem, Settings.TASK_FITNESS);
		RMScheduler scheduler = new RMSchedulerNorm(problem, Settings.TASK_FITNESS);

		try {
			PrintStream printer = new PrintStream(new File(BASE_PATH+"/cpulog.log"));
			scheduler.setPrinter(printer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		scheduler.run(solution);
		
		double value = scheduler.getEvaluatedValue();
		double cpu = scheduler.getCPUusages();

		solution.setObjective(0, value);
		solution.setDeadlines(scheduler.getMissedDeadlineString());

		
		String details = scheduler.getExecutedTasksString();
		GAWriter writer = new GAWriter("scheduler_test_executions.csv", Level.INFO,  null, BASE_PATH);
		writer.info(details);
		writer.close();

		GAWriter timeline_writer = new GAWriter("scheduler_test_timeline.txt", Level.INFO,  null, BASE_PATH);
		timeline_writer.info(scheduler.getTimelinesStr());
		timeline_writer.close();

		GAWriter result_writer = new GAWriter("scheduler_test_result.txt", Level.INFO,  null, BASE_PATH);
		boolean result = scheduler.assertScheduler(result_writer);
		if (result == true){
			System.out.println("Succeed");
		}
		result_writer.close();
		System.out.println("All test are done.");
	}

}