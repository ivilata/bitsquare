package io.bitsquare.testbed;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.bitsquare.common.UserThread;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.P2PServiceListener;
import io.bitsquare.p2p.Utils;
import io.bitsquare.p2p.seed.SeedNode;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SeedNodeApp {
    private static final String dataDirName = "testbed-data";
    /** Numeric identifier of the regtest Bitcoin network. */
    private static final int REGTEST_NETWORK_ID = 2;

    public static void main(String[] args) {
        // Get address from the command line and set as the only seed node.
        final NodeAddress seedAddr = newSeedNodeAddress((args.length > 0) ? args[0] : null);
        final Set<NodeAddress> allSeedAddrs = new HashSet<>(1);
        allSeedAddrs.add(seedAddr);

        final Path dataDir = Paths.get(System.getProperty("user.dir"), dataDirName);
        final SeedNode seedNode = new SeedNode(dataDir.toString());
        final P2PServiceListener seedNodeListener = new P2PServiceListener() {
            @Override
            public void onRequestingDataCompleted() {
                // preliminary data not used in single seed node
            }

            @Override
            public void onNoSeedNodeAvailable() {
                // expected in single seed node
            }

            @Override
            public void onNoPeersAvailable() {
                // expected in single seed node
            }

            @Override
            public void onBootstrapComplete() {
                // not used in single seed node
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

        // TODO: Check if setting a security provider is needed.

        // Set the user thread as an independent non-daemon thread,
        // and give it a name and a exception handler to print errors.
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("SeedNode")
                .setUncaughtExceptionHandler((thread, throwable) -> {
                    throwable.printStackTrace();
                    testLog("EXC %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage());
                })
                .build();
        UserThread.setExecutor(Executors.newSingleThreadExecutor(threadFactory));
        // Run seed node code in the user thread.
        UserThread.execute(() -> {
            testLog("START");
            seedNode.createAndStartP2PService(
                    seedAddr, SeedNode.MAX_CONNECTIONS_DEFAULT,
                    seedAddr.hostName.equals("localhost"), REGTEST_NETWORK_ID,
                    false /*detailed logging*/, allSeedAddrs, seedNodeListener);
        });
        // Automatically wait for the non-daemon user thread.
    }

    /** Get a seed node address based on the given string address.
     *
     * The given address has the format {@code HOSTNAME[:PORT]}.
     * If the whole address or any components are missing they are chosen automatically,
     * defaulting to local host addresses.
     */
    private static NodeAddress newSeedNodeAddress(@Nullable String addr) {
        String hostName = "localhost";
        int port = 0;  // choose automatically

        if (addr != null) {  // host name or port given as arguments
            String[] hnp = addr.split(":", 2);
            if (hnp[0].length() > 0)
                hostName = hnp[0];
            if (hnp.length > 1)
                port = Integer.parseInt(hnp[1]);
        }

        if (port == 0) {  // missing port, choose automatically
            // The address is only considered by ``SeedNodesRepository`` if
            // its port ends in the digit matching the network identifier.
            do {
                port = Utils.findFreeSystemPort();
            } while (port % 10 != REGTEST_NETWORK_ID);
        }

        return new NodeAddress(hostName, port);
    }

    /** Print a timestamped testbed log entry to standard output. */
    private static void testLog(String format, Object... args) {
        System.out.println(
                String.format("TB %s %s",
                        Instant.now().toString(),
                        String.format(format, args)
                )
        );
    }
}
