package org.shijing.asrc.client;

public class DoubleData implements IData {
    private final double value;

    public DoubleData(double value) {
        this.value = value;
    }

    public static DoubleData fromString(String raw) {
        try {
            return new DoubleData(Double.valueOf(raw));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot cast '" + raw + "' to double");
        }
    }

    @Override
    public DataType getType() {
        return DataType.DOUBLE;
    }

    @Override
    public String extKey() {
        return null;
    }

    @Override
    public int compareTo(IData rhs) {
        return Double.compare(value, rhs.toDouble());
    }

    @Override
    public boolean toBool() {
        return value != 0;
    }

    @Override
    public int toInt() {
        return new Double(value).intValue();
    }

    @Override
    public double toDouble() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoubleData that = (DoubleData) o;

        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(value);
        return (int) (temp ^ (temp >>> 32));
    }
}
