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

package io.bitsquare.payment;

import io.bitsquare.app.Version;

import javax.annotation.Nullable;

public final class CryptoCurrencyAccountContractData extends PaymentAccountContractData {
    // That object is sent over the wire, so we need to take care of version compatibility.
    private static final long serialVersionUID = Version.P2P_NETWORK_VERSION;

    private String address;
    // used in crypto note coins. not supported now but hopefully in future, so leave it for now to avoid 
    // incompatibility from serialized data.
    @Nullable
    private String paymentId;

    public CryptoCurrencyAccountContractData(String paymentMethod, String id, int maxTradePeriod) {
        super(paymentMethod, id, maxTradePeriod);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String getPaymentDetails() {
        return "Receivers cryptocurrency address: " + address;
    }

    @Override
    public String getPaymentDetailsForTradePopup() {
        return getPaymentDetails();
    }

    public void setPaymentId(@Nullable String paymentId) {
        this.paymentId = paymentId;
    }

    @Nullable
    public String getPaymentId() {
        return paymentId;
    }
}
