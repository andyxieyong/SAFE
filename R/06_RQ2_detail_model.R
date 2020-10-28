# For comparing between random and distance sampling
#
library(MASS)
library(dplyr)
library(MLmetrics)
library(effsize)
library(plotROC)
library(progress)

BASE_PATH <- '~/projects/RTA_Expr'
RESOURCE_FILE <- sprintf("%s/res/task_descriptions.csv", BASE_PATH)
setwd(sprintf("%s/R", BASE_PATH))
source("libs/conf.R")
source("libs/lib_data.R")     # update_data
source("libs/lib_model.R")    # integrateMC, get_bestsize_point
source("libs/lib_formula.R")
source("libs/lib_evaluate.R")   # find_noFPR
source("libs/lib_draw.R")       # generate_box_plot, get_WCETspace_plot


#########################################################################################
# Settings
#########################################################################################
nRuns.P1 <- c(2)
# second phase parameter ########################################
nModelUpdates = 100
nUpdate = 100
# graph and results ########################################
drawIterations <- c(100)

## RQ2
ApprTypes <- c("D", "R")#,
colorPalette<-c(cbPalette[2], cbPalette[1])

# Prepare paths
{
    SOURCE_PATH <- sprintf('%s/results/20191222_P1_1000_S20_GASearch', BASE_PATH)
    RQ2Folder <- sprintf(sprintf('Updates%d',nModelUpdates))
    EXTEND_PATH = sprintf('%s/refinements/%s', SOURCE_PATH, RQ2Folder)
    OUTPUT_PATH <- sprintf("%s/analysis/06_evaluate_detail", BASE_PATH)
    if(dir.exists(OUTPUT_PATH)==FALSE) dir.create(OUTPUT_PATH, recursive = TRUE)

    for(runID.P1 in nRuns.P1){ 
        cat(sprintf("Working with Run %02d...\n", runID.P1))
        FORMULA_PATH <- sprintf("%s/formula/formula_run%02d", SOURCE_PATH, runID.P1)
        formula.Text =  toString(read.delim(FORMULA_PATH, header=FALSE)[1,])
    
        # update TASK_INFO
        TASK_INFO<-load_taskInfo(sprintf("%s/inputs/reduced_run%02d.csv", SOURCE_PATH, runID.P1))
        
        uncertainIDs <- get_base_names_formula(formula.Text, isNum=TRUE)
        conditions <- data.frame()
        for (ApprType in ApprTypes) {
            output_filename <- sprintf("compare_%s_run%02d",RQ2Folder, runID.P1)
            input_filename <- sprintf("workdata_%s_run%02d", ifelse(ApprType=="R", "random", "distance"), runID.P1)
            
            # Load reference
            refModel <- sprintf("%s/%s_model_result.csv",EXTEND_PATH, input_filename)
            refModel <- read.csv(refModel, header=TRUE)

            if (!is.null(drawIterations)){
                # Load test results
                dataSet <- sprintf("%s/%s.csv", EXTEND_PATH, input_filename)
                dataSet <- read.csv(dataSet, header=TRUE) 
    
                pb<- progress_bar$new(format=sprintf("Updating model based %s sampling [:bar] :current/:total in :elapsed", ApprType), total=length(drawIterations), clear=FALSE, width=80)
                pdf(sprintf("%s/%s_detail_with_%s_data.pdf", OUTPUT_PATH, output_filename, ApprType), width=7, height=2.5)
                for (iter in drawIterations){ 
                    trainingSize <- refModel[refModel$nUpdate==iter,]$TrainingSize
                    training <- dataSet[1:trainingSize,]
    
                    g<-get_WCETspace_plot(training, formula.Text, 33, 30, showTraining=TRUE, showThreshold=TRUE, showMessage=FALSE, showBestPoint = TRUE,
                                          reduceRate = 0.02)
                    plot(g)
                    
                    pb$tick()
                }
                dev.off()
                pb$terminate()
            }
        }
    }
    cat("\tFinished all works\n")
}







