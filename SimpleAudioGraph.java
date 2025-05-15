
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Audio waveform visualizer with signal processing capabilities: - Peak
 * detection - Frequency analysis - Amplitude statistics - Live microphone
 * capture with Start/Stop controls - WAV file loading and analysis
 */
public class SimpleAudioGraph extends JPanel {

    private static final int BUFFER_SIZE = 4096;
    private static final int FILE_SAMPLE_RATE = 44100;
    private static final float SILENCE_THRESHOLD = 0.02f;
    private static final float PEAK_THRESHOLD = 0.7f;
    private static final int MIN_PEAK_DISTANCE = 100;

    private float[] liveWaveform = new float[BUFFER_SIZE / 2];
    private float[] fileWaveform = new float[FILE_SAMPLE_RATE * 5];
    private int fileLength = 0;
    private boolean showingLive = false;
    private List<Integer> peaks = new ArrayList<>();
    private double averageAmplitude = 0.0;
    private double estimatedBPM = 0.0;

    private TargetDataLine line;
    private Thread captureThread;
    private JLabel statsLabel;

    public SimpleAudioGraph() {
        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.BLACK);
        initControls();
    }

    private void initControls() {
        JButton loadButton = new JButton("Load WAV");
        loadButton.addActionListener((ActionEvent e) -> {
            loadWavFile();
            showingLive = false;
            processSignal(fileWaveform, fileLength);
            repaint();
        });

        JButton startButton = new JButton("Start Live");
        startButton.addActionListener((ActionEvent e) -> {
            if (captureThread == null) {
                startCapture();
                showingLive = true;
            }
        });

        JButton stopButton = new JButton("Stop Live");
        stopButton.addActionListener((ActionEvent e) -> {
            stopCapture();
            processSignal(liveWaveform, liveWaveform.length);
            repaint();
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener((ActionEvent e) -> {
            clearWaveforms();
            peaks.clear();
            averageAmplitude = 0.0;
            estimatedBPM = 0.0;
            updateStatsLabel();
            repaint();
        });

        statsLabel = new JLabel("No data", SwingConstants.CENTER);
        statsLabel.setForeground(Color.BLACK);
        statsLabel.setOpaque(true);
        statsLabel.setBackground(new Color(255, 255, 200));
        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.BOLD, 18f));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controls = new JPanel();
        controls.add(loadButton);
        controls.add(startButton);
        controls.add(stopButton);
        controls.add(resetButton);

        setLayout(new BorderLayout());
        add(statsLabel, BorderLayout.NORTH);
        add(controls, BorderLayout.SOUTH);
    }

    private void clearWaveforms() {
        Arrays.fill(liveWaveform, 0f);
        Arrays.fill(fileWaveform, 0f);
        fileLength = 0;
    }

    private void loadWavFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("WAV Files", "wav"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            AudioFormat fmt = ais.getFormat();
            int bytesPerFrame = fmt.getFrameSize();
            byte[] buffer = new byte[BUFFER_SIZE];
            int offset = 0, read;

            // Clear previous data
            Arrays.fill(fileWaveform, 0f);
            fileLength = 0;
            peaks.clear();

            while ((read = ais.read(buffer)) > 0 && offset < fileWaveform.length) {
                int samples = read / bytesPerFrame;
                for (int i = 0; i < samples && offset < fileWaveform.length; i++) {
                    int idx = i * bytesPerFrame;
                    int sample = (buffer[idx + 1] << 8) | (buffer[idx] & 0xFF);
                    fileWaveform[offset++] = sample / 32768f;
                }
            }
            fileLength = offset;

            // Process the loaded file data
            scaleAmplitude(fileWaveform, fileLength);
            processSignal(fileWaveform, fileLength);

            // Update display
            showingLive = false;
            SwingUtilities.invokeLater(() -> {
                updateStatsLabel();
                repaint();
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load WAV: " + ex.getMessage());
        }
    }

    private void startCapture() {
        try {
            AudioFormat format = new AudioFormat(44100f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            captureThread = new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (!Thread.currentThread().isInterrupted()) {
                    int read = line.read(buffer, 0, buffer.length);
                    int samples = read / 2;
                    float maxAmp = 0f;
                    for (int i = 0; i < samples && i < liveWaveform.length; i++) {
                        int sample = (buffer[2 * i + 1] << 8) | (buffer[2 * i] & 0xFF);
                        float val = sample / 32768f;
                        liveWaveform[i] = val;
                        maxAmp = Math.max(maxAmp, Math.abs(val));
                    }
                    if (maxAmp < SILENCE_THRESHOLD) {
                        // treat as silence: clear waveform
                        Arrays.fill(liveWaveform, 0f);
                    } else {
                        scaleAmplitude(liveWaveform, liveWaveform.length);
                    }
                    // Process signal and update display in real-time
                    processSignal(liveWaveform, liveWaveform.length);
                    SwingUtilities.invokeLater(this::repaint);
                }
            });
            captureThread.setDaemon(true);
            captureThread.start();
        } catch (LineUnavailableException ex) {
            JOptionPane.showMessageDialog(this, "Microphone not available: " + ex.getMessage());
        }
    }

    private void stopCapture() {
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }
    }

    private void scaleAmplitude(float[] data, int length) {
        float max = 0f;
        for (int i = 0; i < length; i++) {
            max = Math.max(max, Math.abs(data[i]));
        }
        if (max == 0) {
            return;
        }
        for (int i = 0; i < length; i++) {
            data[i] /= max;
        }
    }

    private void processSignal(float[] data, int length) {
        // Calculate average amplitude using functional programming
        averageAmplitude = IntStream.range(0, length)
                .mapToDouble(i -> Math.abs(data[i]))
                .average()
                .orElse(0.0);

        // Detect peaks using functional programming
        peaks.clear();
        IntStream.range(1, length - 1)
                .filter(i -> isPeak(data, i))
                .forEach(peaks::add);

        // Estimate BPM based on peak intervals
        if (peaks.size() >= 2) {
            double avgInterval = IntStream.range(1, peaks.size())
                    .mapToDouble(i -> peaks.get(i) - peaks.get(i - 1))
                    .average()
                    .orElse(0.0);

            if (avgInterval > 0) {
                estimatedBPM = (60.0 * FILE_SAMPLE_RATE) / avgInterval;
            }
        }

        updateStatsLabel();
    }

    private boolean isPeak(float[] data, int i) {
        return data[i] > PEAK_THRESHOLD
                && data[i] > data[i - 1]
                && data[i] > data[i + 1]
                && (peaks.isEmpty() || i - peaks.get(peaks.size() - 1) > MIN_PEAK_DISTANCE);
    }

    private void updateStatsLabel() {
        String stats = String.format("Avg Amplitude: %.3f | Peaks: %d | Est. BPM: %.1f",
                averageAmplitude, peaks.size(), estimatedBPM);
        statsLabel.setText(stats);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.GREEN);
        float[] data = showingLive ? liveWaveform : fileWaveform;
        int length = showingLive ? liveWaveform.length : fileLength;
        if (length < 2) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        int mid = h / 2;
        double xScale = (double) w / (length - 1);

        // Draw waveform
        for (int i = 1; i < length; i++) {
            int x1 = (int) ((i - 1) * xScale);
            int y1 = mid - (int) (data[i - 1] * mid * 0.8);
            int x2 = (int) (i * xScale);
            int y2 = mid - (int) (data[i] * mid * 0.8);
            g2.drawLine(x1, y1, x2, y2);
        }

        // Draw peaks
        g2.setColor(Color.RED);
        for (int peak : peaks) {
            int x = (int) (peak * xScale);
            int y = mid - (int) (data[peak] * mid * 0.8);
            g2.fillOval(x - 3, y - 3, 6, 6);
        }
    }

    // SimpleAudioGraph method to demonstrate signal processing
    public static void testSignalProcessing() {
        // Generate SimpleAudioGraph signal
        float[] testSignal = new float[1000];
        for (int i = 0; i < testSignal.length; i++) {
            testSignal[i] = (float) Math.sin(i * 0.1) + (float) Math.sin(i * 0.2) * 0.5f;
        }

        // Create instance and process SimpleAudioGraph signal
        SimpleAudioGraph SimpleAudioGraph = new SimpleAudioGraph();
        SimpleAudioGraph.processSignal(testSignal, testSignal.length);

        // Print results
        System.out.println("SimpleAudioGraph Results:");
        System.out.println("Average Amplitude: " + SimpleAudioGraph.averageAmplitude);
        System.out.println("Number of Peaks: " + SimpleAudioGraph.peaks.size());
        System.out.println("Estimated BPM: " + SimpleAudioGraph.estimatedBPM);
    }

    public static void main(String[] args) {

        testSignalProcessing();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Audio Waveform Visualizer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new SimpleAudioGraph());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
