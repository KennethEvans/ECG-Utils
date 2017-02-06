package net.kenevans.ecgutils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/*
 * Created on Mar 3, 2015
 * By Kenneth Evans, Jr.
 */

public class MakeHxMSessionFromPDFImage
{
    public static final String LS = System.getProperty("line.separator");
    public static boolean VERBOSE = true;
    private static int MOVING_AVG_LENGTH = 5;
    private static final String ALIVECOR_SHARE_PREFIX = "pdf_share_";
    private static final String HXM_CSV_TEMPLATE = "ECG-%04d-%02d-%02d-%02d-%02d-%02d-AliveCor.csv";
    private static final String HXM_TRIM_PREFIX_TEMPLATE = "ECG-%04d-%02d-%02d-%02d-%02d-%02d-";
    private static final String HXM_TRIM_SUFFIX_TEMPLATE = "%s.csv";
    /** Where to look for image files. */
    private static final String IMAGE_FILE_DIR = "C:/Scratch/ECG/AliveCor ECGs/Images/";
    /**
     * Where to look for HxM files. Currently used for the output directory for
     * both the output and the trimmed file.
     */
    private static final String HXM_FILE_DIR = "C:/Scratch/ECG/Android/SCH-I545/Current/HxM Monitor/";
    /**
     * Where to look for BCM files. Currently used for finding the file to trim.
     */
    private static final String BCM_FILE_DIR = "C:/Scratch/ECG/Android/SCH-I545/Current/BLE Cardiac Monitor/";
    public static final String SAVE_SESSION_DELIM = ",";
    private static int rrYVals[] = {1064, 1773, 2481, 3190};
    private static int YOFFSET = 20;

    private String imageNamePrefix;
    private String trimNamePrefix;
    private static double T0 = .6;
    private static double X0 = 93;
    private static double X1 = 2456;
    private Calibration calibration = new Calibration(X0, X1, T0);
    private ArrayList<MarkedDouble> rrList = new ArrayList<MarkedDouble>();
    private File hxmFile;
    private File hxmTrimInputFile;
    private File hxmTrimFile;
    private File[] imageFiles;
    private long startTime;
    private long endTime;

    /**
     * Runs the steps to read the image, write the AliceCor file, and create a
     * new trimmed file.
     * 
     * @return If successful or not.
     */
    public boolean run() {
        if(!open()) {
            return false;
        }
        if(!parse()) {
            return false;
        }
        if(!writeHxMFile()) {
            return false;
        }
        if(!openHxMTrimFile()) {
            return false;
        }
        if(!writeHxMTrimFile()) {
            return false;
        }
        return true;
    }

