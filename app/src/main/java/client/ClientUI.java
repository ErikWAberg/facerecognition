/*
 * Copyright 2015 Erik Wiséen Åberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ClientUI {

    private JFrame camCaptureControlFrame;

    private JButton buttonStartCapture;
    private JButton buttonStopCapture;

    private ImageIcon servedImageIcon;
    private JLabel servedTextLabel;
    private String currentDetectedPerson = "";

    /**
     * Sets up a window in which images received from a Face Recognition service
     * is displayed, along with a set of buttons to turn the camera capturing feed
     * on and off.
     */
    public ClientUI() {
        camCaptureControlFrame = new JFrame("Face recognition");
        camCaptureControlFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        camCaptureControlFrame.setLayout(new BorderLayout(10, 5));

        createServedImagePane();
        createCameraControlButtons(camCaptureControlFrame.getContentPane());

        camCaptureControlFrame.pack();
        camCaptureControlFrame.setVisible(true);
    }

    /**
     * Pane showing images received from Face Recognition service.
     */
    private void createServedImagePane() {
        JPanel servedImagePanel = new JPanel();
        BufferedImage bufferedImage = new BufferedImage(1080, 720, 3);
        servedImageIcon = new ImageIcon(resize(bufferedImage, bufferedImage.getWidth() / 4, bufferedImage.getHeight() / 4));
        JLabel servedImageLabel = new JLabel(servedImageIcon);
        servedImagePanel.add(servedImageLabel);

        camCaptureControlFrame.add(servedImagePanel, BorderLayout.CENTER);
        servedTextLabel = new JLabel("Identified person: ");
        camCaptureControlFrame.add(servedTextLabel, BorderLayout.BEFORE_FIRST_LINE);
    }

    /**
     * Create buttons used to start/stop camera capturing.
     * @param contentPane pane to attach buttons to.
     */
    private void createCameraControlButtons(Container contentPane) {
        final JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(1, 2));
        buttonStartCapture = new JButton("Start camera");
        buttonStopCapture = new JButton("Stop camera");
        buttonStopCapture.setEnabled(false);
        panel.add(buttonStartCapture);
        panel.add(buttonStopCapture);
        contentPane.add(panel, BorderLayout.PAGE_END);
    }

    /**
     * Getter for camera start button.
     * @return camera start button.
     */
    public JButton getButtonStartCapture() {
        return buttonStartCapture;
    }

    /**
     * Getter for camera stop button.
     * @return camera stop button.
     */
    public JButton getButtonStopCapture() {
        return buttonStopCapture;
    }

    /**
     * Updates the pane which shows images received from a recognition service.
     * @param bufferedImage the new image to display.
     * @param identifiedPerson the name of the person identified in the image.
     */
    public void updateServedImage(final BufferedImage bufferedImage, final String identifiedPerson) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(!identifiedPerson.equals(currentDetectedPerson)) {
                    currentDetectedPerson = identifiedPerson;
                    servedTextLabel.setText("Identified person: " + identifiedPerson);
                }
                servedImageIcon.setImage(bufferedImage);
                camCaptureControlFrame.repaint();
            }
        });
    }

    /**
     * Resize a BufferedImage to specified width and height.
     * @param image the image to resize.
     * @param width the new image width.
     * @param height the new image height.
     * @return resized image.
     */
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        Graphics2D g2d = (Graphics2D) bi.createGraphics();
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        return bi;
    }


}
