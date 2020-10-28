# DEPENDENCY
#   - conf.R: TASK_INFO, UNIT
#   - lib_metrics: FPRate, TNRate, etc.
cat("loading lib_quadratic.R...\n")
if (Sys.getenv("RSTUDIO")==1){
    library(MLmetrics)
    source('libs/lib_data.R')  # get_task_names
}

# library('tidyverse')
# library(ggplot2)
# library(latex2exp)
# library("MLmetrics")
# library("MASS")

###############################################################
# line function from the sample(x, y) by linear regression
###############################################################
get_line_func_from_sample <- function(sample, formulaText){
    lm_model <- lm(formula = formulaText, data = sample)
    return (function(x){b<-lm_model$coefficients; return(b[[3]]*x^2+b[[2]]*x+b[[1]]);})
}

get_line_func_from_sample_cubic <- function(sample){
    lm_model <- lm(formula = y~ x + I(x^2)+I(x^3), data = sample)
    return (function(x){b<-lm_model$coefficients; return(b[[4]]*x^3 + b[[3]]*x^2+b[[2]]*x+b[[1]]);})
}

########################################################
#### Evaluating related
########################################################
get_evaluate_data <- function(evalpath, sampleType, nUpdate, nIter=1000, nSample=20){
    flist <- list.files(evalpath)
    evalData <- data.frame()
    for(fname in flist){
        if (regexpr(sprintf("%s_[0-9]+_%d",sampleType, nUpdate), fname)<0) next  # 
        eData <- read.csv(sprintf("%s/%s", evalpath, fname), header=TRUE)
        common <- (10 + nIter) * nSample + 1
        eData <- eData[common:nrow(eData),]
        evalData <- rbind(evalData, eData)
    }
    return (evalData)
}

get_evaluate_data_even <- function(evalpath, sampleType, nUpdate, nIter=1000, nSample=20){
    flist <- list.files(evalpath)
    evalData <- data.frame()
    for(fname in flist){
        if (regexpr(sprintf("%s_[0-9]+_%d",sampleType, nUpdate), fname)<0) next  # 
        eData <- read.csv(sprintf("%s/%s", evalpath, fname), header=TRUE)
        common <- (10 + nIter) * nSample + 1
        eData <- eData[common:nrow(eData),]
        evalData <- rbind(evalData, eData)
    }
    
    positive <- evalData[evalData$result==0,]
    negative <- evalData[evalData$result==1,]
    negative <- negative[sample(nrow(negative),nrow(positive)),]
    evalData <- rbind(positive, negative)
    return (evalData)
}

get_evaluate_data_pool <- function(evalpath, nIter=1000, nSample=20){
    flist <- list.files(evalpath)
    evalData <- data.frame()
    for(fname in flist){
        eData <- read.csv(sprintf("%s/%s", evalpath, fname), header=TRUE)
        common <- (10 + nIter) * nSample + 1
        eData <- eData[common:nrow(eData),]
        evalData <- rbind(evalData, eData)
    }
    return (evalData)
}


get_evaluate_data_all <- function(evalpath, nIter=1000, nSample=20){
    flist <- list.files(evalpath)
    evalData <- data.frame()
    for(fname in flist){
        eData <- read.csv(sprintf("%s/%s", evalpath, fname), header=TRUE)
        evalData <- rbind(evalData, eData)
    }
    return (evalData)
}

get_sampled_evaluate_data <- function(evalData, nSize)
{
    #sampling and check
    if (nSize!=0){
        data<-data.frame()
        while(TRUE){
            data<-evalData[sample(nrow(evalData),nSize),]
            Cnt0<-nrow(data[data$result==0,])
            Cnt1<-nrow(data[data$result==1,])
            if (Cnt0==0 | Cnt1==0 | (Cnt0/Cnt1)<0.3) {
                cat(sprintf("Checking evaluation data rate...%.2f\n", (Cnt0/Cnt1)))
                next
            }
            break
        }
    }
    else{
        data<-evalData
    }
    return (data)
}

