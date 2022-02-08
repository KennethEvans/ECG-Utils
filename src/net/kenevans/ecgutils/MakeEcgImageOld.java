package net.kenevans.ecgutils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.UIManager;

/**
 * This is the original version of MakeEcgImage<br>
 * Created on Jun 16, 2019 By Kenneth Evans, Jr.
 */
public class MakeEcgImageOld
{
    // private static final String DEST_DIR = "C:/Scratch/ECG/Polar ECG/Images";
    private static final String DEST_DIR = "C:/Scratch/ECG/Test Images";
    private static final String SRC_DIR = "C:/Scratch/ECG/Polar ECG/CSV";
    private static final int WIDTH = 2550;
    private static final int HEIGHT = 3300;
    private static final int GRAPH_WIDTH = 40 * 5;
    private static final int GRAPH_HEIGHT = 48 * 5;
    private static final int GRAPH_X = 8;
    private static final int GRAPH_Y = 31;
    private static final double SCALE = 11.8;
    private static final String IMAGE_TYPE = "png";
    private static int MINOR_COLOR = 209;
    private static int MAJOR_COLOR = 140;
    private static int BLOCK_COLOR = 51;
    private static int OUTLINE_COLOR = 0;
    private static int CURVE_COLOR = 0;
    private static float MINOR_WIDTH = .1f;
    private static float MAJOR_WIDTH = .2f;
    private static float BLOCK_WIDTH = .3f;
    private static float OUTLINE_WIDTH = .5f;
    private static float CURVE_WIDTH = .3f;

    private static BufferedImage bi;

