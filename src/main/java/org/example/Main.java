String result;
List<Function> functions = new ArrayList<>();
String strClass = null;
int numOfBranches = 0;
Random random = new Random(1);
String rumName;
List<String> constructorArgsTypes;
File dir = new File("src/main/java");
Map<String, Integer> allBranchesInFunctions = new HashMap<>();
Map<String, Set<Integer>> branchesHitByFunction = new HashMap<>();
Set<Integer> allDiscoveredBranches = new HashSet<>();
//MUST MAKE SURE THE OTHER CLASS ISNT CALLED RUN TEST and not public and instanciate first
//make so it only needs to complie once as inefficient

void main() throws IOException, InterruptedException {
    InputStreamReader inputStreamReader = new InputStreamReader(System.in);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    System.out.print("Enter java class file path: ");
    String file = bufferedReader.readLine();
    while (!readFile(file)) {
        System.out.println("Invalid file path");
        System.out.print("Enter java function file path: ");
        file = bufferedReader.readLine();
    }
    compileProgram(result);
    for (Function f : functions) {
        branchesHitByFunction.put(f.functionName, new HashSet<>());
    }
    int maxFunctions = 8;
    int maxTests = 10;
    int runs = 30;
    for (int i = 0; i < runs; i++) {
        System.out.println("\nRun: " + (i + 1) + "/" + runs);
        random = new Random(i);
        resetRunState();
        long start = System.currentTimeMillis();
        Tests best = evolutionaryAlgorithm(50, 102, 10, 100, 40, 1, 0.1, 0.01, maxFunctions, maxTests, 20, 0.5);
        System.out.println("Evolutionary branches: " + best.allTestBranches + " Time: " + (System.currentTimeMillis() - start));
        random = new Random(i);
        resetRunState();
        start = System.currentTimeMillis();
        Tests bestRand = genFullyRandomTests(maxTests, maxFunctions);
        for (int j = 1; j <= 250; j++) {
            Tests rand = genFullyRandomTests(maxTests, maxFunctions);
            if (rand.allTestBranches > bestRand.allTestBranches){
                bestRand = rand;
            }
        }
        System.out.println("Random branches: " + bestRand.allTestBranches + " Time: " + (System.currentTimeMillis() - start));
        Files.writeString(Path.of("evo" + (i + 1) + ".txt"), printResults(best));
        Files.writeString(Path.of("rand" + (i + 1) + ".txt"), printResults(bestRand));
    }
}

private void resetRunState() {
    allDiscoveredBranches.clear();
    for (String string : branchesHitByFunction.keySet()) {
        branchesHitByFunction.get(string).clear();
    }
}
private Tests genFullyRandomTests(int numTests, int numFunctions) throws IOException {
    List<Test> testList = new ArrayList<>();
    int randTests = random.nextInt(numTests) + 1;
    for (int i = 0; i < randTests; i++) {
        int randFuncs = random.nextInt(numFunctions) + 1;
        TestFunction[] testFunctions = new TestFunction[randFuncs];
        for (int j = 0; j < randFuncs; j++) {
            int index = random.nextInt(functions.size());
            Function randomFunction = functions.get(index);
            TestFunction testFunction = new TestFunction(randomFunction.functionName,randomFunction.types,index);
            for (int k = 0; k < testFunction.types.size(); k++) {
                testFunction.mutateArgsRandomly(k);
            }
            testFunctions[j] = testFunction;
        }
        testList.add(new Test(testFunctions));
    }
    Tests tests = new Tests(testList);
    runTests(new Tests[]{tests});
    tests.calculateNumOfBranches();
    tests.calculateNumOfFunctions();
    return tests;
}

