package io.bitsquare.testbed;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.bitsquare.common.Clock;
import io.bitsquare.common.UserThread;
import io.bitsquare.common.crypto.KeyRing;
import io.bitsquare.common.crypto.KeyStorage;
import io.bitsquare.crypto.EncryptionService;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.P2PService;
import io.bitsquare.p2p.P2PServiceListener;
import io.bitsquare.p2p.Utils;
import io.bitsquare.p2p.seed.SeedNodesRepository;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.nio.file.Paths;
import java.security.Security;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Peer application for testbed experiments.
 *
 * You may run this class to create a peer on the regression test network
 * which can contact other testbed peers with the help of a seed node.
 *
 * You must provide an argument with
 * the {@code HOSTNAME:PORT} address of the seed node.
 */
public class PeerApp {
    /** Name of directory under the current one to place data files into. */
    private static final String dataDirName = "testbed-data";
    /** Numeric identifier of the regtest Bitcoin network. */
    private static final int REGTEST_NETWORK_ID = 2;

    public static void main(String[] args) {
        // Build a seed node repository containing only the one given as an argument.
        if (args.length < 1) {
            System.err.println("missing HOSTNAME:PORT address of seed node");
            System.exit(1);
        }
        final NodeAddress seedAddr = new NodeAddress(args[0]);
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

        // Set a security provider to allow key generation, and create peer keys.
        Security.addProvider(new BouncyCastleProvider());
        final KeyStorage peerKeyStorage = new KeyStorage(peerKeysDir);
        final KeyRing peerKeyRing = new KeyRing(peerKeyStorage);
        final EncryptionService peerEncryptionService = new EncryptionService(peerKeyRing);

        // Create a peer with a listener to log interesting events.
        final P2PService peer = new P2PService(allSeedNodes, peerPort, peerTorDir, useLocalhost,
                REGTEST_NETWORK_ID, peerStorageDir, new Clock(), peerEncryptionService, peerKeyRing);
        final P2PServiceListener peerListener = new P2PServiceListener() {
            @Override
            public void onRequestingDataCompleted() {
                testLog("DATA_RECEIVED");
            }

            @Override
            public void onNoSeedNodeAvailable() {
                testLog("NO_SEED_NODE");
            }

            @Override
            public void onNoPeersAvailable() {
                testLog("NO_PEERS");
            }

            @Override
            public void onBootstrapComplete() {
                testLog("BOOTSTRAPPED");
            }

            @Override
            public void onTorNodeReady() {
                testLog("TOR_READY");
            }

            @Override
            public void onHiddenServicePublished() {
                testLog("PUBLISHED");
            }

            @Override
            public void onSetupFailed(Throwable throwable) {
                testLog("SETUP_FAILED");
            }
        };

        // Set the user thread as an independent non-daemon thread,
        // and give it a name and a exception handler to print errors.
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Peer")
                .setUncaughtExceptionHandler((thread, throwable) -> {
                    throwable.printStackTrace();
                    testLog("EXC %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage());
                })
                .build();
        UserThread.setExecutor(Executors.newSingleThreadExecutor(threadFactory));
        // Run peer code in the user thread.
        UserThread.execute(() -> {
            testLog("START");
            peer.start(peerListener);
        });
        // Automatically wait for the non-daemon user thread.
    }

    private static void testLog(String format, Object... args) {
        System.out.println(
                String.format("TB %s %s",
                        Instant.now().toString(),
                        String.format(format, args)
                )
        );
    }
}
