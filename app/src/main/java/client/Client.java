/*
 *
 *  * Copyright 2015 Erik Wiséen Åberg
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package client;


import api.ApiUrls;
import com.esotericsoftware.minlog.Log;
import controll.CameraController;
import controll.MenuBarController;
import controll.ServiceController;
import dto.RecognitionDTO;
import gui.ClientUI;
import opencv.CameraCapture;
import opencv.Util;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public class Client implements Observer, ServiceController, CameraController, MenuBarController {

    private static String SERVICE_TYPE = ApiUrls.ROOT_URL_RECOG + ApiUrls.URL_RECOG_DETECT_IDENTIFY;
    private static String SERVICE_URL = "http://localhost:8080";
    private static String SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
    private static double CAMERA_CAPTURE_INTERVAL_IN_SEC = 0.2;


    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        new Client();

    }

    private ClientUI clientUI;
    private ServiceRequester serviceRequester;
    private CameraCapture cameraCapture;
    private volatile boolean activeCameraCapture = false;
    private JButton buttonCaptureStart, buttonCaptureStop;

    private final ExecutorService executorService;

    private int captureWidth = 1080;
    private int captureHeight = 720;


    public Client() {

        LinkedBlockingQueue<BufferedImage> capturedImageQueue = new LinkedBlockingQueue<BufferedImage>();
        executorService = Executors.newSingleThreadExecutor();

        cameraCapture = new CameraCapture(captureWidth, captureHeight, capturedImageQueue);
        cameraCapture.addObserver(this);
        cameraCapture.setCaptureIntervalInSeconds(CAMERA_CAPTURE_INTERVAL_IN_SEC);

        clientUI = new ClientUI(captureWidth / 4, captureHeight / 4, this, this);

        serviceRequester = new ServiceRequester(capturedImageQueue, SERVICE_REQUEST_URL, this);
    }


    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof CameraCapture) {
            String msg = (String) arg;
            if (msg.equals("STOPPED")) {
                stopCameraCapture(buttonCaptureStart, buttonCaptureStop);
            }
        }

    }

    //---------------- ServiceController interface -----------------
    @Override
    public void receivedRecognitionDto(final RecognitionDTO recognitionResponse) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedImage bufferedImage = Util.identificationDtoToBufferedImage(recognitionResponse);
                clientUI.updateServedImage(bufferedImage, recognitionResponse.getPredictedPerson());
            }
        });

    }


    //---------------- CameraController interface ------------------

    @Override
    public void startCameraCapture(JButton buttonCaptureStart, JButton buttonCaptureStop) {
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

    @Override
    public void stopCameraCapture(JButton buttonCaptureStart, JButton buttonCaptureStop) {
        if (activeCameraCapture) {
            activeCameraCapture = false;
            Log.info("Camera capture stopped");
            buttonCaptureStart.setEnabled(true);
            buttonCaptureStop.setEnabled(false);
            cameraCapture.stopCapture();
            serviceRequester.shutdown();
        }
    }


    //--------------- MenuBarController interface -------------------
    @Override
    public void close() {
        System.exit(0);
    }

    @Override
    public void setCameraCaptureInterval(double interval) {
        Log.info("Changing camera capture interval from '" + CAMERA_CAPTURE_INTERVAL_IN_SEC + "' to '" + interval + "'.");
        CAMERA_CAPTURE_INTERVAL_IN_SEC = interval;
        cameraCapture.setCaptureIntervalInSeconds(CAMERA_CAPTURE_INTERVAL_IN_SEC);
    }

    @Override
    public double getCameraCaptureInterval() {
        return CAMERA_CAPTURE_INTERVAL_IN_SEC;
    }

    @Override
    public void changeServiceUrl(String url) {
        Log.info("Changing service URL from '" + SERVICE_URL + "' to '" + url + "'.");
        SERVICE_URL = url;
        SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
        serviceRequester.setServiceUrl(SERVICE_REQUEST_URL);
    }

    @Override
    public String getServiceUrl() {
        return SERVICE_URL;
    }

    @Override
    public void setServiceType(String serviceType) {
        Log.info("Changing service type from '" + SERVICE_TYPE + "' to '" + serviceType + "'.");
        SERVICE_TYPE = serviceType;
        SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
        serviceRequester.setServiceUrl(SERVICE_REQUEST_URL);

    }


}

