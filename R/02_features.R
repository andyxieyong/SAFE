# This code is for testing about stepwise
# The graph contains two line plot and box plot for each approach(GA, RS)
# The line plot shows the average of fitness values over 10 runs of the experiments.
# The box plot shows the distribution of fitness values over 10 runs of the experiments.
# Each fitness value of 1 run is the average value of fitness values over N samples.
#

############################################################
# R Parameter passing
############################################################
BASE_PATH <- getwd()
args <- commandArgs()
TARGET_PATH <- sprintf("%s/%s", BASE_PATH, args[6]) #sprintf('%s/results/20191222_P1_1000_S20_GASearch', BASE_PATH)
OUTPUT_PATH <- sprintf("%s/%s", BASE_PATH, args[7]) #sprintf("/analysis/02_stepwise", BASE_PATH)  #results/20190723_stepwise
N_RUNS = as.integer(args[8])      # 50
if(dir.exists(OUTPUT_PATH)==FALSE) dir.create(OUTPUT_PATH, recursive=TRUE)
RESOURCE_FILE <- sprintf("%s/input.csv", TARGET_PATH)

############################################################
# Load libraries
############################################################
setwd(sprintf("%s/R", BASE_PATH))
source("libs/conf.R")
source("libs/lib_data.R")     # update_data
source("libs/lib_quadratic.R")
source("libs/lib_draw.R")
source("libs/lib_formula.R")  # get_raw_names, get_base_name does not need lib_data.R
source("libs/lib_metrics.R")  # find_noFPR, FPRate
source("libs/lib_model.R")        # get_intercepts, get_bestsize_point, get_func_points
source("libs/lib_evaluate.R")     # find_noFPR
source("libs/lib_sampling.R") 
library(MASS)
library(dplyr)
library(randomForest)
library(ggplot2)
setwd(sprintf("%s/R", BASE_PATH))

############################################################
# SAFE Parameter parsing and setting 
############################################################
filepath<- sprintf("%s/settings.txt", TARGET_PATH)
params<- parsingParameters(filepath)

nSamples <- params[["N_SAMPLE_WCET"]]
populationSize <- params[['GA_POPULATION']]
iterations.P1 <- params[['GA_ITERATION']]
nRuns.P1 <- c(1:N_RUNS)

# nSamples <- 20
# populationSize <- 10
# iterations.P1 <- 1000
# nRuns.P1 <- c(1:50)
direction = "both"
outputFormula <- TRUE

