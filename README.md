# SAFE

SAFE (Safe WCET Analysis method For real-time task schEdulability) is a tool for Schedulability Analysis of Real-Time Systems with Uncertain Worst-Case Execution Times


### Overview
Schedulability analysis is about determining whether a given set of real-time software tasks are schedulable, i.e., whether task executions always complete before their specified deadlines. It is an important activity at early design and analysis stages of real-time safety critical systems. Schedulability analysis requires as input the worst-case execution times (WCET) for software tasks to be estimated using exact values. However, in practice, engineers of-ten cannot estimate WCET values precisely and prefer to provide a range of WCET values instead. Given a set of real-time tasks with uncertain WCET values, we provide an automated technique to determine for what WCET values the system is likely to meet its deadline constraints, and hence, operate safely. Our approach combines a search-based stress testing algorithm for generating worst case scheduling scenarios with a machine learning logistic regression model for inferring safe WCET ranges. We evaluated our approach by applying it to an industrial satellite system. Our approach efficiently computes WCET ranges within which the satellite system likely satisfies its deadline constraints with high confidence.


### Prerequisite
SAFE runs on the following operating systems:
- Centos Linux operating system, version 7
- MacOS 10.14.6

### SAFE requires the following tools:
- Java 1.8.0.162
- R 3.4.4 (required libraries: dplyr, MLmetrics, effsize, plotROC, progress, MASS, boot, stringr, ggplot2, randomForest, pracma, tidyverse, cubature, scalesa)


### Folder description
* *src* : Containing Java source code for phase1, phase2, and test case generator
* *R* : Containing R scripts to execute SAFE and generate graphs
* *res*: Containing the input task description
* *artifacts*: Containing Java executable files
* *settings.json*: Parameters for the Java executable files
* *run_*.sh*: Shell scripts for conducting each experiments above 


### How to run SAFE?
* Step 1: Run *run_safe.sh*
* Step 2: See output files in *results/SAFE_GASearch*


### How to run experiments?
Note: Due to randomness of SAFE, we repeat our experiments 50 times

##### =Sanity check=
* Step 1: Run *run_sanity_check.sh*
* Step 2: See output files in *results/Sanity_GASearch and PATH/results/Sanity_RandomSearch*

##### =EXP1=
* Step 1: Run *run_exp1.sh*
* Step 2: See output files in *results/EXP1_GASearch*

##### =EXP2=
* Step 1: Run *run_exp2.sh*
* Step 2: See output files in *results/EXP2_GASearch*

