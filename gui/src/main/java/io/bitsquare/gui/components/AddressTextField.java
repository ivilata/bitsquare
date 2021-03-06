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

package io.bitsquare.gui.components;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import io.bitsquare.common.util.Utilities;
import io.bitsquare.gui.main.MainView;
import io.bitsquare.gui.main.overlays.popups.Popup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;

public class AddressTextField extends AnchorPane {
    private static final Logger log = LoggerFactory.getLogger(AddressTextField.class);

    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty paymentLabel = new SimpleStringProperty();
    private final ObjectProperty<Coin> amountAsCoin = new SimpleObjectProperty<>();


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public AddressTextField() {
        TextField textField = new TextField();
        textField.setId("address-text-field");
        textField.setEditable(false);
        textField.textProperty().bind(address);
        String tooltipText = "Open your default bitcoin wallet";
        Tooltip.install(textField, new Tooltip(tooltipText));
        textField.setOnMouseClicked(mouseEvent -> openExtWallet());
        textField.focusTraversableProperty().set(focusTraversableProperty().get());
        //TODO app wide focus
        //focusedProperty().addListener((ov, oldValue, newValue) -> textField.requestFocus());

        Label extWalletIcon = new Label();
        extWalletIcon.setLayoutY(3);
        extWalletIcon.getStyleClass().add("copy-icon");
        Tooltip.install(extWalletIcon, new Tooltip(tooltipText));
        AwesomeDude.setIcon(extWalletIcon, AwesomeIcon.SIGNIN);
        extWalletIcon.setOnMouseClicked(e -> openExtWallet());

        Label copyIcon = new Label();
        copyIcon.setLayoutY(3);
        copyIcon.getStyleClass().add("copy-icon");
        Tooltip.install(copyIcon, new Tooltip("Copy address to clipboard"));
        AwesomeDude.setIcon(copyIcon, AwesomeIcon.COPY);
        copyIcon.setOnMouseClicked(e -> {
            if (address.get() != null && address.get().length() > 0)
                Utilities.copyToClipboard(address.get());
        });

        Label qrCode = new Label();
        qrCode.getStyleClass().add("copy-icon");
        qrCode.setLayoutY(3);
        AwesomeDude.setIcon(qrCode, AwesomeIcon.QRCODE);
        Tooltip.install(qrCode, new Tooltip("Show QR code for this address"));
        qrCode.setOnMouseClicked(e -> {
            if (address.get() != null && address.get().length() > 0) {
                final byte[] imageBytes = QRCode
                        .from(getBitcoinURI())
                        .withSize(300, 220)
                        .to(ImageType.PNG)
                        .stream()
                        .toByteArray();
                Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
                ImageView view = new ImageView(qrImage);

                Pane pane = new Pane(view);
                pane.setPrefSize(320, 240);
                view.relocate(10, 10);

                PopOver popOver = new PopOver(pane);
                popOver.setDetachedTitle("Scan QR code for this address");
                popOver.setDetached(true);
                popOver.setOnHiding(windowEvent -> MainView.removeEffect());

                Window window = getScene().getWindow();
                double x = Math.round(window.getX() + (window.getWidth() - 320) / 2);
                double y = Math.round(window.getY() + (window.getHeight() - 240) / 2);
                popOver.show(getScene().getWindow(), x, y);
                MainView.blur();
            }
        });

        AnchorPane.setRightAnchor(qrCode, 5.0);
        AnchorPane.setRightAnchor(copyIcon, 30.0);
        AnchorPane.setRightAnchor(extWalletIcon, 55.0);
        AnchorPane.setRightAnchor(textField, 77.0);
        AnchorPane.setLeftAnchor(textField, 0.0);

        getChildren().addAll(textField, extWalletIcon, copyIcon, qrCode);
    }

    private void openExtWallet() {
        try {
            Utilities.openURI(URI.create(getBitcoinURI()));
        } catch (Exception e) {
            log.warn(e.getMessage());
            new Popup().warning("Opening a default bitcoin wallet application has failed. " +
                    "Perhaps you don't have one installed?").show();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters/Setters
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void setAddress(String address) {
        this.address.set(address);
    }

    public String getAddress() {
        return address.get();
    }

    public StringProperty addressProperty() {
        return address;
    }

    public Coin getAmountAsCoin() {
        return amountAsCoin.get();
    }

    public ObjectProperty<Coin> amountAsCoinProperty() {
        return amountAsCoin;
    }

    public void setAmountAsCoin(Coin amountAsCoin) {
        this.amountAsCoin.set(amountAsCoin);
    }

    public String getPaymentLabel() {
        return paymentLabel.get();
    }

    public StringProperty paymentLabelProperty() {
        return paymentLabel;
    }

    public void setPaymentLabel(String paymentLabel) {
        this.paymentLabel.set(paymentLabel);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private String getBitcoinURI() {
        return address.get() != null ? BitcoinURI.convertToBitcoinURI(address.get(), amountAsCoin.get(),
                paymentLabel.get(), null) : "";
    }
}
