package dev.lapysh.core.model;

public class ContinuousAverage {
    public long n = 0;
    public double average = 0;

    public ContinuousAverage() {
    }

    public ContinuousAverage(long n, double average) {
        this.n = n;
        this.average = average;
    }

    public ContinuousAverage addValue(double value) {
        // Update the average of a continuous sequence of numbers in constant time
        // https://stackoverflow.com/a/53618572/4386227
        average = average + (value - average) / ++n;
        return this;
    }

    public ContinuousAverage addValue(int value) {
        // Update the average of a continuous sequence of numbers in constant time
        // https://stackoverflow.com/a/53618572/4386227
        average = average + (value - average) / ++n;
        return this;
    }

}
