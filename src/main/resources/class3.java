class TestClass {
    public void advancedConditionTest(int i, boolean b, char c) {
        if (i == 1 && b && c == 'a') {
            System.out.println("advancedConditionTest");
        }
        else if (i == 0 && !b && c == 'b'){
            System.out.println("advancedConditionTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void shortTest(short s){
        if (s == Short.MIN_VALUE){
            System.out.println("shortTest");
        }
        else if (s == Short.MAX_VALUE){
            System.out.println("shortTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void longTest(long s){
        if (s == Long.MIN_VALUE){
            System.out.println("longTest");
        }
        else if (s == Long.MAX_VALUE){
            System.out.println("longTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void floatTest(float s){
        if (s == Float.MIN_VALUE){
            System.out.println("floatTest");
        }
        else if (s == Float.MAX_VALUE){
            System.out.println("floatTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void doubleTest(double s){
        if (s == Double.MIN_VALUE){
            System.out.println("doubleTest");
        }
        else if (s == Double.MAX_VALUE){
            System.out.println("doubleTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void booleanTest(boolean s){
        if (s == true){
            System.out.println("booleanTest");
        }
        else {
            System.out.println("fail");
        }
    }
    public void charTest(char c){
        if (c == 'a'){
            System.out.println("charTest");
        }
        else if (c == 'b'){
            System.out.println("charTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void stringTest(String s){
        if (s.equals("ate")){
            System.out.println("stringTest");
        }
        else if (s.equals("cat")){
            System.out.println("stringTest2");
        }
        else {
            System.out.println("fail");
        }
    }
    public void intTest(int i){
        if (i == Integer.MIN_VALUE){
            System.out.println("intTest");
        }
        else if (i == Integer.MAX_VALUE){
            System.out.println("intTest2");
        }
    }
    public void hangTest(){
        while (true){
            System.out.println("hangTest");
        }
    }
    public void nestedCondition(boolean a, boolean b, boolean c){
        if (a){
            if(b){
                if (!c){
                    System.out.println("nestedCondition");
                }
            }
        }
    }
}