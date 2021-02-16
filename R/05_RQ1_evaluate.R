
# For comparing between random and distance sampling
#tidyverse
library(MASS)
library(dplyr)
library(MLmetrics)
library(effsize)
library(plotROC)
library(progress)
library(boot)

BASE_PATH <- '~/projects/RTA_SAFE'
setwd(sprintf("%s/R", BASE_PATH))
source("libs/conf.R")
source("libs/lib_metrics.R")
source("libs/lib_evaluate.R")
source("libs/lib_draw.R")

#########################################################################################
# Function definition
#########################################################################################
calculate_p_value_byiter <- function(dataR, dataD, iterMax, colname){
    # dataR<-R
    # dataD<-D
    # iterMax<-100
    # colname<-"Prec"
    stat.table<-data.frame()
    for (iter in c(1:iterMax)){
        statR <- dataR[dataR$Iter==iter,][[colname]]
        statD <- dataD[dataD$Iter==iter,][[colname]]
        tryCatch({
            uvalue<-wilcox.test(statR, statD, paired=TRUE) 
        }, error = function(e) {
            uvalue <- list("p.value"=1)
        })
        tryCatch({
            vda<-VD.A(statR, statD)
        }, error = function(e) {
            vda <- list("estimate"=1, "magnitude"="same")
        })
        
        avgR <- mean(statR)
        avgD <- mean(statD)
        
        stat.item<-data.frame(c(iter), c(avgR), c(mean(avgD)), c(uvalue$p.value), c(vda$estimate), c(sprintf("%s",vda$magnitude)))
        colnames(stat.item) <- c("Iteration", "Avg.R", "Avg.D", "P.value", "VDA", "VDA.Text")
        stat.table<-rbind(stat.table, stat.item)
    }
    return (stat.table)
}

#########################################################################################
# Settings
#########################################################################################
# load TASK_INFO (it is used in the library functions)
TARGET_PATH <- sprintf("%s/results/SAFE_GASearch",BASE_PATH)
params<- parsingParameters(sprintf("%s/settings.txt", TARGET_PATH))
TIME_QUANTA <- params[['TIME_QUANTA']]
TASK_INFO<-load_taskInfo(sprintf("%s/input.csv", TARGET_PATH), TIME_QUANTA)


# first phase parameter ########################################
nRuns.P1 <- c(1:50)
# second phase parameter ########################################
nModelUpdates = 100
nUpdate = 100
# graph and results ########################################
Kfold_Iter = 1
ApprTypes <- c("D", "R")

{
    # Prepare path
    RQ2Folder <- sprintf('Updates%d',nModelUpdates)
    EXTEND_PATH = sprintf('%s/refinements/%s', TARGET_PATH, RQ2Folder)
    OUTPUT_PATH <- sprintf("%s/analysis/05_evaluate_%d", BASE_PATH, nModelUpdates)
    if(dir.exists(OUTPUT_PATH)==FALSE) dir.create(OUTPUT_PATH, recursive = TRUE)
    
    total_cv_results <- data.frame()
    total_test_results <- data.frame()
    total_model_results <- data.frame()
    for(runID.P1 in nRuns.P1){
        cat(sprintf("For the phase 1 run %02d...\n", runID.P1))
        FORMULA_PATH <- sprintf("%s/formula/formula_run%02d", TARGET_PATH, runID.P1)
        formula.Text =  toString(read.delim(FORMULA_PATH, header=FALSE)[1,])
        
        for (appType in ApprTypes){
            workfilename <- sprintf('workdata_%s_run%02d', ifelse(appType=="R", "random", "distance"), runID.P1)
            
            test_results <- read.csv(sprintf("%s/%s_test_result.csv", EXTEND_PATH, workfilename), header=TRUE)
            model_results <- read.csv(sprintf("%s/%s_model_result.csv", EXTEND_PATH, workfilename), header=TRUE)
            cv_results <- read.csv(sprintf("%s/%s_termination_result.csv", EXTEND_PATH, workfilename), header=TRUE)
            
            total_test_results <- rbind(total_test_results, data.frame(Run=runID.P1, Type=appType, test_results))
            total_model_results <- rbind(total_model_results, data.frame(Run=runID.P1, Type=appType, model_results[c(1:8)]))
            total_cv_results <- rbind(total_cv_results, data.frame(Run=runID.P1, Type=appType, cv_results))
        }
    } # Runs.P1
    
    # save merged results
    output_filename <- sprintf("RQ_%d_%d_%drun",nModelUpdates, nUpdate, max(nRuns.P1))
    print("Drawing total results ...")
    write.table(total_test_results, sprintf("%s/%s_test_result.csv", OUTPUT_PATH, output_filename), append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
    write.table(total_model_results, sprintf("%s/%s_model_result.csv", OUTPUT_PATH, output_filename), append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
    write.table(total_cv_results, sprintf("%s/%s_termination_result.csv", OUTPUT_PATH, output_filename), append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
    
    # Draw them for each
    save_plot<-function(data, xname, yname, labname, legend="rb", legend_font=15, legend_direct="vertical", img_width=7, img_height=2.5, num_boxplot=10){
        pdf(sprintf("%s/%s_test_%s.pdf",OUTPUT_PATH, output_filename, yname), width=img_width, height=img_height)
        plot(generate_box_plot(data, xname, yname, "# of model refinements", labname, num_boxplot, legend=legend, legend_font=legend_font, legend_direct=legend_direct)) #, ylimit = c(0.9995, 1)
        dev.off()
    }

    # save plot    
    save_plot(total_test_results, "Iter", "Prec", "Precision", legend_font=13, legend_direct="horizontal", img_width=3.5, num_boxplot=5)
    save_plot(total_test_results, "Iter", "Rec", "Recall", legend="lt", legend_font=13, legend_direct="horizontal", img_width=3.5, num_boxplot=5)
    
    d_cv_results <- total_cv_results[total_cv_results$Type=="D",]
    save_plot(d_cv_results, "Iter", "CV.Precision.Sum", "Precision", legend="none", legend_direct="horizontal", legend_font=13)
    
    R <- total_test_results[total_test_results$Type=="R",]
    D <- total_test_results[total_test_results$Type=="D",]
    
    # calculate statistics 
    stat.precision <-calculate_p_value_byiter(R, D, nModelUpdates, "Prec")
    stat.recall <-calculate_p_value_byiter(R, D, nModelUpdates, "Rec")    
    write.table(stat.precision, sprintf("%s/%s_stats_rq2_prec.csv", OUTPUT_PATH, output_filename), append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
    write.table(stat.recall, sprintf("%s/%s_stats_rq2_recall.csv", OUTPUT_PATH, output_filename), append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
    
    print("Done")
}