sample_subset_data <- function(originData, nSample=0, range=0, limitRate=0.3)
{
    #sampling and check
    data<-originData
    if (nSample!=0){
        data<-data.frame()

        while(TRUE){
            # sampling
            if (range==0)
                data<-originData[sample(nrow(originData),nSample),]
            else
                data<-originData[sample(range, nSample),]

            Cnt0<-nrow(data[data$result==0,])
            Cnt1<-nrow(data[data$result==1,])
            if (Cnt0==0 | Cnt1==0 | (Cnt0/Cnt1)<limitRate) {
                cat(sprintf("Try to get a subset data again....%.2f<%.2f\n", (Cnt0/Cnt1), limitRate))
                next
            }
            break
        }
    }
    return (data)
}

sample_subset_data_even <- function(originData, nSample=0)
{
    #sampling and check
    data<-originData
    if (nSample!=0){
        data<-data.frame()
        
        dataPositive<-originData[originData$result==0,]
        dataPositive<-dataPositive[sample(nrow(dataPositive),nSample/2),]
        
        dataNegative<-originData[originData$result==1,]
        dataNegative<-dataNegative[sample(nrow(dataNegative),nSample/2),]
        
        data <- rbind(dataPositive, dataNegative)
    }
    return (data)
}



#########################################################################################
# Functions for the final results 
#########################################################################################
make_training_data<-function(initialData, originData, from, to){
    if (from != to){
        trainingData <- rbind(initialData, originData[(from+1):to,])
        return (trainingData)
    }
    else{
        return (data.frame(initialData))
    }
}
calculate_F1_score<-function(model, evalData, borderP, fixedProb=FALSE){
    predicted <- predict(model, evalData, type="response")
    if (fixedProb)
        predicted <- ifelse(as.numeric(predicted) < 0.5, 0, 1)
    else
        predicted <- ifelse(as.numeric(predicted) < borderP, 0, 1)
    f1 <- F1_Score(y_true=as.double(evalData$result), y_pred=predicted)
    return (f1)
}
calculate_evaluation<-function(rModel, dModel, evalData, borderP, count){
    Positive_value = "0"  # no deadline miss
    rPredicted <- predict(rModel, evalData, type="response")
    rPredicted <- ifelse(as.numeric(rPredicted) < borderP, 0, 1)
    rF1 <- F1_Score(y_true=evalData$result, y_pred=rPredicted, positive=Positive_value)
    rPrec <- Precision(y_true=evalData$result, y_pred=rPredicted, positive=Positive_value)
    rRecall <- Recall(y_true=evalData$result, y_pred=rPredicted, positive=Positive_value)
    rFNR <- FNRate(y_true=evalData$result, y_pred=rPredicted, positive=Positive_value)
    rTNR <- TNRate(y_true=evalData$result, y_pred=rPredicted, positive=Positive_value)
    rFPR <- FPRate(y_true=evalData$result, y_pred=rPredicted, positive=Positive_value)
    
    dPredicted <- predict(dModel, evalData, type="response")
    dPredicted <- ifelse(as.numeric(dPredicted) < borderP, 0, 1)
    dF1 <- F1_Score(y_true=evalData$result, y_pred=dPredicted, positive=Positive_value)
    dPrec <- Precision(y_true=evalData$result, y_pred=dPredicted, positive=Positive_value)
    dRecall <- Recall(y_true=evalData$result, y_pred=dPredicted, positive=Positive_value)
    dFNR <- FNRate(y_true=evalData$result, y_pred=dPredicted, positive=Positive_value)
    dTNR <- TNRate(y_true=evalData$result, y_pred=dPredicted, positive=Positive_value)
    dFPR <- FPRate(y_true=evalData$result, y_pred=dPredicted, positive=Positive_value)
    
    dataset <- data.frame(Iter=rep(count, 2), Type=c("R", "D"),  
                          F1=c(rF1,dF1),
                          Prec=c(rPrec, dPrec),
                          Rec=c(rRecall, dRecall), 
                          FNR=c(rFNR, dFNR),
                          TNR=c(rTNR, dTNR), 
                          FPR=c(rFPR, dFPR))
    
    return (dataset)
}
get_stat_item<-function(data, iter, column){
    data<-test_results
    iter <-1
    column <- 'F1'
    randomValues <-data[(data$Iter==iter & data$Type=="R"),][[column]]
    distValues <-data[(data$Iter==iter & data$Type=="D"),][[column]]
    uvalue<-wilcox.test(randomValues, distValues, paired=TRUE) 
    vda<-VD.A(randomValues, distValues)
    
    stat.item<-data.frame(c(uvalue$p.value), c(vda$estimate), c(sprintf("%s",vda$magnitude)))
    colnames(stat.item)<- c(sprintf("%s.Pvalue",column), sprintf("%s.VDA",column), sprintf("%s.VDAText",column))
    return (stat.item)
}
calculate_stat_evaluation<-function(data, testID){
    maxIter = max(data$Iter)
    stat.table<-data.frame()
    for(iter in 1:maxIter){
        stat.item<-data.frame(nTest=c(testID), Iter=c(iter), get_stat_item(data, iter, "F1"))
        stat.item<-data.frame(stat.item, get_stat_item(data, iter, "Prec"))
        stat.item<-data.frame(stat.item, get_stat_item(data, iter, "Rec"))
        stat.item<-data.frame(stat.item, get_stat_item(data, iter, "FNR"))
        stat.item<-data.frame(stat.item, get_stat_item(data, iter, "TNR"))
        stat.item<-data.frame(stat.item, get_stat_item(data, iter, "FPR"))
        
        stat.table<-rbind(stat.table, stat.item)
    }
    return (stat.table)
}

