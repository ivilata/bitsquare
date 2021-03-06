/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.trade.protocol.trade.tasks.shared;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import io.bitsquare.common.UserThread;
import io.bitsquare.common.taskrunner.TaskRunner;
import io.bitsquare.trade.Trade;
import io.bitsquare.trade.protocol.trade.tasks.TradeTask;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

public class BroadcastAfterLockTime extends TradeTask {
    private static final Logger log = LoggerFactory.getLogger(BroadcastAfterLockTime.class);

    public BroadcastAfterLockTime(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            log.debug("ChainHeight/LockTime: {} / {}", processModel.getTradeWalletService().getBestChainHeight(), trade.getLockTimeAsBlockHeight());
            if (trade.getLockTimeAsBlockHeight() == 0 || processModel.getTradeWalletService().getBestChainHeight() >= trade.getLockTimeAsBlockHeight()) {
                broadcastTx();
            } else {
                ListenableFuture<StoredBlock> blockHeightFuture = processModel.getTradeWalletService().getBlockHeightFuture(trade.getPayoutTx());
                blockHeightFuture.addListener(
                        () -> {
                            try {
                                log.debug("Block height reached " + blockHeightFuture.get().getHeight());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                            broadcastTx();
                        },
                        UserThread::execute);
            }
        } catch (Throwable t) {
            failed(t);
        }
    }

    private void broadcastTx() {
        Transaction payoutTx = trade.getPayoutTx();
        checkNotNull(payoutTx, "payoutTx must not be null at BroadcastAfterLockTime.broadcastTx");
        TransactionConfidence.ConfidenceType confidenceType = payoutTx.getConfidence().getConfidenceType();
        if (confidenceType.equals(TransactionConfidence.ConfidenceType.BUILDING)) {
            trade.setState(Trade.State.PAYOUT_BROAD_CASTED);
            complete();
        } else {
            processModel.getTradeWalletService().broadcastTx(payoutTx, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction transaction) {
                    log.debug("BroadcastTx succeeded. Transaction:" + transaction);
                    trade.setState(Trade.State.PAYOUT_BROAD_CASTED);
                    complete();
                }

                @Override
                public void onFailure(@NotNull Throwable t) {
                    failed(t);
                }
            });
        }
    }
}
