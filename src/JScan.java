import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class JScan {
    private static final String LAN_PREFIX = "192.168.";
    private static final int THREADS = 200;
    private static JTextArea textArea;
    private static JButton startButton;
    private static JButton stopButton;
    private static PrintWriter logWriter;
    private static ExecutorService executorService;
    private static AtomicBoolean isScanning;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(JScan::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("JScan - 1.2.6");
        ImageIcon icon = new ImageIcon(JScan.class.getResource("/JScan.png"));
        frame.setIconImage(icon.getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        startButton = new JButton("Start Scan");
        startButton.addActionListener(e -> startScan());
        frame.add(startButton, BorderLayout.NORTH);

        textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        stopButton = new JButton("Stop Scan");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopScan());
        frame.add(stopButton, BorderLayout.SOUTH);

        frame.pack();
        frame.setSize(400, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        isScanning = new AtomicBoolean(false);

        try {
            logWriter = new PrintWriter(new FileWriter("suckmytoes.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startScan() {
        if (isScanning.get()) {
            return;
        }
        isScanning.set(true);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        int[] portsToScan = new int[65535];
        for (int i = 0; i < 65535; i++) {
            portsToScan[i] = i + 1;
        }
        executorService = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < 256; i++) {
            final int subnet = i;
            executorService.submit(() -> scanIPSubnet(subnet, portsToScan));
        }
    }

    private static void scanIPSubnet(int subnet, int[] portsToScan) {
        String targetPrefix = LAN_PREFIX + subnet + ".";
        for (int i = 1; i <= 255; i++) {
            String targetIp = targetPrefix + i;
            for (int port : portsToScan) {
                if (!isScanning.get()) {
                    break;
                }
                final int finalPort = port;
                scanPort(targetIp, finalPort);
            }
        }
    }

    private static void scanPort(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 100);
            updateTextArea("Port " + port + " is open on IP " + ip);
            logWriter.println("Port " + port + " is open on IP " + ip);
        } catch (IOException e) {
            updateTextArea("Port " + port + " is closed on IP " + ip);
            logWriter.println("Port " + port + " is closed on IP " + ip);
        }
    }

    private static void stopScan() {
        if (!isScanning.get()) {
            return;
        }
        isScanning.set(false);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private static void updateTextArea(String message) {
        SwingUtilities.invokeLater(() -> textArea.append(message + "\n"));
    }
}
