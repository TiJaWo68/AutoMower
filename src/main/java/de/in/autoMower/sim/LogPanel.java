package de.in.autoMower.sim;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class LogPanel extends JPanel {

    private JTextArea textArea;
    private java.util.concurrent.BlockingQueue<String> logQueue = new java.util.concurrent.LinkedBlockingQueue<>();
    private javax.swing.Timer updateTimer;

    public LogPanel() {
        super(new BorderLayout());
        textArea = new JTextArea();
        textArea.setEditable(false);

        // Auto-scroll to bottom
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        // Timer to flush logs to UI every 200ms
        updateTimer = new javax.swing.Timer(200, e -> flushLogs());
        updateTimer.start();

        // Set preferred size for the split pane
        setPreferredSize(new java.awt.Dimension(200, 100));
    }

    public void log(String message) {
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now());
        logQueue.offer("[" + timestamp + "] " + message);
    }

    private void flushLogs() {
        if (logQueue.isEmpty())
            return;

        StringBuilder sb = new StringBuilder();
        String msg;
        while ((msg = logQueue.poll()) != null) {
            sb.append(msg).append("\n");
        }

        textArea.append(sb.toString());

        // Trim if too long
        int len = textArea.getDocument().getLength();
        if (len > 20000) {
            try {
                textArea.replaceRange("", 0, len - 15000);
            } catch (Exception ex) {
            }
        }
    }

    public void clear() {
        if (textArea != null) {
            textArea.setText("");
            logQueue.clear();
        }
    }
}
