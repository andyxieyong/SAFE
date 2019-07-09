package lu.uni.svv.StressTesting.datatype;


public class SummaryItem {
	public double AverageFitness;
	public double BestFitness;
	
	public SummaryItem(double _best, double _avg) {
		AverageFitness = _avg;
		BestFitness = _best;
	}
}