printCDF<-function(typeText, model, evalData, borderP){
    positive_value = "0"  # no deadline miss
    pred_value <- predict(model, evalData, type="response")
    pred_value <- ifelse(as.numeric(pred_value) < borderP, 0, 1)
    cDF <- ConfusionDF(y_pred=pred_value, y_true=evalData$result)
    rownames(cDF) <- c("TP", "FP", "FN", "TN")
    
    cat(sprintf("%s Confusion DF:\n", typeText))
    cat("Predicted +--------Actual-------+\n")
    cat("----------| Positive | Negative |\n")    
    cat(sprintf("Positive  | %8d | %8d |\n", sum(cDF[1,]["Freq"]), sum(cDF[2,]["Freq"])))
    cat(sprintf("Negative  | %8d | %8d |\n", sum(cDF[3,]["Freq"]), sum(cDF[4,]["Freq"])))
    cat("--------------------------------+\n")
}

#########################################
# Generate graphs
get_model_graph<-function(rTrainingData, rModel, dTrainingData, dModel, borderProbability, rF1, dF1, numModel, evalData, runID){
    # create model regression line 0.01
    xID <- 33
    yID <- 30
    tnames <- get_task_names(rTrainingData)
    rSamples<-sample_regression_points(tnames, rModel, nPoints=100, P=borderProbability, min_dist=100*UNIT)
    rLine <- get_line_func_from_sample(rSamples, "T30 ~ T33 + I(T33^2)")
    dSamples<-sample_regression_points(tnames, dModel, nPoints=100, P=borderProbability, min_dist=100*UNIT)
    dLine <- get_line_func_from_sample(dSamples, "T30 ~ T33 + I(T33^2)")
    
    graphs<-list()
    g<-ggplot() +
        geom_point( data=rTrainingData, aes(x=T33, y=T30, color=as.factor(labels)),  size=0.3, alpha=0.5)+
        # geom_point( data=evalData, aes(x=T33, y=T30), color='black', size=0.3, alpha=0.5)+
        # geom_point( data=dSamples, aes(x=T33, y=T30),  size=0.3, alpha=0.6, color=cbPalette[7])+
        # geom_point( data=rSamples, aes(x=T33, y=T30),  size=0.3, alpha=0.6, color=cbPalette[6])+
        stat_function(fun=rLine, geom="line", colour="red", data=rSamples, alpha=0.9) +
        stat_function(fun=dLine, geom="line", colour="blue", data=dSamples, alpha=0.9) +
        scale_colour_manual(values=cbPalette )+
        scale_linetype_manual(values=c("solid", "longdash"))+
        # theme_classic()+
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        xlim(0, TASK_INFO$WCET.MAX[xID]*UNIT) +
        ylim(0, TASK_INFO$WCET.MAX[yID]*UNIT) +
        xlab(sprintf("%s (T%d)",TASK_INFO$NAME[xID], xID)) +
        ylab(sprintf("%s (T%d)",TASK_INFO$NAME[yID], yID)) +
        ggtitle(sprintf("Learning Logistic Regression (#model=%d, Run=%d)", numModel, runID))+
        annotate("text", x = 0, y = TASK_INFO$WCET.MAX[yID]*UNIT, label = sprintf("avg(FPR(random))=%.3f", rF1), color="red",size=3, hjust=0, vjust=1)+
        annotate("text", x = 0, y = TASK_INFO$WCET.MAX[yID]*UNIT-0.2, label = sprintf("avg(FPR(distance))=%.3f", dF1), color="blue", size=3, hjust=0, vjust=-1)
    graphs[[1]] <- g
    g<-ggplot() +
        geom_point( data=dTrainingData, aes(x=T33, y=T30, color=as.factor(labels)),  size=0.3, alpha=0.5)+
        # geom_point( data=evalData, aes(x=T33, y=T30), color='black', size=0.3, alpha=0.5)+
        # geom_point( data=dSamples, aes(x=T33, y=T30),  size=0.3, alpha=0.6, color=cbPalette[7])+
        # geom_point( data=rSamples, aes(x=T33, y=T30),  size=0.3, alpha=0.6, color=cbPalette[6])+
        stat_function(fun=rLine, geom="line", colour="red", data=rSamples, alpha=0.9) +
        stat_function(fun=dLine, geom="line", colour="blue", data=dSamples, alpha=0.9) +
        scale_colour_manual(values=cbPalette )+
        scale_linetype_manual(values=c("solid", "longdash"))+
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        xlim(0, TASK_INFO$WCET.MAX[xID]*UNIT) +
        ylim(0, TASK_INFO$WCET.MAX[yID]*UNIT) +
        xlab(sprintf("%s (T%d)",TASK_INFO$NAME[xID], xID)) +
        ylab(sprintf("%s (T%d)",TASK_INFO$NAME[yID], yID)) +
        ggtitle(sprintf("Learning Logistic Regression (#model=%d, Run=%d)", numModel, runID))+
        annotate("text", x = 0, y = TASK_INFO$WCET.MAX[yID]*UNIT, label = sprintf("avg(FPR(random))=%.3f", rF1), color="red",size=3, hjust=0, vjust=1)+
        annotate("text", x = 0, y = TASK_INFO$WCET.MAX[yID]*UNIT-0.2, label = sprintf("avg(FPR(distance))=%.3f", dF1), color="blue", size=3, hjust=0, vjust=-1)
    graphs[[2]] <- g
    return (graphs)
}

