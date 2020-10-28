# Library for drawing 
cat("loading lib_draw.R...\n")
if (Sys.getenv("RSTUDIO")==1){
    # Add library and dependency
    # source('conf.R')
    source("libs/lib_data.R")         # get_task_names
    source("libs/lib_model.R")        # get_intercepts, get_bestsize_point, get_func_points
    source("libs/lib_evaluate.R")     # find_noFPR
    source("libs/lib_sampling.R") 
}
library(scales)



get_WCETspace_plot<- function(
    data, form, 
    xID, yID, 
    showTraining=TRUE, 
    nSamples=0, 
    probLines=c(), 
    showThreshold=TRUE,
    xlabel=NULL,
    ylabel=NULL,
    title=NULL,
    annotates=c(),
    annotatesLoc=c(),
    showMessage = TRUE,
    showBestPoint = FALSE,
    learnModel=TRUE,
    nNewSamples=0,
    legend="rt",
    reduceRate=1
)
{
    
    # Setting basic frame
    if(showMessage) cat(sprintf("Generating WCET space with x(T%d), y(T%d)....\n", xID, yID))
    g <- ggplot() + 
        # xlim(0, 20) +
        # ylim(0, 0.9) +
        xlim(0, TASK_INFO$WCET.MAX[[xID]]*UNIT) +
        ylim(0, TASK_INFO$WCET.MAX[[yID]]*UNIT) +
        # xlab(sprintf("%s (T%d)",TASK_INFO$NAME[xID], xID)) +
        # ylab(sprintf("%s (T%d)",TASK_INFO$NAME[yID], yID)) +
        xlab(sprintf("T%d WCET", xID)) +
        ylab(sprintf("T%d WCET", yID)) +
        theme_bw() +
        theme(axis.text=element_text(size=15), axis.title=element_text(size=15))
    
    if(is.null(xlabel)==FALSE) g <- g + xlab(xlabel)
    if(is.null(ylabel)==FALSE) g <- g + ylab(ylabel)
    if (is.null(title)==FALSE) g <- g + ggtitle(title)
    
    if (legend=="rb"){
        g<- g+ theme(legend.justification=c(1,0), legend.position=c(0.999, 0.001),legend.text = element_text(size=15), legend.title=element_blank(), legend.background = element_rect(colour = "black", size=0.2))
    }else if (legend=="rt"){
        g<- g+ theme(legend.justification=c(1,1), legend.position=c(0.999, 0.999), legend.text = element_text(size=15), legend.title=element_blank(), legend.background = element_rect(colour = "black", size=0.2))
    } else if (legend=="lt"){
        g<- g+ theme(legend.justification=c(0,1), legend.position=c(0.001, 0.999), legend.text = element_text(size=15), legend.title=element_blank(), legend.background = element_rect(colour = "black", size=0.2))
    } else if (legend=="lb"){
        g<- g+ theme(legend.justification=c(0,0), legend.position=c(0.001, 0.001), legend.text = element_text(size=15), legend.title=element_blank(), legend.background = element_rect(colour = "black", size=0.2))
    } else{
        g<- g+ theme(legend.position = "none")
    }
    
    uData<-update_data(data, c("No deadline miss", "Deadline miss"))
    if (nNewSamples!=0){
        nCnt <- nrow(uData)
        Pt <- nCnt-nNewSamples + 100
        prev <- uData[1:Pt,]
        newP <- uData[(Pt+1):nCnt,]
        uData <- prev
    }
    
    if (showTraining==TRUE){
        if (reduceRate<1){
            showingData <- sample_n(uData, nrow(uData)*reduceRate)
        }else{
            showingData <- uData
        }
        g <- g + 
            geom_point( data=showingData, aes(x=showingData[[sprintf("T%d",xID)]], y=showingData[[sprintf("T%d",yID)]], color=as.factor(labels),shape=as.factor(labels)),  size=1, alpha=1) +
            # scale_colour_manual(values=c(cbPalette[2], cbPalette[1]) )
            scale_colour_manual(values=c("#00BFC4", "#F8766D") )+
            scale_shape_manual(values = c(1, 25)) # 1, 4
        
    }
    else if (nNewSamples!=0){
        if (showTraining==TRUE){
            g <- g + geom_point( data=prev, aes(x=prev[[sprintf("T%d",xID)]], y=prev[[sprintf("T%d",yID)]]), color='gray', size=1, alpha=0.5)
        }
        g <- g + 
            geom_point( data=newP, aes(x=newP[[sprintf("T%d",xID)]], y=newP[[sprintf("T%d",yID)]], color=as.factor(labels)),  size=1, alpha=0.5)+
            scale_colour_manual(values=cbPalette )
    }
    
    if (learnModel==FALSE){
        return (g)
    }
        
    # generate model & find threhold
    mdx <- glm(formula = form, family = "binomial", data = uData)
    uncertainIDs <- get_base_names(names(mdx$coefficients), isNum=TRUE)
    threshold <- find_noFPR(mdx, uData, precise=0.0001)
    # uppper_threshold <- find_noFNR(mdx, uData, precise=0.0001)
    
    # generate sample if user wants
    if (nSamples!=0){
        if(showMessage) cat(sprintf("\tAdding sampling points with %5.2f%% as a threhold ....\n",threshold*100))
        tnames <- get_task_names(uData)
        samples <- sample_based_euclid_distance(tnames, mdx, nSample=nSamples, nCandidate=20, P=threshold)
        g <- g + geom_point( data=samples, aes(x=samples[[sprintf("T%d",xID)]], y=samples[[sprintf("T%d",yID)]]),  size=0.3, alpha=0.5)
    }
    
    # Add probability lines
    if (showThreshold == TRUE) probLines <- c(probLines, threshold)#, uppper_threshold)
    for(prob in probLines){
        if(showMessage) cat(sprintf("\tAdding model line with %5.2f%% ....\n",prob*100))
        
        funcLine <- generate_line_function(mdx, prob, yID, minY=0, maxY=TASK_INFO$WCET.MAX[[yID]]*UNIT)
        # print(funcLine)
        fx <- get_func_points(funcLine, 0, TASK_INFO$WCET.MAX[[xID]]*UNIT, nPoints=300)
        # xfun = seq(0, TASK_INFO$WCET.MAX[[xID]]*UNIT)
        # yfun = sapply(xfun, funcLine)
        # fx = data.frame(x=xfun, y=yfun)
        intercepts<-get_intercepts(mdx, prob, uncertainIDs)

        # set threshold color and the others
        lineColor <- ifelse(threshold==prob, "blue", "black")
        
        if (nrow(fx)!=0){
            if(TASK_INFO$WCET.MAX[[yID]]>TASK_INFO$WCET.MAX[[xID]]){
                i<-nrow(fx)
                for(i in nrow(fx):1){
                    if (fx$x[[i]] <0) break
                }
                xpos <- 0
                ypos <- fx$y[[i]]
            }else{
                i<-1
                for(i in 1:nrow(fx)){
                    if (fx$y[[i]] <0) break
                }
                xpos <- fx$x[[i]]
                ypos <- 0
            }
            
            g<- g + 
                # stat_function(fun=funcLine, color="red", alpha=0.9, linetype="dashed")+
                geom_line(data=fx, aes(x=x, y=y), color=lineColor, alpha=0.9, size=1, linetype="dashed")+
                annotate("text", x=xpos, y=ypos, label = sprintf("P=%.2f%%", prob*100), color=lineColor, size=5, hjust=-0.1, vjust=0.1)
        } else {
            cat(sprintf("\tCannot draw a line with %.2f%% in specified area\n", prob*100))
        }
    }

    # Add Annotates
    for (i in 1:length(annotates)){
        xpos = 0
        ypos = TASK_INFO$WCET.MAX[yID]*UNIT - (i-1)*0.2
        if (length(annotatesLoc) > i){
            xpos = annotatesLoc[i][1]
            ypos = annotatesLoc[i][2]
        }
        g <- g + annotate("text", x = xpos, y = ypos, label = annotates[i], color="blue", size=3, hjust=0, vjust=-1)
    }
    
    if (showBestPoint==TRUE){
        # generate model & find threhold
        mdb <- glm(formula = form, family = "binomial", data = data)
        uncertainIDs <- get_base_names(names(mdb$coefficients), isNum=TRUE)
        bestPoint <- get_bestsize_point(mdb, threshold, targetIDs=uncertainIDs, isGeneral=TRUE)
        bestPoint$X <- bestPoint$X*UNIT
        bestPoint$Y <- bestPoint$Y*UNIT
        bestPoint$Area <- bestPoint$Area*UNIT
        print(bestPoint)
        if (uncertainIDs[1] == yID){
            temp <- bestPoint$X 
            bestPoint$X <- bestPoint$Y
            bestPoint$Y <- temp
        }
        
        bestBorder1 <- data.frame(X=c(0, bestPoint$X), Y=c(bestPoint$Y, bestPoint$Y))
        bestBorder2 <- data.frame(X=c(bestPoint$X, bestPoint$X), Y=c(bestPoint$Y, 0))
        bestPoint <- as.data.frame(bestPoint)
        g <- g+
            geom_rect( data=bestPoint, xmin=0, xmax=bestPoint$X, ymin=0, ymax=bestPoint$Y,
                       fill="green", alpha=0.15, inherit.aes = FALSE)+
            geom_line( data=bestBorder1, aes(x=X, y=Y), 
                       color="black", alpha=0.7, size=1, inherit.aes = FALSE, linetype="dotted")+
            geom_line( data=bestBorder2, aes(x=X, y=Y), 
                       color="black", alpha=0.7, size=1,  inherit.aes = FALSE, linetype="dotted")+
            geom_point( mapping=aes(x=X, y=Y), data=bestPoint, color="black", alpha=0.8, size=2)+
            geom_text( mapping=aes(x=X, y=Y, label="Best-size"), 
                       data=bestPoint, color="black", alpha=0.8, size=6, hjust=-0.1,vjust=-0.2)+
            geom_text( mapping=aes(x=0, y=Y, label=sprintf("%.3fs",bestPoint$Y)), 
                       data=bestPoint, color="black", alpha=0.8, size=5, hjust=-0.1,vjust=1.3)+
            geom_text( mapping=aes(x=X, y=0, label=sprintf("%.3fs",bestPoint$X)), 
                        data=bestPoint, color="black", alpha=0.8, size=5, hjust=1.1,vjust=-0.3)
    }
    
    if(showMessage) cat("Generated graph.\n")
    return (g)
}

