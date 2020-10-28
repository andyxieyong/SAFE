# This code is for generating graph of comparing GA and RS (for sampling WCET)
# The graph contains two line plot and box plot for each approach(GA, RS)
# The line plot shows the average of fitness values over 10 runs of the experiments.
# The box plot shows the distribution of fitness values over 10 runs of the experiments.
# Each fitness value of 1 run is the average value of fitness values over N samples.
#
BASE_PATH <- '~/projects/RTA_Expr'
RESOURCE_FILE <- sprintf("%s/res/task_descriptions.csv", BASE_PATH)
setwd(sprintf("%s/R", BASE_PATH))
source("libs/conf.R")
source("libs/lib_fitness.R")
source("libs/lib_draw.R")

############################################################
# Settings
############################################################
RESULT_PATH = "results"
TARGET_NAME <- "20191222_P1_1000_S20"
APPR <- c("GASearch", "RandomSearch") 
APPRStand <- c("GA", "RS")
SAMPLING = TRUE
runMAX = 50
iterMAX = 1000

significance_level = 0.05

############################################################
# Start to work
############################################################
RESULT_PATH <- sprintf("%s/results",BASE_PATH, TARGET_NAME, APPR[[1]])
OUTPUT_PATH <- sprintf("%s/analysis/01_Compare_Fitness", BASE_PATH)
if(dir.exists(OUTPUT_PATH)==FALSE) dir.create(OUTPUT_PATH, recursive=TRUE)

# generate graph for each task
results<-data.frame()
samples<-data.frame()
iterREPR<-iterMAX

# for the each approach
for (aid in 1:length(APPR)){
    appr_name <- sprintf('%s_%s', TARGET_NAME, APPR[[aid]])
    appr_stand <- APPRStand[[aid]]
    path <- sprintf("%s/%s/_results/", RESULT_PATH, appr_name)
    
    # Load one type data 
    raw_data<- load_raw_files(path, sampling=SAMPLING, 0.0001)
    
    #limit runs
    runs <- length(raw_data)
    runs <- ifelse(runMAX!=0 && runMAX<runs, runMAX, runs)
    print(sprintf("We have %d runs for %s", runs, appr_stand))
    
    # organize data
    agged_data <- agg_fitness(raw_data, runLimit=runs)
    min_iter <- get_min_iter(raw_data, runLimit=runs)
    samples_one <- merge_data(agged_data, iterLimit=min_iter)

    # Add type
    samples_one <- data.frame(samples_one, Type=appr_stand)
    
    samples <- rbind(samples, samples_one)
}

# cutting by iterREPR
samples <- samples[(samples$Iteration<=iterREPR),]

# calculate stats
stat.table<-calculate_stat_evaluation_search(samples, iterREPR, "Fitness")
last_stat <- stat.table[nrow(stat.table),]
if (last_stat$P.value > significance_level){
    print(sprintf("select H0: There are no difference.(p-value: %.4f, A12: %.2f (%s))", last_stat$P.value, last_stat$VDA, last_stat$VDA.Text))
} else{
    print(sprintf("select H1: There are significntly difference. (p-value: %.4f, A12: %.2f (%s))", last_stat$P.value, last_stat$VDA, last_stat$VDA.Text))
}

filename<-sprintf("RQ1_boxplot_runs%d_I%d", runMAX, iterMAX)
write.table(stat.table, sprintf("%s/%s_test.csv", OUTPUT_PATH, filename), append = FALSE, sep = ",", dec = ".",row.names = FALSE, col.names = TRUE)
stat.table <- read.csv(sprintf("%s/%s_test.csv", OUTPUT_PATH, filename), header=TRUE)

# Draw them for each
g<- generate_box_plot(samples, "Iteration", "Fitness", "Iteration", "Fitness value (s)", 10)
pdf(sprintf("%s/%s.pdf", OUTPUT_PATH, filename), width=7, height=3.5)
print(g)
dev.off()

subdata<-samples[samples$Iteration==iterMAX,]
BestRun <- subdata[subdata$Fitness==max(subdata$Fitness),]$Run
print(sprintf("BestRun:%d", BestRun))
