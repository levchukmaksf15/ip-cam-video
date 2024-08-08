package com.videotest.service;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.xuggle.xuggler.Global.DEFAULT_TIME_UNIT;

@Service
@Data
@Slf4j
public class VideoService {

    private boolean statusVideoStreaming = false;
    private static final String STREAM_VIDEO_RESOURCE = "rtsp://admin:ololo111!@192.168.0.105:554/Streaming/Channels/101";
  private static final String VIDEO_RESOURCE =
      "C:\\Users\\AndUser\\Downloads\\video_test\\video_test\\fullVideo.mov";

    private LocalDateTime startTime;
    private List<LocalDateTime> trigerTimeList = new ArrayList<>();
    private LocalDateTime finishTime;

    private int imageWidth = 1920;
    private int imageHeight = 1080;

    private BlockingQueue<opencv_core.IplImage> videoImageQueue = new ArrayBlockingQueue<>(1000);
//    private List<opencv_core.IplImage> videoImageQueue = new ArrayList<>();
    private boolean grabberStatus;

    private OpenCVFrameGrabber grabber;

    public void cameraTest() {
        statusVideoStreaming = true;

        try {
            Thread threadGrab = new Thread(() -> {
                log.info("Grab thread is started");
                grabVideoFromCameraStream();
                log.info("Grab thread is finished");
            });
            Thread threadWrite = new Thread(() -> {
                try {
                    log.info("Writer thread is started");
                    writeFullCameraVideo();
                    log.info("Writer thread is finished");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            threadGrab.setPriority(Thread.MAX_PRIORITY);
            threadGrab.start();
            threadWrite.start();

            threadGrab.join();
            threadWrite.join();
            grabber.stop();
            log.info("Grabber stopped");

            splitVideo();

        } catch (InterruptedException | FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        } finally{
            try {
                grabber.stop();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }

            startTime = null;
            finishTime = null;
            trigerTimeList.clear();
        }
    }

    private void grabVideoFromCameraStream() {
        try {
            grabber = new OpenCVFrameGrabber(STREAM_VIDEO_RESOURCE);
            grabber.start();
            startTime = LocalDateTime.now();
            log.info("Grabber started");

            while (statusVideoStreaming) {
                opencv_core.IplImage iplImage = grabber.grab();
                videoImageQueue.put(iplImage);
            }
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    private void writeFullCameraVideo() throws InterruptedException {
        long nextFrameTime = 0;
        long frameRate = DEFAULT_TIME_UNIT.convert(40, TimeUnit.MILLISECONDS);

        IMediaWriter writer = ToolFactory.makeWriter("fullVideo.mov");
        writer.addVideoStream(0, 0, imageWidth, imageHeight);
        log.info("Writer is started");

        while (statusVideoStreaming || !videoImageQueue.isEmpty()) {
            writer.encodeVideo(0, videoImageQueue.take().getBufferedImage(), nextFrameTime, DEFAULT_TIME_UNIT);
            nextFrameTime += frameRate;
        }
        writer.close();
        log.info("Writer is close");
    }

    private void splitVideo() {
        for (int i = 0; i < trigerTimeList.size(); i++) {
            long second = ChronoUnit.SECONDS.between(startTime, trigerTimeList.get(i));
            cutVideo(second, "video-" + (i + 1));
        }
    }

    private void cutVideo(long second, String videoName) {
        IMediaWriter writer = ToolFactory.makeWriter(videoName + ".mov");
        try {

            grabber = new OpenCVFrameGrabber(VIDEO_RESOURCE);
            grabber.start();

            writer.addVideoStream(0, 0, imageWidth, imageHeight);

            long fps = 25;
            long millisecondsPerFrame = DEFAULT_TIME_UNIT.convert(40, TimeUnit.MILLISECONDS);


            long nextFrameTime = 0;
            long frameNumber = (Math.max(second - 5, 0)) * fps;
            grabber.setFrameNumber((int) frameNumber);

            while (grabber.getFrameNumber() < Math.min((second + 11) * fps, grabber.getLengthInFrames())) {
                writer.encodeVideo(0, grabber.grab().getBufferedImage(), nextFrameTime, DEFAULT_TIME_UNIT);
                nextFrameTime += millisecondsPerFrame;
            }

        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        } finally{
            try {
                grabber.stop();
                writer.close();
                log.info("Video is done");
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //    void videoCutting() {
//        long nextFrameTime = 0;
//        IMediaWriter writer = ToolFactory.makeWriter("test.mov");
//        int counter = 0;
//
//    try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber("rtsp://admin:ololo111!@192.168.1.107:554/Streaming/Channels/101");
//        Java2DFrameConverter converter = new Java2DFrameConverter()) {
//            grabber.start();
//            grabber.setFormat("mjpeg");
////            grabber.setPixelFormat();
//
//            System.out.println("Video opened: ");
//
//            Frame frame = null;
//
//            while ((frame = grabber.grabImage()) != null || counter != 1000) {
////                BufferedImage image = converter.getBufferedImage(frame);
//                BufferedImage image = new BufferedImage(frame.imageWidth, frame.imageHeight, BufferedImage.TYPE_USHORT_555_RGB);
//                Java2DFrameConverter.copy(frame, image);
//
//                writer.encodeVideo(0, image, nextFrameTime, DEFAULT_TIME_UNIT);
//                nextFrameTime += grabber.getTimestamp();
//
//                counter++;
//            }
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        } finally{
//            writer.close();
//        }
//    }

//    void cutVideo(int second) {
//        Video4j.init();
//
//        try(VideoFile video = Videos.open("video.mp4")) {
//            double fps = video.fps();
////            long totalFrames = video.length();
//
//            long firstFrame = (long) fps * second - 10;
//            long lastFrame = (long) fps * second + 5;
//
//            AtomicLong nextFrameTime = new AtomicLong();
////            long frameRate = DEFAULT_TIME_UNIT.convert(500, TimeUnit.MILLISECONDS);
//            long frameRate = 1000 / (long) fps;
//            int width = video.width();
//            int height = video.height();
//
////            video.seekToFrame(firstFrame);
//
//            List<VideoFrame> frames = new ArrayList<>();
////            while (video.currentFrame() != lastFrame) {
////                frames.add(video.currentFrame())
////            }
////            video.
//            IMediaWriter writer = ToolFactory.makeWriter("test.mov");
//            writer.addListener(ToolFactory.makeViewer(
//                    IMediaViewer.Mode.VIDEO_ONLY,
//                    true,
//                    WindowConstants.EXIT_ON_CLOSE
//            ));
//            writer.addVideoStream(0, 0, width, height);
//
//            video.streamFrames()
//                    .skip(firstFrame - 1)
//                    .filter(vf -> vf.number() <= lastFrame)
//                    .map(VideoFrame::toImage)
//                    .forEach(vf -> {
//                        writer.encodeVideo(0, vf, 0, DEFAULT_TIME_UNIT);
//                        nextFrameTime.addAndGet(frameRate);});
//
////            PreviewGenerator gen = new PreviewGenerator(128, 3, 3);
////            gen.preview(video);
//            writer.close();
//
//        }
//    }
}