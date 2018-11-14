package oms.test.conference.apitest;

import static oms.base.MediaCodecs.AudioCodec.G722;
import static oms.base.MediaCodecs.AudioCodec.ILBC;
import static oms.base.MediaCodecs.AudioCodec.ISAC;
import static oms.base.MediaCodecs.AudioCodec.OPUS;
import static oms.base.MediaCodecs.AudioCodec.PCMA;
import static oms.base.MediaCodecs.AudioCodec.PCMU;
import static oms.base.MediaCodecs.VideoCodec.H264;
import static oms.base.MediaCodecs.VideoCodec.VP8;
import static oms.base.MediaCodecs.VideoCodec.VP9;
import static oms.test.conference.util.ConferenceAction.applyOption;
import static oms.test.conference.util.ConferenceAction.createClient;
import static oms.test.conference.util.ConferenceAction.createPublishOptions;
import static oms.test.conference.util.ConferenceAction.createSubscribeOptions;
import static oms.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static oms.test.conference.util.ConferenceAction.getRemoteMixStream;
import static oms.test.conference.util.ConferenceAction.getStats;
import static oms.test.conference.util.ConferenceAction.getToken;
import static oms.test.conference.util.ConferenceAction.join;
import static oms.test.conference.util.ConferenceAction.publish;
import static oms.test.conference.util.ConferenceAction.stop;
import static oms.test.conference.util.ConferenceAction.subscribe;
import static oms.test.util.CommonAction.checkRTCStats;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.AUDIO_ONLY_PRESENTER_ROLE;
import static oms.test.util.Config.MIXED_STREAM_SIZE;
import static oms.test.util.Config.PRESENTER_ROLE;
import static oms.test.util.Config.SLEEP;
import static oms.test.util.Config.TIMEOUT;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;
import static oms.test.util.Config.VIDEO_ONLY_VIEWER_ROLE;
import static oms.test.util.Config.VIEWER_ROLE;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import oms.base.MediaCodecs;
import oms.base.MediaCodecs.AudioCodec;
import oms.base.MediaCodecs.VideoCodec;
import oms.conference.Publication;
import oms.conference.PublishOptions;
import oms.conference.RemoteStream;
import oms.conference.SubscribeOptions;
import oms.conference.Subscription;
import oms.test.conference.util.ConferenceClientObserver;
import oms.test.util.Config;
import oms.test.util.FakeRenderer;
import oms.test.util.TestCallback;

import org.webrtc.RTCStatsReport;

import java.util.HashMap;
import java.util.List;

