package io.bitsquare.testbed;

import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.Utils;
import io.bitsquare.p2p.seed.SeedNode;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

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

        // TODO: Check if setting a security provider is needed.
        // TODO: Setup an executor and wait.

        SeedNode seedNode = new SeedNode(dataDir.toString());
        seedNode.createAndStartP2PService(seedAddr, SeedNode.MAX_CONNECTIONS_DEFAULT,
                seedAddr.hostName.equals("localhost"), REGTEST_NETWORK_ID,
                false /*detailed logging*/, allSeedAddrs, null /*TODO: listener*/);
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
}
