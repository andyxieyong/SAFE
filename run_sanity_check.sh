# All artifacts and R scripts should be executed in this folder.

# Examples of Phase 1
# -r: RUN_MAX in settings.json, the number of experiments
# --runID: RUN_NUM in settings.json, Identity of the experiments, if set runID, P1-StressTesting.jar do experiment only one time even if the RUN_MAX is greater than 1.
# -b: BASE_PATH in settings.json, Output path of the Phase 1
# --simpleSearch: SIMPLE_SEARCH in settings.json, if this parameter is set, the program do Random Search
java -Xms4G -Xmx10G -jar artifacts/P1-StressTesting.jar -r 1 --runID 1 -b results/Sanity_GASearch
java -Xms4G -Xmx10G -jar artifacts/P1-StressTesting.jar -r 1 --runID 1 -b results/Sanity_RandomSearch --simpleSearch
