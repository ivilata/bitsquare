package io.bitsquare.testbed;

import io.bitsquare.common.UserThread;
import io.bitsquare.common.crypto.KeyRing;
import io.bitsquare.common.crypto.PubKeyRing;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.P2PServiceListener;
import io.bitsquare.p2p.peers.Broadcaster;
import io.bitsquare.p2p.storage.messages.BroadcastMessage;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

public class ControllerPeerApp extends PeerApp {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("missing HOSTNAME:PORT address of seed node");
            System.exit(1);
        }
        final NodeAddress seedAddr = new NodeAddress(args[0]);

        initEnvironment("Peer");
        new ControllerPeerApp(seedAddr).run();
    }

    /** How often to send a hello broadcast message to peers (in seconds). */
    private static final int HELLO_INTERVAL_SECS = 10;

    private Broadcaster broadcaster;

    private ControllerPeerApp(NodeAddress seedAddr) {
        super(seedAddr);
    }

    void start() {
        broadcaster = new Broadcaster(peer.getNetworkNode(), peer.getPeerManager());
        UserThread.runPeriodically(this::broadcastHello, HELLO_INTERVAL_SECS, TimeUnit.SECONDS);
    }

    private void broadcastHello() {
        final KeyRing keyRing = peer.getKeyRing();
        checkState(keyRing != null, "keyring missing in already bootstrapped node");
        broadcaster.broadcast(
                new SeedNodeHelloMessage(keyRing.getPubKeyRing()),
                peer.getAddress(), null, true);
     }

    @Override
    P2PServiceListener newTestbedListener() {
        return new ControllerPeerListener(this);
    }
}

class ControllerPeerListener extends TestbedListener {
    private ControllerPeerApp controllerPeerApp;

    ControllerPeerListener(ControllerPeerApp controllerPeerApp) {
        this.controllerPeerApp = controllerPeerApp;
    }

    @Override
    public void onHiddenServicePublished() {
        super.onHiddenServicePublished();
        controllerPeerApp.start();
    }
}

class SeedNodeHelloMessage extends BroadcastMessage {
    public final long localTimeMillis = System.currentTimeMillis();
    public PubKeyRing pubKeyRing;

    SeedNodeHelloMessage(PubKeyRing pubKeyRing) {
        this.pubKeyRing = pubKeyRing;
    }
}
