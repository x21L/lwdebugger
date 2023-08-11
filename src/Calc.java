public class Calc {
    public static void main(String[] args) {
        String helloWorld = "Hello Debugging World";
        int i = 3;
        int j = i * 2;
        int[] k = { 4, 5, 6, 7 };
        Person person = new Person("lukas", 26);
        System.out.println(k[3]);
        int x = 4;
        int l = modAB(5, 2);
        System.out.println(j + x + l + " " + person + " " + helloWorld);
    }

    static int modAB(int a, int b) {
        System.out.println("Calculation of a modulo b");
        int c = a % b;
        return c;
    }
}