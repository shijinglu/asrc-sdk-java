package org.shijing.asrc.client;

public interface IData {
    enum DataType {
        BOOL,
        INTEGER,
        DOUBLE,
        STRING,
        CUSTOMIZED
    }

    DataType getType();

    boolean toBool();

    int toInt();

    double toDouble();

    String extKey();

    int compareTo(IData rhs);
}
