package lu.uni.svv.StressTesting.samples;

import java.io.FileNotFoundException;
import java.util.List;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.IntegerSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.ProblemUtils;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

public class NSGAIIRunner extends AbstractAlgorithmRunner {
	/**
	 * @param args Command line arguments.
	 * @throws JMetalException
	 * @throws FileNotFoundException Invoking command: java org.uma.jmetal.runner.multiobjetive.NSGAIIRunner problemName [referenceFront]
	 */
	public static void main(String[] args) throws JMetalException, FileNotFoundException {
		/*
		 * The first part of the main method declares the type of the problem to solve
		 * (a problem dealing with DoubleSolution individuals in this example)
		 * and the operators.
		 * The referenceParetoFront is used to indicate the name of the optional reference front:
		 */

		String referenceParetoFront = "";

		/*
		 * The next group of sentences parse the program arguments.
		 * A benchmark problem (ZDT1 in the example) is solved by default
		 * when no arguments are indicated:
		 */
		Problem<DoubleSolution> problem;
		String problemName;
		if (args.length == 1) {
			problemName = args[0];
		} else if (args.length == 2) {
			problemName = args[0];
			referenceParetoFront = args[1];
		} else {
			problemName = "org.uma.jmetal.problem.multiobjective.zdt.ZDT1";
			referenceParetoFront = "jmetal-problem/src/test/resources/pareto_fronts/ZDT1.pf";
		}
		
		/*
		Next, the problem is loaded using its class name:
		*/
		problem = ProblemUtils.<DoubleSolution>loadProblem(problemName);
		
		IntegerSolution solution;
		/*
		 * Then, the operators and the algorithm are configured:
		 */
		// Define operators (crossover)
		CrossoverOperator<DoubleSolution> crossover;
		double crossoverProbability = 0.9;
		double crossoverDistributionIndex = 20.0;
		crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

		// Define operators (mutation)
		MutationOperator<DoubleSolution> mutation;
		double mutationProbability = 1.0 / problem.getNumberOfVariables();
		double mutationDistributionIndex = 20.0;
		mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

		// Define operators (selection)
		SelectionOperator<List<DoubleSolution>, DoubleSolution> selection;
		selection = new BinaryTournamentSelection<DoubleSolution>(new RankingAndCrowdingDistanceComparator<DoubleSolution>());

		// Define algorithm with operators and configurations
		Algorithm<List<DoubleSolution>> algorithm;
		algorithm = new NSGAIIBuilder<DoubleSolution>(problem, crossover, mutation)
				.setSelectionOperator(selection)
				.setMaxEvaluations(25000)
				.setPopulationSize(100)
				.build();
		/*
		 * The last step is to run the algorithm and to write the obtained solutions
		 * into two files: one for the variable values and one for the objective values;
		 * optionally, if a reference front has been provided it also prints the values
		 * of all the available quality indicators for the computed results:
		 */
		AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

		List<DoubleSolution> population = algorithm.getResult();
		long computingTime = algorithmRunner.getComputingTime();

		JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");

		printFinalSolutionSet(population);
		if (!referenceParetoFront.equals("")) {
			printQualityIndicators(population, referenceParetoFront);
		}
	}
}
