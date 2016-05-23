package io.bitsquare.testbed;

import io.bitsquare.common.Clock;
import io.bitsquare.common.UserThread;
import io.bitsquare.common.crypto.KeyRing;
import io.bitsquare.common.crypto.KeyStorage;
import io.bitsquare.crypto.EncryptionService;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.P2PService;
import io.bitsquare.p2p.Utils;
import io.bitsquare.p2p.seed.SeedNodesRepository;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.nio.file.Paths;
import java.security.Security;
import java.util.HashSet;
import java.util.Set;

/**
 * Peer application for testbed experiments.
 *
 * You may run this class to create a peer on the regression test network
 * which can contact other testbed peers with the help of a seed node.
 *
 * You must provide an argument with
 * the {@code HOSTNAME:PORT} address of the seed node.
 */
public class PeerApp extends TestbedNodeApp {
    public static void main(String[] args) {
        // Build a seed node repository containing only the one given as an argument.
        if (args.length < 1) {
            System.err.println("missing HOSTNAME:PORT address of seed node");
            System.exit(1);
        }
        final NodeAddress seedAddr = new NodeAddress(args[0]);

        // Set a security provider to allow key generation.
        Security.addProvider(new BouncyCastleProvider());

        initEnvironment("Peer");
        new PeerApp(seedAddr);
    }

    private PeerApp(NodeAddress seedAddr) {
        final boolean useLocalhost = seedAddr.hostName.equals("localhost");
        final Set<NodeAddress> allSeedAddrs = new HashSet<>(1);
        allSeedAddrs.add(seedAddr);
        final SeedNodesRepository allSeedNodes = new SeedNodesRepository();
        if (useLocalhost)
            allSeedNodes.setLocalhostSeedNodeAddresses(allSeedAddrs);
        else
            allSeedNodes.setTorSeedNodeAddresses(allSeedAddrs);

        // Create peer data directories under the current directory.
        final int peerPort = Utils.findFreeSystemPort();
        final File peerDir = Paths.get(
                System.getProperty("user.dir"), dataDirName, String.format("peer-at-%05d", peerPort))
                .toFile();
        final File peerTorDir = new File(peerDir, "tor");
        final File peerStorageDir = new File(peerDir, "db");
        final File peerKeysDir = new File(peerDir, "keys");
        //noinspection ResultOfMethodCallIgnored
        peerKeysDir.mkdirs();  // needed for creating the key ring

        // Create peer keys.
        final KeyStorage peerKeyStorage = new KeyStorage(peerKeysDir);
        final KeyRing peerKeyRing = new KeyRing(peerKeyStorage);
        final EncryptionService peerEncryptionService = new EncryptionService(peerKeyRing);

        // Create a new peer.
        final P2PService peer = new P2PService(allSeedNodes, peerPort, peerTorDir, useLocalhost,
                REGTEST_NETWORK_ID, peerStorageDir, new Clock(), peerEncryptionService, peerKeyRing);

        // Run peer code in the user thread.
        UserThread.execute(() -> {
            testLog("START");
            peer.start(new TestbedListener());
        });
        // Automatically wait for the non-daemon user thread.
    }
}
