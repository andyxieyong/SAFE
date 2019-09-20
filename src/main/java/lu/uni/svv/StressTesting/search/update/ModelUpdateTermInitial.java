package lu.uni.svv.StressTesting.search.update;

import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.renjin.sexp.StringVector;
import org.renjin.sexp.Vector;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptException;

public class ModelUpdateTermInitial extends ModelUpdate {
	
	public ModelUpdateTermInitial(int[] _targetTasks) throws Exception{
		super(_targetTasks);
	}
	
	public boolean prepareTerminationData() {
		super.prepareTerminationData();
		try {
			engine.eval("half_size <-nrow(training)/2");
			engine.eval("testPool <-training[(half_size+1):nrow(training),]");
			engine.eval("training <-training[1:half_size,]");
			
			engine.eval("positive <-testPool[testPool$result==0,]");
			engine.eval("negative <-testPool[testPool$result==1,]");
			engine.eval("negative <-negative[sample(nrow(negative),nrow(positive)),]");
			engine.eval("termination_data <- rbind(positive, negative)");
			Vector dataVector = (Vector) engine.eval("nrow(positive)");
			int nPositive = dataVector.getElementAsInt(0);
			dataVector = (Vector) engine.eval("nrow(negative)");
			int nNegative = dataVector.getElementAsInt(0);
			JMetalLogger.logger.info("Test Data (positive): " + nPositive);
			JMetalLogger.logger.info("Test Data (negative): " + nNegative);

		} catch (ScriptException | EvalException e) {
			JMetalLogger.logger.info("R Error:: " + e.getMessage());
			return false;
		}
		return true;
	}
}