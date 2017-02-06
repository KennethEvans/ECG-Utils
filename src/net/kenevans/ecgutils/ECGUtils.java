package net.kenevans.ecgutils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;

/**
 * ECGUtils is a class that provides utilities for ECG calculations.
 * 
 * @author Kenneth Evans, Jr.
 */
public class ECGUtils
{
    public static final String LS = System.getProperty("line.separator");
    private static SimpleDateFormat hxmDateTimeFormat = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * @return The value of hxmDateTimeFormat.
     */
    public static SimpleDateFormat getHxMDateTimeFormat() {
        return hxmDateTimeFormat;
    }

    /**
     * Writes an HxM file using the given parameters. The endTime and return
     * value can be obtained from the CreateHxMFileReturn.
     * 
     * @param file The file to write.
     * @param startTime The start time of the session.
     * @param rrTimeList A list of HB times (not RR intervals).
     * @param movingAveragelength Length of the moving average for the HR
     *            calculation.
     * @return CreateHxMFileReturn containing the return value and endTime.
     */
    public static CreateHxMFileReturn writeHxMFile(File file, long startTime,
        ArrayList<MarkedDouble> rrTimeList, int movingAveragelength) {
        boolean retVal = true;
        PrintWriter out = null;
        MovingAverage ma = new MovingAverage(movingAveragelength);
        String timeStamp = null;
        Date date = null;
        long endTime = startTime;
        long longVal;
        long hr;
        long rr;
        boolean first = true;
        double dVal;
        double lastDVal = 0;
        try {
            out = new PrintWriter(new FileWriter(file));
            for(MarkedDouble md : rrTimeList) {
                dVal = md.getVal();
                if(first) {
                    first = false;
                    lastDVal = dVal;
                    continue;
                }
                longVal = Math.round(dVal);
                // Need to convert to BLE units of 1/1024 sec.
                rr = Math.round((dVal - lastDVal) * 1.024);
                ma.newNum(60000. / (dVal - lastDVal));
                hr = Math.round(ma.getAvg());
                // // DEBUG
                // System.out.println("hr=" + (60000. / (dVal - lastDVal))
                // + "  avg=" + ma.getAvg());
                endTime = startTime + longVal;
                date = new Date(endTime);
                timeStamp = hxmDateTimeFormat.format(date);
                if(!md.getMarked()) {
                    out.printf("%s,%d,%d,-1,-1" + LS, timeStamp, hr, rr);
                } else {
                    // Output a blank line
                    out.println();
                }
                lastDVal = dVal;
            }
        } catch(Exception ex) {
            Utils.excMsg("Error writing HxM file", ex);
            ex.printStackTrace();
            retVal = false;
        } finally {
            if(out != null) out.close();
        }
        if(retVal) {
            System.out.println();
            System.out.println("Wrote " + file.getPath());
        } else {
            System.out.println();
            System.out.println("Error writing " + file.getPath());
        }
        return new CreateHxMFileReturn(retVal, endTime);
    }

    /**
     * Marks the input list as to whether the value is within plus or minus
     * threshold times the moving average of the surrounding points. That is,
     * points outside the range [avg*(1-threshold), avg*(1+threshold] are
     * marked.
     * 
     * @param vals A MarkedDouble list of values. Typically the values will be
     *            unmarked on entry.
     * @param threshold The threshold.
     * @param hwin The half window. The full window for the moving average is
     *            2*hwin + 1;
     * @return If successful or not.
     */
    public static boolean markOutliers(ArrayList<MarkedDouble> vals,
        double threshold, int hwin) {
        if(vals == null) {
            Utils.errMsg("markOutliersFilter: List is null.");
            return false;
        }

        int nVals = vals.size();
        if(nVals == 0) return true;

        // Initialize the moving average to the first win values or all the data
        // if win > nVals.
        int win = 2 * hwin + 1;
        int nProc = (win < nVals) ? win : nVals;
        MovingAverage ma = new MovingAverage(win);
        ListIterator<MarkedDouble> li = vals.listIterator();
        while(li.hasNext()) {
            ma.newNum(li.next().getVal());
            if(li.nextIndex() == nProc) {
                break;
            }
        }
        // Loop over the data
        MarkedDouble md;
        double dVal, avg, maxVal, minVal;
        li = vals.listIterator();
        int curIndex = li.nextIndex();
        md = li.next();
        dVal = md.getVal();
        while(true) {
            avg = ma.getAvg();
            minVal = (1.0 - threshold) * avg;
            maxVal = (1.0 + threshold) * avg;
            if(dVal > maxVal || dVal < minVal) {
                md.setMarked(true);
            }
            // // DEBUG
            // System.out.printf(
            // "%d x=%.2f nProc=%d avg=%.2f minVal=%.2f maxVal=%.2f %s" + LS,
            // curIndex, dVal, nProc, avg, minVal, maxVal, md.getMarked());
            // Set up for the next value
            if(!li.hasNext()) {
                break;
            }
            curIndex = li.nextIndex();
            md = li.next();
            dVal = md.getVal();
            // Advance the moving average
            if(curIndex > hwin && curIndex <= nVals - hwin - 1) {
                nProc++;
                ma.newNum(dVal);
            }
        }
        return true;
    }

    public static class CreateHxMFileReturn
    {
        boolean retVal;
        long endTime;

        public CreateHxMFileReturn(boolean retVal, long endTime) {
            this.retVal = retVal;
            this.endTime = endTime;
        }

        /**
         * @return The value of retVal.
         */
        public boolean getRetVal() {
            return retVal;
        }

        /**
         * @return The value of endTime.
         */
        public long getEndTime() {
            return endTime;
        }

    }

    /**
     * The main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // Test markOutliers 1
        double[] vals1 = {9, 8, 7, 8, 9, 12, 7, 8, 9, 8};
        // double[] vals2 = {9, 12, 8};
        // double[] vals3 = {9};
        // double[] vals4 = {};
        // double[][] set = {vals1, vals2, vals3, vals4};
        double[][] set = {vals1};
        boolean res;
        double[] vals;
        double frac;
        int hwin;
        // ListIterator<MarkedDouble> li;
        // MarkedDouble md;
        for(int i = 0; i < set.length; i++) {
            System.out.println("processing set " + i);
            vals = set[i];
            ArrayList<MarkedDouble> list = MarkedDouble
                .makeMarkedDoubleArray(vals);
            frac = .2;
            hwin = 10;
            res = markOutliers(list, frac, hwin);
            System.out
                .println("frac=" + frac + " hwin=" + hwin + " res=" + res);
            if(!res) {
                System.out.println("markOutliers failed for vals" + i);
            }
            // li = list.listIterator();
            // while(li.hasNext()) {
            // md = li.next();
            // System.out.println("val=" + md.getVal() + " " + md.getMarked());
            // }
            System.out.println();
        }

        System.out.println();
        System.out.println("All Done");
    }

}
