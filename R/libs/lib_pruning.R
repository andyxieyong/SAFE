# pruning training data set for its balance
# Dependency: None

pruning <- function(data, side, intercepts, IDs){
    tnames <- sprintf("T%d",IDs)
    
    if (side=="negative"){
        pruned <- data
        removed <- data.frame()
        for (tname in tnames){
            condition <- intercepts[[tname]]    
            removed <- rbind(removed, pruned[pruned[[tname]]>condition,])
            pruned <- pruned[pruned[[tname]]<=condition,]
        }
        falses <- nrow(removed[removed$result==0,])
    }
    else{
        removed <- data
        pruned <- data.frame()
        for (tname in tnames){
            condition <- intercepts[[tname]]    
            pruned <- rbind(pruned, removed[removed[[tname]]>=condition,])
            removed <- removed[removed[[tname]]<condition,]
        }
        falses <- nrow(removed[removed$result==1,])
    }
    # print results
    p<- nrow(pruned[pruned$result==0,])/nrow(pruned)
    n<- nrow(pruned[pruned$result==1,])/nrow(pruned)
    cat(sprintf("positive: %.2f, negative: %.2f (error: %d)\n", p, n, falses))
    return(pruned)
}