total.values<-data.frame()
for(runID.P1 in nRuns.P1){
    datafile=sprintf('%s/_samples/sampledata_run%02d.csv', TARGET_PATH, runID.P1)
    
    # load data
    training <- read.csv(datafile, header=TRUE)
    nPoints <- (iterations.P1+populationSize) * nSamples # (iteration + population ) nSample
    training <- training[1:nPoints,]
    
    # print working information
    cat(sprintf("Working S%d Run%02d--------------------\n", nSamples, runID.P1))
    cat(sprintf("Target Path: %s\n", TARGET_PATH))
    cat(sprintf("Datafile   : %s\n", datafile))
    
    ############################################################
    # Checking importance through Random Forest 
    #   - removing terms in a priori is not good way, but if we have big dimension of data set, we can apply them
    ############################################################
    # rf<-randomForest(result ~ ., data=training, mtry=floor(sqrt(column_size)), ntree=sqrt(nrow(training)), importance=T)
    # mtry: depth of tree, usually recommended sqrt(column_size)
    # ntree: number of trees, trade off between accuracy and cost, choose one of them
    # 142 is the value of sqrt(nrow(training)),
    # I think the number will be meaningfull I add it.
    get_relative_importance<-function(rf_model, typeNum){
        # Generate relative importance for the rf model
        tnames<-sprintf("T%d", as.integer(substring(rownames(rf_model$importance),2)))
        impor<-rf_model$importance[,typeNum]
        impor<-impor/sum(impor)
        import_df<-data.frame(Task=tnames, Importance=impor)
        return (import_df)
    }
    select_terms<-function(rel_import, threshold){
        # select terms based on threshold_function from the relative_importance(data.frame)
        selected<- as.character(rel_import[rel_import$Importance>threshold,]$Task)
        
        cat(sprintf("\tselected terms by type2 (%d): %s\n", length(selected), paste(selected)))
        cat(sprintf("\tMean: %.4f", threshold))
        return (selected)
    }
    make_bar_chart<-function(data, nTree, nDepth){
        g<-ggplot(data=data, aes(x=reorder(Task, Importance), y=Importance))+
            geom_bar(stat="identity")+
            geom_text(aes(label=round(Importance,4)), hjust=-0.1, vjust=0.5, color="red", size=3)+
            coord_flip()+
            theme(legend.justification=c(1,0), legend.position=c(1, 0), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))+
            xlab("Task") +
            ylab("Relative importance")+
            ylim(0, 0.8)+
            ggtitle(sprintf("Relative Importance of Terms (nTree=%d, nDepth=%d)",nTree, nDepth))
        return (g)
    }
    
    
    ############################################################
    ## selecting features
    ############################################################
    cat("Selecting features...\n")
    nTreeRange= c(100)#, 2000, 3000, 4000, 5000) #c(100, 142, 200, 300,400, 500)
    depths<-c(floor(sqrt(ncol(training)-1)))#, floor((ncol(training)-1)/3))  #c(1, 3, 12, 15, 18, 20, 23, 26)
    features <- c()
    
    if (depths > 1){
        for(nDepth in depths){
            lib.plots<-list()
            my.plots <- list()
            for(nTree in nTreeRange){
                cat(sprintf("\tRamdomForest with parameters (nDepth=%d, nTree=%d)...\n", nDepth, nTree))
                rf<-randomForest(result ~ ., data=training, mtry=nDepth, ntree=100, importance = TRUE)
                # varImpPlot(rf)  #-- check importance by graph
                # my.plots[[length(my.plots)+1]]<- l
                
                import_df<- get_relative_importance(rf, 2)  # Use only Column 2 (IncNodePurity)
                mean_import<-mean(import_df$Importance)
                features <- select_terms(import_df, mean_import)
                
                values<-data.frame(t(import_df$Importance))
                colnames(values) <- import_df$Task
                total.values <- rbind(total.values, data.frame(Run=runID.P1, Mean=mean(import_df$Importance), values))

                # draw barchart
                my.plots[[length(my.plots)+1]]<- make_bar_chart(import_df, nTree, nDepth)
            }
            
            pdf(sprintf("%s/RF_S%d_nDeepth%d_run%02d_mean%.4f.pdf", OUTPUT_PATH, nSamples, nDepth, runID.P1, mean_import), width=7, height=5)
            for(plot.item in my.plots){
                print(plot.item)
            }
            dev.off()
        }
    }else{
        features <- colnames(training)
        features <- features[-1]
    }
    
    # Print result
    print(features)
    
    
    ############################################################################
    # Logistic regression with formula by choosen significant Tasks from the RF
    ############################################################################
    cat("Learning logistic regression...\n")
    formula_str = get_formula_complex("result", features)
    write(formula_str, file=sprintf("%s/formula_S%d_init", OUTPUT_PATH, nSamples))
    md <- glm(formula = formula_str, family = "binomial", data = training)
    md2 <- stepAIC(md, direction = direction, trace=0) # trace=0, stop to print processing
    
    # Get formula
    cl<- as.character(md2$formula)
    cat(sprintf("\tFormula: %s\n",formula_str))
    formula_str <- sprintf("%s %s %s", cl[2], cl[1], cl[3])
    
    # Save formula into R results folder
    formulaPath <- sprintf("%s/formula_S%d_final", OUTPUT_PATH, nSamples)
    write(formula_str, file=formulaPath)
    cat(sprintf("\tSaved formula into %s\n", formulaPath))
    
    # Save formula into data folder
    if(outputFormula == TRUE){
        formulaPath <- sprintf("%s/formula", TARGET_PATH)  #results/20190723_stepwise
        if(dir.exists(formulaPath)==FALSE) dir.create(formulaPath)
        formulaPath <- sprintf("%s/formula_run%02d", formulaPath, runID.P1)
        write(formula_str, file=formulaPath)
        cat(sprintf("\tSaved formula into %s\n", formulaPath))
    }
    
    ################################################################################
    # learning model with training data which is changed with UNIT
    g<-get_WCETspace_plot(data=training, form=formula_str, xID=33, yID=30,
                          showTraining=TRUE, nSamples=0, probLines=c(), showThreshold=TRUE)

    pdf(sprintf("%s/ModelLine_S%d_nDeepth%d_run%02d_mean%.4f.pdf", OUTPUT_PATH, nSamples, nDepth, runID.P1, mean_import), width=7, height=5)
    print(g)
    dev.off()
    cat("Done.\n\n")
}
write.table(total.values, sprintf("%s/importances_result.csv", OUTPUT_PATH), append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
