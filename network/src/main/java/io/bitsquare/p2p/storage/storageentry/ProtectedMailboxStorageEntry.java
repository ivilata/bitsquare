package io.bitsquare.p2p.storage.storageentry;

import io.bitsquare.app.Version;
import io.bitsquare.common.crypto.Sig;
import io.bitsquare.p2p.storage.P2PDataStorage;
import io.bitsquare.p2p.storage.payload.MailboxStoragePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class ProtectedMailboxStorageEntry extends ProtectedStorageEntry {
    // That object is sent over the wire, so we need to take care of version compatibility.
    private static final long serialVersionUID = Version.P2P_NETWORK_VERSION;
   
    private static final Logger log = LoggerFactory.getLogger(P2PDataStorage.class);

    public transient PublicKey receiversPubKey;
    private final byte[] receiversPubKeyBytes;

    public MailboxStoragePayload getMailboxStoragePayload() {
        return (MailboxStoragePayload) storagePayload;
    }

    public ProtectedMailboxStorageEntry(MailboxStoragePayload mailboxStoragePayload, PublicKey ownerStoragePubKey, int sequenceNumber, byte[] signature, PublicKey receiversPubKey) {
        super(mailboxStoragePayload, ownerStoragePubKey, sequenceNumber, signature);

        this.receiversPubKey = receiversPubKey;
        this.receiversPubKeyBytes = new X509EncodedKeySpec(this.receiversPubKey.getEncoded()).getEncoded();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            in.defaultReadObject();
            receiversPubKey = KeyFactory.getInstance(Sig.KEY_ALGO, "BC").generatePublic(new X509EncodedKeySpec(receiversPubKeyBytes));
            updateTimeStamp();
        } catch (Throwable t) {
            log.error("Exception at readObject: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "ProtectedMailboxData{" +
                "receiversPubKey.hashCode()=" + receiversPubKey.hashCode() +
                "} " + super.toString();
    }
}
