# Library for sampling from the LR model or random
# dependency:
#   - conf.R: WCET.MIN and WCET.MAX in TASK_INFO(data.frame)
#   - lib_model.R: generate_model_line
cat("loading lib_sampling.R...\n")
if (Sys.getenv("RSTUDIO")==1){
    # Add library
    source('libs/lib_model.R')
}

## defind library
# fminbnd function - Brent's algorithm
# fminsearch function - Nelder-mead algorithm
library(pracma)   
# install.packages("neldermead")
# library(neldermead)    # fminbnd by nelder-mead algorithm (Not applied)

########################################################
#### get test data to give to predict function
########################################################
sample_by_random <- function(tasknames, nSample){
    # This function generates random sample points within range of each tasks
    # dependency:
    #   - WCET.MIN and WCET.MAX in TASK_INFO(data.frame)
    # Input:
    #   - tasknames(array): task names
    #   - nSample (int): the number of sample points
    # Output: sampled points(data.frame)
    
    sample_ds <- data.frame()
    # for each task,
    for (tname in tasknames){
        taskID <- strtoi(substring(tname, 2))
        x <- sample(TASK_INFO$WCET.MIN[[taskID]]:TASK_INFO$WCET.MAX[[taskID]], nSample, replace=T)
        if (nrow(sample_ds)==0){
            sample_ds <-data.frame(x)
            colnames(sample_ds) <- c(tname)
        }
        else{
            sample_ds[tname]<-x
        }
    }
    return (sample_ds*UNIT)
}

# ************************************************
# sample points from the model (deprecated, error)
sample_based_distance_old <- function(tasknames, model, nSample, nCandidate, P){
    # This function generates sample points within range of each tasks filtered by distance
    # dependency:
    #   - WCET.MIN and WCET.MAX in TASK_INFO(data.frame)
    # Input:
    #   - data(data.frame): To get how many dimension we use
    #   - nSample (int): the number of sample points
    # Output: sampled points(data.frame)
    
    # get task names
    samples <- data.frame()
    for(x in 1:nSample){
        candidates <- sample_by_random(tasknames, nCandidate)
        sample <- ..select_by_distance_old(candidates, model, P)
        samples <- rbind(samples, sample)
    }
    return (samples)
}

..select_by_distance_old<-function(candidates, model, P){
    
    # calculate denominator (I don't calculate denominator because every points have same denominator)
    deno = 0
    b <- model$coefficients
    for(i in 2:length(b)){deno = deno + b[[i]]^2}
    deno = sqrt(deno)
    
    # get predicted values
    predict_values <- predict(model, newdata=candidates, type="link")
    
    # find minimum index
    min_Index = 1
    min_Value = 2^.Machine$double.digits
    for(x in 1:nrow(candidates)){
        nu = abs( predict_values[x] - log(P/(1-P)) )
        dist <- nu/deno
        if (min_Value>dist){
            min_Index = x
            min_Value = dist
        }
    }
    
    # return one sample of data.frame
    return ( candidates[min_Index,] )
}

# ************************************************
# generate examples from the model within some distance
#   - this function is to make estimated model line
sample_regression_points <- function(tasknames, model, nPoints, P, min_dist){
    # This function generates sample points within range of each tasks filtered by distance
    # dependency:
    #   - WCET.MIN and WCET.MAX in TASK_INFO(data.frame)
    # Input:
    #   - data(data.frame): To get how many dimension we use
    #   - nSample (int): the number of sample points
    # Output: sampled points(data.frame)
    
    # get task names
    samples <- data.frame()
    while(nrow(samples)<nPoints){
        candidates <- sample_by_random(tasknames, nPoints)
        sample <- ..select_within_distance(candidates, model, P, min_dist)
        samples <- rbind(samples, sample)
    }
    return (samples[1:nPoints,])
}

