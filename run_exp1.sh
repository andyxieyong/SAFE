# All artifacts and R scripts should be executed in this folder.

# Examples of Phase 1
# -r: RUN_MAX in settings.json, the number of experiments
# --runID: RUN_NUM in settings.json, Identity of the experiments, if set runID, P1-StressTesting.jar do experiment only one time even if the RUN_MAX is greater than 1.
# -b: BASE_PATH in settings.json, Output path of the Phase 1
java -Xms4G -Xmx10G -jar artifacts/P1-StressTesting.jar -r 1 --runID 1 -b results/EXP1_GASearch

# Feature reduction and treating imbalanced data
# Feature reduction and treating imbalanced data
# 02_features.R <P1_result_path> <output_Path> <Number of experiments>
Rscript R/02_features.R results/EXP1_GASearch analysis/02_features 1
# 03_prune_input.R <P1_result_path> <output_Path> <Number of experiments>
Rscript R/03_prune_input.R results/EXP1_GASearch analysis/03_prune 1

# Examples of generating test data
# -w: WORKNAME in settings.json, the output forlder name which will be located in BASE_PATH
# -b: BASE_PATH in settings.json, Output path of the Phase 1
# -runID: RUN_NUM in settings.json, Identity of the experiments of Phase 1
# --exPoints: the number of points to generate for test
java -Xms4G -Xmx10G -jar artifacts/TestDataGenerator.jar -w testdata -b results/EXP1_GASearch --runID 1 --exPoints 50000

# Examples of Phase 2
# -w: WORKNAME in settings.json, the output forlder name which will be located in EXTEND_PATH (=<BASE_PATH>/refinements)
# -b: BASE_PATH in settings.json, Output path of the Phase 1
# -runID: RUN_NUM in settings.json, Identity of the experiments of Phase 1
# --secondRuntype: the sampling method [distance, random]
# --testData: the path of the test data that will be used to evaluate
java -Xms4G -Xmx10G -jar artifacts/P2-Refinements.jar -w Updates100 -b results/EXP1_GASearch --runID 1 --secondRuntype distance --testData testdata/testdata_run01
java -Xms4G -Xmx10G -jar artifacts/P2-Refinements.jar -w Updates100 -b results/EXP1_GASearch --runID 1 --secondRuntype random --testData testdata/testdata_run01


