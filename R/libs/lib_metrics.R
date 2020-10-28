cat("loading lib_metrics.R...\n")
library(MLmetrics)

FNRate <- function (y_true, y_pred, positive = NULL) {
    Confusion_DF <- ConfusionDF(y_pred, y_true)
    if (is.null(positive) == TRUE) 
        positive <- as.character(Confusion_DF[1, 1])
    TP <- as.integer(sum(subset(Confusion_DF, y_true == positive & y_pred == positive)["Freq"]))
    FN <- as.integer(sum(subset(Confusion_DF, y_true == positive & y_pred != positive)["Freq"]))
    FNR <- FN/(TP + FN)
    return(FNR)
}
TNRate <- function (y_true, y_pred, positive = NULL) {
    Confusion_DF <- ConfusionDF(y_pred, y_true)
    if (is.null(positive) == TRUE) 
        positive <- as.character(Confusion_DF[1, 1])
    TN <- as.integer(sum(subset(Confusion_DF, y_true != positive & y_pred != positive)["Freq"]))
    FP <- as.integer(sum(subset(Confusion_DF, y_true != positive & y_pred == positive)["Freq"]))
    TNR <- TN/(TN + FP)
    return(TNR)
}
FPRate <- function (y_true, y_pred, positive = NULL) {
    # y_true=data$result
    # y_pred=cls.result
    # positive=Positive_value
    FPR=0
    sum_cls <- sum(y_pred)
    if (sum_cls==length(y_pred) || sum_cls==0){ 
        if (positive=="0"){
            if (sum_cls==length(y_pred)){ FPR = 0 }
            else if(sum_cls==0) { FPR = 1 }
        } else{
            if (sum_cls==length(y_pred)){ FPR = 1 }
            else if(sum_cls==0) { FPR = 0 }
        }
    } else {
        Confusion_DF <- ConfusionDF(y_pred, y_true)
        if (is.null(positive) == TRUE) 
            positive <- as.character(Confusion_DF[1, 1])
        TN <- as.integer(sum(subset(Confusion_DF, y_true != positive & y_pred != positive)["Freq"]))
        FP <- as.integer(sum(subset(Confusion_DF, y_true != positive & y_pred == positive)["Freq"]))
        FPR = FP/(TN + FP)
    }
    return(FPR)
}

FNRate <- function (y_true, y_pred, positive = NULL) {
    FNR=0
    sum_cls <- sum(y_pred)
    if (sum_cls==length(y_pred) || sum_cls==0){ 
        if (positive=="0"){
            if (sum_cls==length(y_pred)){ FNR = 1 }
            else if(sum_cls==0) { FNR = 0 }
        } else{
            if (sum_cls==length(y_pred)){ FNR = 0 }
            else if(sum_cls==0) { FNR = 1 }
        }
    } else {
        Confusion_DF <- ConfusionDF(y_pred, y_true)
        if (is.null(positive) == TRUE) 
            positive <- as.character(Confusion_DF[1, 1])
        TP <- as.integer(sum(subset(Confusion_DF, y_true == positive & y_pred == positive)["Freq"]))
        FN <- as.integer(sum(subset(Confusion_DF, y_true == positive & y_pred != positive)["Freq"]))
        FNR <- FN/(TP + FN)
    }
    return(FNR)
}
