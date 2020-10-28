# Library for logistic regression that includes
#   - generate model function
#   - generate points on the model
# Dependency
#   - lib_formula.R : get_base_names
cat("loading lib_model.R...\n")
if (Sys.getenv("RSTUDIO")==1){
    source("libs/lib_formula.R")  # get_raw_names, get_base_name does not need lib_data.R
}



#############################################
# calculate line function
#############################################

# ************************************************
# find index if the base is in tasks (task ID list)
..find_task_index<- function(base, tasks){
    ret <- 0
    for (i in 1:length(tasks)){ 
        if (tasks[[i]]==base) {
            ret = i
        }
    }
    return (ret)
}

# ************************************************
# generate single term info
#   - return poly and base of the term
#   - if term is interection term, we will return a list of two vectors
..get_term_info <- function(term){
    base <- c()
    poly <- c()
    tnames <- strsplit(term, ":")[[1]]
    for(tname in tnames){
        if (startsWith(tname, "I(") == TRUE) {
            base <- c(base, as.integer(substring(tname, 4, nchar(tname)-3)) )
            poly <- c(poly, as.integer(substring(tname, nchar(tname)-1, nchar(tname)-1)) )
        } else if (startsWith(tname, "T") == TRUE) {
            base <- c(base, as.integer(substring(tname, 2)) )
            poly <- c(poly, 1)
        } else{
            base<- c(base, 0)
            poly<- c(poly, 0)
        }
    }
    return (list("base"=base, "poly"=poly))
}

# ************************************************
# generate multiple terms info
#   - poly, index, target (poly for target task)
#   - 
..parsing_coef <- function(coef, TaskIDs, targetID){
    # coef<-model$coefficients
    # targetID<-33
    
    # group variables
    poly<- c()  # poly for the non-target tasks (0, 1, 2)
    index<-c()  # taskIDs Mapped index
    target<-c()  # poly for target task (0, 1, 2)
    
    # grouping for each polynomial
    terms<-names(coef)
    for(idx in 1:length(coef)){
        # idx<-3
        term <- terms[idx]
        info <-..get_term_info(term)
        # info
        if (length(info$poly)==2) {
            # interaction
            tIdx <- ifelse(info$base[[1]]==targetID, 1, 2)
            oIdx <- ifelse(info$base[[1]]==targetID, 2, 1)
            
            target <- c(target, info$poly[[tIdx]])
            index <- c(index, ..find_task_index(info$base[[oIdx]], TaskIDs))
            poly <- c(poly, info$poly[[oIdx]])
        }else {
            # single
            if(info$base==targetID){  # if target task...
                target <- c(target, info$poly)
                index <- c(index, 0)
                poly <- c(poly, 0)
            } else {
                target <- c(target, 0)
                index <- c(index, ..find_task_index(info$base, TaskIDs))
                poly <- c(poly, info$poly)
            }
        }
    }
    # data.frame("name"=names(coef)[1:length(poly)],"coef"=as.double(coef)[1:length(poly)], "poly"=poly,"index"=index,"target"=target)
    info <- data.frame(
        "coef"=as.double(coef), 
        "poly"=poly,
        "index"=index, 
        "target"=target
    )
    rownames(info) <- names(coef)
    return (info)
}

# ************************************************
# to convert vector to string:: c(x,x,x)
# util function to show vector
..to_string <- function(marray){
    t <- sprintf("c(%s",marray[1])
    if(length(marray)>1){
        for (i in 2:length(marray)){
            t<-sprintf("%s,%s", t, marray[i])
        }
    }
    t<-sprintf("%s)", t)
    return (t)
}

# ************************************************
# calculate with X and selected term info (coef, poly, x_index, target)
# Not used currently, because the performance
..term_calculator<-function(tinfo, X){
    value = 0
    for(i in 1:nrow(tinfo)){
        base <- ifelse(tinfo$index[i]!=0, X[tinfo$index[i]], 1)
        value <- value + (tinfo$coef[i]*base^tinfo$poly[i])
    }
    return(value)
}

