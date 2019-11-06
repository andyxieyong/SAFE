package lu.uni.svv.StressTesting.search.update;

import lu.uni.svv.StressTesting.scheduler.RMScheduler;
import lu.uni.svv.StressTesting.search.model.TestingProblem;
import lu.uni.svv.StressTesting.search.model.TimeListSolution;
import lu.uni.svv.StressTesting.utils.GAWriter;
import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.StringVector;
import org.renjin.sexp.Vector;
import org.uma.jmetal.util.JMetalLogger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;


public class ModelUpdateKFold extends ModelUpdate {
	
	public ModelUpdateKFold(int[] _targetTasks) throws Exception{
		super(_targetTasks);
	}
	
	////////////////////////////////////////////////////////////////////////
	// Related termination model
	////////////////////////////////////////////////////////////////////////
	public boolean prepareTerminationData() throws ScriptException, EvalException {
		super.prepareTerminationData();
		JMetalLogger.logger.info("Termination data will be updated every model update");
		return true;
	}
	
	public boolean updateTerminationData() throws ScriptException, EvalException{
		// use newly added data in the training
		engine.eval("tSize<-nrow(training)");
		engine.eval(String.format("termination_data<-training[(tSize-%d):tSize,]", Settings.N_EXAMPLE_POINTS));
		
		return true;
		
	}
	////////////////////////////////////////////////////////////////////////
	// Related test model
	////////////////////////////////////////////////////////////////////////
	public boolean includeTestData()throws ScriptException, EvalException, Exception{
		if (Settings.TEST_NGROUP<=0)
		{
			JMetalLogger.logger.severe("You need to set TEST_NGROUP to get test values");
			JMetalLogger.logger.severe("This means that k value for the k-fold cross validation");
			throw new Exception("Setting Error for TEST_NGROUP");
		}
		
		engine.eval("test.results <- data.frame()");
		engine.eval(String.format("testdata_filename <- \"%s/%s/%s_test_data.csv\"", Settings.EXPORT_PATH, Settings.LR_WORKPATH, filename));
		return true;
	}
	
	public boolean evaluateModel(int _cntUpdate)throws ScriptException, EvalException{
		engine.eval("test.result.group <- data.frame()");
		
		int nFold = 10;
		engine.eval(String.format("print(\"%d-fold cross validation...\")", nFold));
		engine.eval("shuffled <- training[sample(nrow(training)),]");
		engine.eval("sizeMax <- nrow(shuffled)");
		engine.eval(String.format("sizeFold <- as.integer(nrow(shuffled)/%d)", nFold));
		for (int x=1; x<=10; x++) {
			engine.eval(String.format("p1 <- (%d * sizeFold)", x-1));
			engine.eval(String.format("p2 <- (%d * sizeFold)", x));
			
			// seperate test and training data
			if (x!=1) {
				engine.eval("train_fold <- shuffled[1:p1,]");
			}else{
				engine.eval("train_fold <- data.frame()");
			}
			engine.eval("test_fold <- shuffled[(p1+1):p2,]");
			if (x!=nFold)
				engine.eval("train_fold <- rbind(train_fold, shuffled[(p2+1):sizeMax,])");
			
			// generate model for test
			engine.eval("test_model <- glm(formula = formula_str, family = \"binomial\", data = train_fold)");
			
			// evaluate the model
			String cmd = String.format("result.item <- calculate_metrics(test_model, test_fold, %.2f, cntUpdate)", Settings.BORDER_PROBABILITY);
			engine.eval(cmd);
			engine.eval(String.format("result.item <- data.frame(TestSet=%d, result.item)", x));
			engine.eval("test.result.group <- rbind(test.result.group, result.item)");
			
			engine.eval(String.format("test.sample <- data.frame(TestSet=%d, Type=\"Train\", train_fold)",x));
			engine.eval(String.format("test.sample <- rbind(test.sample, data.frame(TestSet=%d, Type=\"Test\", test_fold))",x));
			
			if (_cntUpdate==0 & x==1) {
				engine.eval("write.table(test.sample, testdata_filename, append = FALSE, sep = \",\", dec = \".\",row.names = FALSE, col.names = TRUE)");
			}else {
				engine.eval("write.table(test.sample, testdata_filename, append = TRUE, sep = \",\", dec = \".\",row.names = FALSE, col.names = FALSE)");
			}
		}
		
		engine.eval("print(test.result.group)");
		engine.eval("test.results <- rbind(test.results, test.result.group)");
		return true;
	}
	
}