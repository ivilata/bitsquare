package io.bitsquare.testbed;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.bitsquare.common.UserThread;
import io.bitsquare.p2p.P2PServiceListener;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

abstract class TestbedNodeApp {
    /** Name of directory under the current one to place data files into. */
    static final String dataDirName = "testbed-data";
    /** Numeric identifier of the regtest Bitcoin network. */
    static final int REGTEST_NETWORK_ID = 2;

    /** Initialize Bitsquare environment for execution. */
    static void initEnvironment(String userThreadName) {
        // Set the user thread as an independent non-daemon thread,
        // and give it a name and a exception handler to print errors.
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(userThreadName)
                .setUncaughtExceptionHandler((thread, throwable) -> {
                    throwable.printStackTrace();
                    testLog("EXC %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage());
                })
                .build();
        UserThread.setExecutor(Executors.newSingleThreadExecutor(threadFactory));
    }

    /** Print a timestamped testbed log entry to standard output.
     *
     * You can select these lines with a command like
     * {@code egrep '^(ESC\[[0-9;m]+)?TB '}
     * where {@code ESC} is the actual escape character (e.g Ctrl-V Esc in Bash).
     * The reason for such complex regular expression (instead of just {@code '^TB '})
     * is that loggers may colorize output
     * regardless of whether it is being sent to a pipe.
     */
    static void testLog(String format, Object... args) {
        System.out.println(
                String.format("TB %s %s",
                        Instant.now().toString(),
                        String.format(format, args)
                )
        );
    }
}

/** A P2P service listener that logs interesting events. */
class TestbedListener implements P2PServiceListener {
    @Override
    public void onRequestingDataCompleted() {
        TestbedNodeApp.testLog("DATA_RECEIVED");
    }

    @Override
    public void onNoSeedNodeAvailable() {
        TestbedNodeApp.testLog("NO_SEED_NODE");
    }

    @Override
    public void onNoPeersAvailable() {
        TestbedNodeApp.testLog("NO_PEERS");
    }

    @Override
    public void onBootstrapComplete() {
        TestbedNodeApp.testLog("BOOTSTRAPPED");
    }

    @Override
    public void onTorNodeReady() {
        TestbedNodeApp.testLog("TOR_READY");
    }

    @Override
    public void onHiddenServicePublished() {
        TestbedNodeApp.testLog("PUBLISHED");
    }

    @Override
    public void onSetupFailed(Throwable throwable) {
        TestbedNodeApp.testLog("SETUP_FAILED");
    }
}
