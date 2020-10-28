# Library for evaluating model
# Dependency
#   - lib_task.R : get_task_names
#   - lib_metrics.R: FPRate
cat("loading lib_evaluate.R...\n")
if (Sys.getenv("RSTUDIO")==1){
    library(cubature)
    source("libs/lib_data.R")     # get_task_names
    source("libs/lib_metrics.R")  # find_noFPR, FPRate
}

#############################################
# Area
#  - dependency: TASK_INFO
#############################################
areaUnderLine <- function(model, prob, stepSize, IDs, prev=NULL, UNIT.WCET=1){
    if (length(IDs)<=0) return (-1)
    # get index value
    idx = length(prev)+1
    tID <- IDs[idx]
    
    # get values in [minWCET, maxWCET] by stepSize
    minWCET<-TASK_INFO$WCET.MIN[[tID]]*UNIT.WCET
    maxWCET<-TASK_INFO$WCET.MAX[[tID]]*UNIT.WCET
    values<-seq(minWCET, maxWCET, by=stepSize*UNIT.WCET)
    
    if (length(IDs)==idx){
        # cat(sprintf('T%d - %d [%.3f, %.3f]  ', tID, length(values), minWCET, maxWCET))
        if (idx==1){
            valueSet <- data.frame(values)
        }else{
            valueSet <- data.frame(t(prev), v=values)
        }
        colnames(valueSet)<- sprintf("T%d", IDs)
        pV<-predict(model, valueSet, type='response')
        # cat("\n")
        # cat(pV)
        # cat("\n")
        # cat(ifelse(pV<prob, 1, 0))
        # cat("\n")
        nArea<-sum(ifelse(pV<prob, 1, 0))*stepSize*stepSize  # set positive = 1 to calculate area
    }
    else{
        # cat(sprintf('Task %d points %d in range [%.3f, %.3f]\n', tID, length(values), minWCET, maxWCET))
        nArea<-0
        for(x in values){
            # cat(sprintf('\tselected %10.3f....', x))
            if (idx==1){
                selected <- c(x)
            }else{
                selected <- c(prev, x)
            }
            subArea <- areaUnderLine(model, prob, stepSize, IDs, selected, UNIT.WCET)
            nArea <- nArea + subArea
            # cat(sprintf('subArea(%.6f) ==> Area(%.6f)\n', subArea, nArea))
            if (subArea==0) break
        }
    }
    return (nArea)
}

areaUnderLine_lib <- function(model, prob, IDs, UNIT_WCET=1){
    if (length(IDs)<=0) return (-1)
    
    minLimit<-TASK_INFO$WCET.MIN[IDs]*UNIT_WCET
    maxLimit<-as.numeric(get_intercepts(mdx, 0.5, IDs))
    
    model_function <- function(x) { 
        # make input set
        values<- data.frame(t(x))
        colnames(values)<- sprintf("T%d",IDs)
        
        #return value
        rValue <- predict(model, newdata=values, type="link")
        rValue <- rValue - log(prob/(1-prob))
        return (rValue)
    }
    
    v<-adaptIntegrate(model_function, lowerLimit = minLimit, upperLimit = maxLimit)
    # cat(sprintf("Integral: %15.4f (Error:%.4f)", v$integral, v$error))
    return(v$integral)
}

