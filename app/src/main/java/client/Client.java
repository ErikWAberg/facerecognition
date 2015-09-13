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



import api.ApiUrls;
import com.esotericsoftware.minlog.Log;
import dto.RecognitionDTO;
import opencv.CameraCapture;
import opencv.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.LinkedBlockingQueue;


public class Client implements Observer {

    private ServiceRequester serviceRequester;
    private static String SERVICE_URL = "http://localhost:8080";
    private static String SERVICE_TYPE = ApiUrls.ROOT_URL_RECOG + ApiUrls.URL_RECOG_IDENTIFY_ASYNC;
    // private static String SERVICE_TYPE = ApiUrls.ROOT_URL_RECOG + ApiUrls.URL_RECOG_IDENTIFY;
    private static String SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
    private static double SERVICE_REQUEST_INTERVAL_IN_SEC = 0.5;

    private volatile boolean activeCameraCapture = false;

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        new Client();
    }

    private ClientUI clientUI;
    private CameraCapture cameraCapture;
    private JButton buttonCaptureStart, buttonCaptureStop;

    public Client() {

        LinkedBlockingQueue<BufferedImage> capturedImageQueue = new LinkedBlockingQueue<BufferedImage>();
        cameraCapture = new CameraCapture(720, 1080, capturedImageQueue);
        cameraCapture.addObserver(this);
        cameraCapture.setCaptureIntervalInSeconds(SERVICE_REQUEST_INTERVAL_IN_SEC);

        serviceRequester = new ServiceRequester(capturedImageQueue, SERVICE_REQUEST_URL);
        serviceRequester.addObserver(this);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                clientUI = new ClientUI();
                buttonCaptureStart = clientUI.getButtonStartCapture();
                buttonCaptureStop = clientUI.getButtonStopCapture();
                buttonCaptureStart.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                startCameraCapture(buttonCaptureStart, buttonCaptureStop);
                            }
                        });
                buttonCaptureStop.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                stopCameraCapture(buttonCaptureStart, buttonCaptureStop);
                            }
                        });
            }
        });
    }

    private void startCameraCapture(JButton buttonCaptureStart, JButton buttonCaptureStop) {
        activeCameraCapture = true;
        Log.info("Starting camera capture");
        buttonCaptureStart.setEnabled(false);
        buttonCaptureStop.setEnabled(true);
        new Thread(serviceRequester).start();

        try {
            cameraCapture.startCapture();
        } catch (Exception e1) {
            e1.printStackTrace();
            stopCameraCapture(buttonCaptureStart, buttonCaptureStop);
        }
    }

    private void stopCameraCapture(JButton buttonCaptureStart, JButton buttonCaptureStop) {
        if(activeCameraCapture) {
            activeCameraCapture = false;
            Log.info("Camera capture stopped");
            buttonCaptureStart.setEnabled(true);
            buttonCaptureStop.setEnabled(false);
            cameraCapture.stopCapture();
            serviceRequester.shutdown();
        }
    }

    @Override
    public void update(Observable observable, Object arg) {
        if(observable instanceof CameraCapture) {
            String msg = (String) arg;
            if (msg.equals("STOPPED")) {
                stopCameraCapture(buttonCaptureStart, buttonCaptureStop);
            }
        } else if(observable instanceof ServiceRequester) {
            RecognitionDTO response = (RecognitionDTO) arg;
            BufferedImage bufferedImage = Util.identificationDtoToBufferedImage(response);
            clientUI.updateServedImage(bufferedImage, response.getPredictedPerson());
        }

    }

}