..select_within_distance<-function(candidates, model, P, min_dist){
    
    # calculate denominator (I don't calculate denominator because every points have same denominator)
    deno = 0
    b <- model$coefficients
    for(i in 2:length(b)){deno = deno + b[[i]]^2}
    deno = sqrt(deno)
    
    # get predicted values
    predict_values <- predict(model, newdata=candidates, type="link")
    
    # find minimum index
    accepted=data.frame()
    for(x in 1:nrow(candidates)){
        nu <- abs( predict_values[x] - log(P/(1-P)) )
        dist <- nu/deno
        if (dist<=min_dist){
            accepted <- rbind(accepted, candidates[x,])
        }
    }
    
    # return one sample of data.frame
    return (accepted)
}

# ************************************************
# generate examples from the model in some ranges
sample_based_model_prob_inrange <- function(tasknames, model, nSample, Ps, Prange){
    # This function generates sample points within range of each tasks filtered by distance
    # dependency:
    #   - WCET.MIN and WCET.MAX in TASK_INFO(data.frame)
    # Input:
    #   - data(data.frame): To get how many dimension we use
    #   - nSample (int): the number of sample points
    # Output: sampled points(data.frame)
    
    # get task names
    samples <- data.frame()
    while(nrow(samples)<nSample){
        candidates <- sample_by_random(tasknames, 20)
        sample <- ..select_range_distance(candidates, model, Ps-Prange, Ps+Prange, 0)
        samples <- rbind(samples, sample)
    }
    return (samples[1:nSample,])
}

..select_range_distance<-function(candidates, model, Pmin, Pmax, overBound){
    
    # calculate denominator (I don't calculate denominator because every points have same denominator)
    deno = 0
    b <- model$coefficients
    for(i in 2:length(b)){deno = deno + b[[i]]^2}
    deno = sqrt(deno)
    
    # get predicted values
    predict_values <- predict(model, newdata=candidates, type="link")
    
    # find minimum index
    accepted=data.frame()
    for(x in 1:nrow(candidates)){
        nu_min <- predict_values[x] - log(Pmin/(1-Pmin))
        nu_max <- predict_values[x] - log(Pmax/(1-Pmax))
        accept = FALSE
        if (nu_min >= 0 && nu_max<=0){
            accept=TRUE
        }
        else{
            dist_min <- abs(nu_min)/deno
            dist_max <- abs(nu_max)/deno
            if (dist_min<=overBound || dist_max<=overBound){
                accept=TRUE
            }
        }
        if (accept){
            accepted <- rbind(accepted, candidates[x,])
        }
    }
    
    # return one sample of data.frame
    return (accepted)
}



######################################################################
# generate WCET examples around model line (with threshold P)
#  - generate n candidates and select one the shortest distance from the model
sample_based_prob_distance <- function(tasknames, model, nSample=1000, nCandidate=20, P=threshold){
    # get task names
    samples <- data.frame()
    for(x in 1:nSample){
        candidates <- sample_by_random(tasknames, nCandidate)
        sample <- ..select_based_prob_distance(candidates, model, P)
        samples <- rbind(samples, sample)
    }
    return (samples)
}

# ************************************************
# select one example among candidates 
#  - select based a distance from the model line (with threshold P)
..select_based_prob_distance<-function(candidates, model, P){
    # get predicted values
    predict_values <- predict(model, newdata=candidates, type="response")
    
    # find minimum index
    min_index = -1
    min_diff = 2^.Machine$double.digits
    for (x in 1:length(predict_values)){
        # if (predict_values[x] < P) next
        diff <- abs(predict_values[x] - P)
        if (diff < min_diff){
            min_index <- x
            min_diff <- diff
        }
    }
    return (candidates[min_index,])
}


########################################################
# generate WCET examples around model line (with threshold P)
#  - generate n candidates and select one the shortest distance from the model
#  - this function uses euclidian distance
sample_based_euclid_distance <- function(tasknames, model, nSample, nCandidate, P, isGeneral=TRUE){
    
    # get task names
    targetIDs <- get_base_names(names(model$coefficients), isNum=TRUE)
    samples <- data.frame()
    
    count <-0
    dpoint <- nSample %/% 50
    if (dpoint!=0) cat(sprintf("%d points sampling", nSample))
    for(x in 1:nSample){
        if (dpoint!=0 && count%%dpoint==0){
            cat( ifelse(count%%(dpoint*5)==0, "|", ".") )
        }
        candidates <- sample_by_random(tasknames, nCandidate)
        sample <- ..select_based_euclid_distance(candidates, model, P, targetIDs, isGeneral)
        samples <- rbind(samples, sample)
        count <- count+1
    }
    if (dpoint!=0) cat("finished\n")
    return (samples)
}

