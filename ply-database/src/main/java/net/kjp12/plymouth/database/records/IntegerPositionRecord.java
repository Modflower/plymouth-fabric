package net.kjp12.plymouth.database.records;// Created 2021-07-04T07:23

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class IntegerPositionRecord implements SQLData {
    private int x, y, z, d;

    public IntegerPositionRecord() {
    }

    public IntegerPositionRecord(int x, int y, int z, int d) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.d = d;
    }

    @Override
    public String getSQLTypeName() {
        return "ipos";
    }

    @Override
    public void readSQL(SQLInput stream, String typeName) throws SQLException {
        this.x = stream.readInt();
        this.y = stream.readInt();
        this.z = stream.readInt();
        this.d = stream.readInt();
    }

    @Override
    public void writeSQL(SQLOutput stream) throws SQLException {
        stream.writeInt(x);
        stream.writeInt(y);
        stream.writeInt(z);
        stream.writeInt(d);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getD() {
        return d;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setD(int d) {
        this.d = d;
    }
}
