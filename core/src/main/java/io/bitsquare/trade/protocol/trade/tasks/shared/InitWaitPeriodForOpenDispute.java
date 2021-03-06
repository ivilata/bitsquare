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

import io.bitsquare.common.taskrunner.TaskRunner;
import io.bitsquare.trade.Trade;
import io.bitsquare.trade.protocol.trade.tasks.TradeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitWaitPeriodForOpenDispute extends TradeTask {
    private static final Logger log = LoggerFactory.getLogger(InitWaitPeriodForOpenDispute.class);

    public InitWaitPeriodForOpenDispute(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            int openDisputeTimeAsBlockHeight = processModel.getTradeWalletService().getLastBlockSeenHeight()
                    + trade.getOffer().getPaymentMethod().getMaxTradePeriod();
            trade.setOpenDisputeTimeAsBlockHeight(openDisputeTimeAsBlockHeight);

            int checkPaymentTimeAsBlockHeight = processModel.getTradeWalletService().getLastBlockSeenHeight()
                    + trade.getOffer().getPaymentMethod().getMaxTradePeriod() / 2;
            trade.setCheckPaymentTimeAsBlockHeight(checkPaymentTimeAsBlockHeight);

            complete();
        } catch (Throwable t) {
            failed(t);
        }
    }
}