get_model_graph_for_test<-function(rTrainingData, rModel, dTrainingData, dModel, borderProbability, rF1, dF1, numModel, evalData, runID){
    # create model regression line 0.01
    xID <- 33
    yID <- 30
    tnames <- get_task_names(rTrainingData)
    rSamples<-sample_regression_points(tnames, rModel, nPoints=100, P=borderProbability, min_dist=1000*UNIT)
    rLine <- get_line_func_from_sample(rSamples, "T30 ~ T33 + I(T33^2)")
    dSamples<-sample_regression_points(tnames, dModel, nPoints=100, P=borderProbability, min_dist=1000*UNIT)
    dLine <- get_line_func_from_sample(dSamples, "T30 ~ T33 + I(T33^2)")
    
    g<-ggplot() +
        # geom_point( data=rTrainingData, aes(x=T33, y=T30, color=as.factor(labels)),  size=0.3, alpha=0.5)+
        geom_point( data=evalData, aes(x=T33, y=T30, color=as.factor(labels)), size=0.3, alpha=0.5)+
        # geom_point( data=dSamples, aes(x=T33, y=T30),  size=0.3, alpha=0.6, color=cbPalette[7])+
        # geom_point( data=rSamples, aes(x=T33, y=T30),  size=0.3, alpha=0.6, color=cbPalette[6])+
        stat_function(fun=rLine, geom="line", colour="red", data=rSamples, alpha=0.9) +
        stat_function(fun=dLine, geom="line", colour="blue", data=dSamples, alpha=0.9) +
        scale_colour_manual(values=cbPalette )+
        scale_linetype_manual(values=c("solid", "longdash"))+
        # theme_classic()+
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        xlim(0, TASK_INFO$WCET.MAX[xID]*UNIT/2) +
        ylim(0, TASK_INFO$WCET.MAX[yID]*UNIT/2) +
        xlab(sprintf("%s (T%d)",TASK_INFO$NAME[xID], xID)) +
        ylab(sprintf("%s (T%d)",TASK_INFO$NAME[yID], yID)) +
        ggtitle(sprintf("Learning Logistic Regression (#model=%d, Run=%d)", numModel, runID))+
        annotate("text", x = 0, y = 5, label = sprintf("F1(random)=%.3f", rF1), color="red",size=3, hjust=0, vjust=1)+
        annotate("text", x = 0, y = 4, label = sprintf("F1(distance)=%.3f", dF1), color="blue", size=3, hjust=0, vjust=-1)
}

