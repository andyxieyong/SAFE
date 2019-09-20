package lu.uni.svv.StressTesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.logging.Level;

import javafx.scene.layout.Priority;
import junit.framework.TestCase;
import lu.uni.svv.StressTesting.datatype.Task;
import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.scheduler.RMSchedulerEx;
import lu.uni.svv.StressTesting.scheduler.RMSchedulerNorm;
import lu.uni.svv.StressTesting.scheduler.RMSchedulerRange;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.Settings;

/**
 * Unit test for simple App.
 */
public class SchedulerProof extends TestCase
{
	public static String BASE_PATH = "../results/TimelineLX3";
	
	public SchedulerProof( String testName )
	{
		super( testName );
		
		// open Directory
		File dir = new File(BASE_PATH);
		if (dir.exists() == false) {
			dir.mkdirs();
		}
	}

	public void testQueue() throws Exception{
//		TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 3000, "RMScheduler"); // append last item
//		TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 3000, "RMScheduler"); // append last item
//		TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 100); // full data set
		TestingProblem problem = new TestingProblem("../res/samples/sample_mixed_1.csv", 0.1, 1000, "RMSchedulerRange"); // append last item
		TimeListSolution solution = problem.createSolution();
		
		int[] indexTable = new int[problem.Tasks.length];
		Arrays.fill(indexTable, 0);
		
		PriorityQueue<Task> Q = new PriorityQueue<>(60000, queueComparator);
		Random rand =new Random();
		
		for(int time=0; time<100; time++){
			int tID = rand.nextInt(problem.Tasks.length) + 1;
			Q.add(new Task(tID, indexTable[tID-1], 5, time,10, problem.Tasks[tID-1].Priority));
			
			indexTable[tID-1] += 1;
		}
		while(!Q.isEmpty()){
			Task t = Q.poll();
			System.out.println(t.str());
		}
	}
	public Comparator<Task> queueComparator = new Comparator<Task>(){
		@Override
		public int compare(Task t1, Task t2) {
			if (t1.Priority > t2.Priority)
				return 1;
			else if (t1.Priority < t2.Priority)
				return -1;
			else{
				if (t1.ExecutionID < t2.ExecutionID)
					return -1;
				else if (t1.ExecutionID > t2.ExecutionID)
					return 1;
				return 0;
			}
		}
	};
	
	/**
	 * Test with Periodic tasks
	 * No deadline misses
	 */
	public void testProof() throws Exception
	{
//		TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 3000, "RMScheduler"); // append last item
//		TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 3000, "RMScheduler"); // append last item
//		TestingProblem problem = new TestingProblem("res/LS_data_1018_5.csv", 0.1, 100); // full data set
//		TestingProblem problem = new TestingProblem("../res/samples/sample_mixed_1.csv", 0.1, 100, "RMSchedulerRange"); // append last item
		TestingProblem problem = new TestingProblem("../res/LS_data_20190905_fixed.csv", 0.1, 3000, "RMSchedulerRange"); // append last item
		TimeListSolution solution = problem.createSolution();
		
		//testQueue(problem);
		
		RMScheduler.DETAIL = true;
		RMScheduler.PROOF = true;
//		RMScheduler scheduler = new RMScheduler(this);
//		RMScheduler scheduler = new RMSchedulerEx(problem, Settings.TASK_FITNESS);
//		RMScheduler scheduler = new RMSchedulerNorm(problem, Settings.TASK_FITNESS);
		RMScheduler scheduler = new RMSchedulerRange(problem, Settings.TASK_FITNESS);

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
	
	/**
	 * Test with Periodic tasks
	 * No deadline misses
	 */
	public void testLuxSpace() throws Exception {
		String workingPath = String.format("%s/testcases", BASE_PATH);
		String schedulerPath = String.format("%s/scheduler", BASE_PATH);
		// open Directory
		File dir = new File(workingPath);
		if (dir.exists() == false) dir.mkdirs();
		dir = new File(schedulerPath);
		if (dir.exists() == false) dir.mkdirs();
		
		TestingProblem problem = new TestingProblem("../res/LS_data_20190905_fixed.csv", 0.1, 6000, "RMSchedulerRange"); // append last item
		RMScheduler.DETAIL = true;
		RMScheduler.PROOF = true;
		
		PrintStream resultsStream = new PrintStream(new File(BASE_PATH + "/message.txt"));
		
		for(int testID=0; testID < 1000; testID++){
			System.out.println(String.format("Testing %d ...", testID));
			RMScheduler scheduler = new RMSchedulerRange(problem, Settings.TASK_FITNESS);
			TimeListSolution solution = problem.createSolution();
			try {
				PrintStream printer = new PrintStream(new File(String.format("%s/cpulog_%d.log", workingPath, testID)));
				scheduler.setPrinter(printer);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			scheduler.run(solution);
			
			String details = scheduler.getAllExecutedTasksString();
			GAWriter writer = new GAWriter(String.format("executions_%d.csv", testID), Level.INFO,  null, schedulerPath);
			writer.info(details);
			writer.close();
			
			Task t = scheduler.getMissedDeadlineTask(0);
			if (t==null){
				resultsStream.println(String.format("Test %d:\tNo Deadline miss",testID));
			}
			else{
				int missed = (int) (t.FinishedTime - (t.ArrivedTime + t.Deadline));
				resultsStream.println(String.format("Test %d:\tDeadline missed: Task%02d (%d)", testID, t.ID, missed));
				System.out.println(String.format("Deadline missed: Task%02d (%d)", t.ID, missed));
			}
		}
		System.out.println("Finished all");
		resultsStream.close();
	}

}