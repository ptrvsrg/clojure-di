package com.example.di;

import javax.inject.Inject;

public class DataProcessor {
    private final Calculator calculator;

    @Inject
    public DataProcessor(Calculator calculator) {
        this.calculator = calculator;
    }

    public double process(double x, double y) {
        return calculator.multiply(x, y) + calculator.add(x, y);
    }

    public Calculator getCalculator() {
        return this.calculator;
    }
}
