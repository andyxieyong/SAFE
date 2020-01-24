package lu.uni.svv.StressTesting.search.update;

import org.renjin.eval.EvalException;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptException;

public class ModelUpdateTermTraining extends ModelUpdate {
	
	public ModelUpdateTermTraining() throws Exception{
		super();
	}
	
	public boolean prepareTerminationData()  throws ScriptException, EvalException{
		super.prepareTerminationData();
		JMetalLogger.logger.info("Termination data will be updated every model update");
		return true;
	}
	
	public boolean updateTerminationData() throws ScriptException, EvalException{
		engine.eval("termination_data<-training");

		return true;
		
	}

}