package com.huangyiming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    void additionReturnsSum() {
        assertEquals(4.0, calculator.eval("2", "+", "2"));
    }

    @Test
    void subtractionReturnsDifference() {
        assertEquals(6.0, calculator.eval("10", "-", "4"));
    }

    @Test
    void multiplicationReturnsProduct() {
        assertEquals(21.0, calculator.eval("3", "*", "7"));
    }

    @Test
    void divisionReturnsQuotient() {
        assertEquals(2.0, calculator.eval("10", "/", "5"));
    }

    @Test
    void zeroDividedByNonZeroReturnsZero() {
        assertEquals(0.0, calculator.eval("0", "/", "5"));
    }

    @Test
    void divisionByZeroIsRejected() {
        assertThrows(ArithmeticException.class, () -> calculator.eval("8", "/", "0"));
    }

    @Test
    void zeroDividedByZeroIsRejected() {
        assertThrows(ArithmeticException.class, () -> calculator.eval("0", "/", "0"));
    }

    @Test
    void negativeOperandsAreHandled() {
        assertEquals(-2.0, calculator.eval("-5", "+", "3"));
    }

    @Test
    void decimalOperandsAreHandled() {
        assertEquals(3.0, calculator.eval("1.5", "*", "2"));
    }

    @Test
    void blankOperandIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.eval("", "+", "2"));
    }

    @Test
    void unknownOperatorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.eval("2", "%", "3"));
    }

    @Test
    void nonNumericOperandIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.eval("abc", "+", "2"));
    }
}
