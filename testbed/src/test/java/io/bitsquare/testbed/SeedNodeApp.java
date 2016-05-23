package io.bitsquare.testbed;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.bitsquare.common.UserThread;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.Utils;
import io.bitsquare.p2p.seed.SeedNode;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Single seed node application for testbed experiments.
 *
 * You may run this class to create a seed node on the regression test network
 * which can be used by testbed peers to contact others.
 *
 * You may provide an optional argument with
 * a {@code HOSTNAME[:PORT]} address to listen on.
 * Otherwise an automatic local host address will be chosen for you.
 */
public class SeedNodeApp extends TestbedNodeApp implements Runnable {
    /** Create and run a seed node on the given address.
     *
     * Data is stored in a directory under the current one.
     */
    public static void main(String[] args) {
        final NodeAddress seedAddr = newSeedNodeAddress((args.length > 0) ? args[0] : null);
        final Path dataDir = Paths.get(System.getProperty("user.dir"), dataDirName);
        final SeedNodeApp app = new SeedNodeApp(seedAddr, dataDir);

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
        app.run();
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

    private NodeAddress seedNodeAddress;
    private SeedNode seedNode;

    private SeedNodeApp(NodeAddress seedNodeAddress, Path dataDir) {
        this.seedNodeAddress = seedNodeAddress;
        this.seedNode = new SeedNode(dataDir.toString());
    }

    @Override
    public void run() {
        // Set address as the only seed node.
        final Set<NodeAddress> allSeedAddrs = new HashSet<>(1);
        allSeedAddrs.add(seedNodeAddress);
        testLog("ADDRESS %s", seedNodeAddress);

        // Run seed node code in the user thread.
        UserThread.execute(() -> {
            testLog("START");
            seedNode.createAndStartP2PService(
                    seedNodeAddress, SeedNode.MAX_CONNECTIONS_DEFAULT,
                    seedNodeAddress.hostName.equals("localhost"), REGTEST_NETWORK_ID,
                    false /*detailed logging*/, allSeedAddrs, new TestbedListener());
        });
        // Automatically wait for the non-daemon user thread.
    }
}
