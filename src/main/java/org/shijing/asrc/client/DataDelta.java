package org.shijing.asrc.client;

import java.util.Objects;

public class DataDelta {
    public enum DeltaType {
        UNCHANGED,
        ADDITION,
        DELETION,
        UPDATE
    }

    public final DeltaType deltaType;
    public final IData oldData;
    public final IData nowData;

    public DataDelta(IData oldData, IData nowData, DeltaType deltaType) {
        this.deltaType = deltaType;
        this.oldData = oldData;
        this.nowData = nowData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataDelta delta = (DataDelta) o;

        if (deltaType != delta.deltaType) return false;
        if (!Objects.equals(oldData, delta.oldData)) return false;
        return Objects.equals(nowData, delta.nowData);
    }

    @Override
    public int hashCode() {
        int result = deltaType.hashCode();
        result = 31 * result + (oldData != null ? oldData.hashCode() : 0);
        result = 31 * result + (nowData != null ? nowData.hashCode() : 0);
        return result;
    }
}