generate_box_plot <- function(sample_points, x_col, y_col, x.title, y.title, nBox=20, 
                              title="", ylimit=NULL, colorList=NULL, legend="rb", limY=NULL,
                              legend_direct="vertical", legend_font=15){
    
    # Draw them for each
    avg_results<- aggregate(sample_points[[y_col]], list(Iter=sample_points[[x_col]], Type=sample_points$Type), mean)
    colnames(avg_results) <- c(x_col, "Type", y_col)
    
    # change for drawing
    maxX = max(sample_points[[x_col]])
    print(maxX)
    interval = as.integer(maxX/nBox)
    samples <- sample_points[(sample_points[[x_col]]%%interval==0),]
    avgs <- avg_results[(avg_results[[x_col]]%%interval==0),]
    
    if(is.null(colorList)==TRUE){
        colorList = cbPalette 
    }
    fmt_dcimals <- function(digits=0){
        # return a function responpsible for formatting the 
        # axis labels with a given number of decimals 
        function(x) sprintf("%.4f", round(x,digits))
    }
    g <- ggplot(data=samples, aes(x=as.factor(samples[[x_col]]), y=samples[[y_col]], color=as.factor(Type))) +  #, linetype=as.factor(Type)
        stat_boxplot(geom = "errorbar", width = 0.7, alpha=1, size=0.7) +
        stat_boxplot(geom = "boxplot", width = 0.7, alpha=1, size=0.7, outlier.shape=1, outlier.size=1) +
        geom_line( data=avgs, aes(x=as.factor(avgs[[x_col]]), y=avgs[[y_col]], color=as.factor(Type), group=as.factor(Type)), size=1, alpha=1)+
        theme_bw() +
        scale_colour_manual(values=colorList)+
        xlab(x.title) +
        ylab(y.title) +
        scale_y_continuous(labels = fmt_dcimals(digits=4)) + 
        theme(axis.text=element_text(size=legend_font), axis.title=element_text(size=15))#,face="bold"
    
    if (is.null(limY)==FALSE){
        g<- g + ylim(limY[1], limY[2])
    }
    
    if (legend=="rb"){
        g<- g+ theme(legend.justification=c(1,0), legend.position=c(0.999, 0.001), legend.direction = legend_direct, legend.title=element_blank(), legend.text = element_text(size=legend_font), legend.background = element_rect(colour = "black", size=0.2))
    }else if (legend=="rt"){
        g<- g+ theme(legend.justification=c(1,1), legend.position=c(0.999, 0.999), legend.direction = legend_direct, legend.title=element_blank(), legend.text = element_text(size=legend_font), legend.background = element_rect(colour = "black", size=0.2))
    } else if (legend=="lt"){
        g<- g+ theme(legend.justification=c(0,1), legend.position=c(0.001, 0.999), legend.direction = legend_direct, legend.title=element_blank(), legend.text = element_text(size=legend_font), legend.background = element_rect(colour = "black", size=0.2))
    } else if (legend=="lb"){
        g<- g+ theme(legend.justification=c(0,0), legend.position=c(0.001, 0.001), legend.direction = legend_direct, legend.title=element_blank(), legend.text = element_text(size=legend_font), legend.background = element_rect(colour = "black", size=0.2))
    } else{
        g<- g+ theme(legend.position = "none")
    }
    
    if (!is.null(ylimit)){
        g <- g + ylim(ylimit[[1]], ylimit[[2]])
    }    
    if (title!=""){
        g <- g + ggtitle(title)
    }
    return (g)
}


