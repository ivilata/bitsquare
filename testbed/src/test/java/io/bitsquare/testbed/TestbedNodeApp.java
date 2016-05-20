package io.bitsquare.testbed;

import java.time.Instant;

abstract class TestbedNodeApp {
    /** Name of directory under the current one to place data files into. */
    static final String dataDirName = "testbed-data";
    /** Numeric identifier of the regtest Bitcoin network. */
    static final int REGTEST_NETWORK_ID = 2;

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
