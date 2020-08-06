package net.kenevans.ecgutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.ListIterator;

import javax.swing.JOptionPane;

/**
 * ReadEliteHRV reads Elite HRV RR data files and generates a CSV summary file
 * and optionally HxM files for Bluetooth Cardiac Monitor (BCM). The summary
 * file has the name given in outName, typically EliteHRVData.csv. It should be
 * renamed with a date. An ODS file with a plot can be generated by taking an
 * old ODS file and adding the new data. There should be a Directions sheet in
 * the old ODS file for how to do the whole procedure.
 * 
 * The output from Elite HRV has changed: It was formerly a CSV file, is now a
 * TXT file with RR values for each session.
 * 
 * @author Kenneth Evans, Jr.
 */
public class ReadEliteHRV
{
    private static final String LS = Utils.LS;
    /**
     * How the data are stored: .txt files (current) or a CSV file (earlier).
     */
    // private static final ReadMode readMode = ReadMode.CSV;
    private static final ReadMode readMode = ReadMode.TXT;
    /**
     * In TEST mode the input and output files and directories are different.
     * 
     * @see #inTxtDir
     * @see #inCsvName
     * @see #outName
     * @see #HXM_FILE_DIR
     */
    private static final boolean TEST = false;
    /** Whether to write HxM files for each line. */
    private static final boolean WRITE_HXM_FILE = true;
    /** Whether to filter outliers from the RR values. */
    private static final boolean FILTER_OUTLIERS = false;
    /** The threshold for marking outliers. */
    private static final double THRESHOLD = .10;
    /** The moving average window for marking outliers. */
    private static final int HWIN = 20;
    // Old data needed to be converted to ms
    // private static final double RR_RAW_CONVERSION_FACTOR = 1.024;
    // Data as of 2016-08-17 are ms and don't need conversion
    private static final double RR_RAW_CONVERSION_FACTOR = 1;
    private static final String HXM_CSV_TEMPLATE = "ECG-%04d-%02d-%02d-%02d-%02d-%02d-EliteHRV.csv";
    private static final String HXM_CSV_TEMPLATE_FILTERED = "ECG-%04d-%02d-%02d-%02d-%02d-%02d-EliteHRVFiltered.csv";
    /** Where to write HxM files. */
    private static final String HXM_FILE_DIR = TEST
        ? "C:/Scratch/ECG/Elite HRV/TEST"
        : "C:/Scratch/ECG/Elite HRV";
    private static final boolean PROMPT_TO_OVERWRITE_HXM_FILE = false;
    private static int MOVING_AVG_LENGTH = 5;
    private static final double AHRV = -.65;
    private static final double BHRV = 15.6;
    // Using tabs makes it easier to read
    // But makes spreadsheet calculations difficult
    // private static final String DELIMITER = ",\t";
    private static final String DELIMITER = ",";

    /**
     * Directory where .txt files are found. Depends on the value of TEST.
     * 
     * @see #TEST
     */
    private static final String inTxtDir = TEST
        // TODO Change this if necessary
        ? "C:/Scratch/ECG/Elite HRV/TXT"
        : "C:/Scratch/ECG/Elite HRV/TXT";
    /**
     * Name of the input CSV file. Depends on the value of TEST.
     * 
     * @see #TEST
     */
    private static final String inCsvName = TEST
        // ? "C:/Scratch/ECG/Elite HRV/export.test.csv"
        ? "C:/Scratch/ECG/Elite HRV/export.102115.csv"
        : "C:/Scratch/ECG/Elite HRV/export.csv";
    /**
     * Name of the output CSV file. Depends on the value of TEST.
     * 
     * @see #TEST
     */
    private static final String outName = TEST
        ? "C:/Scratch/ECG/Elite HRV/EliteHRVData.test." + readMode + ".csv"
        : "C:/Scratch/ECG/Elite HRV/EliteHRVData.csv";

    private static int nWriteHxMErrors;
    private static int nHxMFilesWritten;
    private static int nErrors;

    /**
     * ReadMode Ways the data are stored.
     * 
     * @author Kenneth Evans, Jr.
     */
    private static enum ReadMode {
        CSV, TXT
    };

