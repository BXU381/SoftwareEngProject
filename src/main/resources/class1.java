class TestClass {

    public TestClass(int i) {
        if (i > 50){
            System.out.println("hit");
        }
    }

    public int ifStatementTest(int i) {
        if (i > 50){
            return i;
        }
        if (i < -50){
            return -i;
        }
        return 0;
    }
    public int ifElifElseTest(int i) {
        if (i > 50){
            return i;
        }
        else if (i < -50){
            return -i;
        }
        else  {
            return 0;
        }
    }
    public String stringTest(String s) {
        if (s.length() < 4){
            return s.substring(0, 10);
        }
        return s;
    }
    public boolean booleanTest(boolean b) {
        if (b == true){
            return true;
        }
        return false;
    }
    public short shortTest(short s) {
        if (s > 5){
            return Short.MAX_VALUE;
        }
        return s;
    }
    public byte byteTest(byte b) {
        if (b > 64){
            return Byte.MAX_VALUE;
        }
        return b;
    }
    public char charTest(char c) {
        if (c > '9'){
            return (char) (c+5);
        }
        return (char) (c-5);
    }
    public boolean floatTest(float f) {
        if (f > 3.14){
            return true;
        }
        return false;
    }
    public double doubleTest(double d) {
        if (d < 3.14){
            return d;
        }
        return 0;
    }
    public long longTest(long l) {
        if (l > 0){
            return l;
        }
        if (l < 0){
            return -l;
        }
        return 0;
    }
    public int intTest(int i) {
        if (i > 0){
            return i;
        }
        return 0;
    }
}