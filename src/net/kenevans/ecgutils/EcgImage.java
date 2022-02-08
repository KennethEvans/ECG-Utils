package net.kenevans.ecgutils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

/**
 * A Java version of the Android EcgImage. Class to create an ECG plot. Note
 * that the units used are the size of a small box. These are scaled to the
 * desired page size. <br>
 * Created on Feb 7, 2022 By Kenneth Evans, Jr.
 */
public class EcgImage
{
    private static final int WIDTH = 2550;
    private static final int HEIGHT = 3300;
    private static final int GRAPH_WIDTH = 40 * 5;
    private static final int GRAPH_HEIGHT = 48 * 5;
    private static final int GRAPH_X = 8;
    private static final int GRAPH_Y = 31;
    private static final float SCALE = 11.8f;
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

    public static BufferedImage createImage(double samplingRate,
        BufferedImage logo, String patientName, String date, String id,
        String firmware, String batteryLevel, String notes, String devhr,
        String calchr, String nPeaks, String duration, double[] ecgvals,
        boolean[] peakvals) throws Exception {
        // Graphics
        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, WIDTH, HEIGHT);

        // Fonts
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 36);
        Font fontBold = new Font(Font.SANS_SERIF, Font.BOLD, 36);
        Font fontInfo = new Font(Font.SANS_SERIF, Font.PLAIN, 30);
        Font fontLogo = new Font(Font.SANS_SERIF, Font.BOLD, 48);

        // Headers
        g2d.setColor(Color.BLACK);
        g2d.setFont(fontBold);
        g2d.drawString("Patient:", 100, 120);
        g2d.setFont(font);
        g2d.drawString(patientName, 300, 120);

        g2d.setFont(fontBold);
        g2d.drawString("Notes:", 850, 120);
        g2d.setFont(font);
        g2d.drawString(notes, 1025, 120);

        g2d.setFont(fontBold);
        g2d.drawString("Recorded:", 100, 165);
        g2d.setFont(font);
        g2d.drawString(date, 300, 165);

        g2d.setFont(fontBold);
        g2d.drawString("Duration:", 100, 210);
        g2d.setFont(font);
        g2d.drawString(duration, 300, 210);

        g2d.setFont(fontBold);
        g2d.drawString("Device ID:", 100, 255);
        g2d.setFont(font);
        g2d.drawString(id, 300, 255);

        g2d.setFont(fontBold);
        g2d.drawString("Battery:", 850, 255);
        g2d.setFont(font);
        g2d.drawString(batteryLevel, 1025, 255);

        g2d.setFont(fontBold);
        g2d.drawString("Firmware:", 500, 255);
        g2d.setFont(font);
        g2d.drawString(firmware, 700, 255);

        g2d.setFont(fontBold);
        g2d.drawString("Device HR:", 100, 300);
        g2d.setFont(font);
        g2d.drawString(devhr, 300, 300);

        g2d.setFont(fontBold);
        g2d.drawString("Calc HR:", 500, 300);
        g2d.setFont(font);
        g2d.drawString(calchr, 700, 300);

        g2d.setFont(fontBold);
        g2d.drawString("Peaks:", 850, 300);
        g2d.setFont(font);
        g2d.drawString(nPeaks, 1025, 300);

        String scale = "Scale: 25 mm/s, 10 mm/mV ";
        g2d.setFont(fontInfo);
        g2d.drawString(scale, 2075, 350);

        // Do the icon
        BufferedImage image = ImageIO.read(MakeEcgImage.class.getClassLoader()
            .getResource("resources/polar_ecg.png"));
        g2d.drawImage(image, 2050, 116, null);
        g2d.setFont(fontLogo);
        g2d.setPaint(new Color(211, 0, 36));
        g2d.drawString("KE.Net ECG", 2170, 180);

        // Set the scaling
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

        // Draw the curves
        g2d.setStroke(new BasicStroke(CURVE_WIDTH));
        g2d.setPaint(new Color(CURVE_COLOR, CURVE_COLOR, CURVE_COLOR));
        Line2D line2d = new Line2D.Double();
        int index = 0;
        float y0 = 0, y;
        float x0 = 0, x;
        float offsetX = GRAPH_X;
        float offsetY = GRAPH_Y + 30;
        float valueStep = 200.f / ((float)samplingRate * 8);
        for(double val : ecgvals) {
            x = index * valueStep;
            y = (float)(-10 * val);
            if(index == 0) {
                x0 = x;
                y0 = y;
                index++;
                continue;
            } else if(index == 8 * samplingRate) {
                offsetX -= (8 * samplingRate) * valueStep;
                offsetY += 60;
            } else if(index == 16 * samplingRate) {
                offsetX -= (8 * samplingRate) * valueStep;
                offsetY += 60;
            } else if(index == 24 * samplingRate) {
                offsetX -= (8 * samplingRate) * valueStep;
                offsetY += 60;
            } else if(index > 32 * samplingRate) {
                // Handle writing to the next page
                break;
            }
            line2d.setLine(x0 + offsetX, y0 + offsetY, x + offsetX,
                y + offsetY);
            // System.out.println((x0 + offsetX) + " " + (y0 + offsetY) + " "
            // + (x + offsetX) + " " + (y + offsetY));
            g2d.draw(line2d);

            // QRS Marks
            if(peakvals != null && peakvals[index]) {
                line2d.setLine(x + offsetX, offsetY + 28, x + offsetX,
                    offsetY + 30);
                // System.out.println((x0 + offsetX) + " " + (y0 + offsetY) + "
                // "
                // + (x + offsetX) + " " + (y + offsetY));
                g2d.draw(line2d);
            }
            y0 = y;
            x0 = x;
            index++;
        }

        return bi;
    }
}