    /**
     * Process from multiple text files.
     */
    public static void processTxt() {
        System.out.println("TXT Mode");
        System.out.println("Input Directory: " + inTxtDir);
        System.out.println("Output: " + outName);
        System.out
            .println("RR Raw Conversion Factor: " + RR_RAW_CONVERSION_FACTOR);
        nWriteHxMErrors = 0;
        nHxMFilesWritten = 0;
        nErrors = 0;
        // Check if parameters are ok
        if(!checkParameters()) {
            return;
        }
        int nFiles = 0;
        BufferedReader in = null;
        PrintWriter out = null;
        int[] rrVals;
        ArrayList<Integer> rrInList = null;
        String timeStamp = null;
        String curFile = "Starting";
        try {
            out = new PrintWriter(new FileWriter(outName));
            out.write(
                "Time,MinHr,AvgHR,MaxHr,RMSSD,LnRMSSD,SDNN,NN50,PNN50,nRRVals,HRV"
                    + LS);
            String line;
            int rr;
            // Get the files
            File inDir = new File(inTxtDir);
            File[] txtFiles = inDir.listFiles();
            for(File file : txtFiles) {
                if(file.isDirectory()) {
                    continue;
                }
                curFile = file.getPath();
                // Get the timestamp from the file name
                timeStamp = timeStampFromFile(file);
                if(timeStamp == null) {
                    nErrors++;
                    continue;
                }
                nFiles++;
                in = new BufferedReader(new FileReader(file));
                // Get the RR values
                rrInList = new ArrayList<Integer>();
                while((line = in.readLine()) != null) {
                    if(line.length() == 0) continue;
                    rr = Integer.parseInt(line);
                    rrInList.add(rr);
                }
                in.close();
                rrVals = new int[rrInList.size()];
                int i = 0;
                for(int iVal : rrInList) {
                    rrVals[i++] = iVal;
                }
                processSession(out, timeStamp, rrVals);
            }
            out.close();
            System.out.println("Total files processed: " + +nFiles + " "
                + "Non-matching files: " + nErrors);
            if(WRITE_HXM_FILE) {
                System.out.println("FilesWritten: " + nHxMFilesWritten + " "
                    + "Bad files: " + nWriteHxMErrors);
            }
        } catch(Exception ex) {
            System.err.println("Error at file " + curFile);
            ex.printStackTrace();
        }
    }

    /**
     * Process from a single CSV file.
     */
    public static void processCSV() {
        System.out.println("CSV Mode");
        System.out.println("Input: " + inCsvName);
        System.out.println("Output: " + outName);
        System.out
            .println("RR Raw Conversion Factor: " + RR_RAW_CONVERSION_FACTOR);
        nWriteHxMErrors = 0;
        nHxMFilesWritten = 0;
        nErrors = 0;
        // Check if parameters are ok
        if(!checkParameters()) {
            return;
        }
        int lineNum = 0;
        BufferedReader in = null;
        PrintWriter out = null;
        String timeStamp = null;
        int nRrVals;
        int[] rrVals;
        try {
            in = new BufferedReader(new FileReader(inCsvName));
            out = new PrintWriter(new FileWriter(outName));
            out.write(
                "Time,MinHr,AvgHR,MaxHr,RMSSD,LnRMSSD,SDNN,NN50,PNN50,nRRVals,HRV"
                    + LS);
            String line;
            String[] tokens;
            String[] rrTokens;
            while((line = in.readLine()) != null) {
                lineNum++;
                if(line.length() == 0) continue;
                // Regex to read CSV files with some values quoted
                tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                // First token is the date and time
                // Second token is a comma-delimited string of raw RR values
                // Other tokens may be user tags
                if(tokens.length < 2) {
                    nErrors++;
                    System.out.println("Error at line " + lineNum);
                    System.out.println("  " + line);
                    continue;
                }
                // Get the rr values
                rrTokens = tokens[1].replaceAll("\"", "").split(",");
                nRrVals = rrTokens.length;
                if(nRrVals < 2) {
                    nErrors++;
                    System.out
                        .println("Fewer than 2 RR values at line " + lineNum);
                    System.out.println("  " + line);
                    continue;
                }

                rrVals = new int[nRrVals];
                for(int i = 0; i < nRrVals; i++) {
                    rrVals[i] = Integer.parseInt(rrTokens[i]);
                }
                timeStamp = tokens[0];
                processSession(out, timeStamp, rrVals);
            }
            in.close();
            out.close();
            System.out.println("Input: " + inCsvName);
            System.out.println("Output: " + outName);
            System.out.println(
                "RR Raw Conversion Factor: " + RR_RAW_CONVERSION_FACTOR);
            System.out.println(
                "Lines processed: " + lineNum + " " + "Bad lines: " + nErrors);
            if(WRITE_HXM_FILE) {
                System.out.println("FilesWritten: " + nHxMFilesWritten + " "
                    + "Bad files: " + nWriteHxMErrors);
            }
        } catch(Exception ex) {
            System.err.println("Error at line " + lineNum);
            ex.printStackTrace();
        }
    }

