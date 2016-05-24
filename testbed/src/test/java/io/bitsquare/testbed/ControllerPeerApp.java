package io.bitsquare.testbed;

import io.bitsquare.common.UserThread;
import io.bitsquare.common.crypto.KeyRing;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.P2PServiceListener;
import io.bitsquare.p2p.peers.Broadcaster;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

/**
 * Controller peer application for testbed experiments.
 *
 * You may run this class to create a controller peer on the regression test network
 * which can contact other testbed peers with the help of a seed node
 * and manage them for running a test.
 *
 * You must provide an argument with
 * the {@code HOSTNAME:PORT} address of the seed node.
 */
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

    /** Setup the sending and reception of messages. */
    void start() {
        broadcaster = new Broadcaster(peer.getNetworkNode(), peer.getPeerManager());
        UserThread.runPeriodically(this::broadcastHello, HELLO_INTERVAL_SECS, TimeUnit.SECONDS);
    }

    /** Periodically announce public key so that other peers can send us direct messages. */
    private void broadcastHello() {
        final KeyRing keyRing = peer.getKeyRing();
        checkState(keyRing != null, "keyring missing in already bootstrapped node");
        testLog("XXXX SEND_HELLO");
        broadcaster.broadcast(
                new ControllerPeerHelloMessage(peer.getNetworkNode().getNodeAddress(), keyRing.getPubKeyRing()),
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
