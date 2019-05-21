package lu.uni.svv.StressTesting.datatype;

import java.math.BigDecimal;

public class SummaryItem {
	public BigDecimal AverageFitness;
	public BigDecimal BestFitness;
	
	public SummaryItem(BigDecimal _best, BigDecimal _avg) {
		AverageFitness = _avg;
		BestFitness = _best;
	}
}