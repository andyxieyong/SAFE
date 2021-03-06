package lu.uni.svv.StressTesting.search.update;

import lu.uni.svv.StressTesting.utils.Settings;
import org.renjin.eval.EvalException;
import org.renjin.sexp.Vector;
import org.uma.jmetal.util.JMetalLogger;
import javax.script.ScriptException;


public class ModelUpdateThreshold extends ModelUpdate {
	
	public ModelUpdateThreshold() throws Exception{
		super();
		
	}
	
	/**
	 *
	 * @param formula
	 * @return
	 */
	public double initializeModel(String formula) throws ScriptException, EvalException {
		// set formula
		engine.eval(String.format("formula_str<- \"%s\"", formula));
		
		// learning logistic regression with simple formula
		engine.eval("base_model <- glm(formula = formula_str, family = \"binomial\", data = training)");
		engine.eval("cntUpdate <- 0");
		
		// update borderProbability and area
		engine.eval("uncertainIDs <- get_base_names(names(base_model$coefficients), isNum=TRUE)"); //c(30, 33)
		engine.eval(String.format("borderProbability<-find_noFPR(base_model, training, precise=%.6f)", Settings.MODEL_PROB_PRECISION));
		engine.eval(String.format("borderProbability<-ifelse(borderProbability==0, %.6f, borderProbability)", Settings.MODEL_PROB_PRECISION));
		engine.eval("print(sprintf(\"The result of find_noFPR: %.6f\", borderProbability))");
		engine.eval("areaMC <- integrateMC(10000, base_model, IDs=uncertainIDs, prob=borderProbability, UNIT.WCET=UNIT)");
		engine.eval("bestPoint <- get_bestsize_point(base_model, borderProbability, targetIDs=uncertainIDs, isGeneral=TRUE)");
		
		// keep coefficients
		engine.eval("coef <- t(data.frame(base_model$coefficients))");
		engine.eval("colnames(coef) <- get_raw_names(names(base_model$coefficients))");
		engine.eval("coef.item <- data.frame(nUpdate=cntUpdate, TrainingSize=nrow(training), Probability=borderProbability, BestX=bestPoint$X, BestY=bestPoint$Y, BestPointArea=bestPoint$Area, Area=areaMC, coef)");
		engine.eval("rownames(coef.item) <- c(cntUpdate)");
		engine.eval("coef.results <- coef.item");
		
		JMetalLogger.logger.info("Initialized model: " + getModelText("base_model"));
		
		Vector dataVector = (Vector)engine.eval("borderProbability");
		double prob = dataVector.getElementAsDouble(0);
		JMetalLogger.logger.info(String.format("probability: %.6f", prob));
		return prob;
	}
	
	public double updateModel(double _prob) throws ScriptException, EvalException{
		engine.eval("print(sprintf(\"Number of data set: %d\", nrow(training)))");
		engine.eval("prev_model<-base_model");
		engine.eval("base_model <- glm(formula = prev_model$formula, family = \"binomial\", data = training)");
		engine.eval("cntUpdate <- cntUpdate + 1");
		
		// update borderProbability and area
		engine.eval(String.format("borderProbability <- find_noFPR(base_model, training, precise=%.6f)", Settings.MODEL_PROB_PRECISION));
		engine.eval(String.format("borderProbability<-ifelse(borderProbability==0, %.6f, borderProbability)", Settings.MODEL_PROB_PRECISION));
		engine.eval("print(sprintf(\"The result of find_noFPR: %.6f\", borderProbability))");
		engine.eval("areaMC <- integrateMC(10000, base_model, IDs=uncertainIDs, prob=borderProbability, UNIT.WCET=UNIT)");
		engine.eval("bestPoint <- get_bestsize_point(base_model, borderProbability, targetIDs=uncertainIDs, isGeneral=TRUE)");
		
		engine.eval("coef <- t(data.frame(base_model$coefficients))");
		engine.eval("colnames(coef) <- get_raw_names(names(base_model$coefficients))");
		engine.eval("coef.item <- data.frame(nUpdate=cntUpdate, TrainingSize=nrow(training), Probability=borderProbability, BestX=bestPoint$X, BestY=bestPoint$Y, BestPointArea=bestPoint$Area, Area=areaMC, coef)");
		engine.eval("rownames(coef.item) <- c(cntUpdate)");
		engine.eval("coef.results <- rbind(coef.results, coef.item)");
		
		Vector dataVector = (Vector)engine.eval("borderProbability");
		double newProb = dataVector.getElementAsDouble(0);
		JMetalLogger.logger.info("Updated model: " + getModelText("base_model"));
		JMetalLogger.logger.info(String.format("probability: %.6f", newProb));
		
		updateTerminationData();
		return newProb;
	}
	
}