# ************************************************
# To generate model line function
#   - model with probability threshold
#   - related tasks
#   - target task ID, and range(minY, maxY) of target task
generate_line_function<-function(model, P, targetY, minY, maxY){
    
    # get tasks from the formula and remove target task
    TaskIDs <- get_base_names(names(model$coefficients), isNum=TRUE)
    idx<-..find_task_index(targetY, TaskIDs)
    if(idx==0){
        cat("target task does not exists in the model formula!!")
        return (NULL)
    }
    TaskIDs <- TaskIDs[-idx]
    info <- ..parsing_coef(model$coefficients, TaskIDs, targetY)
    # print(info)
    
    if ((2 %in% info$target)==FALSE) {
        # 1st order
        func<-function(X){
            # removed because the slow performance
            # b = ..term_calculator(info[info$target==1,], X)
            # c = ..term_calculator(info[info$target==0,], X) - log(P/(1-P))
            b=c=0
            for(i in 1:nrow(info)){
                base <- ifelse(info$index[i]!=0, X[info$index[i]], 1)
                value <- (info$coef[i]*base^info$poly[i])
                if (info$target[i]==1) b <- b + value
                if (info$target[i]==0) c <- c + value
            }
            c <- c - log(P/(1-P))
            return (c/-b)
        }
        # a = list()
        # b = list()
        # c = 0
        # k = 0
        # print(info)
        # for(i in 1:nrow(info)){
        #     row <- info[i,]
        #     if(row$index =0 && row$target==1) k <- row$coef[i]
        #     if(row$target==0 && row$poly==2){
        #         a[row$index] <- ifelse(is.null(a[row$index])==TRUE, row$coef, a[row$index] + info$coef)
        #     }
        #     if(row$target==0 && row$poly==1){
        #         print(b)
        #         if(is.null(b[row$index])==TRUE) { b[row$index] <- row$coef }
        #         else { b[row$index] <- b[row$index] + row$coef }
        #     }
        #     if(row$index==0 && row$target==0 && row$poly==0) c <- c + row$coef
        # }
        # c <- c - log(P/(1-P))
        # a <- as.interger(a)
        # b <- as.interger(b)
        # print(sprintf("a=%.4f, b=%.4f, c=%.4f / k=%.4f", a, b, c, k))
        # func<-function(X){
        #     return ( (sum(a*X^2) + sum(b*X) + c) / -k )
        # }
    }else{
        # 2nd order (use sqrt )
        func<-function(X){
            # removed because the slow performance
            # a = ..term_calculator(info[info$target==2,], X)
            # b = ..term_calculator(info[info$target==1,], X)
            # c = ..term_calculator(info[info$target==0,], X) - log(P/(1-P))
            a=b=c=0
            for(i in 1:nrow(info)){
                base <- ifelse(info$index[i]!=0, X[info$index[i]], 1)
                value <- (info$coef[i]*base^info$poly[i])
                
                if (info$target[i]==2) a <- a + value
                if (info$target[i]==1) b <- b + value
                if (info$target[i]==0) c <- c + value
            }
            c <- c - log(P/(1-P))
            
            inV = (b^2)-(4*a*c)
            if (inV<0){
                # print(sprintf("return Inf because inV:%.6f", inV))
                return (Inf)
            }
            s1 = (-b + sqrt(inV)) / (2*a)
            s2 = (-b - sqrt(inV)) / (2*a)
            if (s1>=minY && s1 <=maxY){
                return (s1)
            } else if (s2>=minY && s2 <=maxY){
                return (s2)
            } else {
                # print(sprintf("return Inf %.2f - (s1:%.4f, s2:%.4f) [%.4f,%.4f ]", X, s1, s2, minY, maxY))
                return (Inf)
            }
        }
    }
    return(func)
} # line function

# ************************************************
# To get points on the function in range of X
#   - function which f(X)
#   - minX, maxY: range of X
#   - nPoints: number of points
get_func_points<-function(fun, minX, maxX, nPoints){

    # generate x values
    candidates <- c(minX)
    by <- (maxX - minX) / nPoints
    x <- 0
    while(x< maxX){
        x <- x + by
        if (x>=minX)
            candidates <- c(candidates, x)
    }
    candidates <- c(candidates, maxX)
    
    # generate y values
    vX <- c()
    vY <- c()
    for(value in candidates){
        tryCatch({
            v<-fun(value)    
        }, error = function(e) {
            v<-Inf
        })
        if (v==Inf) next
        if (v==0) next
        vX <- c(vX, value)
        vY <- c(vY, v)
    }
    return (data.frame("x"=vX, "y"=vY))
}


#############################################
# related get intercept
#############################################
# dimension=0 : interaction
..find_idx <- function(cof, targetID, dimension){
    cnames <- names(cof)
    targetX <- 0
    
    for (x in 1:length(cnames)){
        term <- cnames[x]
        
        if (dimension==2){
            if (startsWith(term, "I(") == FALSE){next}
            tID <- as.integer(substring(term, 4, nchar(term)-3))
            if (tID == targetID){
                targetX <- x
                break
            }
        }
        if (dimension==1){
            if (startsWith(term, "T") == FALSE){next}
            tID<- as.integer(substring(term, 2))
            if (tID == targetID){
                targetX <- x
                break
            }
        }
    }
    return(targetX)
}

#************************************************
#
..delta<-function(a,b,c){
    b^2-4*a*c
}

#************************************************
# 
..qSolver <- function(w1,w2,c){
    dt<- ..delta(w1,w2,c) 
    if(dt > 0){ # first case D>0
        x_1 = (-w2+sqrt(dt))/(2*w1)
        x_2 = (-w2-sqrt(dt))/(2*w1)
        result = c(x_1,x_2)
    }
    else if(dt == 0){ # second case D=0
        result = -w2/(2*w1)
    }
    else {NaN} # third case D<0  "There are no real roots."
    #     result = -w2/(2*w1)
    # }
}