    /**
     * Processes one session from either a .txt file or a line in a CSV file.
     * 
     * @param out
     * @param timeStamp
     * @param rrIntVals
     */
    public static void processSession(PrintWriter out, String timeStamp,
        int[] rrIntVals) {
        int nRrVals = rrIntVals.length;
        double[] rrVals = new double[nRrVals];
        double[] hrVals = new double[nRrVals];
        double[] rrVals1 = null;
        double[] hrVals1 = null;
        double[] sdVals1 = null;
        Statistics rrStats = null;
        Statistics hrStats = null;
        Statistics sdStats = null;
        double sdnn, rmssd, minHr, maxHr, avgHr, lnRmssd, pnn50;
        long hrv;
        int nn50;
        int j = 0;
        for(int val : rrIntVals) {
            rrVals[j] = val * RR_RAW_CONVERSION_FACTOR;
            hrVals[j] = 60000. / rrVals[j];
            j++;
        }

        // Mark outliers
        ArrayList<MarkedDouble> rrList = MarkedDouble
            .makeMarkedDoubleArray(rrVals);
        if(FILTER_OUTLIERS) {
            ECGUtils.markOutliers(rrList, THRESHOLD, HWIN);
        }

        // Find out how many unmarked values there are
        ListIterator<MarkedDouble> li = rrList.listIterator();
        MarkedDouble md;
        int nRrVals1 = 0;
        while(li.hasNext()) {
            md = li.next();
            if(!md.getMarked()) {
                nRrVals1++;
            }
        }
        rrVals1 = new double[nRrVals1];
        hrVals1 = new double[nRrVals1];
        li = rrList.listIterator();
        int ii = 0, jj = 0;
        while(li.hasNext()) {
            md = li.next();
            if(!md.getMarked()) {
                rrVals1[jj] = rrVals[ii];
                hrVals1[jj] = hrVals[ii];
                jj++;
            }
            ii++;
        }

        // Calculate the successive differences
        sdVals1 = new double[nRrVals1 - 1];
        nn50 = 0;
        for(int i = 1; i < nRrVals1; i++) {
            sdVals1[i - 1] = rrVals1[i] - rrVals1[i - 1];
            if(Math.abs(sdVals1[i - 1]) > 50) {
                nn50++;
            }
        }
        pnn50 = (double)nn50 / (nRrVals1 - 1);

        rrStats = new Statistics(rrVals1);
        hrStats = new Statistics(hrVals1);
        sdStats = new Statistics(sdVals1);
        minHr = hrStats.getMin();
        avgHr = hrStats.getMean();
        maxHr = hrStats.getMax();
        sdnn = rrStats.getSigma();
        rmssd = sdStats.getRms();
        lnRmssd = Math.log(rmssd);
        hrv = Math.round(AHRV + BHRV * lnRmssd);

        // DEBUG
        // System.out.print("RR:");
        // for(int i = 0; i < nRrVals1; i++) {
        // System.out.printf(" %8.2f", rrVals1[i]);
        // }
        // System.out.println();
        // System.out.print("SD:");
        // for(int i = 0; i < nRrVals1 - 1; i++) {
        // System.out.printf(" %8.2f", sdVals1[i]);
        // }
        // System.out.println();
        // System.out.printf("RMSSD=%.2f" + LS, rmssd);
        // System.out.printf("Min%%=%.2f Max%%=%.2f " + LS,
        // 100. * rrStats.getMin() / rrStats.getMean(),
        // 100. * rrStats.getMax() / rrStats.getMean());

        out.write(timeStamp);
        out.write(DELIMITER + String.format("%.2f", minHr));
        out.write(DELIMITER + String.format("%.2f", avgHr));
        out.write(DELIMITER + String.format("%.2f", maxHr));
        out.write(DELIMITER + String.format("%.2f", rmssd));
        out.write(DELIMITER + String.format("%.2f", lnRmssd));
        out.write(DELIMITER + String.format("%.2f", sdnn));
        out.write(DELIMITER + String.format("%d", nn50));
        out.write(DELIMITER + String.format("%.2f", pnn50 * 100));
        out.write(DELIMITER + String.format("%d", nRrVals1));
        out.write(DELIMITER + String.format("%d", hrv));
        out.write(LS);

        if(WRITE_HXM_FILE) {
            nHxMFilesWritten++;
            //
            boolean res = writeHxMFile(timeStamp, rrList);
            if(!res) {
                nWriteHxMErrors++;
            }
        }
    }

