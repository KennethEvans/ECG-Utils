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

/*
 * Created on Jun 16, 2019
 * By Kenneth Evans, Jr.
 */

public class MakeEcgImage
{
    private static final String DEST_DIR = "C:/Scratch/ECG/Polar ECG/Images";
    private static final String SRC_DIR = "C:/Scratch/ECG/Polar ECG/CSV Files";
    private static String DEFAULT_DEST_FILENAME = DEST_DIR + "/" + "Test.png";
    private static String DEFAULT_SRC_FILENAME = SRC_DIR + "/"
        + "PolarECG-2019-04-30_13-26.csv";
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
     * @return The File or null on failure.
     */
    public static File openEcgFile(String fileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(SRC_DIR));
        chooser.setDialogTitle("Pick ECG File");
        int result = chooser.showOpenDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return null;

        File file = chooser.getSelectedFile();
        return file;
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
        Font font = new Font("TimesRoman", Font.PLAIN, 36);
        Font fontBold = new Font("TimesRoman", Font.BOLD, 36);
        Font fontInfo = new Font("TimesRoman", Font.PLAIN, 30);
        Font fontLogo = new Font("Helvetica", Font.BOLD, 48);

        BufferedReader in = null;
        in = new BufferedReader(new FileReader(file));
        String line;

        g2d.setFont(fontBold);
        g2d.drawString("Patient:", 97, 143);

        String date = in.readLine();
        g2d.setFont(fontBold);
        g2d.drawString("Recorded:", 97, 188);
        g2d.setFont(font);
        g2d.drawString(date, 356, 188);

        line = in.readLine();
        while(line.startsWith("ID") || line.startsWith("Battery")
            || line.startsWith("Firmware")) {
            line = in.readLine();
        }
        String notes = line;
        g2d.setFont(fontBold);
        g2d.drawString("Notes:", 982, 143);
        g2d.setFont(font);
        g2d.drawString(notes, 1150, 143);

        String hr = in.readLine().substring(3);
        g2d.setFont(fontBold);
        g2d.drawString("Heart Rate:", 96, 232);
        g2d.setFont(font);
        g2d.drawString(hr, 355, 232);

        String[] tokens = in.readLine().split(" ");
        int values = Integer.parseInt(tokens[0]);
        // Assuming duration is in seconds
        double duration = Double.parseDouble(tokens[2]);
        double valueStep = duration / values / .04;
        String durationString = duration + " " + tokens[3];
        g2d.setFont(fontBold);
        g2d.drawString("Duration:", 637, 232);
        g2d.setFont(font);
        g2d.drawString(durationString, 825, 232);

        String scale = "Scale: 25 mm/s, 10 mm/mV ";
        g2d.setFont(fontInfo);
        g2d.drawString(scale, 2117, 350);

        // Do the icon
        BufferedImage image = ImageIO.read(MakeEcgImage.class.getClassLoader()
            .getResource("resources/polar_ecg.png"));
        g2d.drawImage(image, 2100, 116, null);
        g2d.setFont(fontLogo);
        g2d.setPaint(new Color(211, 0, 36));
        g2d.drawString("Polar ECG", 2210, 180);

        // Draw the small curves
        AffineTransform scalingTransform = AffineTransform
            .getScaleInstance(SCALE, SCALE);
        g2d.setPaint(Color.BLACK);
        g2d.transform(scalingTransform);
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

        // File inputFile = new File(DEFAULT_SRC_FILENAME);
        File inputFile = openEcgFile(null);
        if(inputFile == null) {
            System.out.println("Failed to process " + inputFile);
            System.out.println();
            System.out.println("Aborted");
            return;
        }
        System.out.println("Processing " + inputFile);
        BufferedImage bi = createImage();
        try {
            processFile(inputFile, bi);
        } catch(Exception ex) {
            System.out.println("Failed to process " + inputFile);
            ex.printStackTrace();
            System.out.println();
            System.out.println("Aborted");
            return;
        }
        File outputFile = new File(DEST_DIR + "/"
            + inputFile.getName().replaceFirst("[.][^.]+$", "") + ".png");
        try {
            saveImage(bi, outputFile);
        } catch(Exception ex) {
            System.out.println("Failed to save " + outputFile);
            ex.printStackTrace();
            System.out.println();
            System.out.println("Aborted");
            return;
        }

        System.out.println();
        System.out.println("All Done");
    }

}
