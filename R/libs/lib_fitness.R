library(tidyverse)
library(effsize)

############################################################
# Library part
############################################################
# load files 
load_raw_files<-function(base_path, sampling=FALSE, timeUNIT=1){
  filepath <- sprintf(base_path)
  files <- list.files(filepath)
  # max_cnt <- ifelse(limit>0, limit, length(files))
  
  data_set <- list()
  for (i in 1:length(files)){
    filename <- sprintf('%s%s', filepath, files[i])
    # print(filename)
    data <- read.csv(file=filename, header = TRUE)
    if (sampling==FALSE)
      colnames(data)<-c('Iteration', 'Fitness')
    else
      colnames(data)<-c('Iteration', 'SampleID', 'Fitness')
    data$Fitness <- data$Fitness * timeUNIT
    data_set[[i]] <- data
  }
  return (data_set)
}

# get_min_row
get_min_iter<- function(data_set, runLimit=0){
  #Set length of runs
  runs <- length(data_set)
  runs <- ifelse(runLimit!=0 && runLimit<runs, runLimit, runs)
  
  min_iter <- 10000000
  for (i in 1:runs){
    iter_size<-max(data_set[[i]]$Iteration)
    min_iter <- min(iter_size, min_iter)
  }
  return (min_iter)
}

# aggregation data set
agg_fitness<-function(data_set, runLimit=0, func=mean){
  #Set length of runs
  runs <- length(data_set)
  runs <- ifelse(runLimit!=0 && runLimit<runs, runLimit, runs)
  
  agged <- list()
  for(i in 1:runs){
    agg<- aggregate(data_set[[i]]['Fitness'], list(Iteration=data_set[[i]]$Iteration), func)
    agged[[i]] <- agg
  }
  return(agged)
}

# aggregation data set
agg_fitness_max<-function(data_set, runLimit=0){
  #Set length of runs
  runs <- length(data_set)
  runs <- ifelse(runLimit!=0 && runLimit<runs, runLimit, runs)
  
  agged <- list()
  for(i in 1:runs){
    agg<- aggregate(data_set[[i]]['Fitness'], list(Iteration=data_set[[i]]$Iteration), max)
    agged[[i]] <- agg
  }
  return(agged)
}

# aggregation data set
agg_fitness_min<-function(data_set, runLimit=0){
  #Set length of runs
  runs <- length(data_set)
  runs <- ifelse(runLimit!=0 && runLimit<runs, runLimit, runs)
  
  agged <- list()
  for(i in 1:runs){
    agg<- aggregate(data_set[[i]]['Fitness'], list(Iteration=data_set[[i]]$Iteration), min)
    agged[[i]] <- agg
  }
  return(agged)
}

#merge_data
merge_data<-function(data_set, column_name='Run', runLimit=0, iterLimit=0){
  #Set length of runs
  runs <- length(data_set)
  runs <- ifelse(runLimit!=0 && runLimit<runs, runLimit, runs)
  
  results <- data.frame()
  for (runID in 1:runs){
    data <- data_set[[runID]]
    names <- c(colnames(data), column_name)
    
    # data filter
    if (iterLimit != 0)
      data <- data[data$Iteration <= iterLimit,]

    # make data
    column_values <- rep.int(runID, nrow(data))
    data<- data.frame(data, column_values)
    colnames(data) <- names
    
    results <- rbind(results, data)
  }
  return (results)
}


############################################################
# Check statistically diffrence
############################################################
stat_compare <- function(data, iter, significance_level=0.05){
  fitness_RS <-data[(data$Iteration==iter & data$Type=="RS"),]$Fitness
  fitness_GA <-data[(data$Iteration==iter & data$Type=="GA"),]$Fitness
  uvalue<-wilcox.test(fitness_RS, fitness_GA, paired=TRUE) 
  if (uvalue$p.value>significance_level){
    # select H0: There are no difference.
    return (TRUE)
  }
  else{
    # select H1: There are significntly difference.
    return (FALSE)
  }
}

calculate_stat_evaluation_search<-function(data, iterMAX, column){
    stat.table<-data.frame()
    for(iter in 1:iterMAX){
        fitness_RS <-data[(data$Iteration==iter & data$Type=="RS"),][[column]]
        fitness_GA <-data[(data$Iteration==iter & data$Type=="GA"),][[column]]

        uvalue<-wilcox.test(fitness_RS, fitness_GA, paired=TRUE) 
        vda<-VD.A(fitness_RS, fitness_GA)

        avgRS <- mean(fitness_RS)
        avgGA <- mean(fitness_GA)
        
        stat.item<-data.frame(c(iter), c(avgRS), c(mean(avgGA)), c(uvalue$p.value), c(vda$estimate), c(sprintf("%s",vda$magnitude)))
        colnames(stat.item) <- c("Iteration", "Avg.RS", "Avg.GA", "P.value", "VDA", "VDA.Text")
        stat.table<-rbind(stat.table, stat.item)
    }
    return (stat.table)
}