    /**
     * Writes a session file using the RR values.
     * 
     * @return
     */
    public static boolean writeHxMFile(String startTimeString,
        ArrayList<MarkedDouble> rrVals) {
        // The startTime is of the form: "2015-03-09 22:50:02"
        // 012345678901234567890
        int year = Integer.parseInt(startTimeString.substring(1, 5));
        int month = Integer.parseInt(startTimeString.substring(6, 8));
        int day = Integer.parseInt(startTimeString.substring(9, 11));
        int hour = Integer.parseInt(startTimeString.substring(12, 14));
        int min = Integer.parseInt(startTimeString.substring(15, 17));
        int sec = Integer.parseInt(startTimeString.substring(18, 20));

        // Start time
        GregorianCalendar cal = new GregorianCalendar(year, month - 1, day,
            hour, min, sec);
        // System.out.println(sdf.format(cal.getTime()));
        long startTime = cal.getTimeInMillis();

        // Output file
        String template = HXM_CSV_TEMPLATE;
        if(FILTER_OUTLIERS) {
            template = HXM_CSV_TEMPLATE_FILTERED;
        }
        String hxmName = String.format(template, year, month, day, hour, min,
            sec);
        File hxmFile = new File(HXM_FILE_DIR, hxmName);
        if(PROMPT_TO_OVERWRITE_HXM_FILE && hxmFile.exists()) {
            int result = JOptionPane.showConfirmDialog(null,
                "File exists:" + LS + hxmFile.getPath() + LS
                    + "OK to overwrite?",
                "File Exists", JOptionPane.OK_CANCEL_OPTION);
            if(result != JOptionPane.OK_OPTION) {
                return false;
            }
        }

        // Make the rrList, which is the start times
        double curTime = startTime;
        ArrayList<MarkedDouble> rrTimes = new ArrayList<MarkedDouble>();
        rrTimes.add(new MarkedDouble(curTime));
        ListIterator<MarkedDouble> li = rrVals.listIterator();
        MarkedDouble md;
        while(li.hasNext()) {
            md = li.next();
            // Has to be in real time units
            // Compensate for RR_RAW_CONVERSTION_FACTOR
            curTime += md.getVal() / RR_RAW_CONVERSION_FACTOR;
            rrTimes.add(new MarkedDouble(curTime, md.getMarked()));
        }
        ECGUtils.CreateHxMFileReturn res = ECGUtils.writeHxMFile(hxmFile,
            startTime, rrTimes, MOVING_AVG_LENGTH);
        return res.getRetVal();
    }

    /**
     * Generates a time stamp String from a file name.
     * 
     * @param file The name of the file.
     * @return null if not of the form yyyy-mm-dd hh-mm-ss.txt
     */
    private static String timeStampFromFile(File file) {
        String ext = Utils.getExtension(file);
        if(!ext.toLowerCase().equals("txt")) {
            return null;
        }
        String fileName = file.getName();
        // Must be of the form yyyymmddhhmmss.txt
        if(fileName.length() != 23) {
            return null;
        }
        String timeStamp = null;
        try {
            int year = Integer.parseInt(fileName.substring(0, 4));
            int month = Integer.parseInt(fileName.substring(5, 7));
            int day = Integer.parseInt(fileName.substring(8, 10));
            int hour = Integer.parseInt(fileName.substring(11, 13));
            int min = Integer.parseInt(fileName.substring(14, 16));
            int sec = Integer.parseInt(fileName.substring(17, 19));
            timeStamp = String.format("\"%04d-%02d-%02d %02d:%02d:%02d\"", year,
                month, day, hour, min, sec);
        } catch(Exception ex) {
            return null;
        }
        return timeStamp;
    }

    // /**
    // * Generates a time stamp String from a file name. Uses the old filename
    // * format.
    // *
    // * @param file The name of the file.
    // * @return null if not of the form yyyymmddhhmmss.txt
    // */
    // private static String timeStampFromFile0(File file) {
    // String ext = Utils.getExtension(file);
    // if(!ext.toLowerCase().equals("txt")) {
    // return null;
    // }
    // String fileName = file.getName();
    // // Must be of the form yyyymmddhhmmss.txt
    // if(fileName.length() != 18) {
    // return null;
    // }
    // String timeStamp = null;
    // try {
    // int year = Integer.parseInt(fileName.substring(0, 4));
    // int month = Integer.parseInt(fileName.substring(4, 6));
    // int day = Integer.parseInt(fileName.substring(6, 8));
    // int hour = Integer.parseInt(fileName.substring(8, 10));
    // int min = Integer.parseInt(fileName.substring(10, 12));
    // int sec = Integer.parseInt(fileName.substring(12, 14));
    // timeStamp = String.format("\"%04d-%02d-%02d %02d:%02d:%02d\"", year,
    // month, day, hour, min, sec);
    // } catch(Exception ex) {
    // return null;
    // }
    // return timeStamp;
    // }

    /**
     * Checks hard-coded parameters are ok.
     * 
     * @return
     */
    private static boolean checkParameters() {
        if(WRITE_HXM_FILE) {
            System.out.println("Writing HxM files");
            File hxmDir = new File(HXM_FILE_DIR);
            System.out.println("HxM Output Directory: " + hxmDir.getPath());
            if(!hxmDir.exists()) {
                System.out.println("HxM Output Directory does not exist!");
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        switch(readMode) {
        case TXT:
            processTxt();
            break;
        case CSV:
            processCSV();
            break;
        }
    }

}
