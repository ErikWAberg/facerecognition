package com.facerecog.rest.service;

import dto.RecognitionDTO;
import opencv.FaceDetector;
import opencv.FaceRecogniser;
import opencv.Util;
import org.bytedeco.javacpp.BytePointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

@Service
public class RecognitionService implements CommandLineRunner {

    private FaceDetector detector;
    private FaceRecogniser recogniser;
    private Logger logger = LoggerFactory.getLogger(RecognitionService.class);

    @Override
    public void run(String... args) throws Exception {
        recogniser = new FaceRecogniser("/resources/main/recognition/training");
        detector = new FaceDetector("/resources/main/detection/haar/frontalface_alt.xml");
    }

    private static RecognitionDTO createIdentificationResponse(String predictedPerson, Mat mat) {
        RecognitionDTO recognitionDTO = new RecognitionDTO();
        recognitionDTO.setPredictedPerson(predictedPerson);
        recognitionDTO.setBytes(Util.matToBytes(mat));
        recognitionDTO.setCols(mat.cols());
        recognitionDTO.setRows(mat.rows());
        recognitionDTO.setType(mat.type());
        return recognitionDTO;
    }

    public String simpleIdentification(byte[] byteImage) {
        long t1 = System.currentTimeMillis();
        IplImage iplColored = IplImage.create(1080, 720, IPL_DEPTH_8U, 3);
        BytePointer bp = iplColored.imageData();
        bp.put(byteImage);
        IplImage iplGray = Util.iplImage2gray(iplColored);
        Mat matGray = cvarrToMat(iplGray);
        String predictedPerson = recogniser.predictPerson(matGray);
        logger.info("Request completed after: " + (System.currentTimeMillis() - t1));
        return predictedPerson;
    }

    public Callable<RecognitionDTO> detectedAndIdentifyAsync(final byte[] byteImage, final int imageType, final int imageWidth, final int imageHeight) {
        return new Callable<RecognitionDTO>() {
            @Override
            public RecognitionDTO call() throws Exception {
                return detectAndIdentify(byteImage, imageType, imageWidth, imageHeight);
            }
        };
    }

    public RecognitionDTO detectAndIdentify(byte[] byteImage, int type, int width, int height) {
        long t1 = System.currentTimeMillis();
        int matType = -1;

        switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR: matType = CV_8UC3; break;
            case BufferedImage.TYPE_BYTE_GRAY: matType = CV_8UC1; break;
            default:
                throw new IllegalArgumentException("Unrecognized type");
        }
        Mat imageMat = new Mat(height, width, matType);
        imageMat.ptr().put(byteImage);

        if(matType != CV_8UC1) {
            Mat matGray = new Mat();
            cvtColor(imageMat, matGray, CV_RGB2GRAY);
            imageMat = matGray;
        }

        Mat imageMatResized = new Mat(imageMat.rows() / 4, imageMat.cols() / 4, imageMat.type());
        cvResize(imageMat.asCvMat(), imageMatResized.asCvMat(), CV_INTER_AREA);

        //cvEqualizeHist(imageMatResized.asCvMat(), imageMatResized.asCvMat());

        String predictedPerson = recogniser.predictPerson(imageMatResized);
        detector.detectFaces(imageMatResized);

        RecognitionDTO response = createIdentificationResponse(predictedPerson, imageMatResized);

        logger.info("Request completed after: " + (System.currentTimeMillis() - t1) + "ms (" + predictedPerson + ")");

        return response;
    }
}




