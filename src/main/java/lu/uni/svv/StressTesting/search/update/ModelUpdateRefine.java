package lu.uni.svv.StressTesting.search.update;

import org.renjin.eval.EvalException;
import org.renjin.sexp.Vector;
import org.uma.jmetal.util.JMetalLogger;
import javax.script.ScriptException;


public class ModelUpdateRefine extends ModelUpdate {
	
	public ModelUpdateRefine() throws Exception{
		super();
		
	}
	
	/**
	 *
	 * @param formula
	 * @return
	 */
	public boolean initializeModel(String formula) throws ScriptException, EvalException {
		// set formula
		engine.eval(String.format("formula_str<- \"%s\"", formula));
		
		// learning logistic regression with simple formula
		engine.eval("base_model <- glm(formula = formula_str, family = \"binomial\", data = training)");
		engine.eval("cntUpdate <- 0");
		engine.eval("cntUpdate <- 0");
		
		// update borderProbability and area
		engine.eval("uncertainIDs <- get_base_names(names(base_model$coefficients), isNum=TRUE)"); //c(30, 33)
		engine.eval("borderProbability<-find_noFPR(base_model, training, precise=0.0001)");
		engine.eval("areaMC <- integrateMC(10000, base_model, IDs=uncertainIDs, prob=borderProbability, UNIT.WCET=UNIT)");
		
		// keep coefficients
		engine.eval("coef <- t(data.frame(base_model$coefficients))");
		engine.eval("colnames(coef) <- get_raw_names(names(base_model$coefficients))");
		engine.eval("coef.item <- data.frame(nUpdate=cntUpdate, TrainingSize=nrow(training), Probability=borderProbability, Area=areaMC, coef)");
		engine.eval("rownames(coef.item) <- c(cntUpdate)");
		engine.eval("coef.results <- coef.item");
		
		JMetalLogger.logger.info("Initialized model: " + getModelText("base_model"));
		
		return true;
	}
	
	public double updateModel(double _prob) throws ScriptException, EvalException{
		engine.eval("print(sprintf(\"Number of data set: %d\", nrow(training)))");
		engine.eval("prev_model<-base_model");
		engine.eval("base_model <- glm(formula = prev_model$formula, family = \"binomial\", data = training)");
		engine.eval("cntUpdate <- cntUpdate + 1");
		
		// update borderProbability and area
		engine.eval("borderProbability <- find_noFPR(base_model, training, precise=0.0001)");
		engine.eval("areaMC <- integrateMC(10000, base_model, IDs=uncertainIDs, prob=borderProbability, UNIT.WCET=UNIT)");
		
		engine.eval("coef <- t(data.frame(base_model$coefficients))");
		engine.eval("colnames(coef) <- get_raw_names(names(base_model$coefficients))");
		engine.eval("coef.item <- data.frame(nUpdate=cntUpdate, TrainingSize=nrow(training), Probability=borderProbability, Area=areaMC, coef)");
		engine.eval("rownames(coef.item) <- c(cntUpdate)");
		engine.eval("coef.results <- rbind(coef.results, coef.item)");
		
		Vector dataVector = (Vector)engine.eval("borderProbability");
		double newProb = dataVector.getElementAsDouble(0);
		JMetalLogger.logger.info("Updated model: " + getModelText("base_model"));
		
		updateTerminationData();
		return newProb;
	}
	
}