package lu.uni.svv.StressTesting.search.update;

import org.renjin.eval.EvalException;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptException;

public class ModelUpdateTermTraining extends ModelUpdate {
	
	public ModelUpdateTermTraining(int[] _targetTasks) throws Exception{
		super(_targetTasks);
	}
	
	public boolean prepareTerminationData() {
		super.prepareTerminationData();
		JMetalLogger.logger.info("Termination data will be updated every model update");
		return true;
	}
	
	public boolean updateTerminationData(){
		try {
			engine.eval("termination_data<-training");

		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
		
	}

}