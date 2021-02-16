# For merging the test data set
#
BASE_PATH <- '~/projects/RTA_SAFE'
setwd(sprintf("%s/R", BASE_PATH))
source("libs/conf.R")
library(stringr)

check_str<-function(str, pattern, check_str){
    q<-str_match(str, pattern)
    if (!is.na(q[1])){
        if (q[2] == check_str)
            return (TRUE)
    }
    return (FALSE)
}
merge_testdata<-function(origin, target, runID){
    flist <- list.files(origin)
    evalData <- data.frame()
    cnt <- 0
    for(fname in flist){
        print(fname)
        if(!check_str(fname, "_run([0-9]+)", sprintf("%02d",runID))) next
        eData <- read.csv(sprintf("%s/%s", origin, fname), header=TRUE)
        evalData <- rbind(evalData, eData)
        cnt <- cnt+1
    }
    if (nrow(evalData)==0){
        print("No files to merge or no directory for this.")
        return (0)
    }
    if(dir.exists(target)==FALSE) dir.create(target, recursive=TRUE)
    savepath <- sprintf("%s/testdata_N%d_run%02d.csv", target, nrow(evalData), runID)
    write.table(evalData, savepath, append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
    print(sprintf("Merged %d files into %s", cnt, savepath))
}


#########################################################################################
#########################################################################################
#########################################################################################
TARGET_PATH <- sprintf("%s/results/SAFE_GASearch",BASE_PATH)
params<- parsingParameters(sprintf("%s/settings.txt", TARGET_PATH))
RUN_MAX <- c(1:params[['RUN_MAX']])

nRuns.P1 = c (1:RUN_MAX)

for(runID.P1 in nRuns.P1){
    print(sprintf("working with run %d", runID.P1))
    ORIGIN_PATH<-sprintf('%s/testdata_run%d', TARGET_PATH, runID.P1)
    DIST_PATH <- sprintf('%s/testdataR', TARGET_PATH)
    merge_testdata(ORIGIN_PATH, DIST_PATH, runID.P1)
}
