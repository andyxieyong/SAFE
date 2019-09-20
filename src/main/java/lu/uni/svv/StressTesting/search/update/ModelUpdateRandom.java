package lu.uni.svv.StressTesting.search.update;

import org.renjin.eval.EvalException;
import org.renjin.sexp.StringVector;
import org.uma.jmetal.util.JMetalLogger;
import javax.script.ScriptException;


/**
 * Not used class. This is included in the ModelUpdata class.
 */
public class ModelUpdateRandom extends ModelUpdate {
	
	public ModelUpdateRandom(int[] _targetTasks) throws Exception{
		super(_targetTasks);
	}
	
	public long[] samplingNewPoints(int nSample, int nCandidate, double P){
		// sampling new points by random
		long[] samples = null;
		try {
			engine.eval("tnames <- get_task_names(training)");
			String codeText = String.format("sampled_data <- get_random_sampling(tnames, nSample=%d)", nSample);
			engine.eval(codeText);
			
			StringVector nameVector = (StringVector)engine.eval("colnames(sampled_data)");
			String[] names = nameVector.toArray();
			
			samples = get_row_longlist("sampled_data");
			
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return null;
		}
		
		return samples;
	}
}