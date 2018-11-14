package oms.test.conference.apitest;

import static oms.test.conference.util.ConferenceAction.createClient;
import static oms.test.conference.util.ConferenceAction.getRemoteForwardStream;
import static oms.test.conference.util.ConferenceAction.getToken;
import static oms.test.conference.util.ConferenceAction.join;
import static oms.test.conference.util.ConferenceAction.leave;
import static oms.test.conference.util.ConferenceAction.publish;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.MIXED_STREAM_SIZE;
import static oms.test.util.Config.PRESENTER_ROLE;
import static oms.test.util.Config.TIMEOUT;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import oms.conference.RemoteStream;
import oms.test.conference.util.ConferenceClientObserver;

public class LeaveTest extends TestBase {
    @LargeTest
    public void testLeave_shouldBePeaceful() {
        try {
            client1 = createClient(null);
            client1.join(getToken(PRESENTER_ROLE, USER1_NAME), null);
            client1.leave();
            client1 = null;
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @SmallTest
    @LargeTest
    public void testLeave_checkEventsTriggered() {
        try {
            client1 = createClient(null);
            observer2 = new ConferenceClientObserver(USER2_NAME, 1);
            client2 = createClient(observer2);
            join(client1, getToken(PRESENTER_ROLE, USER1_NAME), null, null, true);
            join(client2, getToken(PRESENTER_ROLE, USER2_NAME), observer2, null, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            observer1 = new ConferenceClientObserver(USER1_NAME, 1);
            client1.addObserver(observer1);
            publish(client1, localStream1, null, observer2, true);
            int streamsN = client2.info().getRemoteStreams().size() - MIXED_STREAM_SIZE;
            RemoteStream forwardStream = getRemoteForwardStream(client2, streamsN - 1);
            assertTrue(observer1.getResultForPublish(TIMEOUT));
            leave(client1, observer1, observer2);
            assertTrue(observer2.streamObservers.get(forwardStream.id()).getResult(TIMEOUT));
            client1 = null;
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