integrateMC <- function(n,model,IDs, prob, UNIT.WCET=1, upper=NULL, lower=NULL, graph.on=FALSE){
    # calculate range
    if(is.null(lower)) minLimit<-TASK_INFO$WCET.MIN[IDs]*UNIT.WCET else minLimit<-lower
    if(is.null(upper)) maxLimit<-as.numeric(get_intercepts(model, prob, IDs)) else maxLimit<-upper

    # sampling for integral each IDs
    examples<-data.frame()
    for(x in 1:length(minLimit)){
        values<-runif(n,minLimit[[x]], maxLimit[[x]])
        if (ncol(examples)==0){
            examples<-data.frame(values)
        }else{
            examples<-cbind(examples, values)
        }
    }
    colnames(examples) <- sprintf("T%d",IDs)
    
    # calculate integral in monte carlo method
    v<-predict(model, newdata=examples, type='response')
    mc<-sum(ifelse(v<prob, 1, 0))/n
    fullArea<-prod(minLimit-maxLimit)
    ret<-list("Integral"=mc*fullArea, "McRate"=mc)
    
    # Showing data
    if (graph.on==TRUE){
        examples<-data.frame(examples, labels=ifelse(v<prob, TRUE, FALSE))
        g <- ggplot() +
            geom_point( data=examples, aes(x=examples[[2]], y=examples[[1]], color=as.factor(labels)),  size=0.3, alpha=0.5)+
            scale_colour_manual(values=c(cbPalette[2], cbPalette[1], '#555555') )+
            scale_linetype_manual(values=c("solid", "longdash"))+
            # theme_classic()+
            # theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
            theme(legend.justification=c(0,1), legend.position=c(0, 1), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
            xlim(minLimit[2], maxLimit[2]) +
            ylim(minLimit[1], maxLimit[1]) +
            xlab(sprintf("%s (T%d)",TASK_INFO$NAME[IDs[2]], IDs[2])) +
            ylab(sprintf("%s (T%d)",TASK_INFO$NAME[IDs[1]], IDs[1])) + 
            ggtitle(sprintf("Area of function (N=%d, Integral=%.4f)", n, mc*fullArea))
        ret[["graph"]]<-g
    }
    return(ret)
}

#############################################
# Find no FPR probability
#############################################
find_noFPR <-function(model, data, precise=0.001){
    Positive_value = "0"  # no deadline miss
    predicted <- as.numeric(predict(model, data, type="response"))
    
    low<-1
    high<-as.integer(1/precise)-1
    tryCatch({
        while(low<high){
            mid<- as.integer((low+high)/2)
            # cat(sprintf("mid=%.4f from (low=%.4f, high=%.4f)...", mid*precise, low*precise, high*precise))
            cls.result <- ifelse(predicted < (mid*precise), 0, 1)
            FPR <- FPRate(y_true=data$result, y_pred=cls.result, positive=Positive_value)
            if(FPR>0){
                high <- mid-1
            }else{
                low <- mid+1
            }
            # cat(sprintf('==> %.4f\n', FPR))
        }
        # Final check
        threshold <- max(high, low)
        cls.result <- ifelse(predicted < (threshold*precise), 0, 1)
        FPR <- FPRate(y_true=data$result, y_pred=cls.result, positive=Positive_value)   
        if (FPR>0)
            return((threshold-1)*precise)
        return(threshold*precise)
    }, error = function(e) {
        cat('ERROR:: Error to calculate noFPR probability\n');
        message(e)
        return (0.5)
        }
    )
}


#############################################
# Find no FPR probability
#############################################
find_noFNR <-function(model, data, precise=0.001){
    Positive_value = "0"  # no deadline miss
    predicted <- as.numeric(predict(model, data, type="response"))
    
    low<-1
    high<-as.integer(1/precise)-1
    tryCatch({
        while(low<high){
            mid<- as.integer((low+high)/2)
            # cat(sprintf("mid=%.4f from (low=%.4f, high=%.4f)...", mid*precise, low*precise, high*precise))
            cls.result <- ifelse(predicted < (mid*precise), 0, 1)
            FNR <- FNRate(y_true=data$result, y_pred=cls.result, positive=Positive_value)
            if(FNR>0){
                low <- mid+1
            }else{
                high <- mid-1
            }
            # cat(sprintf('==> %.4f\n', FNR))
        }
        # Final check
        threshold <- max(high, low)
        cls.result <- ifelse(predicted < (threshold*precise), 0, 1)
        FNR <- FNRate(y_true=data$result, y_pred=cls.result, positive=Positive_value)   
        if (FNR>0)
            return((threshold+1)*precise)
        return(threshold*precise)
    }, error = function(e) {
        cat('ERROR:: Error to calculate noFNate probability\n');
        message(e)
        return (0.5)
    }
    )
}

#############################################
# evaluating with test data
#############################################
calculate_metrics<-function(model, evalData, borderP, count){

    Positive_value = "0"  # no deadline miss
    predicted <- predict(model, evalData, type="response")
    predicted <- ifelse(as.numeric(predicted) < borderP, 0, 1)

    
    # basic values
    cdf <-ConfusionDF(y_pred=predicted, y_true=evalData$result)
    nTP <- as.integer(subset(cdf, y_true==Positive_value & y_pred==Positive_value)["Freq"])
    nFP <- as.integer(subset(cdf, y_true!=Positive_value & y_pred==Positive_value)["Freq"])
    nFN <- as.integer(subset(cdf, y_true==Positive_value & y_pred!=Positive_value)["Freq"])
    nTN <- as.integer(subset(cdf, y_true!=Positive_value & y_pred!=Positive_value)["Freq"])
    
    rFPR = nFP/(nTN + nFP)
    rFNR = nFN/(nTP + nFN)
    rTNR = nTN/(nTN + nFP)

    rPrec = nTP/(nTP + nFP)
    rRecall =  nTP/(nTP + nFN)
    rF1 = 2 * (rPrec * rRecall)/(rPrec + rRecall)

    dataset <- data.frame(Iter=c(count),  
                          F1=c(rF1),
                          Prec=c(rPrec),
                          Rec=c(rRecall), 
                          FNR=c(rFNR),
                          TNR=c(rTNR), 
                          FPR=c(rFPR),
                          TP=c(nTP),
                          FP=c(nFP),
                          FN=c(nFN),
                          TN=c(nTN))
    return (dataset)
}

#############################################
# evaluating with training data
#############################################
kfoldCV <- function(model, data, K){
    #Randomly shuffle the data
    testSet<-data[sample(nrow(data)),]
    
    #Create 10 equally size folds
    folds <- cut(seq(1,nrow(testSet)),breaks=10, labels=FALSE)
    
    #Perform 10 fold cross validation
    values <- data.frame()
    for(i in 1:K){
        #Segement your data by fold using the which() function 
        testIndexes <- which(folds==i, arr.ind=TRUE)
        testPart <- testSet[testIndexes, ]
        trainPart <- testSet[-testIndexes, ]
        
        #Use test and train data partitions however you desire...
        md_test = glm(formula = model$formula, family = "binomial", data=trainPart)
        uncertainIDs <- get_base_names(names(md_test$coefficients), isNum=TRUE)
        borderP<-find_noFPR(md_test, trainPart, precise=0.0001)
        
        eval.result <- calculate_metrics(md_test, testPart, borderP, i)
        values <- rbind(values, eval.result)
    }
    
    prec_TP <- sum(values$TP)
    prec_FP <- sum(values$FP)
    precisionSum <- prec_TP/(prec_TP + prec_FP)
    df <- data.frame(CV.Precision.Avg=mean(values$Prec), 
                     CV.Precision.Sum=precisionSum, 
                     CV.TP=sum(values$TP), 
                     CV.FP=sum(values$FP), 
                     CV.FN=sum(values$FN), 
                     CV.TN=sum(values$TN))
    return (df)
}



