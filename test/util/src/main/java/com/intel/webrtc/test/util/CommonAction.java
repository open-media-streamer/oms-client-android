package com.intel.webrtc.test.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import com.intel.webrtc.base.IcsVideoCapturer;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.MediaConstraints.AudioTrackConstraints;
import com.intel.webrtc.base.MediaConstraints.VideoTrackConstraints;
import com.intel.webrtc.base.VideoCapturer;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.util.HashMap;
import java.util.Map;

public class CommonAction {

    public static VideoCapturer createDefaultCapturer() {
        VideoTrackConstraints vmc = VideoTrackConstraints.create(true);
        vmc.setResolution(1280, 720);
        vmc.setFramerate(30);
        vmc.setCameraFacing(VideoTrackConstraints.CameraFacing.FRONT);
        return new IcsVideoCapturer(vmc);
    }

    public static LocalStream createLocalStream(boolean enableAudio, VideoCapturer capturer) {
        AudioTrackConstraints amc = null;
        if (enableAudio) {
            amc = new AudioTrackConstraints();
        }
        LocalStream localCameraStream = new LocalStream(capturer, amc);
        if (capturer != null) {
            assertTrue(localCameraStream.hasVideo());
        }
        if (enableAudio) {
            assertTrue(localCameraStream.hasAudio());
        }
        return localCameraStream;
    }

    public static void checkRTCStats(RTCStatsReport stats, HashMap<String, String> expectation,
            boolean outbound, boolean audioEnabled, boolean VideoEnabled) {
        String videoCodedId = "";
        String audioCodedId = "";
        boolean videoFound = false;
        boolean audioFound = false;
        Map<String, RTCStats> RTCStats = stats.getStatsMap();
        for (String key : RTCStats.keySet()) {
            RTCStats detailRTCStats = RTCStats.get(key);
            String RTCStatId = detailRTCStats.getId();
            if (RTCStatId.contains("RTCMediaStreamTrack_" + (outbound ? "sender" : "receiver"))) {
                if (detailRTCStats.getMembers().get("kind").equals("audio")) {
                    audioFound = true;
                }
                if (detailRTCStats.getMembers().get("kind").equals("video")) {
                    videoFound = true;
                    if (expectation != null) {
                        if (expectation.containsKey("frameWidth")) {
                            assertEquals(expectation.get("frameWidth"), String.valueOf(
                                    detailRTCStats.getMembers().get("frameWidth")));
                        }
                        if (expectation.containsKey("frameHeight")) {
                            assertEquals(expectation.get("frameHeight"), String.valueOf(
                                    detailRTCStats.getMembers().get("frameHeight")));
                        }
                    }
                }
            }

            if (key.contains(outbound ? "RTCOutboundRTPVideoStream" : "RTCInboundRTPVideoStream")) {
                videoCodedId = String.valueOf(detailRTCStats.getMembers().get("codecId"));
            }
            if (key.contains(outbound ? "RTCOutboundRTPAudioStream" : "RTCInboundRTPAudioStream")) {
                audioCodedId = String.valueOf(detailRTCStats.getMembers().get("codecId"));
            }
        }

        assertTrue(audioEnabled == audioFound);
        assertTrue(VideoEnabled == videoFound);

        if (videoFound && expectation != null && expectation.containsKey("videoCodec")) {
            if (!videoCodedId.equals("")) {
                String videoCodec = String.valueOf(RTCStats.get(videoCodedId).getMembers()
                        .get("mimeType")).split("/")[1];
                assertEquals(expectation.get("videoCodec").toLowerCase(), videoCodec.toLowerCase());
            } else {
                fail("VideoCodecId expected.");
            }
        }
        if (audioFound && expectation != null && expectation.containsKey("audioCodec")) {
            if (!audioCodedId.equals("")) {
                String audioCodec = String.valueOf(RTCStats.get(audioCodedId).getMembers()
                        .get("mimeType")).split("/")[1];
                assertEquals(expectation.get("audioCodec").toLowerCase(), audioCodec.toLowerCase());
            } else {
                fail("AudioCodecId expected.");
            }
        }
    }
}
