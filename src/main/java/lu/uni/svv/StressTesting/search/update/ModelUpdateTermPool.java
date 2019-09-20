package lu.uni.svv.StressTesting.search.update;

import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.renjin.sexp.Vector;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptException;

public class ModelUpdateTermPool extends ModelUpdate {
	
	public ModelUpdateTermPool(int[] _targetTasks) throws Exception{
		super(_targetTasks);
	}
	
	public boolean prepareTerminationData() {
		super.prepareTerminationData();
		try {
			engine.eval("termination_data <- test_data");

		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
}