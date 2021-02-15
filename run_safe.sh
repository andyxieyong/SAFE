# All artifacts and R scripts should be executed in this folder.

# Examples of Phase 1
# -r: RUN_MAX in settings.json, the number of experiments
# --runID: RUN_NUM in settings.json, Identity of the experiments, if set runID, P1-StressTesting.jar do experiment only one time even if the RUN_MAX is greater than 1.
# -b: BASE_PATH in settings.json, Output path of the Phase 1
java -Xms4G -Xmx10G -jar artifacts/P1-StressTesting.jar -r 1 --runID 1 -b results/SAFE_GASearch

# Feature reduction and treating imbalanced data
# 02_features.R <BASE_PATH of P1> <Output path of this script> <Number of runs>
Rscript R/02_features.R results/SAFE_GASearch analysis/02_features 1
# 03_prune_input.R <BASE_PATH of P1> <Output path of this script> <Number of runs>
Rscript R/03_prune_input.R results/SAFE_GASearch analysis/03_prune 1

# Examples of Phase 2
# -w: WORKNAME in settings.json, the output folder name which will be located in EXTEND_PATH (=<BASE_PATH>/refinements)
# -b: BASE_PATH in settings.json, Output path of the Phase 1
# -runID: RUN_NUM in settings.json, Identity of the experiments of Phase 1
# --secondRuntype: the sampling method [distance, random]
java -Xms4G -Xmx10G -jar artifacts/P2-Refinements.jar -w Updates100 -b results/SAFE_GASearch --runID 1 --secondRuntype distance


