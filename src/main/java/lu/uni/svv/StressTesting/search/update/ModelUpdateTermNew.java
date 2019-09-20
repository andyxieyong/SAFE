package lu.uni.svv.StressTesting.search.update;

import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptException;

public class ModelUpdateTermNew extends ModelUpdate {
	
	public ModelUpdateTermNew(int[] _targetTasks) throws Exception{
		super(_targetTasks);
	}
	
	public boolean prepareTerminationData() {
		super.prepareTerminationData();
		JMetalLogger.logger.info("Termination data will be updated every model update");
		return true;
	}
	
	public boolean updateTerminationData(){
		// use newly added data in the training
		try {
			engine.eval("tSize<-nrow(training)");
			engine.eval(String.format("termination_data<-training[(tSize-%d):tSize,]", Settings.UPDATE_ITERATION));
		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
		
	}
}