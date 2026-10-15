package com.huangyiming;

public class App {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java -jar calculator-1.0.jar <number> <operator> <number>");
            System.out.println("Example: java -jar calculator-1.0.jar 2 + 2");
            return;
        }

        Calculator calculator = new Calculator();
        try {
            double result = calculator.eval(args[0], args[1], args[2]);
            System.out.println(result);
        } catch (IllegalArgumentException | ArithmeticException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
