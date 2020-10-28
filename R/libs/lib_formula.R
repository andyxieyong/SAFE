# related formula
# DEPENDENCY
#   - lib_data: get_task_names
cat("loading lib_formula.R...\n")
if (Sys.getenv("RSTUDIO")==1){
    source('libs/lib_data.R')  # get_task_names
}

########################################################
# Generate formula with data which starts result
########################################################
get_formula<- function(data){
    # change time unit
    terms <- get_task_names(data)
    
    formula<- ""
    for (term in terms){
        if (formula == "")
            formula <- sprintf("result ~ %s",term)
        else
            formula <- sprintf("%s + %s",formula, term)
    }
    
    # Add quadratic terms
    for (term in terms){
        formula <- sprintf("%s + I(%s^2)",formula, term)
    }
    
    for(x1 in 1:(length(terms)-1)){
        for(x2 in (x1+1):length(terms)){
            formula <- sprintf("%s + %s:%s",formula, terms[x1], terms[x2])
        }
    }
    
    return (formula)
}

get_formula_simple<- function(data){
    ## linear terms + quadratic 3 terms
    terms <- get_task_names(data)
    
    formula<- ""
    for (term in terms){
        if (formula == "")
            formula <- sprintf("result ~ %s",term, term)
        else
            formula <- sprintf("%s + %s",formula, term, term)
    }
    formula <- sprintf("%s + I(T23^2) + I(T30^2) + I(T33^2)",formula)
    return (formula)
}

get_formula_linear_quad<- function(data){
    ## linear terms + quadratic 3 terms
    terms <- get_task_names(data)
    
    formula<- ""
    for (term in terms){
        if (formula == "")
            formula <- sprintf("result ~ %s",term, term)
        else
            formula <- sprintf("%s + %s",formula, term, term)
    }
    # Add quadratic terms
    for (term in terms){
        formula <- sprintf("%s + I(%s^2)",formula, term)
    }
    return (formula)
}

get_formula_simple_inter<- function(data){
    ## linear terms + quadratic 3 terms + interactions
    terms <- get_task_names(data)
    
    formula<- ""
    for (term in terms){
        if (formula == "")
            formula <- sprintf("result ~ %s",term, term)
        else
            formula <- sprintf("%s + %s",formula, term, term)
    }
    formula <- sprintf("%s + I(T23^2) + I(T30^2) + I(T33^2)",formula)
    
    #interactions
    for(x1 in 1:(length(terms)-1)){
        for(x2 in (x1+1):length(terms)){
            formula <- sprintf("%s + %s:%s",formula, terms[x1], terms[x2])
        }
    }
    return (formula)
}

get_formula_simple_quadratic<- function(data){
    # change time unit
    terms <- get_task_names(data)
    
    formula<- ""
    for (term in terms){
        if (formula == "")
            formula <- sprintf("result ~ %s",term)
        else
            formula <- sprintf("%s + %s",formula, term)
    }
    
    # Add quadratic terms
    for (term in terms){
        formula <- sprintf("%s + I(%s^2)",formula, term)
    }
    
    return (formula)
}

################
# alternative stepwise
get_significant_terms_formula<- function(model){
    
    md.coeffex <- summary(model)$coefficients    # NA values doesn't have coefficients.
    significants <- md.coeffex[,4]
    rows <- rownames(md.coeffex)
    
    # generate formula
    new_formula <- ""
    for(r in 1:length(rows)){
        if (significants[r] > 0.1) next
        if (new_formula=="")
            new_formula <- sprintf("result ~ %s", rows[r])
        else
            new_formula <- sprintf("%s + %s", new_formula, rows[r])
        # print(sprintf("%s: %.4f", rows[r], significants[r]))
    }
    return (new_formula)
}


###########
# generate formula with selected terms
###########
get_formula_complex<- function(y, terms){
    formula<- ""
    # Add linear terms
    for (term in terms){
        if (formula == "")
            formula <- sprintf("%s ~ %s",y, term)
        else
            formula <- sprintf("%s + %s",formula, term, term)
    }
    # Add quadratic terms
    for (term in terms){formula <- sprintf("%s + I(%s^2)",formula, term)}
    # Add interaction terms
    if (length(terms) > 1){
        for(x1 in 1:(length(terms)-1)){
            for(x2 in (x1+1):length(terms)){
                formula <- sprintf("%s + %s:%s",formula, terms[x1], terms[x2])
            }
        }
    }
    return (formula)
}

get_formula_linear<- function(y, terms){
    formula<- ""
    # Add linear terms
    for (term in terms){
        if (formula == "")
            formula <- sprintf("%s ~ %s", y, term)
        else
            formula <- sprintf("%s + %s",formula, term, term)
    }
    return (formula)
}


#############################################
# formula term reducing functions
#############################################
#************************************************
# return task names from the formula terms
#   - this is for the saving model information to file
get_raw_names<- function(names){
    for (x in 1:length(names)){
        if (startsWith(names[[x]], "I")==TRUE){
            names[[x]] <- substring(names[[x]], 3, nchar(names[[x]])-1)
        }
        
        if (startsWith(names[[x]], "(")==TRUE){
            names[[x]] <- substring(names[[x]], 2, nchar(names[[x]])-1)
        }
    }
    return(names)
}

#************************************************
# get unique task ID from list of terms of formula from model
#   - isNum : if it is TRUE, it returns number
get_base_names<- function(names, isNum=FALSE){
    new_names <- c()
    for (x in 1:length(names)){
        # for quadratics
        if (startsWith(names[[x]], "I")==TRUE){
            item <- substring(names[[x]], 3, nchar(names[[x]])-3)
            if (is.na(match(item, new_names))==TRUE){
                new_names <- c(new_names, item)
            }
            next
        }
        # for intercept
        if (startsWith(names[[x]], "(")==TRUE) next
        
        # for interections
        pos = regexpr(':', names[[x]])
        if (pos>0){
            item = substring(names[[x]], 1, pos-1)
            if (is.na(match(item, new_names))==TRUE){
                new_names <- c(new_names, item)
            }
            item = substring(names[[x]], pos+1)
            if (is.na(match(item, new_names))==TRUE){
                new_names <- c(new_names, item)
            }
        }
        else{
            if (is.na(match(names[[x]], new_names))==TRUE){
                new_names <- c(new_names, names[[x]])
            }
        }
    }
    if (isNum==TRUE){
        for (x in 1:length(new_names)){
            if(startsWith(new_names[[x]], "T")==TRUE){
                new_names[[x]] <- substring(new_names[[x]], 2)
            }
        }
        new_names<- strtoi(new_names)
        new_names<- sort(new_names)
    }
    return(new_names)
}

# get unique task ID from list of terms of formula
#   - isNum : if it is TRUE, it returns number
get_base_names_formula <- function(formula.text, isNum=FALSE){
    ridx <- regexpr("~", formula.text)
    right <- substring(formula.text, ridx+1)
    terms <- strsplit(right, "\\+")[[1]]
    terms <- trimws(terms)
    return (get_base_names(terms, isNum))
}

