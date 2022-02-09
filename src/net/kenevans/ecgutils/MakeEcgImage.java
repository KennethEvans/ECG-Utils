package net.kenevans.ecgutils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Created on Jun 16, 2019 By Kenneth Evans, Jr.
 */
public class MakeEcgImage
{
    public static final String LS = System.getProperty("line.separator");
    // private static final String DEST_DIR = "C:/Scratch/ECG/Polar ECG/Images";
    private static final String DEST_DIR = "C:/Scratch/ECG/Test Images";
    private static final String SRC_DIR = "C:/Scratch/ECG/Polar ECG/CSV";

    private static final String IMAGE_TYPE = "png";

    private static BufferedImage bi;

    /**
     * Brings up a JFileChooser to pick the ECG file.
     * 
     * @param L
     * @return The Files selected or null on failure.
     */
    public static File[] openEcgFiles(File suggestedFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(SRC_DIR));
        chooser.setSelectedFile(suggestedFile);
        chooser.setDialogTitle("Pick ECG Files");
        chooser.setMultiSelectionEnabled(true);
        int result = chooser.showOpenDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return null;
        File[] files = chooser.getSelectedFiles();
        return files;
    }

    public static File saveEcgFile(File suggestedFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(DEST_DIR));
        chooser.setSelectedFile(suggestedFile);
        chooser.setDialogTitle("Save Image File");
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showSaveDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return null;
        File file = chooser.getSelectedFile();
        return file;
    }

    private static void processFile(File file) throws Exception {
        if(file == null) {
            System.out.println("processFile: file is null");
            return;
        }
        if(!file.exists()) {
            System.out
                .println("processFile: Does not exist: " + file.getPath());
            return;
        }

        double samplingRate = 130.;
        BufferedImage logo;
        String patientName = "";
        String date = "NA";
        String id = "NA";
        String firmware = "NA";
        String batteryLevel = "NA";
        String notes = "NA";
        String devhr = "NA";
        String calchr = "NA";
        String npeaks = "NA";
        String duration = "NA";
        double[] ecgvals = null;
        boolean[] peakvals = null;

        // Check for new format
        boolean newFormat = true;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line = in.readLine();
            if(!line.startsWith("application=")) newFormat = false;
        }

        // Reset to the beginning
        List<Double> ecgList = new ArrayList<>();
        List<Boolean> peakList = new ArrayList<>();
        boolean header = true;
        String[] tokens;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line = "";
            if(newFormat) {
                while(header) {
                    line = in.readLine();
                    tokens = line.split(",");
                    if(tokens.length == 0) continue;
                    try {
                        Double.parseDouble(tokens[0]);
                        header = false;
                        ecgList.add(Double.parseDouble(tokens[0]));
                        if(tokens.length > 1) {
                            peakList.add(tokens[1].equals("0") ? false : true);
                        }
                    } catch(NumberFormatException ex) {
                        header = true;
                    }
                    if(header) {
                        int start = line.indexOf("=") + 1;
                        if(line.startsWith("stoptime")) {
                            date = line.substring(start);
                        } else if(line.startsWith("duration")) {
                            duration = line.substring(start);
                        } else if(line.startsWith("nsamples")) {
                            // Do nothing
                        } else if(line.startsWith("samplingrate")) {
                            // Do nothing
                        } else if(line.startsWith("stopdevicehr")) {
                            devhr = line.substring(start);
                        } else if(line.startsWith("stopcalculatedhr")) {
                            calchr = line.substring(start);
                        } else if(line.startsWith("npeaks")) {
                            npeaks = line.substring(start);
                        } else if(line.startsWith("devicename")) {
                            // Do nothing
                        } else if(line.startsWith("deviceid")) {
                            id = line.substring(start);
                        } else if(line.startsWith("battery")) {
                            batteryLevel = line.substring(start);
                        } else if(line.startsWith("firmware")) {
                            firmware = line.substring(start);
                        } else if(line.startsWith("note")) {
                            notes = line.substring(start);
                        }
                    }
                }

            } else {
                date = in.readLine();

                // Read lines that may not be there
                boolean repeat = true;
                while(repeat) {
                    line = in.readLine();
                    repeat = false;
                    if(line.startsWith("ID")) {
                        id = line.substring(5);
                        repeat = true;
                    } else if(line.startsWith("Battery")) {
                        batteryLevel = line.substring(15);
                        repeat = true;
                    } else if(line.startsWith("Firmware")) {
                        firmware = line.substring(10);
                        repeat = true;
                    } else if(line.startsWith("Polar")) {
                        repeat = true;
                    }
                }
                // The current line should be notes
                notes = line;
                // The next line is HR
                devhr = in.readLine().substring(3);
                // Next line is 3900 values 30.0 sec
                tokens = in.readLine().split(" ");
                // int nValues = Integer.parseInt(tokens[0]);
                duration = tokens[2] + " " + tokens[3];
            }

            // Read the ecg values
            while((line = in.readLine()) != null) {
                tokens = line.split(",");
                ecgList.add(Double.parseDouble(tokens[0]));
                if(tokens.length > 1) {
                    peakList.add(tokens[1].equals("0") ? false : true);
                }
            }

            // Convert ecg to double array
            int nSamples = ecgList.size();
            ecgvals = new double[nSamples];
            for(int i = 0; i < ecgList.size(); i++) {
                ecgvals[i] = ecgList.get(i);
            }
            // Convert peaks to a boolean array
            if(peakList.isEmpty()) {
                peakvals = null;
            } else if(peakList.size() != nSamples) {
                peakvals = null;
                System.out.println("!!! ecgList.size=" + nSamples
                    + " peakList.size=" + peakList.size());
            } else {
                peakvals = new boolean[nSamples];
                for(int i = 0; i < nSamples; i++) {
                    peakvals[i] = peakList.get(i);
                }
            }
        }

        // Get the logo
        logo = ImageIO.read(MakeEcgImage.class.getClassLoader()
            .getResource("resources/polar_ecg.png"));

        // Create the image
        bi = EcgImage.createImage(samplingRate, logo, patientName, date, id,
            firmware, batteryLevel, notes, devhr, calchr, npeaks, duration,
            ecgvals, peakvals);

        // Cleanup
        System.out.println("Processed " + file.getPath());
    }

    private static void saveImage(BufferedImage bi, File file)
        throws IOException {
        ImageIO.write(bi, IMAGE_TYPE, file);
        System.out.println("Wrote " + file.getPath());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Throwable t) {
            t.printStackTrace();
            return;
        }

        System.out.println("MakeEcgImage");
        File[] inputFiles = openEcgFiles(null);
        if(inputFiles == null || inputFiles.length == 0) {
            System.out.println("No files chosen");
            System.out.println();
            System.out.println("Aborted");
            return;
        }
        for(File file : inputFiles) {
            if(file == null) {
                System.out.println("Failed to process " + file);
            }
            System.out.println();
            System.out.println("Processing " + file);
            bi = null;
            try {
                processFile(file);
            } catch(Exception ex) {
                System.out.println("Failed to process " + file);
                ex.printStackTrace();
                System.out.println();
                System.out.println("Aborted");
                continue;
            }

            // Save the file
            File outputFile = new File(DEST_DIR + "/"
                + file.getName().replaceFirst("[.][^.]+$", "") + ".png");
            File saveFile = saveEcgFile(outputFile);
            if(saveFile == null) {
                System.out.println("No file to save");
                System.out.println();
                System.out.println("Aborted");
            }
            if(saveFile.exists()) {
                int selection = JOptionPane.showConfirmDialog(null,
                    "File already exists:" + LS + saveFile.getPath()
                        + "\nOK to replace?",
                    "Warning", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if(selection != JOptionPane.OK_OPTION) {
                    System.out.println("Save canceled");
                    continue;

                }
            }
            try {
                saveImage(bi, saveFile);
            } catch(Exception ex) {
                System.out.println("Failed to save " + saveFile);
                ex.printStackTrace();
                System.out.println();
                System.out.println("Aborted");
                return;
            }
        }

        System.out.println();
        System.out.println("All Done");
    }

}
