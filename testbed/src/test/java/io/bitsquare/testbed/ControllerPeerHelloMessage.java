package io.bitsquare.testbed;

import io.bitsquare.common.crypto.PubKeyRing;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.storage.messages.BroadcastMessage;

class ControllerPeerHelloMessage extends BroadcastMessage {
    public final long localTimeMillis = System.currentTimeMillis();
    public NodeAddress nodeAddress;
    public PubKeyRing pubKeyRing;

    ControllerPeerHelloMessage(NodeAddress nodeAddress, PubKeyRing pubKeyRing) {
        this.nodeAddress = nodeAddress;
        this.pubKeyRing = pubKeyRing;
    }
}