private void removeUselessFunctions(Tests best) throws IOException {
    runTests(new Tests[]{best});
    best.calculateNumOfBranches();
    int branches = best.allTestBranches;
    for(int i = 0; i < best.tests.size(); i++) {
        Test test = best.tests.get(i);
        TestFunction[] snapshot = test.testFunctions.clone();
        for (TestFunction testFunction : snapshot) {
            if (testFunction == null){
                continue;
            }
            int removeIndex = removeTestFunction(test, testFunction);
            int beforeNumBranches = best.allTestBranches;
            runTests(new Tests[]{best});
            best.calculateNumOfBranches();
            if (branches > best.allTestBranches) {
                addTestFunction(test, testFunction, removeIndex);
                best.allTestBranches = beforeNumBranches;
            } else {
                branches = best.allTestBranches;
            }
        }
    }
}
private void repairMissingBranches(Tests best) throws IOException {
    runTests(new Tests[]{best});
    best.calculateNumOfBranches();
    Set<Integer> covered = new HashSet<>();
    for (Test test : best.tests) {
        if (test.branches != null){
            covered.addAll(test.branches);
        }
    }
    Set<Integer> missing = new HashSet<>(allDiscoveredBranches);
    missing.removeAll(covered);
    if (!missing.isEmpty()){
        for (String name : branchesHitByFunction.keySet()) {
            Set<Integer> branches = branchesHitByFunction.get(name);
            boolean miss = false;
            for (Integer branch : branches) {
                if (missing.contains(branch)) {
                    miss = true;
                    break;
                }
            }
            if (!miss){
                continue;
            }
            Function goal = null;
            int targetIndex = -1;
            for (int i = 0; i < functions.size(); i++) {
                if (functions.get(i).functionName.equals(name)) {
                    goal = functions.get(i);
                    targetIndex = i;
                    break;
                }
            }
            if (goal == null){
                continue;
            }
            for (int j = 0; j < 200; j++) {
                TestFunction testFunction = new TestFunction(goal.functionName, goal.types, targetIndex);
                for (int i = 0; i < testFunction.args.length; i++){
                    testFunction.mutateArgs(i);
                }
                Test newTest = new Test(new TestFunction[]{testFunction});
                Tests findTest = new Tests(new ArrayList<>(List.of(newTest)));
                runTests(new Tests[]{findTest});
                findTest.calculateNumOfBranches();
                if (findTest.allTestBranches > 0) {
                    boolean isNewBranch = false;
                    for (Integer branch : newTest.branches) {
                        if (missing.contains(branch)) {
                            isNewBranch = true;
                            break;
                        }
                    }
                    if (isNewBranch) {
                        best.tests.add(newTest);
                        missing.removeAll(newTest.branches);
                        break;
                    }
                }
            }
            if (missing.isEmpty()){
                break;
            }
        }
        best.calculateNumOfBranches();
    }
}
private void addTestFunction(Test test, TestFunction addFunction, int addIndex) {
    TestFunction[] functionArr = new TestFunction[test.testFunctions.length + 1];
    int i = 0;
    for (TestFunction testFunction : test.testFunctions) {
        if (i == addIndex) {
            functionArr[i++] = addFunction;
        }
        if (testFunction != null) {
            functionArr[i++] = testFunction;
        }
    }
    if (i == addIndex) {
        functionArr[i] = addFunction;
    }
    test.testFunctions = functionArr;
}
private int removeTestFunction(Test test, TestFunction removeFunction) {
    TestFunction[] functionArr = new TestFunction[test.testFunctions.length-1];
    int index=0;
    int removeIndex = -1;
    for (int i = 0; i < test.testFunctions.length; i++) {
        if (test.testFunctions[i] == removeFunction) {
            removeIndex = i;
            break;
        }
    }
    if (removeIndex != -1){
        for (int i = 0; i < test.testFunctions.length; i++) {
            if (i != removeIndex) {
                functionArr[index++] = test.testFunctions[i];
            }
        }
        test.testFunctions = functionArr;
    }
    return removeIndex;
}
private String printResults(Tests tests) {
    StringBuilder sb = new StringBuilder();
    int k = 1;
    for (Test test : tests.tests) {
        sb.append("\nTest:").append(k).append("\n");
        k++;
        boolean first = true;
        sb.append(strClass).append(" test = new ").append(strClass).append("(");
        if (test.constructor != null) {
            for (int i = 0; i < test.constructor.args.length; i++) {
                String arg = test.constructor.args[i].toString();
                if (!first) sb.append(", ");
                String type = (!constructorArgsTypes.isEmpty() && constructorArgsTypes.get(i) != null) ? constructorArgsTypes.get(i) : "";
                if (type.equals("String")) sb.append("\"").append(arg).append("\"");
                else if (type.equals("char")) sb.append("'").append(arg).append("'");
                else sb.append(arg);
                first = false;
            }
        }
        sb.append(");\n");
        for (int i = 0; i < test.testFunctions.length; i++) {
            TestFunction testFunction = test.testFunctions[i];
            if (testFunction != null) {
                sb.append("test.").append(testFunction.functionName).append("(");
                first = true;
                for (int j = 0; j < testFunction.args.length; j++) {
                    String arg = testFunction.args[j] != null ? testFunction.args[j].toString() : "null";
                    if (!first) sb.append(", ");
                    String type = (!testFunction.types.isEmpty() && testFunction.types.get(j) != null) ? testFunction.types.get(j) : "";
                    if (type.equals("String") && !arg.equals("null")) sb.append("\"").append(arg).append("\"");
                    else if (type.equals("String")) sb.append("null");
                    else if (type.equals("char")) sb.append("'").append(arg).append("'");
                    else sb.append(arg);
                    first = false;
                }
                sb.append(");\n");
            }
        }
    }
    return sb.toString();
}
private Tests evolutionaryAlgorithm(int mu, int lamda, int eletism, int iterations, int numOfMutations, double argMutationChance,double funcMutationChance, double testMutationChance, int maxFuncInTest, int maxTests, int k, double crossoverChance) throws IOException {
    if(lamda%2 != 0){
        lamda--;
    }
    if (mu > lamda){
        mu = lamda;
    }
    if (eletism > mu){
        eletism = mu - 1;
    }
    //gen some random tests
    Tests[] tests = genRandomTests(mu, maxTests, maxFuncInTest);
    rankTests(tests);
    Tests bestTest = new Tests(new ArrayList<>(tests[0].tests));
    for (int i = 0; i < iterations; i++) {
        //System.out.println("Iteration " + i);
        rankTests(tests);
        //gen lamda children by crossover and mutations
        Tests[] lamdaArr = new  Tests[lamda];
        for (int j = 0; j < lamda; j += 2) {
            Tests[] children = generateChild(tests, k, crossoverChance, numOfMutations, argMutationChance, funcMutationChance, testMutationChance, maxTests, maxFuncInTest);
            lamdaArr[j] = children[0];
            lamdaArr[j+1] = children[1];
        }
        //rank the children
        runTests(lamdaArr);
        for (int j = 0; j < lamda; j++) {
            lamdaArr[j].calculateNumOfBranches();
            lamdaArr[j].calculateNumOfFunctions();
        }
        rankTests(lamdaArr);
        //the next gen will be eletism number of parents and the best children
        Tests[] newGeneration = new Tests[mu];
        for (int j = 0; j < eletism; j++) {
            newGeneration[j] = tests[j];
        }
        int index = 0;
        for (int j = eletism; j < mu; j++) {
            newGeneration[j] = lamdaArr[index++];
        }
        tests = newGeneration;
        if (tests[0].allTestBranches > bestTest.allTestBranches) {
            List<Test> clone = new ArrayList<>();
            for (Test test : tests[0].tests) {
                clone.add(new Test(test));
            }
            bestTest = new Tests(clone);
            bestTest.allTestBranches = tests[0].allTestBranches;
            bestTest.numOfFunctions = tests[0].numOfFunctions;
        }
    }
    repairMissingBranches(bestTest);
    removeUselessFunctions(bestTest);
    //removeEmptyTests(bestTest);

    return bestTest;
}
private void removeEmptyTests(Tests tests) {
    for (int i = 0; i < tests.tests.size(); i++) {
        Test test = tests.tests.get(i);
        if (test.testFunctions.length == 0){
            tests.tests.remove(test);
        }
    }
}
private void runTests(Tests[] testsArr) throws IOException {
    //create 2 files 1 for the new program to get the info from and one for this program to receive
    File in = File.createTempFile("input", ".txt", dir);
    File out = File.createTempFile("output", ".txt", dir);
    try {
        StringBuilder functionID = new StringBuilder();
        List<String> args = new ArrayList<>();
        for (int i = 0; i < testsArr.length; i++) {
            Tests tests = testsArr[i];
            if (tests != null && tests.tests != null){
                //-2 to signify that it is the next group of tests
                if (i > 0){
                    functionID.append("-2 ");
                }
                for (int j = 0; j < tests.tests.size(); j++) {
                    Test test = tests.tests.get(j);
                    if (test != null && test.testFunctions != null){
                        //-1 to signify it is the next test
                        if (j > 0) {
                            functionID.append("-1 ");
                        }
                        //for the constructor it is -3
                        functionID.append("-3 ");
                        if (test.constructor != null) {
                            for (Object arg : test.constructor.args) {
                                if (arg != null) {
                                    args.add(arg.toString());
                                }
                                else {
                                    args.add("null");
                                }
                            }
                        }
                        functionID.append(funcToIndex(test.testFunctions)).append(" ");
                        for (TestFunction function : test.testFunctions) {
                            if (function != null && function.args != null) {
                                for (Object arg : function.args) {
                                    if (arg == null) {
                                        args.add("null");
                                    }
                                    else{
                                        args.add( arg.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //writes the list of all funcs and seperators and then there respected args on every new line
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(in))) {
            bufferedWriter.write(functionID.toString().trim());
            bufferedWriter.newLine();
            for (String arg : args) {
                bufferedWriter.write(arg);
                bufferedWriter.newLine();
            }
        }
        //run the command java RunTest file_path
        ProcessBuilder processBuilder = new ProcessBuilder("java", rumName, in.getAbsolutePath());
        processBuilder.directory(dir);
        processBuilder.redirectOutput(out);
        Process process = processBuilder.start();
        boolean finished = process.waitFor(200, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        int testsID = 0;
        int testIndex = 0;
        Set<Integer> branches = new HashSet<>();
        //read the output from running the program and assign results to the tests
        try (BufferedReader reader = new BufferedReader(new FileReader(out))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()){
                    int value = Integer.parseInt(trimmed);
                    if (value == -2) {
                        assignBranches(testsArr, testsID, testIndex, branches);
                        branches.clear();
                        testsID++;
                        testIndex = 0;
                    } else if (value == -1) {
                        assignBranches(testsArr, testsID, testIndex, branches);
                        branches.clear();
                        testIndex++;
                    } else {
                        branches.add(value);
                    }
                }
            }
        }
        assignBranches(testsArr, testsID, testIndex, branches);
        //updates variables to check what branches have been hit and discovered and by what functions
        for (Tests tests : testsArr) {
            if (tests != null) {
                for (Test test : tests.tests) {
                    if (test != null && test.testFunctions != null && test.branches != null) {
                        for (TestFunction testFunction : test.testFunctions) {
                            if (testFunction != null && branchesHitByFunction.get(testFunction.functionName) != null){
                                branchesHitByFunction.get(testFunction.functionName).addAll(test.branches);
                            }
                        }
                        allDiscoveredBranches.addAll(test.branches);

                    }
                }
            }
        }
    } catch (Exception e) {
        System.out.println(e.getMessage());
    }
    finally {
        in.delete();
        out.delete();
    }
}
private void assignBranches(Tests[] testsArr, int testsID, int testIndex, Set<Integer> branches) {
    if (testsID < testsArr.length) {
        Tests tests = testsArr[testsID];
        if (tests != null && tests.tests != null && testIndex < tests.tests.size()) {
            Test test = tests.tests.get(testIndex);
            if (test != null){
                test.branches = new ArrayList<>(branches);
                test.numOfBranches = branches.size();
            }
        }
    }
}
private void rankTests(Tests[] tests) {
    for (int i = 0; i < tests.length; i++) {
        boolean swapped = false;
        for (int j = 0; j < tests.length - 1; j++) {
            Tests tests1 = tests[j];
            Tests tests2 = tests[j+1];
            if (tests1.allTestBranches < tests2.allTestBranches) {
                Tests temp = tests[j];
                tests[j] = tests[j+1];
                tests[j+1] = temp;
                swapped = true;
            }
            else if (tests1.allTestBranches == tests2.allTestBranches && calcTestsScore(tests1) < calcTestsScore(tests2)) {
                Tests temp = tests[j];
                tests[j] = tests[j+1];
                tests[j+1] = temp;
                swapped = true;
            }
        }
        if (!swapped) break;
    }
}
private double calcTestsScore(Tests tests) {
    Map<String, Integer> numOfFunctionsInTests = new HashMap<>();
    double score = 0;
    // counts the number of functions in all tests
    for (Test test : tests.tests) {
        if (test.testFunctions != null){
            for (TestFunction testFunction : test.testFunctions) {
                if (testFunction != null) {
                    if (numOfFunctionsInTests.containsKey(testFunction.functionName)) {
                        numOfFunctionsInTests.put(testFunction.functionName, numOfFunctionsInTests.get(testFunction.functionName) + 1);
                    } else {
                        numOfFunctionsInTests.put(testFunction.functionName, 1);
                    }
                }
            }
        }
    }
    // score based on call count and branch potential
    for (String functionName : numOfFunctionsInTests.keySet()) {
        int numberOfFunctions = numOfFunctionsInTests.get(functionName);
        int allBranchesInFunction = 0;
        if (allBranchesInFunctions.containsKey(functionName)){
            allBranchesInFunction = allBranchesInFunctions.get(functionName);
        }
        int foundBranches = 0;
        if (branchesHitByFunction.containsKey(functionName)) {
            foundBranches = branchesHitByFunction.get(functionName).size();
        }
        //reduce score for functions with no branches
        if (allBranchesInFunction == 0) {
            score -= 6 * numberOfFunctions;
            continue;
        }
        //increase score for the number of undiscovered branches in that function
        score += 80 * (allBranchesInFunction - foundBranches);
        //encourage calling a variety of functions instead of the same ones
        // KEEP VARITEY SCORE LOWER THAN NO BRANCH INCREASE
        score -= 3 * (numberOfFunctions);
    }
    //try make sure if a new branch is found it is kept
    for (Test test : tests.tests) {
        if (test.branches != null){
            for (Integer branch : test.branches) {
                if (!allDiscoveredBranches.contains(branch)){
                    score += 100;
                }
                score += 5;
            }
        }
    }
    score += numOfFunctionsInTests.size();
    return score;
}

private Tests[] generateChild(Tests[] tests, int k, double crossoverChance, int numOfMutations, double argMutationChance, double funcMutationChance, double testMutationChance,int maxTests, int maxFuncInTest) {
    //tournement selection
    Tests parent1 = tournementSelection(tests, k);
    Tests parent2 = tournementSelection(tests, k);
    List<List<Test>> children;
    //crossover on either the functions or tests
    if (random.nextDouble() < crossoverChance) {
        children = crossoverFunctions(parent1, parent2, maxFuncInTest);
    }
    else {
        children = crossoverTests(parent1, parent2, maxFuncInTest);
    }
    Tests child1 = new Tests(new ArrayList<>(children.get(0).subList(0, Math.min(maxTests, children.get(0).size()))));
    Tests child2 = new Tests(new ArrayList<>(children.get(1).subList(0, Math.min(maxTests, children.get(1).size()))));
    //mutate either the args function or test x many times
    for (int i = 0; i < numOfMutations; i++) {
        if (!child1.tests.isEmpty()) {
            Test randomTest = child1.tests.get(random.nextInt(child1.tests.size()));
            if (randomTest != null && randomTest.testFunctions != null && randomTest.testFunctions.length > 0) {
                if(random.nextDouble() < argMutationChance){
                    randomTest.mutateArgs();
                }
                if(random.nextDouble() < funcMutationChance){
                    randomTest.mutateFunctions(functions, maxFuncInTest);
                }
            }
        }
        if (!child2.tests.isEmpty()) {
            Test randomTest = child2.tests.get(random.nextInt(child2.tests.size()));
            if (randomTest != null && randomTest.testFunctions != null && randomTest.testFunctions.length > 0) {
                if(random.nextDouble() < argMutationChance){
                    randomTest.mutateArgs();
                }
                if(random.nextDouble() < funcMutationChance){
                    randomTest.mutateFunctions(functions, maxFuncInTest);
                }
            }
        }
        if(random.nextDouble() < testMutationChance) {
            child1.mutateTests(maxTests);
            child2.mutateTests(maxTests);
        }
    }
    //do some repairing if the child gets a bit unlucky
    for (int i = 0; i < child1.tests.size(); i++) {
        Test test = child1.tests.get(i);
        if (test == null || test.testFunctions == null || test.testFunctions.length == 0) {
            child1.tests.remove(i);
        }
    }
    for (int i = 0; i < child2.tests.size(); i++) {
        Test test = child2.tests.get(i);
        if (test == null || test.testFunctions == null || test.testFunctions.length == 0) {
            child2.tests.remove(i);
        }
    }
    if (child1.tests.isEmpty()) {
        child1.tests.add(genRandFuncsArr(2));
    }
    if (child2.tests.isEmpty()) {
        child2.tests.add(genRandFuncsArr(2));
    }
    return new Tests[]{child1,child2};
}
private Tests tournementSelection(Tests[] tests, int k){
    int best = random.nextInt(tests.length);
    for (int i = 1; i < k; i++) {
        int current = random.nextInt(tests.length);
        if (tests[current].allTestBranches > tests[best].allTestBranches) {
            best = current;
        }
    }
    return tests[best];
}
private List<List<Test>> crossoverFunctions(Tests parent1, Tests parent2, int maxFuncsInTest) {
    List<Test> child1Tests = new ArrayList<>();
    List<Test> child2Tests = new ArrayList<>();
    int numToGenerate = random.nextInt(Math.min(parent1.getNumOfTests(), parent2.getNumOfTests()), Math.max(parent1.getNumOfTests(), parent2.getNumOfTests()) + 1);
    if (numToGenerate < 1) {
        numToGenerate = 1;
    }
    //for a random number of times between min and max size of the parents crossover random tests functions
    for (int i = 0; i < numToGenerate; i++) {
        Test test1 = parent1.tests.get(random.nextInt(parent1.tests.size()));
        Test test2 = parent2.tests.get(random.nextInt(parent2.tests.size()));
        TestFunction[] funcs1 = test1.testFunctions;
        TestFunction[] funcs2 = test2.testFunctions;
        int split1 = random.nextInt(funcs1.length + 1);
        int split2 = random.nextInt(funcs2.length + 1);
        List<TestFunction> newFuncs1 = new ArrayList<>();
        for (int j = 0; j < split1; j++) {
            if (newFuncs1.size() < maxFuncsInTest) {
                TestFunction copy = new TestFunction(funcs1[j].functionName, funcs1[j].types, funcs1[j].index);
                copy.args = funcs1[j].args.clone();
                newFuncs1.add(copy);
            }
        }
        for (int j = split2; j < funcs2.length; j++) {
            if (newFuncs1.size() < maxFuncsInTest) {
                TestFunction copy = new TestFunction(funcs2[j].functionName, funcs2[j].types, funcs2[j].index);
                copy.args = funcs2[j].args.clone();
                newFuncs1.add(copy);
            }
        }
        List<TestFunction> newFuncs2 = new ArrayList<>();
        for (int j = 0; j < split2; j++) {
            if (newFuncs2.size() < maxFuncsInTest) {
                TestFunction copy = new TestFunction(funcs2[j].functionName, funcs2[j].types, funcs2[j].index);
                copy.args = funcs2[j].args.clone();
                newFuncs2.add(copy);
            }
        }
        for (int j = split1; j < funcs1.length; j++) {
            if (newFuncs2.size() < maxFuncsInTest) {
                TestFunction copy = new TestFunction(funcs1[j].functionName, funcs1[j].types, funcs1[j].index);
                copy.args = funcs1[j].args.clone();
                newFuncs2.add(copy);
            }
        }
        if (newFuncs1.isEmpty()) {
            int rand = random.nextInt(funcs1.length);
            TestFunction copy = new TestFunction(funcs1[rand].functionName, funcs1[rand].types, funcs1[rand].index);
            copy.args = funcs1[rand].args.clone();
            newFuncs1.add(copy);
        }
        if (newFuncs2.isEmpty()) {
            int rand = random.nextInt(funcs2.length);
            TestFunction copy = new TestFunction(funcs2[rand].functionName, funcs2[rand].types, funcs2[rand].index);
            copy.args = funcs2[rand].args.clone();
            newFuncs2.add(copy);
        }
        child1Tests.add(new Test(newFuncs1.toArray(new TestFunction[0])));
        child2Tests.add(new Test(newFuncs2.toArray(new TestFunction[0])));
    }
    return List.of(child1Tests, child2Tests);
}
private List<List<Test>> crossoverTests(Tests parent1, Tests parent2, int maxTests) {
    List<Test> child1Tests = new ArrayList<>();
    List<Test> child2Tests = new ArrayList<>();
    int size1 = parent1.tests.size();
    int size2 = parent2.tests.size();
    int split1 = random.nextInt(size1 + 1);
    int split2 = random.nextInt(size2 + 1);
    //crosses over random tests from the two parents
    for (int i = 0; i < split1; i++) {
        child1Tests.add(new Test(parent1.tests.get(i)));
    }
    for (int i = split2; i < size2; i++) {
        child1Tests.add(new Test(parent2.tests.get(i)));
    }
    for (int i = 0; i < split2; i++) {
        child2Tests.add(new Test(parent2.tests.get(i)));
    }
    for (int i = split1; i < size1; i++) {
        child2Tests.add(new Test(parent1.tests.get(i)));
    }
    if (child1Tests.size() > maxTests) {
        child1Tests = new ArrayList<>(child1Tests.subList(0, maxTests));
    }
    if (child2Tests.size() > maxTests) {
        child2Tests = new ArrayList<>(child2Tests.subList(0, maxTests));
    }
    if (child1Tests.isEmpty()) {
        child1Tests.add(new Test(parent1.tests.get(0)));
    }
    if (child2Tests.isEmpty()) {
        child2Tests.add(new Test(parent2.tests.get(0)));
    }
    return List.of(child1Tests, child2Tests);
}
private Tests[] genRandomTests(int mu, int maxTests, int maxFuncInTest) throws IOException {
    Tests[] tests = new Tests[mu];
    List<Function> functionsWithBranches = new ArrayList<>();
    for (Function function : functions) {
        if (allBranchesInFunctions.containsKey(function.functionName) && allBranchesInFunctions.get(function.functionName) > 0) {
            functionsWithBranches.add(function);
        }
    }
    for (int i = 0; i < mu; i++) {
        List<Test> testList = new ArrayList<>();
        int numTests = random.nextInt(2, maxTests + 1);

        for (int j = 0; j < numTests; j++) {
            int numFuncs = random.nextInt(2, maxFuncInTest + 1);
            TestFunction[] testFunctions = new TestFunction[numFuncs];
            for (int k = 0; k < numFuncs; k++) {
                Function mainfunction;
                if (!functionsWithBranches.isEmpty() && random.nextDouble() < 0.8) {
                    List<Function> uncoveredFunctions = new ArrayList<>();
                    for (Function function : functionsWithBranches) {
                        int possible = 0;
                        if (allBranchesInFunctions.containsKey(function.functionName)) {
                            possible =  allBranchesInFunctions.get(function.functionName);
                        }
                        int found = 0;
                        if (branchesHitByFunction.containsKey(function.functionName)) {
                            found = branchesHitByFunction.get(function.functionName).size();
                        }
                        if (found < possible) {
                            uncoveredFunctions.add(function);
                        }
                    }
                    if (!uncoveredFunctions.isEmpty()) {
                        mainfunction = uncoveredFunctions.get(random.nextInt(uncoveredFunctions.size()));
                    } else {
                        mainfunction = functionsWithBranches.get(random.nextInt(functionsWithBranches.size()));
                    }
                } else {
                    mainfunction = functions.get(random.nextInt(functions.size()));
                }
                int index = functions.indexOf(mainfunction);
                TestFunction testFunction = new TestFunction(mainfunction.functionName, mainfunction.types, index);
                for (int l = 0; l < testFunction.args.length; l++) {
                    testFunction.mutateArgs(l);
                }
                testFunctions[k] = testFunction;
            }
            testList.add(new Test(testFunctions));
        }
        tests[i] = new Tests(testList);
    }
    //exe program on the list of tests to see what branches are hit
    runTests(tests);

    //calc fitnesses
    for (int i = 0; i < mu; i++) {
        tests[i].calculateNumOfFunctions();
        tests[i].calculateNumOfBranches();
    }

    return tests;
}
private Test genRandFuncsArr(int numberOfFunctions) {
    int maxSize = functions.size();
    Test funcArr = new Test(new TestFunction[numberOfFunctions]);
    for(int i = 0; i < numberOfFunctions; i++){
        int index = random.nextInt(maxSize);
        Function function = functions.get(index);
        TestFunction testFunction = new TestFunction(function.functionName,function.types, index);
        int argNum = function.types.size();
        for (int j = 0; j < argNum; j++) {
            testFunction.mutateArgs(j);
        }
        funcArr.testFunctions[i] = testFunction;
    }
    return funcArr;
}
private String funcToIndex(TestFunction[] funcOrder){
    if (funcOrder == null || funcOrder.length == 0){
        return " ";
    }
    StringBuilder stringBuilder = new StringBuilder();
    for (TestFunction testFunction : funcOrder) {
        if (testFunction != null) {
            stringBuilder.append(testFunction.index).append(" ");
        }
    }
    return stringBuilder.toString().trim();
}
private void compileProgram(String newClass) {
    try {
        rumName = "RunTest";
        String newProgram = createTest(newClass);
        Path path = Path.of(dir.getPath(), rumName + ".java");
        Files.writeString(path, newProgram);
        ProcessBuilder pb = new ProcessBuilder("javac", rumName + ".java").directory(dir);
        Process compile = pb.start();
        if (compile.waitFor() != 0) {
            System.out.println("failed to run");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

//REMEMBER TO CAST ALL DATA TYPES
private String createTest(String newFunction) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("public class ").append(rumName).append(" {\n")
            .append("    static int loopIteration = 0;\n")
            .append("    public static void main(String[] args) {\n")
            .append("        try {\n")
            .append("            ").append(strClass).append(" test = null;\n")
            .append("            String[] ids;\n")
            //do it this way or import whatever looks cleaner
            .append("            java.util.List<String> argsList = new java.util.ArrayList<>();\n")
            .append("            \n")
            .append("            if (args.length == 1 && new java.io.File(args[0]).exists()) {\n")
            //same with this (might be more time efficient this way)
            .append("                java.io.BufferedReader bufferReader = new java.io.BufferedReader(new java.io.FileReader(args[0]));\n")
            .append("                ids = bufferReader.readLine().split(\" \");\n")
            .append("                String line;\n")
            .append("                while ((line = bufferReader.readLine()) != null) {\n")
            .append("                    argsList.add(line);\n")
            .append("                }\n")
            .append("                bufferReader.close();\n")
            .append("            } else {\n")
            .append("                ids = args[0].split(\" \");\n")
            .append("                for (int i = 1; i < args.length; i++) {\n")
            .append("                    argsList.add(args[i]);\n")
            .append("                }\n")
            .append("            }\n")
            .append("            \n")
            .append("            String[] argsArr = argsList.toArray(new String[0]);\n")
            .append("            int argID = 0;\n")
            .append("            \n")
            .append("            for (String stringID : ids) {\n")
            .append("                if (!stringID.isEmpty()){\n")
            .append("                    int id = Integer.parseInt(stringID);\n")
            .append("                    if (id == -1) {\n")
            .append("                        System.out.println(-1);\n")
            .append("                        continue;\n")
            .append("                    }\n")
            .append("                    if (id == -2) {\n")
            .append("                        System.out.println(-2);\n")
            .append("                        continue;\n")
            .append("                    }\n")
            .append("                    if (id == -3) {\n");
    if (constructorArgsTypes == null || constructorArgsTypes.isEmpty()) {
        stringBuilder.append("                       test = new ").append(strClass).append("();\n");
    }
    else{
        stringBuilder.append("                       test = new ").append(strClass).append("(")
                .append(makeArgs(constructorArgsTypes))
                .append(");\n");
    }
    stringBuilder.append("                    }\n");
    stringBuilder.append("                    switch (id) {\n");
    for (int i = 0; i < functions.size(); i++) {
        Function function = functions.get(i);
        stringBuilder.append("                        case ").append(i).append(":\n")
                .append("                            RunTest.loopIteration = 0;\n")
                .append("                            test.").append(function.functionName).append("(");
        stringBuilder.append(makeArgs(function.types));
        stringBuilder.append(");\n")
                .append("                            break;\n");
    }
    stringBuilder.append("                    }\n")
            .append("                }\n")
            .append("            }\n")
            .append("        } catch (Exception e) {\n")
            .append("            e.printStackTrace();\n")
            .append("        }\n")
            .append("    }\n")
            .append("}\n")
            .append(newFunction.trim());
    return stringBuilder.toString();
}
private String makeArgs(List<String> types){
    StringBuilder stringBuilder = new StringBuilder();
    for (int j = 0; j < types.size(); j++) {
        String type = types.get(j);
        switch (type) {
            case "int": stringBuilder.append("Integer.parseInt(argsArr[argID++])"); break;
            case "double": stringBuilder.append("Double.parseDouble(argsArr[argID++])"); break;
            case "float": stringBuilder.append("Float.parseFloat(argsArr[argID++])"); break;
            case "long": stringBuilder.append("Long.parseLong(argsArr[argID++])"); break;
            case "short": stringBuilder.append("Short.parseShort(argsArr[argID++])"); break;
            case "boolean": stringBuilder.append("Boolean.parseBoolean(argsArr[argID++])"); break;
            case "char": stringBuilder.append("argsArr[argID++].charAt(0)"); break;
            case "byte": stringBuilder.append("Byte.parseByte(argsArr[argID++])"); break;
            default: stringBuilder.append("argsArr[argID++]"); break;
        }
        if (j < types.size() - 1) stringBuilder.append(", ");
    }
    return stringBuilder.toString();
}
private boolean readFile(String file){
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        boolean isComment = false;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            boolean isString = false;
            boolean prevbackslash = false;
            boolean isSout = false;
            int soutStartIndex = -1;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                //if the previous was a backslash then this is either a char in a string or something fancy
                if (prevbackslash) {
                    stringBuilder.append(c);
                    prevbackslash = false;
                    continue;
                }
                //if the char is a \ then the next char in string will still be in the string
                if (c == '\\' && isString) {
                    prevbackslash = true;
                    stringBuilder.append(c);
                    continue;
                }
                //if the char is " then we in/going out of quotes
                if (c == '"') {
                    isString = !isString;
                    stringBuilder.append(c);
                    continue;
                }
                //we are in a string
                if (isString) {
                    stringBuilder.append(c);
                    continue;
                }
                //if there is a letter after go in
                if (i + 1 < line.length()) {
                    String twoChar = String.valueOf(c) + line.charAt(i + 1);
                    //ignore rest of line as its a big comment
                    if (!isComment && twoChar.equals("//")) {
                        break;
                    }
                    //start of partial comment
                    if (!isComment && twoChar.equals("/*")) {
                        isComment = true;
                        i++;
                        continue;
                    }
                    //end of partial comment
                    if (isComment && twoChar.equals("*/")) {
                        isComment = false;
                        i++;
                        continue;
                    }
                }
                //if none of the above is happening and we are still in a comment then move on
                if (isComment) {
                    continue;
                }
                //now we check for print lines and need to remove it so it wont affect reading terminal
                String print = "System.out.println(";
                if (i + print.length() <= line.length() && !isSout) {
                    String str = line.substring(i, i + print.length());
                    if (str.equals(print)) {
                        isSout = true;
                        soutStartIndex = i + print.length() - 1;
                    }
                }
                print = "IO.println(";
                if (i + print.length() <= line.length() && !isSout) {
                    String str = line.substring(i, i + print.length());
                    if (str.equals(print)) {
                        isSout = true;
                        soutStartIndex = i + print.length() -1;
                    }
                }
                //if we are in print find end and skip the whole print
                if (isSout) {
                    int endIndex = findEndOfSOUT(line, soutStartIndex);
                    if (endIndex != -1) {
                        i = endIndex;
                        isSout = false;
                        soutStartIndex = -1;
                        continue;
                    }
                }
                //if nothing above then add char
                stringBuilder.append(c);
            }
            //add new line
            stringBuilder.append('\n');
        }
        result = insertPrintAfterCondition(stringBuilder.toString());
    } catch (Exception e) {
        System.out.println(e.getMessage());
        return false;
    }
    return true;
}

private int findEndOfSOUT(String line, int soutStartIndex) {
    int numOfBrackets = 0;
    boolean isString = false;
    boolean prevbackslash = false;
    boolean isChar = false;
    for (int i = soutStartIndex; i < line.length(); i++) {
        char c = line.charAt(i);
        //same checks as above
        if (prevbackslash) {
            prevbackslash = false;
            continue;
        }
        if (c == '\\' && (isString || isChar)) {
            prevbackslash = true;
            continue;
        }
        if (c == '\'' && !isString) {
            isChar = !isChar;
            continue;
        }
        if (c == '"' && !isChar) {
            isString = !isString;
            continue;
        }
        //counts brackets until end is found then finds following ; (i think python syntax would have been easier)
        if (!isString && !isChar) {
            if (c == '(') {
                numOfBrackets++;
            }
            else if (c == ')') {
                numOfBrackets--;
            }
            if (numOfBrackets == 0) {
                int endIndex = i;
                while (endIndex < line.length() && line.charAt(endIndex) != ';') {
                    endIndex++;
                }
                return endIndex;
            }
        }
    }
    return -1;
}
private String insertPrintAfterCondition(String allLines) {
    StringBuilder result = new StringBuilder();
    int i = 0;
    //else if needs to be first as otherwise it will be picked up as a else then an if
    String[] conditions = {"else if", "if", "while", "for", "case", "else", "catch", "finally", "default"};
    boolean isString = false;
    boolean prevbackslash = false;
    boolean isChar = false;
    int loops = 0;
    String currentFunction = null;

    while (i < allLines.length()) {
        char c = allLines.charAt(i);
        //same as above
        if (prevbackslash) {
            result.append(c);
            prevbackslash = false;
            i++;
            continue;
        }
        if (c == '\\' && (isString || isChar)) {
            prevbackslash = true;
            result.append(c);
            i++;
            continue;
        }
        if (c == '\'' && !isString) {
            isChar = !isChar;
            result.append(c);
            i++;
            continue;
        }
        if (c == '"' && !isChar) {
            isString = !isString;
            result.append(c);
            i++;
            continue;
        }
        if (isString || isChar) {
            result.append(c);
            i++;
            continue;
        }
        //comment checking
        if (i + 1 < allLines.length()) {
            String twoChar = String.valueOf(c) + allLines.charAt(i + 1);
            if (twoChar.equals("//")) {
                int lineEnd = allLines.indexOf('\n', i);
                if (lineEnd == -1) lineEnd = allLines.length();
                result.append(allLines, i, lineEnd);
                i = lineEnd;
                continue;
            }
            if (twoChar.equals("/*")) {
                int commentEnd = allLines.indexOf("*/", i + 2);
                if (commentEnd == -1) commentEnd = allLines.length();
                else commentEnd += 2;
                result.append(allLines, i, commentEnd);
                i = commentEnd;
                continue;
            }
        }
        //find if its a class or not
        if (strClass == null && i + 6 <= allLines.length()) {
            if (allLines.startsWith("class ", i) || allLines.startsWith("public class ", i)) {
                if (allLines.startsWith("public class ", i)) {
                    i += 13;
                } else {
                    i += 6;
                }
                int start = i;
                int end = allLines.indexOf("{", start);
                result.append("class ");
                if (end != -1) {
                    String classLine = allLines.substring(start, end).trim();
                    String[] partsWithWhite = classLine.split(" ");
                    List<String> parts = new ArrayList<>();
                    for (String part : partsWithWhite) {
                        if (!part.trim().isEmpty()) {
                            parts.add(part);
                        }
                    }
                    if (!parts.isEmpty()) {
                        strClass = parts.get(0);
                        continue;
                    }
                }
            }
        }
        //get all funcs
        if (i + 7 <= allLines.length() && allLines.startsWith("public ", i)) {
            int lineEnd = allLines.indexOf("\n", i);
            if (lineEnd == -1) lineEnd = allLines.length();
            String line = allLines.substring(i, lineEnd).trim();

            if (line.contains("(") && line.contains(")")) {
                int openBracket = line.indexOf("(");
                int closeBracket = line.indexOf(")");

                if (openBracket != -1 && closeBracket != -1 && closeBracket > openBracket) {
                    String beforeParen = line.substring(7, openBracket).trim();
                    String[] partsWithWhite = beforeParen.split(" ");
                    List<String> parts = new ArrayList<>();
                    for (String part : partsWithWhite) {
                        if (!part.trim().isEmpty()) {
                            parts.add(part);
                        }
                    }
                    // Parse arguments
                    String argsString = line.substring(openBracket + 1, closeBracket).trim();
                    List<String> argTypes = new ArrayList<>();

                    if (!argsString.isEmpty()) {
                        String[] arguments = argsString.split(",");
                        for (String arg : arguments) {
                            String[] argPartsWithWhite = arg.split(" ");
                            List<String> argParts = new ArrayList<>();
                            for (String part : argPartsWithWhite) {
                                if (!part.trim().isEmpty()) {
                                    argParts.add(part);
                                }
                            }
                            if (argParts.size() >= 2) {
                                argTypes.add(argParts.get(0));
                            }
                        }
                    }
                    if (parts.size() == 1 && strClass != null && parts.get(0).equals(strClass)) {
                        constructorArgsTypes = new ArrayList<>(argTypes);
                    } else if (parts.size() >= 2) {
                        String functionName = parts.get(parts.size() - 1);
                        boolean exists = false;
                        for (Function function : functions) {
                            if (function.functionName.equals(functionName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            functions.add(new Function(functionName, argTypes));
                            allBranchesInFunctions.put(functionName, 0);
                        }
                        currentFunction = functionName;
                    }
                }
            }
        }
        //finds the conditions
        boolean isCondition = false;
        for (String condition : conditions) {
            if (i + condition.length() <= allLines.length()) {
                String potentialCondition = allLines.substring(i, i + condition.length());
                if (potentialCondition.equals(condition)) {
                    char next = allLines.charAt(i + condition.length());
                    char prev = ' ';
                    if (i != 0){
                        prev = allLines.charAt(i-1);
                    }
                    if ((prev == ' ' || prev == '\n' || prev == '}') && (next == ' ' || next == '{' || next == '(' || next == ':')) {
                        //for switch case conditions (they dont have any squigglys)
                        if (condition.equals("case") || condition.equals("default")) {
                            int startIndex = i + condition.length();
                            boolean isisString = false;
                            boolean isisChar = false;
                            int colonIndex = -1;
                            boolean prevBackslash = false;
                            for (int j = startIndex; j < allLines.length(); j++) {
                                char ch = allLines.charAt(j);
                                if (prevBackslash) {
                                    prevBackslash = false;
                                    continue;
                                }
                                if (ch == '\\' && (isisString || isisChar)) {
                                    prevBackslash = true;
                                    continue;
                                }
                                if (ch == '\'' && !isisString) {
                                    isisChar = !isisChar;
                                    continue;
                                }
                                if (ch == '"' && !isisChar) {
                                    isisString = !isisString;
                                    continue;
                                }
                                if (!isisString && !isisChar && ch == ':'){
                                    colonIndex = j;
                                }
                                if (!isisString && !isisChar && ch == '\n') {
                                    break;
                                }
                            }
                            if (colonIndex > -1) {
                                isCondition = true;
                                result.append(condition);
                                i += condition.length();
                                result.append(allLines, i, colonIndex + 1);
                                result.append("System.out.println(\"").append(numOfBranches).append("\");");
                                if (currentFunction != null) {
                                    if (allBranchesInFunctions.containsKey(currentFunction)) {
                                        int val = allBranchesInFunctions.get(currentFunction);
                                        allBranchesInFunctions.put(currentFunction, ++val);
                                    }
                                    else {
                                        allBranchesInFunctions.put(currentFunction, 1);
                                    }
                                }
                                numOfBranches++;
                                i = colonIndex + 1;
                                break;
                            }
                        }
                        int squigglyBracketIndex = findNextSquiggly(allLines, i + condition.length(), condition.equals("for"));
                        if (squigglyBracketIndex > -1) {
                            isCondition = true;
                            result.append(condition);
                            i += condition.length();
                            result.append(allLines, i, squigglyBracketIndex + 1);
                            result.append("System.out.println(\"").append(numOfBranches).append("\");");
                            if (condition.equals("while") || condition.equals("for")) {
                                result.append("if(RunTest.loopIteration++ > 50){ break;}");
                            }
                            if (currentFunction != null) {
                                if (allBranchesInFunctions.containsKey(currentFunction)) {
                                    int val = allBranchesInFunctions.get(currentFunction);
                                    allBranchesInFunctions.put(currentFunction, ++val);
                                }
                                else {
                                    allBranchesInFunctions.put(currentFunction, 1);
                                }
                            }
                            numOfBranches++;
                            i = squigglyBracketIndex + 1;
                            break;
                        }
                        else if (squigglyBracketIndex < -1) {
                            int squigglyIndex = -(squigglyBracketIndex + 2);
                            boolean isConditionWithExtra = condition.equals("if") || condition.equals("else if") || condition.equals("while") || condition.equals("for") || condition.equals("catch");
                            int start = -1;
                            if (!isConditionWithExtra){
                                start = i + condition.length();
                            }
                            int distance = 0;
                            boolean foundOpen = false;
                            boolean isisString = false;
                            boolean isisChar = false;
                            boolean prevBackslash = false;
                            for (int j = i + condition.length(); j < allLines.length(); j++) {
                                char character = allLines.charAt(j);
                                if (prevBackslash) { prevBackslash = false;
                                    continue;
                                }
                                if (character == '\\' && (isisString || isisChar)) {
                                    prevBackslash = true;
                                    continue;
                                }
                                if (character == '\'' && !isisString) {
                                    isisChar = !isisChar;
                                    continue;
                                }
                                if (character == '"' && !isisChar) {
                                    isisString = !isisString;
                                    continue;
                                }
                                if (!isisString && !isisChar) {
                                    if (character == '(') {
                                        distance++;
                                        foundOpen = true;
                                    }
                                    else if (character == ')') {
                                        distance--;
                                        if (foundOpen && distance == 0){
                                            start = j + 1;
                                        }
                                    }
                                }
                            }
                            if (start == -1 || start > squigglyIndex) {
                                result.append(allLines, i, squigglyIndex + 1);
                                i = squigglyIndex + 1;
                                break;
                            }
                            isCondition = true;
                            result.append(condition);
                            i += condition.length();
                            result.append(allLines, i, start);
                            result.append("{System.out.println(\"").append(numOfBranches).append("\");");
                            result.append(allLines, start, squigglyIndex + 1);
                            result.append("}");
                            if (currentFunction != null) {
                                if (allBranchesInFunctions.containsKey(currentFunction)) {
                                    int val = allBranchesInFunctions.get(currentFunction);
                                    allBranchesInFunctions.put(currentFunction, ++val);
                                } else {
                                    allBranchesInFunctions.put(currentFunction, 1);
                                }
                            }
                            numOfBranches++;
                            i = squigglyIndex + 1;
                            break;
                        }
                    }
                }
            }
        }
        if (!isCondition) {
            result.append(c);
            i++;
        }
    }
    return result.toString();
}
private int findNextSquiggly(String allLines, int startIndex, boolean isFor) {
    boolean isString = false;
    boolean prevbackslash = false;
    boolean isChar = false;
    //same as above again but finding {
    for (int i = startIndex; i < allLines.length(); i++) {
        char c = allLines.charAt(i);
        if (prevbackslash) {
            prevbackslash = false;
            continue;
        }
        if (c == '\\' && (isString || isChar)) {
            prevbackslash = true;
            continue;
        }
        if (c == '\'' && !isString) {
            isChar = !isChar;
            continue;
        }
        if (c == '"' && !isChar) {
            isString = !isString;
            continue;
        }
        if (!isString && !isChar && c == '{') {
            return i;
        }
        if (!isString && !isChar && !isFor && c == ';') {
            return -(i + 2);
        }

    }
    return -1;
}
class Function {
    String functionName;
    List<String> types;
    public Function(String functionName, List<String> types) {
        this.functionName = functionName;
        this.types = types;
    }
}
class TestFunction extends Function {
    Object[] args;
    int index;
    public TestFunction(String functionName, List<String> types, int index) {
        super(functionName, types);
        this.index = index;
        args = new Object[types.size()];
    }
    public void mutateArgs(int argIndex){
        String type = types.get(argIndex);
        switch (type) {
            case "int", "short", "long", "double":
                args[argIndex] = mutateInt(args[argIndex],type);
                break;
            case "boolean":
                if (args[argIndex] == null){
                    args[argIndex] = random.nextBoolean();
                }
                else {
                    args[argIndex] = mutateBool((boolean) args[argIndex]);
                }
                break;
            case "char":

                args[argIndex] = mutateChar();
                break;
            case "String":
                args[argIndex] = mutateStr((String) args[argIndex]);
                break;
            case "byte":
                if (args[argIndex] == null){
                    args[argIndex] = random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
                }
                else {
                    args[argIndex] = mutateByte(((Number)args[argIndex]).byteValue());
                }
                break;
            case  "float":
                if (args[argIndex] == null){
                    args[argIndex] = switch (random.nextInt(6)) {
                        case 0 -> Float.MAX_VALUE;
                        case 1 -> Float.MIN_VALUE;
                        case 2 -> (float) 0;
                        case 3 -> (float)1;
                        case 4 -> (float) -1;
                        default -> random.nextFloat(-1000, 1001);
                    };
                }
                else {
                    args[argIndex] = mutateFloat((float) args[argIndex]);
                }
                break;
        }
    }
    public void mutateArgsRandomly(int argIndex){
        String type = types.get(argIndex);
        switch (type) {
            case "int", "short", "long", "double":
                args[argIndex] = random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
                break;
            case "boolean":
                args[argIndex] = random.nextBoolean();
                break;
            case "char":
                args[argIndex] = random.nextInt(0, Character.MAX_VALUE);
                break;
            case "String":
                int leng = random.nextInt(0,1000);
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < leng; i++) {
                    stringBuilder.append(random.nextInt(0, Character.MAX_VALUE));
                }
                args[argIndex] = stringBuilder.toString();
                break;
            case "byte":
                args[argIndex] = random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
                break;
            case  "float":
                args[argIndex] = random.nextFloat();
                break;
        }
    }
    private Number mutateInt(Object objNum, String type){
        if (objNum == null) {
            return switch (random.nextInt(6)) {
                case 0 -> getMinValue(type);
                case 1 -> getMaxValue(type);
                case 2 -> 0;
                case 3 -> 1;
                case 4 -> -1;
                default -> random.nextInt(-1000, 1001);
            };
        }
        int num = ((Number)objNum).intValue();
        //so this means 8/15 for increment 5/15 for edge case and 2/15 for random
        return switch (random.nextInt(0, 20)) {
            case 0 -> ++num;
            case 4 -> --num;
            case 8 -> 0;
            case 9 -> -1;
            case 10 -> 1;
            case 13,14,15 -> random.nextInt(-20, 20);
            case 16, 1, 2, 3 -> getMinValue(type);
            case 17, 5, 6, 7 -> getMaxValue(type);
            case 18 -> random.nextInt(-100,101);
            default -> random.nextInt(-1000,1001);
        };
    }
    private Number getMaxValue(String type) {
        return switch (type) {
            case "int" -> Integer.MAX_VALUE;
            case "long" -> Long.MAX_VALUE;
            case "short" -> Short.MAX_VALUE;
            case "double" -> Double.MAX_VALUE;
            default -> 0;
        };
    }

    private Number getMinValue(String type) {
        return switch (type) {
            case "int" -> Integer.MIN_VALUE;
            case "long" -> Long.MIN_VALUE;
            case "short" -> Short.MIN_VALUE;
            case "double" -> Double.MIN_VALUE;
            default -> 0;
        };
    }
    private float mutateFloat(float num){
        //so this means 8/15 for increment 5/15 for edge case and 2/15 for random
        return switch (random.nextInt(0, 20)) {
            case 0 -> ++num;
            case 4 -> --num;
            case 8 -> 0;
            case 9 -> -1;
            case 10 -> 1;
            case 13,14, 7 -> random.nextFloat(-20, 20);
            case 16, 1, 2, 5 -> Float.MAX_VALUE;
            case 17, 3, 15, 6 -> Float.MIN_VALUE;
            case 18 -> random.nextFloat(-100,101);
            default -> random.nextFloat(-1000,1001);
        };
    }
    private byte mutateByte(byte num){
        //so this means 8/15 for increment 5/15 for edge case and 2/15 for random
        return (byte) switch (random.nextInt(0, 13)) {
            case 0 -> ++num;
            case 4 -> --num;
            case 8 -> 0;
            case 9 -> -1;
            case 10 -> 1;
            case 11, 1, 5 -> Byte.MAX_VALUE;
            case 12, 2, 6 -> Byte.MIN_VALUE;
            default -> random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        };
    }
    private boolean mutateBool(boolean bool){
        return !bool;
    }
    private char mutateChar(){
        return switch (random.nextInt(20)) {
            case 0  -> 'a';
            case 1  -> 'b';
            case 2  -> 'A';
            case 3  -> 'B';
            case 4  -> '0';
            case 5  -> ' ';
            case 6  -> (char)(random.nextInt(26) + 'a');
            case 7  -> (char)(random.nextInt(26) + 'A');
            default -> (char) random.nextInt(32, 127);
        };
    }
    private String mutateStr(String str){
        if (str == null || str.isEmpty()) {
            return genString();
        }
        int rand = random.nextInt(10);
        if (rand < 1) {
            int i = random.nextInt(str.length());//remove char
            return str.substring(0, i) + str.substring(i+1);
        }
        if (rand < 2) {//repalce char
            int i = random.nextInt(str.length());
            return str.substring(0, i) + mutateChar() + str.substring(i+1);
        }
        if (rand < 3) {//add char
            return str + mutateChar();
        }
        if (rand < 4) {//empty
            return "";
        }
        if (rand < 5) {//null
            return null;
        }
        if (rand < 6){
            return String.valueOf(mutateChar());
        }
        else {//new string
            return genString();
        }
    }
    private String genString(){
        int rand = random.nextInt(0, 10);
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < rand; i++){
            result.append(mutateChar());
        }
        return result.toString();
    }
}
//ADD TEST CASES FOR ARRS AND LISTS
class Test {
    TestFunction[] testFunctions;
    int numOfBranches;
    List<Integer> branches;
    TestFunction constructor;

    public Test(TestFunction[] testFunctions){
        this.testFunctions = testFunctions;
        this.branches = new ArrayList<>();
        createConstructor();
    }
    //this helps make a new copy of the tests that isnt tied to the original
    public Test(Test test){
        this.testFunctions = new TestFunction[test.testFunctions.length];
        for(int i = 0; i < test.testFunctions.length; i++){
            TestFunction testFunction = test.testFunctions[i];
            if (testFunction != null){
                this.testFunctions[i] = new TestFunction(testFunction.functionName,testFunction.types,testFunction.index);
                this.testFunctions[i].args  = testFunction.args.clone();
            }
        }
        this.numOfBranches = test.numOfBranches;
        if (test.branches != null){
            this.branches = new ArrayList<>(test.branches);
        }
        else {
            this.branches = new ArrayList<>();
        }
        createConstructor();
    }
    private void createConstructor(){
        if (constructorArgsTypes != null && !constructorArgsTypes.isEmpty()) {
            this.constructor = new TestFunction(strClass, constructorArgsTypes, -1);
            for (int i = 0; i < constructor.args.length; i++) {
                constructor.mutateArgs(i);
            }
        }
        else{
            this.constructor = null;
        }
    }
    public void mutateArgs(){
        if (constructor != null && random.nextDouble() < 0.3) {
            constructor.mutateArgs(random.nextInt(constructor.args.length));
            return;
        }
        if (testFunctions != null && testFunctions.length != 0){
            TestFunction function = testFunctions[random.nextInt(testFunctions.length)];
            if (function != null && function.args != null && function.args.length != 0) {
                function.mutateArgs(random.nextInt(function.args.length));
            }
        }
    }
    public void mutateFunctions(List<Function> allTestFunctions, int maxFunctions){
        if (allTestFunctions.isEmpty()) return;
        List<Function> bestFunctions = new ArrayList<>();
        for (Function function : allTestFunctions) {
            int possibleBranches = 0;
            if (allBranchesInFunctions.containsKey(function.functionName)){
                possibleBranches = allBranchesInFunctions.get(function.functionName);
            }
            Set<Integer> foundBranches = new HashSet<>();
            if (branchesHitByFunction.containsKey(function.functionName)){
                foundBranches.addAll(branchesHitByFunction.get(function.functionName));
            }
            if (possibleBranches > foundBranches.size()) {
                bestFunctions.add(function);
            }
        }
        Function function;
        int funcIndex;
        //70% chance to pick a func with undiscovered branches otherwise completely random
        if (!bestFunctions.isEmpty() && random.nextDouble() < 0.7) {
            function = bestFunctions.get(random.nextInt(bestFunctions.size()));
            funcIndex = allTestFunctions.indexOf(function);
        } else {
            funcIndex = random.nextInt(allTestFunctions.size());
            function = allTestFunctions.get(funcIndex);
        }

        TestFunction testFunction = new TestFunction(function.functionName, function.types, funcIndex);
        for(int i = 0; i < testFunction.args.length; i++){
            testFunction.mutateArgs(i);
        }
        int possibleBranches = 0;
        if (allBranchesInFunctions.containsKey(function.functionName)){
            possibleBranches = allBranchesInFunctions.get(function.functionName);
        }
        //if less than 3 func always add, if no possible branches delete or replace then 10% to swap
        //5% to delete unless less than 2 func then add then 55% to add unless more than maxFuncs then replace
        //finally if none of the above (30% chance) repalce.
        if (testFunctions.length < 3) {
            addFunction(testFunction);
            return;
        }
        int rand = random.nextInt(100);
        if (possibleBranches == 0) {
            if (rand < 50 && testFunctions.length > 2) {
                deleteFunction();
            } else {
                replaceFunction(testFunction);
            }
            return;
        }
        if (rand < 10) {
            swapFunction();
        }
        else if (rand < 15) {
            if (testFunctions.length > 2) {
                deleteFunction();
            } else {
                addFunction(testFunction);
            }
        }
        else if (rand < 70) {
            if (testFunctions.length < maxFunctions) {
                addFunction(testFunction);
            } else {
                replaceFunction(testFunction);
            }
        }
        else {
            replaceFunction(testFunction);
        }
    }
    private void swapFunction(){
        if (testFunctions.length > 1){
            for (int i = 0; i < testFunctions.length; i++) {
                int j = i + random.nextInt(testFunctions.length - i);
                TestFunction temp = testFunctions[i];
                testFunctions[i] = testFunctions[j];
                testFunctions[j] = temp;
            }
        }
    }
    private void addFunction(TestFunction testFunction) {
        int newSize = testFunctions.length + 1;
        TestFunction[] temp = new TestFunction[newSize];
        int addIndex = random.nextInt(newSize);
        int oldIndex = 0;
        for (int i = 0; i < newSize; i++) {
            if (i == addIndex) {
                temp[i] = testFunction;
            } else {
                temp[i] = testFunctions[oldIndex++];
            }
        }
        testFunctions = temp;
    }
    private void deleteFunction(){
        if (testFunctions.length > 1){
            int index = random.nextInt(testFunctions.length);
            TestFunction[] temp = new TestFunction[testFunctions.length - 1];
            int newIndex = 0;
            for(int i=0; i < testFunctions.length;i++)
                if (index != i){
                    temp[newIndex++] = testFunctions[i];
                }
            testFunctions = temp;
        }
    }
    private void replaceFunction(TestFunction testFunction) {
        if (testFunctions.length == 0) {
            addFunction(testFunction);
            return;
        }
        int i = random.nextInt(testFunctions.length);
        testFunctions[i] = testFunction;
    }
}
class Tests{
    int allTestBranches;
    int numOfFunctions;
    List<Test> tests;
    public Tests(List<Test> tests){
        this.tests = tests;
    }
    public int getNumOfTests(){
        return tests.size();
    }
    public void calculateNumOfBranches(){
        Set<Integer> uniqueBranches = new HashSet<>();
        if (tests != null && !tests.isEmpty()){
            for(Test test : tests){
                if (test != null && test.branches != null) {
                    uniqueBranches.addAll(test.branches);
                }
            }
            allTestBranches = uniqueBranches.size();
        }
    }
    public void calculateNumOfFunctions(){
        int functions = 0;
        for (Test test : tests){
            functions += test.testFunctions.length;
        }
        numOfFunctions = functions;
    }
    public void mutateTests(int maxTests){
        if (tests.isEmpty()) return;
        if(random.nextBoolean()){
            if (tests.size() < maxTests) {
                addTest();
            }
            else {
                deleteTest();
            }
        }
        else{
            if (tests.size() > 1) {
                deleteTest();
            }
            else if (tests.size() < maxTests) {
                addTest();
            }
        }
    }
    private void addTest(){
        int index = random.nextInt(tests.size());
        tests.add(new Test(tests.get(index)));
    }
    //maybe change to remove the worst test if i get around to it?
    private void deleteTest(){
        if (tests.size() > 1){
            int index = random.nextInt(tests.size());
            tests.remove(index);
        }
    }
}