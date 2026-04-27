class TestClass {
    int constructorTest;
    boolean orderTest1Hit = false;
    boolean orderTest2Hit = false;

    public TestClass(int i) {
        this.constructorTest = i;
    }
    public boolean constructorTest() {
        if (constructorTest == 1) {
            return true;
        }
        else {
            return false;
        }
    }
    public void switchCaseTest(int i) {
        switch (i) {
            case 0:
                System.out.println("case 1");
                break;
            case 1:
                System.out.println("case 2");
                break;
            default:
                System.out.println("case 5");
                break;
        }
    }
    public void tryCatchFinallyTest(int i) {
        try{
            if (i == 1 || i < -500){
                throw new ArithmeticException();
            }
            else {
                i = i + 1;
            }
        }
        catch (ArithmeticException e){
            System.out.println("ArithmeticException");
        }
        finally {
            System.out.println("finally");
        }
    }
    public void whileTest(int i) {
        while (i > 0) {
            i--;
        }
    }
    public void forTest(int i) {
        for (int j = 0; j < i; j++) {
            System.out.println(j);
        }
    }
    public void advancedConditionTest(int i, boolean b, char c) {
        if (i == 1 || b && c == 'a') {
            System.out.println("advancedConditionTest");
        }
        else if (i == 0 || !b && c == 'b'){
            System.out.println("advancedConditionTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void functionOrderTest1() {
        orderTest1Hit = true;
    }
    public void functionOrderTest2() {
        if (orderTest1Hit){
            orderTest2Hit = true;
        }
    }
    public void functionOrderTest3() {
        if (orderTest2Hit){
            return;
        }
    }
}