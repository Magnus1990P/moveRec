package com.example.maov.moverec;

/**
 * Created by maov on 09.09.2015.
 */
public class Data {
    private int ID, N;
    private float X, Y, Z;
    private long T;

    public Data(int i, int n, float x, float y, float z, long t) {
        ID = i;
        N = n;
        T = t;
        X = x;
        Y = y;
        Z = z;
    }

    public String csv( ){
        return Integer.toString(ID) + "," + Integer.toString(N) + "," +
                Long.toString(T)  + "," + Float.toString(X) + "," +
                Float.toString(Y)  + "," + Float.toString(Z);

    }
}