    /**
     * Brings up a JFileChooser to pick an image file and calls
     * initializeNameBasedItems.
     * 
     * @return If successful or not.
     */
    public boolean open() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(IMAGE_FILE_DIR));
        chooser.setDialogTitle("Open an Image File");
        int result = chooser.showOpenDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return false;
        // // Save the selected path for next time
        // defaultPath = chooser.getSelectedFile().getParentFile().getPath();
        // Process the file
        File file = chooser.getSelectedFile();
        initializeNameBasedItems(file);
        // boolean res = openFile(file);
        // if(!res) return false;
        return true;
    }

    /**
     * Initializes fields that depend on the data in the file name.
     * 
     * @param file
     * @return If successful or not.
     */
    public boolean initializeNameBasedItems(File file) {
        String name = file.getName();
        int pos = name.indexOf(".");
        if(pos == -1) {
            Utils.errMsg("Bad file name:" + LS + name);
            return false;
        }
        // File names are assumed to be of the form:
        // [prefix]ecg-yyyymmdd-hhmmss.nn.suffix.
        // The imageNamePrefix is [prefix]ecg-yyyymmdd-hhmmss
        imageNamePrefix = name.substring(0, pos);
        // If the name starts with the ALIVECOR_SHARE_PREFIX, remove it for
        // further processing
        if(name.startsWith(ALIVECOR_SHARE_PREFIX)) {
            name = name.substring(ALIVECOR_SHARE_PREFIX.length());
        }
        int year = Integer.parseInt(name.substring(4, 8));
        int month = Integer.parseInt(name.substring(8, 10));
        int day = Integer.parseInt(name.substring(10, 12));
        int hour = Integer.parseInt(name.substring(13, 15));
        int min = Integer.parseInt(name.substring(15, 17));
        int sec = Integer.parseInt(name.substring(17, 19));

        // Start time
        GregorianCalendar cal = new GregorianCalendar(year, month - 1, day,
            hour, min, sec);
        // System.out.println(sdf.format(cal.getTime()));
        startTime = cal.getTimeInMillis();

        // Output file
        String hxmName = String.format(HXM_CSV_TEMPLATE, year, month, day,
            hour, min, sec);
        hxmFile = new File(HXM_FILE_DIR, hxmName);
        if(hxmFile.exists()) {
            int result = JOptionPane.showConfirmDialog(null, "File exists:"
                + LS + hxmFile.getPath() + LS + "OK to overwrite?",
                "File Exists", JOptionPane.OK_CANCEL_OPTION);
            if(result != JOptionPane.OK_OPTION) {
                return false;
            }
        }

        // HxM Trim File
        trimNamePrefix = String.format(HXM_TRIM_PREFIX_TEMPLATE, year, month,
            day, hour, min, sec);

        // Input files
        imageFiles = file.getParentFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(file.getName().startsWith(imageNamePrefix)) {
                    return true;
                }
                return false;
            }
        });
        if(imageFiles == null || imageFiles.length == 0) {
            Utils.errMsg("No input files found");
            return false;
        }
        Arrays.sort(imageFiles);
        System.out.println("");
        System.out.println("Input files:");
        for(File file1 : imageFiles) {
            System.out.println(file1);
        }
        return true;
    }

    /**
     * Parses the image looking for R markers.
     * 
     * @return If successful or not.
     */
    public boolean parse() {
        // Loop over pages
        if(VERBOSE) {
            System.out.println();
        }
        File file = null;
        int rgb, y;
        int nMarkerPoints = 0;
        int markerSum = 0;
        double xMarker;
        int nBadMarkers = 0;
        int nOverlappingMarkers = 0;
        boolean processingMarker = false;
        int markerRGB;
        for(int page = 0; page < imageFiles.length; page++) {
            file = imageFiles[page];
            BufferedImage image = openImageFile(file);
            if(VERBOSE) {
                System.out.println("Processing " + file.getPath());
                // printInfo(image);
            }
            if(image == null) {
                Utils.errMsg("File for page " + page + " is null" + LS
                    + file.getPath());
                return false;
            }

            // Loop over the lines with markers
            if(page == 0) {
                markerRGB = 0xff000000;
            } else {
                markerRGB = 0xff333333;
            }
            for(int line = 0; line < 4; line++) {
                y = rrYVals[line];
                if(VERBOSE) {
                    System.out.println("Page " + page + " Line " + line + " y="
                        + y);
                }
                processingMarker = false;

                for(int x = 0; x < image.getWidth(); x++) {
                    rgb = image.getRGB(x, y);
                    // // DEBUG
                    // if(x < 350) {
                    // System.out.printf(" %08x", rgb);
                    // }
                    // if(rgb == 0xff000000) {
                    // System.out.print(" " + x);
                    // }
                    if(rgb == markerRGB) {
                        if(!processingMarker) {
                            processingMarker = true;
                            nMarkerPoints = 0;
                            markerSum = 0;
                        }
                        markerSum += x;
                        nMarkerPoints++;
                    } else {
                        if(processingMarker) {
                            processingMarker = false;
                            if(nMarkerPoints < 6) {
                                // Not a marker
                                continue;
                            } else if(nMarkerPoints > 9) {
                                // Should not happen
                                nBadMarkers++;
                                System.out.println("Bad marker " + nBadMarkers
                                    + ": page=" + page + " line=" + line
                                    + " nMarkerPoints=" + nMarkerPoints + "x="
                                    + x);
                                continue;
                            } else if(nMarkerPoints == 6) {
                                // Isolated marker
                                xMarker = (double)markerSum
                                    / (double)nMarkerPoints;
                            } else {
                                // Is overlapping a grid line
                                nOverlappingMarkers++;
                                // Check if the space above is a marker color
                                rgb = image.getRGB(x - 1, y - YOFFSET);
                                if(rgb == markerRGB) {
                                    // Not a grid line, must be the right edge
                                    xMarker = (6. * x - 1.) / 6.;
                                } else {
                                    // Is a gridLine, marker starts at left edge
                                    xMarker = (6. * (x - nMarkerPoints) + 15.) / 6.;
                                }
                                System.out.println("Overlapping marker "
                                    + nOverlappingMarkers + ": page=" + page
                                    + " line=" + line + " nMarerPoints="
                                    + nMarkerPoints + "x=" + x + " XMarker="
                                    + xMarker);
                            }
                            // Add the time in ms to the list
                            rrList.add(new MarkedDouble(calibration.time(
                                xMarker, line, page)));
                            if(VERBOSE) {
                                System.out.println(" "
                                    + xMarker
                                    + "\tt="
                                    + String.format("%.2f",
                                        calibration.time(xMarker, line, page)));
                            }
                        }
                    }
                }
            }
            // End of parsing
            if(nBadMarkers > 0) {
                Utils.errMsg("Found " + nBadMarkers
                    + " RR markers with width != 6 for" + LS + file.getPath());
                return false;
            }
        }
        return true;
    }

    /**
     * Reads the image file and creates a BufferedImage.
     * 
     * @param file
     * @return If successful or not.
     */
    public BufferedImage openImageFile(File file) {
        if(!file.exists()) {
            Utils.errMsg("File does not exist" + LS + file.getPath());
            return null;
        }
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
            if(image == null) {
                String msg = "Cannot read file:" + LS + file.getName() + LS;
                Utils.errMsg(msg);
            }
        } catch(Exception ex) {
            Utils.errMsg("Error processing file:" + LS
                + ((file != null) ? file.getName() : "null") + LS + ex + LS
                + ex.getMessage());
        }
        return image;
    }

    /**
     * Writes the AliveCor session file from the list of RR values.
     * 
     * @return
     */
    public boolean writeHxMFile() {
        ECGUtils.CreateHxMFileReturn res = ECGUtils.writeHxMFile(hxmFile,
            startTime, rrList, MOVING_AVG_LENGTH);
        endTime = res.getEndTime();
        return res.getRetVal();
    }

    /**
     * Prints information for the given image.
     * 
     * @param image
     */
    public void printInfo(BufferedImage image) {
        System.out.println(info(image));
    }

    /**
     * Gets an information String for the given image.
     * 
     * @param image
     */
    public String info(BufferedImage image) {
        String info = "";
        if(image == null) {
            info += "No image";
            return info;
        }
        // if(imageFile != null) {
        // info += imageFile.getPath() + LS;
        // info += imageFile.getName() + LS;
        // } else {
        // info += "Unknown file" + LS;
        // }
        // info += LS;
        info += image.getWidth() + " x " + image.getHeight() + LS;
        Map<String, String> types = new HashMap<String, String>();
        types.put("5", "TYPE_3BYTE_BGR");
        types.put("6", "TYPE_4BYTE_ABGR");
        types.put("7", "TYPE_4BYTE_ABGR_PRE");
        types.put("12", "TYPE_BYTE_BINARY");
        types.put("10", "TYPE_BYTE_GRAY");
        types.put("13", "TYPE_BYTE_INDEXED");
        types.put("0", "TYPE_CUSTOM");
        types.put("2", "TYPE_INT_ARGB");
        types.put("3", "TYPE_INT_ARGB_PRE");
        types.put("4", "TYPE_INT_BGR");
        types.put("1", "TYPE_INT_RGB");
        types.put("9", "TYPE_USHORT_555_RGB");
        types.put("8", "TYPE_USHORT_565_RGB");
        types.put("11", "TYPE_USHORT_GRAY");
        Integer type = new Integer(image.getType());
        String stringType = types.get(type.toString());
        if(stringType == null) stringType = "Unknown";
        info += "Type: " + stringType + " [" + type + "]" + LS;
        info += "Properties:" + LS;
        String[] props = image.getPropertyNames();
        if(props == null) {
            info += "  No properties found" + LS;
        } else {
            for(int i = 0; i < props.length; i++) {
                info += "  " + props[i] + ": " + image.getProperty(props[i])
                    + LS;
            }
        }
        info += "ColorModel:" + LS;
        // The following assumes a particular format for toString()
        String colorModel = image.getColorModel().toString();
        String[] tokens = colorModel.split(" ");
        String colorModelName = tokens[0];
        info += "  " + colorModelName + LS;
        info += "  ";
        for(int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            if(token.equals("=")) {
                i++;
                info += "= " + tokens[i] + LS + "  ";
            } else {
                info += token + " ";
            }
        }
        info += LS;

        // The profile is always sRGB as implemented.
        // // Find the ICC profile used
        // String desc = ImageUtils.getICCProfileName(image);
        // if(desc != null) {
        // info += "ICC Profile=" + desc + LS;
        // info += "  (This is what Java ImageIO is using and may not be" + LS
        // + "    the same as any embedded ICC profile in the file.)" + LS;
        // }

        return info;
    }

    /**
     * Brings up a JFileChooser to pick the session file to trim.
     * 
     * @return The File or null on failure.
     */
    public boolean openHxMTrimFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(BCM_FILE_DIR));
        chooser.setDialogTitle("Pick HxM File to Trim");
        int result = chooser.showOpenDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return false;

        hxmTrimInputFile = chooser.getSelectedFile();
        if(hxmTrimInputFile == null) {
            return false;
        }
        String hxmTrimName = trimNamePrefix
            + String.format(HXM_TRIM_SUFFIX_TEMPLATE, hxmTrimInputFile
                .getName().substring(0, 3));
        hxmTrimFile = new File(HXM_FILE_DIR, hxmTrimName);
        return true;
    }

    /**
     * Parses the untrimmed file and writes the trimmed file.
     * 
     * @return If successful.
     */
    public boolean writeHxMTrimFile() {
        boolean retVal = true;
        if(hxmTrimInputFile == null) return false;
        if(hxmTrimFile == null) return false;
        if(hxmTrimFile.exists()) {
            int result = JOptionPane.showConfirmDialog(null, "File exists:"
                + LS + hxmTrimFile.getPath() + LS + "OK to overwrite?",
                "File Exists", JOptionPane.OK_CANCEL_OPTION);
            if(result != JOptionPane.OK_OPTION) {
                return false;
            }
        }

        BufferedReader in = null;
        PrintWriter out = null;
        int nLinesWritten = 0;
        try {
            in = new BufferedReader(new FileReader(hxmTrimInputFile));
            out = new PrintWriter(new FileWriter(hxmTrimFile));
            String line;
            int lineNum = 0;
            long dateNum;
            String[] tokens;
            while((line = in.readLine()) != null) {
                lineNum++;
                tokens = line.trim().split(SAVE_SESSION_DELIM);
                if(line.trim().length() == 0) {
                    nLinesWritten++;
                    out.println(line);
                    continue;
                }
                // Skip lines starting with #
                if(tokens[0].trim().startsWith("#")) {
                    nLinesWritten++;
                    out.println(line);
                    continue;
                }
                try {
                    dateNum = ECGUtils.getHxMDateTimeFormat().parse(tokens[0])
                        .getTime();
                } catch(Exception ex) {
                    Utils.warnMsg("Failed to parse time at line " + lineNum);
                    out.close();
                    return false;
                }
                if(dateNum >= startTime && dateNum <= endTime) {
                    nLinesWritten++;
                    out.println(line);
                }
            }

            // Cleanup
            in.close();
            out.close();
            in = null;
            out = null;
        } catch(Exception ex) {
            Utils.excMsg("Error writing HxM trim file", ex);
            retVal = false;
        } finally {
            try {
                if(in != null) in.close();
                if(out != null) out.close();
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        if(retVal) {
            System.out.println();
            System.out.println("Wrote " + nLinesWritten + " lines to "
                + hxmTrimFile.getPath());
        } else {
            System.out.println();
            System.out.println("Error writing " + hxmTrimFile.getPath());
        }
        return retVal;
    }

    /**
     * The main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // Set the native look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Throwable t) {
            t.printStackTrace();
            return;
        }

        System.out.println("MakeHxMSessionFromPDFImage");
        MakeHxMSessionFromPDFImage app = new MakeHxMSessionFromPDFImage();
        // app.openFile(new File(IMAGE_FILE_PATH));
        boolean res = app.run();
        if(!res) {
            System.out.println();
            System.out.println("Aborting");
            return;
        }
        System.out.println();
        System.out.println("All Done");
    }

    /**
     * Calibration is a class to handle the calibration of the image. It
     * contains calibration parameters and a method to transfor x pixel values
     * into time.
     * 
     * @author Kenneth Evans, Jr.
     */
    public class Calibration
    {
        private double t0;
        private double a;
        private double b;

        Calibration(double x0, double x1, double t0) {
            a = 8. / (x1 - x0);
            b = -a * x0;
            this.t0 = t0;
            // System.out.println("a=" + a + " b=" + b);
        }

        /**
         * Returns the time for a given x, line, and page.
         * 
         * @param x
         * @param line
         * @param page
         * @return
         */
        public double time(double x, int line, int page) {
            double t = a * x + b + 8 * line + 32 * page - t0;
            return 1000. * t;
        }

    }

}
