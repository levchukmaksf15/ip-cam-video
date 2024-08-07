package com.videotest.controller;

import com.videotest.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/start")
    public void startVideo() {
        videoService.setStartTime(LocalDateTime.now());
        videoService.cameraTest();
    }

    @GetMapping("/finish")
    public void finishVideo() {
        videoService.setStatusVideoStreaming(false);
        videoService.setFinishTime(LocalDateTime.now());
    }

    @GetMapping("/trigger")
    public void trigger() {
        videoService.getTrigerTimeList().add(LocalDateTime.now());
        log.info("Triggers: " + videoService.getTrigerTimeList());
    }
}