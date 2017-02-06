package net.kenevans.ecgutils;

import java.util.ArrayList;

/*
 * Created on Jun 26, 2015
 * By Kenneth Evans, Jr.
 */

/**
 * Class to hold a double value and a flag to indicate whether it is marked or
 * not.
 * 
 * @author Kenneth Evans, Jr.
 */
public class MarkedDouble
{
    private double val;
    private boolean marked;

    /**
     * MarkedDouble constructor that sets marked to false;
     * 
     * @param val
     */
    MarkedDouble(double val) {
        this.val = val;
        this.marked = false;
    }

    /**
     * Makes an ArrayList<MarkedDouble> from the given vals. The values are not
     * marked.
     * 
     * @param vals
     * @return
     */
    public static ArrayList<MarkedDouble> makeMarkedDoubleArray(double[] vals) {
        int nVals = vals.length;
        ArrayList<MarkedDouble> list = new ArrayList<MarkedDouble>(nVals);
        for(int i = 0; i < vals.length; i++) {
            list.add(new MarkedDouble(vals[i]));
        }
        return list;
    }

    /**
     * MarkedDouble constructor.
     * 
     * @param val
     * @param marked
     */
    MarkedDouble(double val, boolean marked) {
        this.val = val;
        this.marked = marked;
    }

    /**
     * @return The value of marked.
     */
    public boolean getMarked() {
        return marked;
    }

    /**
     * @param marked The new value for marked.
     */
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    /**
     * @return The value of val.
     */
    public double getVal() {
        return val;
    }

}
