########################################################
#### configuration
########################################################
# load library
cat("loading conf.R...\n")
# library(ggplot2)
# library(latex2exp)
library(stringr)

# The palette with grey:
# cbPalette <- c( "#00BFC4", "#F8766D", "#009E73", "#D55E00", "#0072B2", "#999999", "#E69F00", "#56B4E9", "#009E73", "#F0E442",   "#CC79A7")
cbPalette <- c( "#000000", "#AAAAAA", "#009E73", "#D55E00", "#0072B2", "#999999", "#E69F00", "#56B4E9", "#009E73", "#F0E442",   "#CC79A7")
######################################################
# loading resource
######################################################
TIME_QUANTA<-0.1
load_taskInfo <- function(filename){
    info <- read.csv(file=filename, header = TRUE)
    info <- data.frame(
      ID = c(1:nrow(info)),
      info
    )
    colnames(info)<- c("ID", "NAME", "TYPE", "PRIORITY", "WCET.MIN", "WCET.MAX", "PERIOD", "INTER.MIN", "INTER.MAX", "DEADLINE", "DEADLINE.TYPE")#,"RESULT.MIN", "RESULT.MAX")
    info$WCET.MIN = as.integer(round(info$WCET.MIN/TIME_QUANTA))
    info$WCET.MAX = as.integer(round(info$WCET.MAX/TIME_QUANTA))
    info$PERIOD = as.integer(round(info$PERIOD/TIME_QUANTA))
    info$INTER.MIN = as.integer(round(info$INTER.MIN/TIME_QUANTA))
    info$INTER.MAX = as.integer(round(info$INTER.MAX/TIME_QUANTA))
    info$DEADLINE = as.integer(round(info$DEADLINE/TIME_QUANTA))
    return (info)
}
TASK_INFO<-load_taskInfo(RESOURCE_FILE)

######################################################
# settings
######################################################
UNIT <- 0.0001
UNIT_STR<-"(s)"

parsingParameters <- function(filepath) {
    params<-list()
    con = file(filepath, "r")
    while ( TRUE ) {
        line = readLines(con, n = 1)
        if ( length(line) == 0 ) {
            break
        }
        strs = strsplit(line, ":")
        result<-strs[[1]]
        if (length(result)<=1){
            next
        }
        name<- str_trim(result[1])
        value<- str_trim(result[2])
        # print(sprintf("%s : %s", name, value))    
        if (name=="GA_ITERATION") params["GA_ITERATION"] = as.integer(value)
        if (name=="GA_POPULATION") params["GA_POPULATION"] = as.integer(value)
        if (name=="N_SAMPLE_WCET") params["N_SAMPLE_WCET"] = as.integer(value)
        if (name=="RUN_MAX") params["RUN_MAX"] = as.integer(value)
    }
    close(con)
    return(params)
}