get_model_graph_for_training<-function(rTrainingData, rModel, dTrainingData, dModel, borderProbability, rF1, dF1, numModel, evalData, runID){
    # create model regression line 0.01
    xID <- 33
    yID <- 30
    tnames <- get_task_names(rTrainingData)
    rSamples<-sample_regression_points(tnames, rModel, nPoints=100, P=borderProbability, min_dist=1000*UNIT)
    rLine <- get_line_func_from_sample(rSamples, "T30 ~ T33 + I(T33^2)")
    dSamples<-sample_regression_points(tnames, dModel, nPoints=100, P=borderProbability, min_dist=1000*UNIT)
    dLine <- get_line_func_from_sample(dSamples, "T30 ~ T33 + I(T33^2)")
    
    g<-ggplot() +
        geom_point( data=rTrainingData, aes(x=T33, y=T30, color=as.factor(labels)),  size=0.3, alpha=0.5)+
        stat_function(fun=rLine, geom="line", colour="red", data=rSamples, alpha=0.9) +
        stat_function(fun=dLine, geom="line", colour="blue", data=dSamples, alpha=0.9) +
        scale_colour_manual(values=cbPalette )+
        scale_linetype_manual(values=c("solid", "longdash"))+
        # theme_classic()+
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        xlim(0, TASK_INFO$WCET.MAX[xID]*UNIT/2) +
        ylim(0, TASK_INFO$WCET.MAX[yID]*UNIT/2) +
        xlab(sprintf("%s (T%d)",TASK_INFO$NAME[xID], xID)) +
        ylab(sprintf("%s (T%d)",TASK_INFO$NAME[yID], yID)) +
        ggtitle(sprintf("Learning Logistic Regression (#model=%d, Run=%d)", numModel, runID))+
        annotate("text", x = 0, y = 5, label = sprintf("F1(random)=%.3f", rF1), color="red",size=3, hjust=0, vjust=1)+
        annotate("text", x = 0, y = 4, label = sprintf("F1(distance)=%.3f", dF1), color="blue", size=3, hjust=0, vjust=-1)
}

get_behavior_grpah<- function(test_result, avg_results, yText, yDataColumn){
    g <- ggplot(data=test_result, aes(x=as.factor(Iter), y=test_result[[yDataColumn]], color=as.factor(Type))) +
        stat_boxplot(geom = "errorbar", width = 0.5, alpha=0.8) +
        geom_boxplot( size=0.5, alpha=0.8, outlier.shape=NA, outlier.size = 0, coef = 0)+
        geom_line( data=avg_results, aes(x=as.factor(Iter), y=avg_results[[yDataColumn]], color=as.factor(Type), group=as.factor(Type)), size=0.6, alpha=1)+
        # theme_classic()+
        scale_colour_manual(values=cbPalette)+
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        # ylim(0.825, 0.875) +
        xlab("Number of model updates") +
        ylab(yText)
    # ggtitle(sprintf("Comparing Random vs Distance-Based Sampling (update=%d)",nUpdate))
    return (g)
}
generate_box_plot <- function(sample_points, x_col, y_col, x.title, y.title, nBox=20, title="", ylimit=NULL, colorList=NULL){
    
    # Draw them for each
    avg_results<- aggregate(sample_points[[y_col]], list(Iter=sample_points[[x_col]], Type=sample_points$Type), mean)
    colnames(avg_results) <- c(x_col, "Type", y_col)
    
    # change for drawing
    maxX = max(sample_points[[x_col]])
    interval = as.integer(maxX/nBox)
    samples <- sample_points[(sample_points[[x_col]]%%interval==0),]
    avgs <- avg_results[(avg_results[[x_col]]%%interval==0),]
    
    if(is.null(colorList)==TRUE){
        colorList = cbPalette
    }
    
    g <- ggplot(data=samples, aes(x=as.factor(samples[[x_col]]), y=samples[[y_col]], color=as.factor(Type))) +
        stat_boxplot(geom = "errorbar", width = 0.5, alpha=0.8) +
        geom_boxplot( size=0.5, alpha=0.8, outlier.shape=NA, outlier.size = 0, coef = 0)+
        geom_line( data=avgs, aes(x=as.factor(avgs[[x_col]]), y=avgs[[y_col]], color=as.factor(Type), group=as.factor(Type)), size=1, alpha=1)+
        # theme_classic()+
        scale_colour_manual(values=colorList)+
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        xlab(x.title) +
        ylab(y.title)
    
    if (!is.null(ylimit)){
        g <- g + ylim(ylimit[[1]], ylimit[[2]])
    }    
    if (title!=""){
        g <- g + ggtitle(title)
    }
    return (g)
}