get_WCETspace_plot_with_previous<- function(
    data, form, 
    xID, yID, 
    showTraining=TRUE, 
    nSamples=0, 
    probLines=c(), 
    showThreshold=TRUE,
    xlabel=NULL,
    ylabel=NULL,
    title=NULL,
    annotates=c(),
    annotatesLoc=c(),
    showMessage = TRUE,
    showBestPoint = FALSE,
    learnModel=TRUE
)
{
    g <- ggplot() + 
        xlim(0, TASK_INFO$WCET.MAX[[xID]]*UNIT) +
        ylim(0, TASK_INFO$WCET.MAX[[yID]]*UNIT) +
        xlab(sprintf("%s (T%d)",TASK_INFO$NAME[xID], xID)) +
        ylab(sprintf("%s (T%d)",TASK_INFO$NAME[yID], yID))
    
    if(is.null(xlabel)==FALSE) g <- g + xlab(xlabel)
    if(is.null(ylabel)==FALSE) g <- g + ylab(ylabel)
    if (is.null(title)==FALSE) g <- g + ggtitle(title)
    
    uData<-update_data(data, c("Positive", "Negative"))
    nCnt <- nrow(uData)
    previous <- uData[1:(nCnt-nSamples),]
    newSamples <- uData[(nCnt-nSamples+1):nCnt,]
    
    if (showTraining==TRUE){
        g <- g + 
            geom_point( data=previous, aes(x=uData[[sprintf("T%d",xID)]], y=uData[[sprintf("T%d",yID)]]), color='gray', size=0.3, alpha=0.5)+
            geom_point( data=newSamples, aes(x=uData[[sprintf("T%d",xID)]], y=uData[[sprintf("T%d",yID)]], color=as.factor(labels)),  size=0.3, alpha=0.5)+
            scale_colour_manual(values=cbPalette )+
            theme(legend.justification=c(1,1), legend.position=c(1, 1), legend.title=element_blank(), plot.title=element_text(hjust = 0.5))
    }
    
    if (learnModel==FALSE){
        return (g)
    }
    
    # generate model & find threhold
    mdx <- glm(formula = form, family = "binomial", data = uData)
    uncertainIDs <- get_base_names(names(mdx$coefficients), isNum=TRUE)
    threshold <- find_noFPR(mdx, uData, precise=0.0001)
    
    # generate sample if user wants
    if (nSamples!=0){
        if(showMessage) cat(sprintf("\tAdding sampling points with %5.2f%% as a threhold ....\n",threshold))
        tnames <- get_task_names(uData)
        samples <- sample_based_euclid_distance(tnames, mdx, nSample=nSamples, nCandidate=20, P=threshold)
        g <- g + geom_point( data=samples, aes(x=samples[[sprintf("T%d",xID)]], y=samples[[sprintf("T%d",yID)]]),  size=0.3, alpha=0.5)
    }
    
    # Add probability lines
    if (showThreshold == TRUE) probLines <- c(probLines, threshold)
    for(prob in probLines){
        if(showMessage) cat(sprintf("\tAdding model line with %5.2f%% ....\n",prob*100))
        
        funcLine <- generate_line_function(mdx, prob, yID, minY=0, maxY=TASK_INFO$WCET.MAX[[yID]]*UNIT)
        fx <- get_func_points(funcLine, 0, TASK_INFO$WCET.MAX[[xID]]*UNIT, nPoints=100)
        intercepts<-get_intercepts(mdx, prob, uncertainIDs)
        
        # set threshold color and the others
        lineColor <- ifelse(threshold==prob, "blue", "black")
        
        if(TASK_INFO$WCET.MAX[[yID]]>TASK_INFO$WCET.MAX[[xID]]){
            xpos <- intercepts[[sprintf("T%d",xID)]]
            ypos <- 0
            if (is.nan(xpos)==TRUE){
                xpos <- 0
                cat("\t\txpos changed to 0\n")
            }
        }else{
            xpos <- 0
            ypos <- intercepts[[sprintf("T%d",yID)]]
            if (is.nan(ypos)==TRUE){
                ypos <- 0
                cat("\t\typos changed to 0\n")
            }
        }
        if (nrow(fx)!=0){
            g<- g + 
                # stat_function(fun=funcLine, color="red", alpha=0.9, linetype="dashed")+
                geom_line(data=fx, aes(x=x, y=y), color=lineColor, alpha=0.9, linetype="dashed")+
                annotate("text", x = xpos, y = ypos, label = sprintf("P=%.2f%%", prob*100), color=lineColor, size=3, hjust=0, vjust=0.1)
        } else {
            cat(sprintf("\tCannot draw a line with %.2f%% in specified area\n", prob*100))
        }
    }
    
    # Add Annotates
    for (i in 1:length(annotates)){
        xpos = 0
        ypos = TASK_INFO$WCET.MAX[yID]*UNIT - (i-1)*0.2
        if (length(annotatesLoc) > i){
            xpos = annotatesLoc[i][1]
            ypos = annotatesLoc[i][2]
        }
        g <- g + annotate("text", x = xpos, y = ypos, label = annotates[i], color="blue", size=3, hjust=0, vjust=-1)
    }
    
    if (showBestPoint==TRUE){
        
        bestPoint <- get_bestsize_point(mdx, threshold, targetIDs=c(xID,yID), isGeneral=TRUE)
        bestBorder1 <- data.frame(X=c(0, bestPoint$X), Y=c(bestPoint$Y, bestPoint$Y))
        bestBorder2 <- data.frame(X=c(bestPoint$X, bestPoint$X), Y=c(bestPoint$Y, 0))
        bestPoint <- as.data.frame(bestPoint)
        g <- g+
            geom_rect( data=bestPoint, xmin=0, xmax=bestPoint$X, ymin=0, ymax=bestPoint$Y,
                       fill="green", alpha=0.15, inherit.aes = FALSE)+
            geom_line( data=bestBorder1, aes(x=X, y=Y), 
                       color="black", alpha=0.7, inherit.aes = FALSE, linetype="dotted")+
            geom_line( data=bestBorder2, aes(x=X, y=Y), 
                       color="black", alpha=0.7, inherit.aes = FALSE, linetype="dotted")+
            geom_point( mapping=aes(x=X, y=Y), data=bestPoint, color="black", alpha=0.8, size=2)+
            geom_text( mapping=aes(x=X, y=Y, label="best-size"), 
                       data=bestPoint, color="black", alpha=0.8, size=4, hjust=-0.1,vjust=-0.2)+
            geom_text( mapping=aes(x=0, y=Y, label=sprintf("%.3fs",bestPoint$Y)), 
                       data=bestPoint, color="black", alpha=0.8, size=3, hjust=-0.1,vjust=1.3)+
            geom_text( mapping=aes(x=X, y=0, label=sprintf("%.3fs",bestPoint$X)), 
                       data=bestPoint, color="black", alpha=0.8, size=3, hjust=1.1,vjust=-0.3)
    }
    
    if(showMessage) cat("Generated graph.\n")
    return (g)
}