    private static BufferedImage createImage() {
        bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, WIDTH, HEIGHT);
        AffineTransform scalingTransform = AffineTransform
            .getScaleInstance(SCALE, SCALE);
        g2d.transform(scalingTransform);
        // Draw the small grid lines
        g2d.setStroke(new BasicStroke(MINOR_WIDTH));
        g2d.setPaint(new Color(MINOR_COLOR, MINOR_COLOR, MINOR_COLOR));
        for(int i = 0; i < GRAPH_WIDTH; i++) {
            g2d.drawLine(GRAPH_X + i, GRAPH_Y, GRAPH_X + i,
                GRAPH_Y + GRAPH_HEIGHT);
        }
        for(int i = 0; i < GRAPH_HEIGHT; i++) {
            g2d.drawLine(GRAPH_X, GRAPH_Y + i, GRAPH_X + GRAPH_WIDTH,
                GRAPH_Y + i);
        }
        // Draw the large grid lines
        g2d.setStroke(new BasicStroke(MAJOR_WIDTH));
        g2d.setPaint(new Color(MAJOR_COLOR, MAJOR_COLOR, MAJOR_COLOR));
        for(int i = 0; i < GRAPH_WIDTH; i += 5) {
            g2d.drawLine(GRAPH_X + i, GRAPH_Y, GRAPH_X + i,
                GRAPH_Y + GRAPH_HEIGHT);
        }
        for(int i = 0; i < GRAPH_HEIGHT; i += 5) {
            g2d.drawLine(GRAPH_X, GRAPH_Y + i, GRAPH_X + GRAPH_WIDTH,
                GRAPH_Y + i);
        }
        // Draw the block grid lines
        g2d.setStroke(new BasicStroke(BLOCK_WIDTH));
        g2d.setPaint(new Color(BLOCK_COLOR, BLOCK_COLOR, BLOCK_COLOR));
        for(int i = 0; i < GRAPH_WIDTH; i += 25) {
            g2d.drawLine(GRAPH_X + i, GRAPH_Y, GRAPH_X + i,
                GRAPH_Y + GRAPH_HEIGHT);
        }
        for(int i = 0; i < GRAPH_HEIGHT; i += 60) {
            g2d.drawLine(GRAPH_X, GRAPH_Y + i, GRAPH_X + GRAPH_WIDTH,
                GRAPH_Y + i);
        }
        // Draw the outline
        g2d.setStroke(new BasicStroke(OUTLINE_WIDTH));
        g2d.setPaint(new Color(OUTLINE_COLOR, OUTLINE_COLOR, OUTLINE_COLOR));
        g2d.drawLine(GRAPH_X, GRAPH_Y, GRAPH_X + GRAPH_WIDTH, GRAPH_Y);
        g2d.drawLine(GRAPH_X, GRAPH_Y + GRAPH_HEIGHT, GRAPH_X + GRAPH_WIDTH,
            GRAPH_Y + GRAPH_HEIGHT);
        g2d.drawLine(GRAPH_X, GRAPH_Y, GRAPH_X, GRAPH_Y + GRAPH_HEIGHT);
        g2d.drawLine(GRAPH_X + GRAPH_WIDTH, GRAPH_Y, GRAPH_X + GRAPH_WIDTH,
            GRAPH_Y + GRAPH_HEIGHT);
        return bi;
    }

    /**
     * Brings up a JFileChooser to pick the ECG file.
     * 
     * @param L
     * @return The Files selected or null on failure.
     */
    public static File[] openEcgFiles(String fileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(SRC_DIR));
        chooser.setDialogTitle("Pick ECG Files");
        chooser.setMultiSelectionEnabled(true);
        int result = chooser.showOpenDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return null;

        File[] files = chooser.getSelectedFiles();
        return files;
    }

    private static void processFile(File file, BufferedImage bi)
        throws Exception {
        if(file == null) {
            System.out.println("processFile: file is null");
            return;
        }
        if(!file.exists()) {
            System.out
                .println("processFile: Does not exist: " + file.getPath());
            return;
        }
        Graphics2D g2d = bi.createGraphics();
        g2d.setPaint(Color.BLACK);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 36);
        Font fontBold = new Font(Font.SANS_SERIF, Font.BOLD, 36);
        Font fontInfo = new Font(Font.SANS_SERIF, Font.PLAIN, 30);
        Font fontLogo = new Font(Font.SANS_SERIF, Font.BOLD, 48);

        BufferedReader in = null;
        in = new BufferedReader(new FileReader(file));
        String line = "";

        g2d.setFont(fontBold);
        g2d.drawString("Patient:", 100, 143);

        String date = in.readLine();
        g2d.setFont(fontBold);
        g2d.drawString("Recorded:", 100, 188);
        g2d.setFont(font);
        g2d.drawString(date, 300, 188);

        // Read lines that may not be there
        boolean repeat = true;
        while(repeat) {
            line = in.readLine();
            repeat = false;
            if(line.startsWith("ID")) {
                repeat = true;
                g2d.setFont(fontBold);
                g2d.drawString("Device ID:", 100, 278);
                g2d.setFont(font);
                g2d.drawString(line.substring(5), 300, 278);
            } else if(line.startsWith("Battery")) {
                g2d.setFont(fontBold);
                g2d.drawString("Battery:", 850, 278);
                g2d.setFont(font);
                g2d.drawString(line.substring(15), 1025, 278);
                repeat = true;
            } else if(line.startsWith("Firmware")) {
                g2d.setFont(fontBold);
                g2d.drawString("Firmware:", 500, 278);
                g2d.setFont(font);
                g2d.drawString(line.substring(10), 700, 278);
                repeat = true;
            } else if(line.startsWith("Firmware")) {
                repeat = true;
            }
        }

        // The current line should be notes
        String notes = line;
        g2d.setFont(fontBold);
        g2d.drawString("Notes:", 850, 143);
        g2d.setFont(font);
        g2d.drawString(notes, 1025, 143);

        String hr = in.readLine().substring(3);
        g2d.setFont(fontBold);
        g2d.drawString("Heart Rate:", 100, 233);
        g2d.setFont(font);
        g2d.drawString(hr, 300, 233);

        String[] tokens = in.readLine().split(" ");
        int values = Integer.parseInt(tokens[0]);
        // Assuming duration is in seconds
        double duration = Double.parseDouble(tokens[2]);
        double valueStep = duration / values / .04;
        String durationString = duration + " " + tokens[3];
        g2d.setFont(fontBold);
        g2d.drawString("Duration:", 500, 232);
        g2d.setFont(font);
        g2d.drawString(durationString, 700, 232);

        String scale = "Scale: 25 mm/s, 10 mm/mV ";
        g2d.setFont(fontInfo);
        g2d.drawString(scale, 2075, 350);

        // Do the icon
        BufferedImage image = ImageIO.read(MakeEcgImageOld.class
            .getClassLoader().getResource("resources/polar_ecg.png"));
        g2d.drawImage(image, 2050, 116, null);
        g2d.setFont(fontLogo);
        g2d.setPaint(new Color(211, 0, 36));
        g2d.drawString("KE.Net ECG", 2170, 180);

        // Draw the curves
        AffineTransform scalingTransform = AffineTransform
            .getScaleInstance(SCALE, SCALE);
        g2d.transform(scalingTransform);
        g2d.setPaint(Color.BLACK);
        g2d.setStroke(new BasicStroke(CURVE_WIDTH));
        g2d.setPaint(new Color(CURVE_COLOR, CURVE_COLOR, CURVE_COLOR));
        int index = 0;
        double y0 = 0, y;
        double x0 = 0, x;
        double offsetX = GRAPH_X;
        double offsetY = GRAPH_Y + 30;
        Line2D line2d = new Line2D.Double();
        while((line = in.readLine()) != null) {
            x = index * valueStep;
            y = -10. * Double.parseDouble(line);
            if(index == 0) {
                x0 = x;
                y0 = y;
                index++;
                continue;
            } else if(index == 1040) {
                offsetX -= 1040 * valueStep;
                offsetY += 60;
            } else if(index == 2080) {
                offsetX -= 1040 * valueStep;
                offsetY += 60;
            } else if(index == 3120) {
                offsetX -= 1040 * valueStep;
                offsetY += 60;
            } else if(index > 4160) {
                // Handle writing to the next page
                break;
            }
            line2d.setLine(x0 + offsetX, y0 + offsetY, x + offsetX,
                y + offsetY);
            // System.out.println((x0 + offsetX) + " " + (y0 + offsetY) + " "
            // + (x + offsetX) + " " + (y + offsetY));
            g2d.draw(line2d);
            y0 = y;
            x0 = x;
            index++;
        }

        // Cleanup
        in.close();
        in = null;
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

        System.out.println("MakeEcgImageOld");
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
            BufferedImage bi = createImage();
            try {
                processFile(file, bi);
            } catch(Exception ex) {
                System.out.println("Failed to process " + file);
                ex.printStackTrace();
                continue;
            }
            File outputFile = new File(DEST_DIR + "/"
                + file.getName().replaceFirst("[.][^.]+$", "") + ".png");
            try {
                saveImage(bi, outputFile);
            } catch(Exception ex) {
                System.out.println("Failed to save " + outputFile);
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
