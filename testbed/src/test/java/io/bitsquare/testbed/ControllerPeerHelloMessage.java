package io.bitsquare.testbed;

import io.bitsquare.common.crypto.PubKeyRing;
import io.bitsquare.p2p.storage.messages.BroadcastMessage;

class ControllerPeerHelloMessage extends BroadcastMessage {
    public final long localTimeMillis = System.currentTimeMillis();
    public PubKeyRing pubKeyRing;

    ControllerPeerHelloMessage(PubKeyRing pubKeyRing) {
        this.pubKeyRing = pubKeyRing;
    }
}