generate_ROC_plot<- function(rModel, dModel, evalData, count, runID){
    # test rModel
    rPredicted <- predict(rModel, evalData, type="response")
    df <- data.frame(predictions=rPredicted, labels=evalData$result, Type=rep("R", nrow(evalData)))
    
    # test dModel
    dPredicted <- predict(dModel, evalData, type="response")
    df2 <- data.frame(predictions=dPredicted, labels=evalData$result, Type=rep("D", nrow(evalData)))
    df <- rbind(df, df2)
    
    
    p <- ggplot(df, aes(m = predictions, d = labels, color=as.factor(Type)))+ 
        geom_roc(n.cuts=10, labels=FALSE, show.legend = TRUE, size=0.5, pointsize = 0.3) + 
        style_roc(theme = theme_grey) + 
        # geom_rocci(fill="pink", sig.level = 0.05) +
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        # ylab("TPR (True-positive rate)") +
        # xlab("FPR (False-positive rate)") +
        ggtitle(sprintf("ROC curve (#model=%d, #Run=%d)",count, runID))
    return (p)
}

# 
# stats_data<-data.frame(Threshold=c(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0),
#                        TPR = c(0.99, 0.98, 0.97, 0.95, 0.80, 0.70, 0.50, 0.40, 0.33, 0.30),
#                        TNR = c(0.30, 0.33, 0.40, 0.50, 0.60, 0.70, 0.95, 0.97, 0.99, 0.99))


roc_threshold_graph<-function(model, evalData, title){
    positiveValue = "0"
    # predicted <- predict(rModel, testData, type="response")
    predicted <- predict(model, evalData, type="response")
    TNR<-c()
    TPR<-c()
    Threshold<-c()
    Intersection <- 0
    for (t in 1:99){
        # print(sprintf("working threshold %.2f...",t*0.01))
        predicted_threshold <- ifelse(as.numeric(predicted) <= (t*0.01), 0, 1)
        Threshold<- c(Threshold, (t*0.01))
        TPR.item<-Recall(y_true=evalData$result, y_pred=predicted_threshold, positive=positiveValue)
        TNR.item<- TNRate(y_true=evalData$result, y_pred=predicted_threshold, positive=positiveValue)
        TPR<-c(TPR, TPR.item)
        TNR<-c(TNR, TNR.item)
        if (abs(TPR.item - TNR.item) <=0.001)
            Intersection <- t
    }
    stats_data <- data.frame(Threshold, Value=TPR, Type=rep("TPR", length(TPR)))
    stats_data <- rbind(stats_data, data.frame(Threshold, Value=TNR, Type=rep("TNR", length(TNR))))
    
    g<-ggplot(stats_data, aes(x = Threshold, y=Value, linetype=Type)) + 
        geom_line()+
        # geom_line(aes(y = TPR), color = "black", linetype="solid") + 
        # geom_line(aes(y = TNR), color = "black", linetype="dashed" )+
        theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
        xlab("Threshold") +
        ylab("TPR (True-positive rate)")+
        # xlim(0, 1) +
        # ylim(0, 1) +
        scale_y_continuous(sec.axis = sec_axis(trans = ~., name = "TNR (True-nagative rate)"))+
        ggtitle(title)
    if (Intersection!=0)
        g<- g + annotate("text", x = 0.01*Intersection, y = 0, label = sprintf("Interaction=%.2f",Intersection*0.01), color="blue", size=4, hjust=0, vjust=0)
    return (g)
}


print_plots<-function(graphs, filename){
    pdf(filename, width=7, height=5)
    for (item in graphs){
        print(item)
    }
    dev.off()
}