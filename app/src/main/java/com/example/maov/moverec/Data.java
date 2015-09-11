package com.example.maov.moverec;

/**
 * Created by maov on 09.09.2015.
 */
public class Data {
    private int ID, N;
    private float aX, aY, aZ;
    private float gX, gY, gZ;
    private long T;

    private int[] cont = {0, 0, 0};

    public Data(int i, int n, long t) {
        ID = i;     N  = n;     T  = t;
        aX = aY = aZ = 0;
        gX = gY = gZ = 0;
        cont[0] = 1;    //init
        cont[1] = -1;   //acc
        cont[2] = -1;   //gyro
    }

    public String csv( ){
        return Integer.toString(ID) + "," + Integer.toString(N) + "," + Long.toString(T)    + "," +
                Float.toString(aX)  + "," + Float.toString(aY)  + "," + Float.toString(aZ)  + "," +
                Float.toString(gX)  + "," + Float.toString(gY)  + "," + Float.toString(gZ)  + "\n";
    }

    public boolean isGyroSet(){
        return (cont[2] == 1);
    }

    public boolean isAccSet(){
        return (cont[1] == 1);
    }

    public void addAcc(float[] acc){
        aX = acc[0];
        aY = acc[1];
        aZ = acc[2];
        cont[1] = 1;
    }
    public void addGyro(float[] gyro){
        gX = gyro[0];
        gY = gyro[1];
        gZ = gyro[2];
        cont[2] = 1;
    }
}
