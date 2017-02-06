package net.kenevans.ecgutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Utils
{
    public static final String LS = System.getProperty("line.separator");

    /**
     * Generic method to get a file suing a JFileChooder
     * 
     * @param defaultPath
     * @return the File or null if aborted.
     */
    public static File getOpenFile(String defaultPath) {
        File file = null;
        JFileChooser chooser = new JFileChooser();
        if(defaultPath != null) {
            chooser.setCurrentDirectory(new File(defaultPath));
        }
        int result = chooser.showOpenDialog(null);
        if(result == JFileChooser.APPROVE_OPTION) {
            // Save the selected path for next time
            defaultPath = chooser.getSelectedFile().getParentFile().getPath();
            // Process the file
            file = chooser.getSelectedFile();
        }
        return file;
    }

    /**
     * Error message dialog
     * 
     * @param msg
     */
    public static void errMsg(final String msg) {
        // Show it in a message box
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, msg, "Error",
                    JOptionPane.ERROR_MESSAGE);
                System.out.println(msg);
            }
        });
    }

    /**
     * Warning message dialog
     * 
     * @param msg
     */
    public static void warnMsg(final String msg) {
        // Show it in a message box
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, msg, "Warning",
                    JOptionPane.WARNING_MESSAGE);
                System.out.println(msg);
            }
        });
    }

    /**
     * Information message dialog
     * 
     * @param msg
     */
    public static void infoMsg(final String msg) {
        // Show it in a message box
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, msg, "Information",
                    JOptionPane.INFORMATION_MESSAGE);
                System.out.println(msg);
            }
        });
    }

    /**
     * Exception message dialog. Displays message plus the exception and
     * exception message.
     * 
     * @param msg
     * @param ex
     */
    public static void excMsg(String msg, Exception ex) {
        final String fullMsg = msg += LS + "Exception: " + ex + LS
            + ex.getMessage();
        // Show it in a message box
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, fullMsg, "Error",
                    JOptionPane.ERROR_MESSAGE);
                System.out.println(fullMsg);
            }
        });
    }

    /**
     * Exception message dialog. Displays message plus the error and error
     * message.
     * 
     * @param msg
     * @param ex
     */
    public static void excMsg(String msg, Error ex) {
        final String fullMsg = msg += LS + "Error: " + ex + LS
            + ex.getMessage();
        // Show it in a message box
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, fullMsg, "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Exception message dialog. Displays message plus the throwable and
     * throwable message.
     * 
     * @param msg
     * @param t
     */
    public static void excMsg(String msg, Throwable t) {
        final String fullMsg = msg += LS + "Throwable: " + t + LS
            + t.getMessage();
        // Show it in a message box
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, fullMsg, "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Get the extension of a file (without the dot).
     * 
     * @param file
     * @return
     */
    public static String getExtension(File file) {
        String ext = null;
        String s = file.getName();
        int i = s.lastIndexOf('.');
        if(i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Returns the contents of a file as a byte array.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public byte[] loadFileBytes(File file) throws IOException {
        if(file == null) return null;
        if(!file.exists()) {
            throw new IOException("File does not exist");
        }
        int len = (int)file.length();
        if(len == 0) {
            throw new IOException("File is empty");
        }
        byte bytes[] = new byte[len]; // Has to be int here
        int nRead = 0;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            nRead = in.read(bytes);
            if(nRead != len) {
                throw new IOException("Only read " + nRead + " of " + len
                    + " bytes");
            }
            return bytes;
        } finally {
            if(in != null) in.close();
        }
    }

    /**
     * Generates a timestamp.
     * 
     * @return String timestamp with the current time
     */
    public static String timeStamp() {
        Date now = new Date();
        final SimpleDateFormat defaultFormatter = new SimpleDateFormat(
            "MMM dd, yyyy HH:mm:ss.SSS");
        return defaultFormatter.format(now);
    }

    /**
     * Generates a timestamp given a pattern
     * 
     * @param pattern appropriate for SimpleDateFormat
     * @return String timestamp with the current time
     */
    public static String timeStamp(String pattern) {
        Date now = new Date();
        final SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.format(now);
    }

}
