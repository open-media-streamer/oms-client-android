package oms.test.p2p.apitest;

import static oms.test.p2p.util.P2PAction.checkRemoteStreamEnded;
import static oms.test.p2p.util.P2PAction.connect;
import static oms.test.p2p.util.P2PAction.createPeerClient;
import static oms.test.p2p.util.P2PAction.publish;
import static oms.test.p2p.util.P2PAction.stop;
import static oms.test.util.CommonAction.createDefaultCapturer;
import static oms.test.util.CommonAction.createLocalStream;
import static oms.test.util.Config.P2P_SERVER;
import static oms.test.util.Config.TIMEOUT_LONG;
import static oms.test.util.Config.USER1_NAME;
import static oms.test.util.Config.USER2_NAME;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import oms.p2p.Publication;
import oms.test.p2p.util.P2PClientObserver;
import oms.test.util.TestObserver;

public class StopTest extends TestBase {

    @LargeTest
    public void testStopClient_beforeConnect_shouldBePeaceful() {
        try {
            user1 = createPeerClient(null);
            stop(user1, "DoesNotMatter", null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testStopClient_beforeDoingAnything_shouldBePeaceful() {
        try {
            user1 = createPeerClient(null);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            stop(user1, "DoesNotMatter", null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testStopClient_nullPeer_shouldThrowException() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            publish(user1, localStream1, USER2_NAME, observer2, true, true);
            try {
                stop(user1, null, null);
                fail("RuntimeException expected.");
            } catch (RuntimeException e) {
                stop(user1, USER2_NAME, observer2);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testStopClient_afterPublish_checkEvents() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(user1, USER2_NAME, observer2, publication);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testStopClient_afterStreamAdded_checkEvents() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(user2, USER1_NAME, observer2, publication);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testStopClient_twice_shouldBePeaceful() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(user1, USER2_NAME, observer2, publication);
            stop(user1, USER2_NAME, null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @SmallTest
    @LargeTest
    public void testStopPublication_checkEvents() {
        try {
            observer1 = new P2PClientObserver(USER1_NAME);
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(observer1);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication1 = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            Publication publication2 = publish(user2, localStream1, USER1_NAME, observer1, true,
                    true);
            stop(publication1, observer2, 0, true);
            stop(publication2, observer1, 0, true);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testStopPublication_twice_shouldBePeaceful() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            stop(publication, observer2, 0, true);
            stop(publication, null, 0, false);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @LargeTest
    public void testStopClient_simultaneouslyWithTwoClient_shouldSucceed() {
        try {
            observer2 = new P2PClientObserver(USER2_NAME);
            user1 = createPeerClient(null);
            user2 = createPeerClient(observer2);
            user1.addAllowedRemotePeer(USER2_NAME);
            user2.addAllowedRemotePeer(USER1_NAME);
            connect(user1, USER1_NAME, P2P_SERVER, true);
            connect(user2, USER2_NAME, P2P_SERVER, true);
            capturer1 = createDefaultCapturer();
            localStream1 = createLocalStream(true, capturer1);
            Publication publication = publish(user1, localStream1, USER2_NAME, observer2, true,
                    true);
            TestObserver publicationObserver = new TestObserver();
            publication.addObserver(publicationObserver);
            user1.stop(USER2_NAME);
            user2.stop(USER1_NAME);
            assertTrue(publicationObserver.getResult(TIMEOUT_LONG));
            checkRemoteStreamEnded(observer2.remoteStreamObservers.values());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
