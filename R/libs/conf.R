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
load_taskInfo <- function(filename, timeQuanta){
    info <- read.csv(file=filename, header = TRUE)
    # info <- data.frame(
    #   ID = c(1:nrow(info)),
    #   info
    # )
    colnames(info)<- c("ID", "NAME", "TYPE", "PRIORITY", "OFFSET", "WCET.MIN", "WCET.MAX", "PERIOD", "INTER.MIN", "INTER.MAX", "DEADLINE", "DEADLINE.TYPE")#,"RESULT.MIN", "RESULT.MAX")
    info$WCET.MIN = as.integer(round(info$WCET.MIN/timeQuanta))
    info$WCET.MAX = as.integer(round(info$WCET.MAX/timeQuanta))
    info$PERIOD = as.integer(round(info$PERIOD/timeQuanta))
    info$INTER.MIN = as.integer(round(info$INTER.MIN/timeQuanta))
    info$INTER.MAX = as.integer(round(info$INTER.MAX/timeQuanta))
    info$DEADLINE = as.integer(round(info$DEADLINE/timeQuanta))
    return (info)
}


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
        name <- str_trim(result[1])
        valueT <- str_trim(str_replace_all(result[2], "\"", ""))
        valueI <- as.integer(valueT)
        valueD <- as.double(valueT)
        if (is.na(valueI)==TRUE && is.na(valueD)==TRUE){
          value <- valueT
          # value <- ifelse(is.na(as.double(valueT))==TRUE, valueT, as.double(valueT))
        } else{
          value <- ifelse(valueI!=valueD, valueD, valueI)
        }
        if (tolower(value)=="false") value <- FALSE
        if (tolower(value)=="true") value <- TRUE
        params[[name]] = value
    }
    close(con)
    return(params)
}
library(gdata)
