package lu.uni.svv.StressTesting.search.update;

import org.renjin.eval.EvalException;
import org.renjin.sexp.StringVector;
import org.uma.jmetal.util.JMetalLogger;
import javax.script.ScriptException;


/**
 * Not used class. This is included in the ModelUpdata class.
 */
public class ModelUpdateRandom extends ModelUpdate {
	
	public ModelUpdateRandom() throws Exception{
		super();
	}
	
	public long[] samplingNewPoints(int nSample, int nCandidate, double P)  throws ScriptException, EvalException{
		// sampling new points by random
		long[] samples = null;
		
		engine.eval("tnames <- get_task_names(training)");
		String codeText = String.format("sampled_data <- sample_by_random(tnames, nSample=%d)", nSample);
		engine.eval(codeText);
		
		StringVector nameVector = (StringVector)engine.eval("colnames(sampled_data)");
		String[] names = nameVector.toArray();
		
		samples = get_row_longlist("sampled_data");

		
		return samples;
	}
}