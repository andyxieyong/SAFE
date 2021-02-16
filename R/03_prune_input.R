# pruning of input.csv from the phase1 result
# if there are imbalanced data, it generates input_reduced.csv
# This file works for only one run of phase1
#   
############################################################
# R Parameter passing
############################################################
BASE_PATH <- getwd()
args <- commandArgs()
args <- args[-(1:5)]  # get sublist from arguments (remove unnecessary arguments)
TARGET_PATH <- sprintf("%s/%s", BASE_PATH, args[1]) #sprintf('%s/results/20191222_P1_1000_S20_GASearch', BASE_PATH)
OUTPUT_PATH <- sprintf("%s/%s", BASE_PATH, args[2]) #sprintf("/analysis/02_stepwise", BASE_PATH)  #results/20190723_stepwise

if(dir.exists(OUTPUT_PATH)==FALSE) dir.create(OUTPUT_PATH, recursive=TRUE)

############################################################
# Load libraries
############################################################
setwd(sprintf("%s/R", BASE_PATH))
library(MASS)
library(dplyr)
library(MLmetrics)
library(ggplot2)
source("libs/conf.R")
source("libs/lib_data.R")     # get_task_names
source("libs/lib_model.R")     # get_intercepts
source("libs/lib_pruning.R")
source("libs/lib_draw.R")
source("libs/lib_formula.R")  # get_raw_names, get_base_name does not need lib_data.R
source("libs/lib_metrics.R")  # find_noFPR, FPRate
source("libs/lib_evaluate.R")     # find_noFPR

############################################################
# SAFE Parameter parsing and setting 
############################################################
filepath<- sprintf("%s/settings.txt", TARGET_PATH)
params<- parsingParameters(filepath)

nSamples <- params[["N_SAMPLE_WCET"]]
populationSize <- params[['GA_POPULATION']]
iterations.P1 <- params[['GA_ITERATION']]
nRuns.P1 <- c(1:params[['RUN_MAX']])
TIME_QUANTA <- params[['TIME_QUANTA']]

for(runID.P1 in nRuns.P1){
    ################################################################################
    # Loading resource (it changed during the work)
    RESOURCE_FILE <- sprintf("%s/input.csv", TARGET_PATH)
    TASK_INFO<-load_taskInfo(RESOURCE_FILE, TIME_QUANTA)
    
    # for saving information
    NEW_RESOURCE_PATH <- sprintf("%s/inputs", TARGET_PATH)
    if(dir.exists(NEW_RESOURCE_PATH)==FALSE) dir.create(NEW_RESOURCE_PATH, recursive=TRUE)
    RESOURCE2_FILE<- sprintf("%s/reduced_run%02d.csv", NEW_RESOURCE_PATH, runID.P1)
    
    ################################################################################
    # load data and train model
    print(sprintf("--Pruning for run %d in phase 1 ...", runID.P1))
    FORMULA_PATH <- sprintf('%s/formula/formula_run%02d', TARGET_PATH, runID.P1)
    formula.Text =  toString(read.delim(FORMULA_PATH, header=FALSE)[1,])
    training <- read.csv(sprintf('%s/_samples/sampledata_run%02d.csv', TARGET_PATH, runID.P1), header= TRUE)
    
    # reduce points
    nPoints <- (iterations.P1+populationSize) * nSamples # (iteration + population ) nSamples
    training <- training[1:nPoints,]
    
    base_model <- glm(formula = formula.Text, family = "binomial", data = training)
    base_model$coefficients
    
    ################################################################################
    # calculating balance level
    positive <- nrow(training[training$result==0,])
    negative <- nrow(training[training$result==1,])
    if (positive > negative){
        balanceRate <- negative/positive
        balanceSide <- "positive"
        balanceProb <- find_noFPR(base_model, training, precise=0.0001)
    }else{
        balanceRate <- positive/negative
        balanceSide <- "negative"
        balanceProb <- find_noFNR(base_model, training, precise=0.0001)
        # if (balanceProb<0.999) balanceProb<-0.999
    }
    
    ################################################################################
    # pruning
    uncertainIDs <- get_base_names(names(base_model$coefficients), isNum=TRUE)
    df<-list()
    for(tID in uncertainIDs){
        df[sprintf("T%d",tID)] <- TASK_INFO$WCET.MAX[[tID]]
    }
    intercepts<-as.data.frame(df)
    
    if(balanceRate<0.5){
        intercepts <- get_intercepts(base_model, balanceProb, uncertainIDs)
        intercepts <- complement_intercepts(intercepts, uncertainIDs, TASK_INFO)
        print(intercepts)
        training <- pruning(training, balanceSide, intercepts, uncertainIDs)
        # change input data
        for (tID in uncertainIDs){
            tname <- sprintf("T%d", tID)
            TASK_INFO$WCET.MAX[[tID]] <- intercepts[1, tname]
            # print(sprintf("T%d=%d", tID, TASK_INFO$WCET.MAX[[tID]]))
        }
        
        ################################################################################
        #save training data
        taskinfo <- TASK_INFO
        taskinfo <- taskinfo[-c(1)]
        taskinfo$WCET.MIN = taskinfo$WCET.MIN*TIME_QUANTA
        taskinfo$WCET.MAX = taskinfo$WCET.MAX*TIME_QUANTA
        taskinfo$PERIOD = taskinfo$PERIOD*TIME_QUANTA
        taskinfo$INTER.MIN = taskinfo$INTER.MIN*TIME_QUANTA
        taskinfo$INTER.MAX = taskinfo$INTER.MAX*TIME_QUANTA
        taskinfo$DEADLINE = taskinfo$DEADLINE*TIME_QUANTA
    }
    write.table(taskinfo, RESOURCE2_FILE, append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
    
    
    ################################################################################
    # showing
    # print(sprintf("balanceProb=%.2f%%",balanceProb*100))
    g<-get_WCETspace_plot(data=training, form=formula.Text, xID=33, yID=30, 
                          showTraining=TRUE, nSamples=0, probLines=c(), showThreshold=TRUE)
    
    pdf(sprintf("%s/ModelLine_S%d_run%02d.pdf", OUTPUT_PATH, nSamples, runID.P1), width=7, height=5)
    print(g)
    dev.off()
    
}# for run phase 1