public class SubscribeTest extends TestBase {
    @LargeTest
    public void testSubscribe_beforeJoin_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            client2 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            subscribe(client2, mixSteam, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client2 = null;
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_withoutOption_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            Subscription subscription = subscribe(client1, mixSteam, null, true, true);
            RTCStatsReport statsReport = getStats(subscription, true);
            HashMap<String, String> expectation = new HashMap<>();
            expectation.put("videoCodec", "VP8");
            checkRTCStats(statsReport, expectation, false, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_withVideoCodec_shouldSucceed() {
        try {
            VideoCodec[] videoCodecs = new VideoCodec[]{VP8, VP9, H264};
            String[] checkCodecs = new String[]{"vp8", "vp9", "h264"};
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            PublishOptions publishOptions = createPublishOptions(new MediaCodecs.AudioCodec[]{},
                    new VideoCodec[]{H264});
            publish(client1, localStream1, publishOptions, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            for (int i = 0; i < videoCodecs.length; i++) {
                SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                        new VideoCodec[]{videoCodecs[i]}, null);
                Subscription subscription = subscribe(client2, forwardStream, subOption, true,
                        true);
                RTCStatsReport statsReport = getStats(subscription, true);
                HashMap<String, String> expectation = new HashMap<>();
                expectation.put("videoCodec", checkCodecs[i]);
                checkRTCStats(statsReport, expectation, false, true, true);
                stop(subscription, forwardStream, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_withAudioCodec_shouldSucceed() {
        try {
            AudioCodec[] audioCodecs = new AudioCodec[]{OPUS, PCMU, PCMA, G722, ISAC, ILBC};
            String[] checkCodecs = new String[]{"opus", "pcmu", "pcma", "g722", "isac", "ilbc"};
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            PublishOptions publishOptions = createPublishOptions(new MediaCodecs.AudioCodec[]{OPUS},
                    new VideoCodec[]{});
            publish(client1, localStream1, publishOptions, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            for (int i = 0; i < audioCodecs.length; i++) {
                SubscribeOptions subOption = createSubscribeOptions(
                        new AudioCodec[]{audioCodecs[i]},
                        new VideoCodec[]{}, null);
                Subscription subscription = subscribe(client2, forwardStream, subOption, true,
                        true);
                RTCStatsReport statsReport = getStats(subscription, true);
                HashMap<String, String> expectation = new HashMap<>();
                expectation.put("audioCodec", checkCodecs[i]);
                checkRTCStats(statsReport, expectation, false, true, true);
                stop(subscription, forwardStream, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_withBitrateMultiplier_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            List<Double> bitrateMultipliers =
                    mixSteam.subscriptionCapability.videoSubscriptionCapabilities
                            .bitrateMultipliers;
            for (Double bitrateMultiplier : bitrateMultipliers) {
                HashMap<String, String> videoParams = new HashMap<>();
                videoParams.put("bitrateMultiplier", String.valueOf(bitrateMultiplier));
                SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                        new VideoCodec[]{}, videoParams);
                Subscription subscription = subscribe(client1, mixSteam, subOption, true, true);
                stop(subscription, mixSteam, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_twiceOnSameStream_shouldFailAt2nd() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(null);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            client1.addObserver(observer1);
            client2.addObserver(observer2);
            publish(client1, localStream1, null, observer1, true);
            RemoteStream remoteMixStream = getRemoteMixStream(client2);
            subscribe(client2, remoteMixStream, null, true, true);
            subscribe(client2, remoteMixStream, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_NullStream_shouldThrowException() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            try {
                subscribe(client1, null, null, false, false);
                fail("RuntimeException expected.");
            } catch (RuntimeException ignored) {
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_videoOnly_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(false, capturer1);
            publish(client1, localStream1, null, observer1, true);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(null,
                    new MediaCodecs.VideoCodec[]{},
                    null);
            Subscription subscription = subscribe(client1, forwardStream, subOption, true, true);
            RTCStatsReport statsReport = getStats(subscription, true);
            checkRTCStats(statsReport, null, false, false, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_audioOnly_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            localStream1 = createLocalStream(true, null);
            publish(client1, localStream1, null, observer1, true);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(new MediaCodecs.AudioCodec[]{},
                    null,
                    null);
            Subscription subscription = subscribe(client1, forwardStream, subOption, false, true);
            RTCStatsReport statsReport = getStats(subscription, true);
            checkRTCStats(statsReport, null, false, true, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_videoOnlyStreamWithAudioOnly_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(false, capturer1);
            publish(client1, localStream1, null, observer1, true);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(new MediaCodecs.AudioCodec[]{},
                    null,
                    null);
            subscribe(client1, forwardStream, subOption, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_audioOnlyStreamWithVideoOnly_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            localStream1 = createLocalStream(true, null);
            publish(client1, localStream1, null, observer1, true);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(null,
                    new MediaCodecs.VideoCodec[]{},
                    null);
            subscribe(client1, forwardStream, subOption, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_withResolution_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            List<HashMap<String, Integer>> resolutions =
                    mixSteam.subscriptionCapability.videoSubscriptionCapabilities.resolutions;
            for (HashMap<String, Integer> resolution : resolutions) {
                HashMap<String, String> videoParams = new HashMap<>();
                videoParams.put("width", String.valueOf(resolution.get("width")));
                videoParams.put("height", String.valueOf(resolution.get("height")));
                SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                        new VideoCodec[]{}, videoParams);
                Subscription subscription = subscribe(client1, mixSteam, subOption, true, true);
                stop(subscription, mixSteam, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_withViewerRole_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(VIEWER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{},
                    null);
            subscribe(client2, forwardStream, subOption, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_withVideoOnlyViewer_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(VIDEO_ONLY_VIEWER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{},
                    null);
            subscribe(client2, forwardStream, subOption, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_withAudioOnlyPresenter_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(AUDIO_ONLY_PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{},
                    null);
            subscribe(client2, forwardStream, subOption, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_audioOnlyByAudioOnlyPresenter_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(AUDIO_ONLY_PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{}, null, null);
            subscribe(client2, forwardStream, subOption, false, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_videoOnlyWithAudioOnlyPresenter_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(AUDIO_ONLY_PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(null, new VideoCodec[]{}, null);
            subscribe(client2, forwardStream, subOption, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_videoOnlyWithVideoOnlyViewer_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(VIDEO_ONLY_VIEWER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(null, new VideoCodec[]{}, null);
            subscribe(client2, forwardStream, subOption, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_audioWithVideoOnlyViewer_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(VIDEO_ONLY_VIEWER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                    new VideoCodec[]{},
                    null);
            subscribe(client2, forwardStream, subOption, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_withKeyFrameInterval_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            List<Integer> keyFrameIntervals =
                    forwardStream.subscriptionCapability.videoSubscriptionCapabilities
                            .keyFrameIntervals;
            for (int keyFrameInterval : keyFrameIntervals) {
                HashMap<String, String> videoParams = new HashMap<>();
                videoParams.put("keyFrameInterval", String.valueOf(keyFrameInterval));
                SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                        new VideoCodec[]{}, videoParams);
                Subscription subscription = subscribe(client1, forwardStream, subOption, true,
                        true);
                stop(subscription, forwardStream, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_withFrameRate_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            List<Integer> frameRates =
                    forwardStream.subscriptionCapability.videoSubscriptionCapabilities.frameRates;
            for (int frameRate : frameRates) {
                HashMap<String, String> videoParams = new HashMap<>();
                videoParams.put("frameRate", String.valueOf(frameRate));
                SubscribeOptions subOption = createSubscribeOptions(new AudioCodec[]{},
                        new VideoCodec[]{}, videoParams);
                Subscription subscription = subscribe(client1, forwardStream, subOption, true,
                        true);
                stop(subscription, forwardStream, true);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @SmallTest
    @LargeTest
    public void testSubscribe_differentStream_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            int streamsN = client1.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client1, streamsN - 1);
            subscribe(client1, mixSteam, null, true, true);
            subscribe(client1, forwardStream, null, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_afterSubscriptionStop_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            Subscription subscription = subscribe(client2, forwardStream, null, true, true);
            stop(subscription, forwardStream, true);
            subscribe(client2, forwardStream, null, true, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_twiceWithoutWaitCallBack_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            TestCallback<Subscription> callback1 = new TestCallback<>();
            TestCallback<Subscription> callback2 = new TestCallback<>();
            client1.subscribe(forwardStream, callback1);
            client2.subscribe(forwardStream, callback2);
            assertTrue(callback1.getResult(true, TIMEOUT));
            assertTrue(callback2.getResult(true, TIMEOUT));
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_onStreamEndedRemoteStream_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(client1, localStream1, null, observer2, true);
            assertTrue(observer2.getResultForPublish(Config.TIMEOUT));
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            Subscription subscription = subscribe(client2, forwardStream, null, true, true);
            stop(publication, observer2, true);
            subscription.stop();
            subscribe(client2, forwardStream, null, false, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }finally {
            client1.addObserver(observer1);
        }
    }

    @LargeTest
    public void testSubscribe_applyOption_shouldSucceed() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            Subscription subscription = subscribe(client1, mixSteam, null, true, true);
            HashMap<String, String> videoParams = new HashMap<>();
            HashMap<String, Integer> resolution =
                    mixSteam.subscriptionCapability.videoSubscriptionCapabilities.resolutions.get(
                            0);
            videoParams.put("width", String.valueOf(resolution.get("width")));
            videoParams.put("height", String.valueOf(resolution.get("height")));
            applyOption(subscription, videoParams, true);
            FakeRenderer renderer = new FakeRenderer();
            mixSteam.attach(renderer);
            assertTrue(localStream1.hasVideo() == (renderer.getFramesRendered(SLEEP) != 0));
            assertTrue(renderer.frameHeight() == resolution.get("height"));
            assertTrue(renderer.frameWidth() == resolution.get("width"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testSubscribe_applyOptionWrongParameter_shouldFail() {
        try {
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1 = createClient(observer1);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(client1, localStream1, null, observer1, true);
            RemoteStream mixSteam = getRemoteMixStream(client1);
            Subscription subscription = subscribe(client1, mixSteam, null, true, true);
            HashMap<String, String> videoParams = new HashMap<>();
            videoParams.put("width", "-1");
            videoParams.put("height", "-1");
            applyOption(subscription, videoParams, false);
            videoParams = new HashMap<>();
            videoParams.put("bitrateMultiplier", "-1");
            applyOption(subscription, videoParams, false);
            videoParams = new HashMap<>();
            videoParams.put("frameRate", "-1");
            applyOption(subscription, videoParams, false);
            videoParams = new HashMap<>();
            videoParams.put("keyFrameInterval", "-1");
            applyOption(subscription, videoParams, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
