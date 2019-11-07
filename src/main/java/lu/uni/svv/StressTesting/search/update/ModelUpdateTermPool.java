package lu.uni.svv.StressTesting.search.update;

import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.renjin.sexp.Vector;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptException;

public class ModelUpdateTermPool extends ModelUpdate {
	
	public ModelUpdateTermPool() throws Exception{
		super();
	}
	
	public boolean prepareTerminationData()  throws ScriptException, EvalException{
		super.prepareTerminationData();

		engine.eval("termination_data <- test_data");
		return true;
	}
}