# ************************************************
# select one example among candidates
#  - generate n candidates and select one the shortest distance from the model
#  - this function uses euclidian distance
#  - targetIDs : selected features in the formula (the last one is one that used to function result)
..select_based_euclid_distance<-function(candidates, model, P, targetIDs, isGeneral=TRUE){

    # generate IDs
    answerID <- targetIDs[length(targetIDs)]
    pointsIDs <- targetIDs[1:(length(targetIDs)-1)]
    
    # generate points
    answers <- candidates[[sprintf("T%d", answerID)]]
    points <- NULL
    for ( x in pointsIDs){ points<- cbind(points, candidates[[sprintf("T%d", x)]]) }
    points<- as.data.frame(points)
    colnames(points) <- sprintf("T%d", pointsIDs)
    
    # find minimum index
    min_Index = 1
    min_Value = 2^.Machine$double.digits
    for(px in 1:nrow(points)){
        pointX <- as.vector(points[px,])
        pointY <- answers[px]
       
        if (isGeneral==TRUE){
            fx<-generate_line_function(model, P, answerID, TASK_INFO$WCET.MIN[answerID]*UNIT, TASK_INFO$WCET.MAX[answerID]*UNIT)
        } else{
            fx<-get_model_func_quadratic(model, P, TASK_INFO$WCET.MIN[answerID]*UNIT, TASK_INFO$WCET.MAX[answerID]*UNIT)
        }
        
        dist_func<-function(X){
            # print(sprintf("X:%s",..to_string(X)))
            dx <- Norm(X - pointX)
            dy <- fx(X) - pointY
            dist<- sqrt(dx^2 + dy^2)
            return (dist)
        }
        # find minimum distance in range (WCET.MIN, WCET.MAX)
        xID <- pointsIDs[1]
        intercepts<-get_intercepts(model, P, targetIDs)
        v<-fminbnd(dist_func, TASK_INFO$WCET.MIN[[xID]]*UNIT, TASK_INFO$WCET.MAX[[xID]]*UNIT) #
        # v<-fminbnd(dist_func, TASK_INFO$WCET.MIN[[xID]]*UNIT, intercepts[[sprintf("T%d",xID)]]) #TASK_INFO$WCET.MAX[[xID]]*UNIT) 
        # print(sprintf("xmin=%.4f, fmin=%.4f, niter=%d, estim.prec=%e", v$xmin, v$fmin, v$niter, v$estim.prec))
        if (min_Value > v$fmin){
            min_Index = px
            min_Value = v$fmin
        }
    }
    
    return (candidates[min_Index,])
}

select_based_euclid_distance_fx<-function(candidates, fx){

    # find minimum index
    min_Index = 1
    min_Value = 2^.Machine$double.digits
    for(px in 1:nrow(candidates)){
        pointX <- candidates$X[px]
        pointY <- candidates$Y[px]
        
        dist_func<-function(X){
            # print(sprintf("X:%s",..to_string(X)))
            dx <- Norm(X - pointX)
            dy <- fx(X) - pointY
            dist<- sqrt(dx^2 + dy^2)
            return (dist)
        }
        # find minimum distance in range (WCET.MIN, WCET.MAX)
        v<-fminbnd(dist_func, 0, 3.6)
        # print(sprintf("xmin=%.4f, fmin=%.4f, niter=%d, estim.prec=%e", v$xmin, v$fmin, v$niter, v$estim.prec))
        if (min_Value > v$fmin){
            min_Index = px
            min_Value = v$fmin
        }
    }
    
    return (candidates[min_Index,])
}