#************************************************
# get intercepts from the model with P
get_intercepts<-function(model, Plist, IDs){

    cof<-model$coefficients
    df<-data.frame()
    for ( P in Plist){
        # nItem <- data.frame()
        nItem <- data.frame(t(rep(0, length(IDs))))
        colnames(nItem) <- sprintf("T%d",IDs)
        for (id in IDs){
            idxInter<- 1    # intercept
            idx1<-..find_idx(cof, id, 1) # 1 dimen
            idx2<-..find_idx(cof, id, 2) # 2 dimen
            if(idx2!=0){
                value=..qSolver(cof[idx2], ifelse(idx1==0, 0, cof[idx1]), cof[idxInter]-log(P/(1-P)))[1]
            }
            else{
                value=(log(P/(1-P))-cof[idxInter]) / cof[idx1]
            }
            nItem[sprintf("T%d",id)] <- value
        }
        df <- rbind(df, nItem)
    }
    rownames(df) <- Plist
    return (df)
}

#************************************************
# complement intercepts: if there is nan or over the initial bound, we set initial bound
complement_intercepts <- function(intercepts, uncertainIDs, task_info){
    for(tID in uncertainIDs){
        tname = sprintf("T%d",tID)
        if (is.nan(intercepts[1,tname])==TRUE){
            intercepts[tname] = task_info$WCET.MAX[[tID]]
        } else{
            intercepts[tname] = min(ceiling(intercepts[[tname]]), task_info$WCET.MAX[[tID]])
        }
    }
    return (intercepts)
}


#############################################
# calculate line function (for test)
# This functions for the specific formula  
#############################################
get_model_func_linear <- function(model, P){
    w <- as.double(model$coefficients)
    inter <- log(P/(1-P))
    f <- function(x){
        return (-1*((w[1] + w[3]*x + w[4]*x^2-inter)/(w[2] + w[5]*x)))
    }
    return(f)
}

get_model_func_quadratic <- function(model, P, minY, maxY){
    w <- as.double(model$coefficients)
    inter <- log(P/(1-P))
    f <- function(fX_value){
        a = w[4]
        b = w[3] + w[5]*fX_value
        c = w[1] + w[2]*fX_value - inter
        inV = (b^2)-(4*a*c)
        if (inV<0){
            return (Inf)
        }
        s1 = (-b + sqrt(inV)) / (2*a)
        s2 = (-b - sqrt(inV)) / (2*a)
        if (s1>=minY && s1 <=maxY){
            return (s1)
        } else if (s2>=minY && s2 <=maxY){
            return (s2)
        } else {
            return (0)
        }
    }
    return(f)
}

#############################################
# find maximum area by a point on the model function
# This functions for the specific formula  
#############################################
get_bestsize_point<-function(model, P, targetIDs, isGeneral=TRUE, modelUNIT=1){
    
    # generate IDs
    answerID <- targetIDs[length(targetIDs)]
    pointsIDs <- targetIDs[1:(length(targetIDs)-1)]
    
    # find minimum index
    if (isGeneral==TRUE){
        fx<-generate_line_function(model, P, answerID, TASK_INFO$WCET.MIN[answerID]*modelUNIT, TASK_INFO$WCET.MAX[answerID]*modelUNIT)
    } else{
        fx<-get_model_func_quadratic(model, P, TASK_INFO$WCET.MIN[answerID]*modelUNIT, TASK_INFO$WCET.MAX[answerID]*modelUNIT)
    }
        
    area_func<-function(X){
        multi <- 1
        for(v in X){
            multi <- multi * v
        }
        area <- multi * fx(X)
        # print(sprintf("Area(%.2f, %.2f) = %.2f", X, fx(X), area))
        return (area)
    }
    # find minimum distance in range (WCET.MIN, WCET.MAX)
    xID <- pointsIDs[1]
    intercepts<-get_intercepts(model, P, targetIDs)
    v<-fminbnd(area_func, TASK_INFO$WCET.MIN[[xID]]*modelUNIT, intercepts[[sprintf("T%d",xID)]], maximum=TRUE)
    # print(sprintf("xmin=%.4f, fmin=%.4f, niter=%d, estim.prec=%e", v$xmin, v$fmin, v$niter, v$estim.prec))
    ymax <- fx(v$xmin)
    area <- ymax * v$xmin
    # print(sprintf("Xmax=%.4f, Ymax=%.4f, Area=%.4f", v$xmin, ymax, area))
    rlist<-list(X=v$xmin, Y=ymax, Area=area)
    # names(rlist)<-c(sprintf("T%d",xID), sprintf("T%d", answerID), "Area")
    return (rlist)
}


