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

package io.bitsquare.gui.main.offer.createoffer;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import io.bitsquare.app.BitsquareApp;
import io.bitsquare.btc.FeePolicy;
import io.bitsquare.common.UserThread;
import io.bitsquare.common.util.Tuple2;
import io.bitsquare.common.util.Tuple3;
import io.bitsquare.common.util.Utilities;
import io.bitsquare.gui.Navigation;
import io.bitsquare.gui.common.view.ActivatableViewAndModel;
import io.bitsquare.gui.common.view.FxmlView;
import io.bitsquare.gui.components.AddressTextField;
import io.bitsquare.gui.components.BalanceTextField;
import io.bitsquare.gui.components.InputTextField;
import io.bitsquare.gui.components.TitledGroupBg;
import io.bitsquare.gui.main.MainView;
import io.bitsquare.gui.main.account.AccountView;
import io.bitsquare.gui.main.account.content.arbitratorselection.ArbitratorSelectionView;
import io.bitsquare.gui.main.account.settings.AccountSettingsView;
import io.bitsquare.gui.main.funds.FundsView;
import io.bitsquare.gui.main.funds.withdrawal.WithdrawalView;
import io.bitsquare.gui.main.offer.OfferView;
import io.bitsquare.gui.main.overlays.popups.Popup;
import io.bitsquare.gui.main.overlays.windows.OfferDetailsWindow;
import io.bitsquare.gui.main.portfolio.PortfolioView;
import io.bitsquare.gui.main.portfolio.openoffer.OpenOffersView;
import io.bitsquare.gui.util.FormBuilder;
import io.bitsquare.gui.util.Layout;
import io.bitsquare.locale.BSResources;
import io.bitsquare.locale.TradeCurrency;
import io.bitsquare.payment.PaymentAccount;
import io.bitsquare.trade.offer.Offer;
import io.bitsquare.user.Preferences;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.bitcoinj.core.Coin;
import org.controlsfx.control.PopOver;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static io.bitsquare.gui.util.FormBuilder.*;
import static javafx.beans.binding.Bindings.createStringBinding;

@FxmlView
public class CreateOfferView extends ActivatableViewAndModel<AnchorPane, CreateOfferViewModel> {

    private final Navigation navigation;
    private final OfferDetailsWindow offerDetailsWindow;

    private ScrollPane scrollPane;
    private GridPane gridPane;
    private ImageView imageView;
    private AddressTextField addressTextField;
    private BalanceTextField balanceTextField;
    private ProgressIndicator spinner;
    private TitledGroupBg payFundsPane;
    private Button nextButton, cancelButton1, cancelButton2, placeOfferButton;
    private InputTextField amountTextField, minAmountTextField, priceTextField, volumeTextField;
    private TextField totalToPayTextField, currencyTextField;
    private Label directionLabel, amountDescriptionLabel, addressLabel, balanceLabel, totalToPayLabel, totalToPayInfoIconLabel, amountBtcLabel, priceCurrencyLabel,
            volumeCurrencyLabel, minAmountBtcLabel, priceDescriptionLabel, volumeDescriptionLabel, spinnerInfoLabel, currencyTextFieldLabel,
            currencyComboBoxLabel;
    private ComboBox<PaymentAccount> paymentAccountsComboBox;
    private ComboBox<TradeCurrency> currencyComboBox;
    private PopOver totalToPayInfoPopover;

    private OfferView.CloseHandler closeHandler;

    private ChangeListener<Boolean> amountFocusedListener;
    private ChangeListener<Boolean> minAmountFocusedListener;
    private ChangeListener<Boolean> priceFocusedListener;
    private ChangeListener<Boolean> volumeFocusedListener;
    private ChangeListener<Boolean> showWarningInvalidBtcDecimalPlacesListener;
    private ChangeListener<Boolean> showWarningInvalidFiatDecimalPlacesPlacesListener;
    private ChangeListener<Boolean> showWarningAdjustedVolumeListener;
    private ChangeListener<String> errorMessageListener;
    private ChangeListener<Boolean> isSpinnerVisibleListener;
    private ChangeListener<Boolean> placeOfferCompletedListener;
    private ChangeListener<Coin> feeFromFundingTxListener;
    private EventHandler<ActionEvent> paymentAccountsComboBoxSelectionHandler;

    private EventHandler<ActionEvent> currencyComboBoxSelectionHandler;
    private int gridRow = 0;
    private final Preferences preferences;
    private ChangeListener<String> tradeCurrencyCodeListener;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, lifecycle
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    private CreateOfferView(CreateOfferViewModel model, Navigation navigation, OfferDetailsWindow offerDetailsWindow, Preferences preferences) {
        super(model);

        this.navigation = navigation;
        this.offerDetailsWindow = offerDetailsWindow;
        this.preferences = preferences;
    }

    @Override
    protected void initialize() {
        addScrollPane();
        addGridPane();
        addPaymentGroup();
        addAmountPriceGroup();
        addFundingGroup();

        createListeners();

        balanceTextField.setup(model.address.get(), model.getFormatter());
        paymentAccountsComboBox.setConverter(new StringConverter<PaymentAccount>() {
            @Override
            public String toString(PaymentAccount paymentAccount) {
                return paymentAccount.getAccountName() + " (" + paymentAccount.getSingleTradeCurrency().getCode() + ", " +
                        BSResources.get(paymentAccount.getPaymentMethod().getId()) + ")";
            }

            @Override
            public PaymentAccount fromString(String s) {
                return null;
            }
        });
    }

    @Override
    protected void activate() {
        addBindings();
        addListeners();

        directionLabel.setText(model.getDirectionLabel());
        amountDescriptionLabel.setText(model.getAmountDescription());
        addressTextField.setAddress(model.getAddressAsString());
        addressTextField.setPaymentLabel(model.getPaymentLabel());

        paymentAccountsComboBox.setItems(model.getPaymentAccounts());
        paymentAccountsComboBox.getSelectionModel().select(model.getPaymentAccount());

        onPaymentAccountsComboBoxSelected();

        if (spinner != null && placeOfferButton.isVisible())
            spinner.setProgress(-1);
    }

    @Override
    protected void deactivate() {
        removeBindings();
        removeListeners();
        if (balanceTextField != null)
            balanceTextField.cleanup();

        if (spinner != null)
            spinner.setProgress(0);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void initWithData(Offer.Direction direction, TradeCurrency tradeCurrency) {
        model.initWithData(direction, tradeCurrency);

        if (direction == Offer.Direction.BUY) {
            imageView.setId("image-buy-large");

            placeOfferButton.setId("buy-button-big");
            placeOfferButton.setText("Review place offer for buying bitcoin");
            nextButton.setId("buy-button");
        } else {
            imageView.setId("image-sell-large");
            // only needed for sell
            totalToPayTextField.setPromptText(BSResources.get("createOffer.fundsBox.totalsNeeded.prompt"));

            placeOfferButton.setId("sell-button-big");
            placeOfferButton.setText("Review place offer for selling bitcoin");
            nextButton.setId("sell-button");
        }
    }

    // called form parent as the view does not get notified when the tab is closed
    public void onClose() {
        // we use model.placeOfferCompleted to not react on close which was triggered by a successful placeOffer
        if (model.dataModel.isWalletFunded.get() && !model.placeOfferCompleted.get())
            new Popup().information("You have already funds paid in.\n" +
                    "In the \"Funds/Available for withdrawal\" section you can withdraw those funds.").show();
    }

    public void setCloseHandler(OfferView.CloseHandler closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void onTabSelected(boolean isSelected) {
        model.dataModel.onTabSelected(isSelected);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI actions
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void onPlaceOffer() {
        if (model.isBootstrapped()) {
            if (model.hasAcceptedArbitrators()) {
                Offer offer = model.createAndGetOffer();
                offerDetailsWindow.onPlaceOffer(() ->
                        model.onPlaceOffer(offer, () ->
                                offerDetailsWindow.hide()))
                        .show(offer);
            } else {
                new Popup().warning("You have no arbitrator selected.\n" +
                        "You need to select at least one arbitrator.")
                        .actionButtonText("Go to \"Arbitrator selection\"")
                        .onAction(() -> navigation.navigateTo(MainView.class, AccountView.class, AccountSettingsView.class, ArbitratorSelectionView.class))
                        .show();
            }
        } else {
            new Popup().information("You need to wait until your are bootstrapped to the network.\n" +
                    "That might take up to about 2 minutes at startup.").show();
        }
    }

    private void onShowPayFundsScreen() {
        model.onShowPayFundsScreen();

        amountTextField.setMouseTransparent(true);
        minAmountTextField.setMouseTransparent(true);
        priceTextField.setMouseTransparent(true);
        volumeTextField.setMouseTransparent(true);
        currencyComboBox.setMouseTransparent(true);
        paymentAccountsComboBox.setMouseTransparent(true);

        spinner.setProgress(-1);

        if (!BitsquareApp.DEV_MODE) {
            String key = "securityDepositInfo";
            new Popup().backgroundInfo("To ensure that both traders follow the trade protocol they need to pay a security deposit.\n\n" +
                    "The deposit will stay in your local trading wallet until the offer gets accepted by another trader.\n" +
                    "It will be refunded to you after the trade has successfully completed.")
                    .actionButtonText("Visit FAQ web page")
                    .onAction(() -> Utilities.openWebPage("https://bitsquare.io/faq#6"))
                    .closeButtonText("I understand")
                    .dontShowAgainId(key, preferences)
                    .show();

            key = "createOfferFundWalletInfo";
            String tradeAmountText = model.isSellOffer() ? "the trade amount, " : "";
            new Popup().headLine("Fund your trading wallet").instruction("You need to pay in " +
                    model.totalToPay.get() + " to your local Bitsquare trading wallet.\n" +
                    "The amount is the sum of " + tradeAmountText + "the security deposit, the trading fee and " +
                    "the bitcoin mining fee.\n\n" +
                    "Please send from your external Bitcoin wallet the exact amount to the address: " +
                    model.getAddressAsString() + "\n" +
                    "(you can copy the address in the screen below after closing that popup)\n\n" +
                    "Make sure you use a sufficiently high mining fee of at least " +
                    model.formatter.formatCoinWithCode(FeePolicy.getMinRequiredFeeForFundingTx()) +
                    " to avoid problems that your transaction does not get confirmed in the blockchain.\n" +
                    "Transactions with a lower fee will not be accepted.\n\n" +
                    "You can see the status of your incoming payment and all the details in the screen below.")
                    .dontShowAgainId(key, preferences)
                    .show();
        }

        nextButton.setVisible(false);
        nextButton.setManaged(false);
        cancelButton1.setVisible(false);
        cancelButton1.setManaged(false);
        cancelButton1.setOnAction(null);

        payFundsPane.setVisible(true);
        totalToPayLabel.setVisible(true);
        totalToPayInfoIconLabel.setVisible(true);
        totalToPayTextField.setVisible(true);
        addressLabel.setVisible(true);
        addressTextField.setVisible(true);
        balanceLabel.setVisible(true);
        balanceTextField.setVisible(true);
        placeOfferButton.setVisible(true);
        cancelButton2.setVisible(true);
        //root.requestFocus();

        setupTotalToPayInfoIconLabel();
    }

    private void onPaymentAccountsComboBoxSelected() {
        PaymentAccount paymentAccount = paymentAccountsComboBox.getSelectionModel().getSelectedItem();
        if (paymentAccount != null) {
            currencyComboBox.setVisible(paymentAccount.hasMultipleCurrencies());
            if (paymentAccount.hasMultipleCurrencies()) {
                currencyComboBox.setItems(FXCollections.observableArrayList(paymentAccount.getTradeCurrencies()));

                // we select combobox following the user currency, if user currency not available in account, we select first
                TradeCurrency tradeCurrency = model.getTradeCurrency();
                if (paymentAccount.getTradeCurrencies().contains(tradeCurrency))
                    currencyComboBox.getSelectionModel().select(tradeCurrency);
                else
                    currencyComboBox.getSelectionModel().select(paymentAccount.getTradeCurrencies().get(0));

                model.onPaymentAccountSelected(paymentAccount);
            } else {
                currencyTextField.setText(paymentAccount.getSingleTradeCurrency().getNameAndCode());
                model.onPaymentAccountSelected(paymentAccount);
                model.onCurrencySelected(paymentAccount.getSingleTradeCurrency());
            }
        } else {
            currencyComboBox.setVisible(false);
            currencyTextField.setText("");
        }
    }

    private void onCurrencyComboBoxSelected() {
        model.onCurrencySelected(currencyComboBox.getSelectionModel().getSelectedItem());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Navigation
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void close() {
        if (closeHandler != null)
            closeHandler.close();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Bindings, Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////


    private void addBindings() {
        amountBtcLabel.textProperty().bind(model.btcCode);
        priceCurrencyLabel.textProperty().bind(createStringBinding(() ->
                model.tradeCurrencyCode.get() + "/" + model.btcCode.get(), model.btcCode, model.tradeCurrencyCode));
        volumeCurrencyLabel.textProperty().bind(model.tradeCurrencyCode);
        minAmountBtcLabel.textProperty().bind(model.btcCode);

        priceDescriptionLabel.textProperty().bind(createStringBinding(() ->
                BSResources.get("createOffer.amountPriceBox.priceDescription", model.tradeCurrencyCode.get()), model.tradeCurrencyCode));

        volumeDescriptionLabel.textProperty().bind(createStringBinding(model.volumeDescriptionLabel::get, model.tradeCurrencyCode, model
                .volumeDescriptionLabel));

        amountTextField.textProperty().bindBidirectional(model.amount);
        minAmountTextField.textProperty().bindBidirectional(model.minAmount);
        priceTextField.textProperty().bindBidirectional(model.price);
        volumeTextField.textProperty().bindBidirectional(model.volume);
        volumeTextField.promptTextProperty().bind(model.volumePromptLabel);

        totalToPayTextField.textProperty().bind(model.totalToPay);
        addressTextField.amountAsCoinProperty().bind(model.totalToPayAsCoin);

        // Validation
        amountTextField.validationResultProperty().bind(model.amountValidationResult);
        minAmountTextField.validationResultProperty().bind(model.minAmountValidationResult);
        priceTextField.validationResultProperty().bind(model.priceValidationResult);
        volumeTextField.validationResultProperty().bind(model.volumeValidationResult);

        // buttons
        placeOfferButton.disableProperty().bind(model.isPlaceOfferButtonDisabled);
        cancelButton2.disableProperty().bind(model.cancelButtonDisabled);

        spinner.visibleProperty().bind(model.isSpinnerVisible);
        spinnerInfoLabel.visibleProperty().bind(model.isSpinnerVisible);
        spinnerInfoLabel.textProperty().bind(model.spinnerInfoText);

        // payment account
        currencyComboBox.prefWidthProperty().bind(paymentAccountsComboBox.widthProperty());
        currencyComboBox.managedProperty().bind(currencyComboBox.visibleProperty());
        currencyComboBoxLabel.visibleProperty().bind(currencyComboBox.visibleProperty());
        currencyComboBoxLabel.managedProperty().bind(currencyComboBox.visibleProperty());
        currencyTextField.visibleProperty().bind(currencyComboBox.visibleProperty().not());
        currencyTextField.managedProperty().bind(currencyComboBox.visibleProperty().not());
        currencyTextFieldLabel.visibleProperty().bind(currencyComboBox.visibleProperty().not());
        currencyTextFieldLabel.managedProperty().bind(currencyComboBox.visibleProperty().not());
    }

    private void removeBindings() {
        amountBtcLabel.textProperty().unbind();
        priceCurrencyLabel.textProperty().unbind();
        volumeCurrencyLabel.textProperty().unbind();
        minAmountBtcLabel.textProperty().unbind();
        priceDescriptionLabel.textProperty().unbind();
        volumeDescriptionLabel.textProperty().unbind();
        amountTextField.textProperty().unbindBidirectional(model.amount);
        minAmountTextField.textProperty().unbindBidirectional(model.minAmount);
        priceTextField.textProperty().unbindBidirectional(model.price);
        volumeTextField.textProperty().unbindBidirectional(model.volume);
        totalToPayTextField.textProperty().unbind();
        addressTextField.amountAsCoinProperty().unbind();
        amountTextField.validationResultProperty().unbind();
        minAmountTextField.validationResultProperty().unbind();
        priceTextField.validationResultProperty().unbind();
        volumeTextField.validationResultProperty().unbind();
        placeOfferButton.disableProperty().unbind();
        cancelButton2.disableProperty().unbind();
        spinner.visibleProperty().unbind();
        spinnerInfoLabel.visibleProperty().unbind();
        spinnerInfoLabel.textProperty().unbind();
        currencyComboBox.managedProperty().unbind();
        currencyComboBoxLabel.visibleProperty().unbind();
        currencyComboBoxLabel.managedProperty().unbind();
        currencyTextField.visibleProperty().unbind();
        currencyTextField.managedProperty().unbind();
        currencyTextFieldLabel.visibleProperty().unbind();
        currencyTextFieldLabel.managedProperty().unbind();
        currencyComboBox.prefWidthProperty().unbind();
        volumeTextField.promptTextProperty().unbind();
    }

    private void createListeners() {
        amountFocusedListener = (o, oldValue, newValue) -> {
            model.onFocusOutAmountTextField(oldValue, newValue, amountTextField.getText());
            amountTextField.setText(model.amount.get());
        };

        minAmountFocusedListener = (o, oldValue, newValue) -> {
            model.onFocusOutMinAmountTextField(oldValue, newValue, minAmountTextField.getText());
            minAmountTextField.setText(model.minAmount.get());
        };
        priceFocusedListener = (o, oldValue, newValue) -> {
            model.onFocusOutPriceTextField(oldValue, newValue, priceTextField.getText());
            priceTextField.setText(model.price.get());
        };
        volumeFocusedListener = (o, oldValue, newValue) -> {
            model.onFocusOutVolumeTextField(oldValue, newValue, volumeTextField.getText());
            volumeTextField.setText(model.volume.get());
        };
        showWarningInvalidBtcDecimalPlacesListener = (o, oldValue, newValue) -> {
            if (newValue) {
                new Popup().warning(BSResources.get("createOffer.amountPriceBox.warning.invalidBtcDecimalPlaces")).show();
                model.showWarningInvalidBtcDecimalPlaces.set(false);
            }
        };
        showWarningInvalidFiatDecimalPlacesPlacesListener = (o, oldValue, newValue) -> {
            if (newValue) {
                new Popup().warning(BSResources.get("createOffer.amountPriceBox.warning.invalidFiatDecimalPlaces")).show();
                model.showWarningInvalidFiatDecimalPlaces.set(false);
            }
        };
        showWarningAdjustedVolumeListener = (o, oldValue, newValue) -> {
            if (newValue) {
                new Popup().warning(BSResources.get("createOffer.amountPriceBox.warning.adjustedVolume")).show();
                model.showWarningAdjustedVolume.set(false);
                volumeTextField.setText(model.volume.get());
            }
        };
        errorMessageListener = (o, oldValue, newValue) -> {
            if (newValue != null)
                UserThread.runAfter(() -> new Popup().error(BSResources.get("createOffer.amountPriceBox.error.message", model.errorMessage.get()) +
                        "\n\nThere have no funds left your wallet yet.\n" +
                        "Please try to restart you application and check your network connection to see if you can resolve the issue.")
                        .show(), 100, TimeUnit.MILLISECONDS);
        };
        isSpinnerVisibleListener = (ov, oldValue, newValue) -> spinner.setProgress(newValue ? -1 : 0);

        feeFromFundingTxListener = (observable, oldValue, newValue) -> {
            log.debug("feeFromFundingTxListener " + newValue);
            if (!model.dataModel.isFeeFromFundingTxSufficient()) {
                new Popup().warning("The mining fee from your funding transaction is not sufficiently high.\n\n" +
                        "You need to use at least a mining fee of " +
                        model.formatter.formatCoinWithCode(FeePolicy.getMinRequiredFeeForFundingTx()) + ".\n\n" +
                        "The fee used in your funding transaction was only " +
                        model.formatter.formatCoinWithCode(newValue) + ".\n\n" +
                        "The trade transactions might take too much time to be included in " +
                        "a block if the fee is too low.\n" +
                        "Please check at your external wallet that you set the required fee and " +
                        "do a funding again with the correct fee.\n\n" +
                        "In the \"Funds/Open for withdrawal\" section you can withdraw those funds.")
                        .closeButtonText("Close")
                        .onClose(() -> {
                            close();
                            navigation.navigateTo(MainView.class, FundsView.class, WithdrawalView.class);
                        })
                        .show();
            }
        };

        paymentAccountsComboBoxSelectionHandler = e -> onPaymentAccountsComboBoxSelected();
        currencyComboBoxSelectionHandler = e -> onCurrencyComboBoxSelected();

        tradeCurrencyCodeListener = (observable, oldValue, newValue) -> {
            priceTextField.clear();
            volumeTextField.clear();
        };

        placeOfferCompletedListener = (o, oldValue, newValue) -> {
            if (BitsquareApp.DEV_MODE) {
                close();
                navigation.navigateTo(MainView.class, PortfolioView.class, OpenOffersView.class);
            } else if (newValue) {
                // We need a bit of delay to avoid issues with fade out/fade in of 2 popups 
                String key = "createOfferSuccessInfo";
                if (preferences.showAgain(key)) {
                    UserThread.runAfter(() -> new Popup().headLine(BSResources.get("createOffer.success.headline"))
                                    .feedback(BSResources.get("createOffer.success.info"))
                                    .dontShowAgainId(key, preferences)
                                    .actionButtonText("Go to \"My open offers\"")
                                    .onAction(() -> {
                                        UserThread.runAfter(() ->
                                                        navigation.navigateTo(MainView.class, PortfolioView.class, OpenOffersView.class),
                                                100, TimeUnit.MILLISECONDS);
                                        close();
                                    })
                                    .onClose(this::close)
                                    .show(),
                            1);
                } else {
                    close();
                }
            }
        };
    }

    private void addListeners() {
        model.tradeCurrencyCode.addListener(tradeCurrencyCodeListener);

        // focus out
        amountTextField.focusedProperty().addListener(amountFocusedListener);
        minAmountTextField.focusedProperty().addListener(minAmountFocusedListener);
        priceTextField.focusedProperty().addListener(priceFocusedListener);
        volumeTextField.focusedProperty().addListener(volumeFocusedListener);

        // warnings
        model.showWarningInvalidBtcDecimalPlaces.addListener(showWarningInvalidBtcDecimalPlacesListener);
        model.showWarningInvalidFiatDecimalPlaces.addListener(showWarningInvalidFiatDecimalPlacesPlacesListener);
        model.showWarningAdjustedVolume.addListener(showWarningAdjustedVolumeListener);
        model.errorMessage.addListener(errorMessageListener);
        model.isSpinnerVisible.addListener(isSpinnerVisibleListener);
        model.dataModel.feeFromFundingTxProperty.addListener(feeFromFundingTxListener);

        model.placeOfferCompleted.addListener(placeOfferCompletedListener);

        // UI actions
        paymentAccountsComboBox.setOnAction(paymentAccountsComboBoxSelectionHandler);
        currencyComboBox.setOnAction(currencyComboBoxSelectionHandler);
    }

    private void removeListeners() {
        model.tradeCurrencyCode.removeListener(tradeCurrencyCodeListener);

        // focus out
        amountTextField.focusedProperty().removeListener(amountFocusedListener);
        minAmountTextField.focusedProperty().removeListener(minAmountFocusedListener);
        priceTextField.focusedProperty().removeListener(priceFocusedListener);
        volumeTextField.focusedProperty().removeListener(volumeFocusedListener);

        // warnings
        model.showWarningInvalidBtcDecimalPlaces.removeListener(showWarningInvalidBtcDecimalPlacesListener);
        model.showWarningInvalidFiatDecimalPlaces.removeListener(showWarningInvalidFiatDecimalPlacesPlacesListener);
        model.showWarningAdjustedVolume.removeListener(showWarningAdjustedVolumeListener);
        model.errorMessage.removeListener(errorMessageListener);
        model.isSpinnerVisible.removeListener(isSpinnerVisibleListener);
        model.dataModel.feeFromFundingTxProperty.removeListener(feeFromFundingTxListener);

        model.placeOfferCompleted.removeListener(placeOfferCompletedListener);

        // UI actions
        paymentAccountsComboBox.setOnAction(null);
        currencyComboBox.setOnAction(null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Build UI elements
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void addScrollPane() {
        scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setOnScroll(e -> InputTextField.hideErrorMessageDisplay());
        AnchorPane.setLeftAnchor(scrollPane, 0d);
        AnchorPane.setTopAnchor(scrollPane, 0d);
        AnchorPane.setRightAnchor(scrollPane, 0d);
        AnchorPane.setBottomAnchor(scrollPane, 0d);
        root.getChildren().add(scrollPane);
    }

    private void addGridPane() {
        gridPane = new GridPane();
        gridPane.setPadding(new Insets(30, 25, -1, 25));
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        ColumnConstraints columnConstraints1 = new ColumnConstraints();
        columnConstraints1.setHalignment(HPos.RIGHT);
        columnConstraints1.setHgrow(Priority.SOMETIMES);
        columnConstraints1.setMinWidth(200);
        ColumnConstraints columnConstraints2 = new ColumnConstraints();
        columnConstraints2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
        scrollPane.setContent(gridPane);
    }

    private void addPaymentGroup() {
        addTitledGroupBg(gridPane, gridRow, 2, "Select payment account");

        paymentAccountsComboBox = addLabelComboBox(gridPane, gridRow, "Payment account:", Layout.FIRST_ROW_DISTANCE).second;
        paymentAccountsComboBox.setPromptText("Select payment account");

        // we display either currencyComboBox (multi currency account) or currencyTextField (single)
        Tuple2<Label, ComboBox> currencyComboBoxTuple = addLabelComboBox(gridPane, ++gridRow, "Currency:");
        currencyComboBoxLabel = currencyComboBoxTuple.first;
        currencyComboBox = currencyComboBoxTuple.second;
        currencyComboBox.setPromptText("Select currency");
        currencyComboBox.setConverter(new StringConverter<TradeCurrency>() {
            @Override
            public String toString(TradeCurrency tradeCurrency) {
                return tradeCurrency.getNameAndCode();
            }

            @Override
            public TradeCurrency fromString(String s) {
                return null;
            }
        });

        Tuple2<Label, TextField> currencyTextFieldTuple = addLabelTextField(gridPane, gridRow, "Currency:", "", 5);
        currencyTextFieldLabel = currencyTextFieldTuple.first;
        currencyTextField = currencyTextFieldTuple.second;
    }

    private void addAmountPriceGroup() {
        addTitledGroupBg(gridPane, ++gridRow, 2, "Set amount and price", Layout.GROUP_DISTANCE);

        imageView = new ImageView();
        imageView.setPickOnBounds(true);
        directionLabel = new Label();
        directionLabel.setAlignment(Pos.CENTER);
        directionLabel.setPadding(new Insets(-5, 0, 0, 0));
        directionLabel.setId("direction-icon-label");
        VBox imageVBox = new VBox();
        imageVBox.setAlignment(Pos.CENTER);
        imageVBox.setSpacing(6);
        imageVBox.getChildren().addAll(imageView, directionLabel);
        GridPane.setRowIndex(imageVBox, gridRow);
        GridPane.setRowSpan(imageVBox, 2);
        GridPane.setMargin(imageVBox, new Insets(Layout.FIRST_ROW_AND_GROUP_DISTANCE, 10, 10, 10));
        gridPane.getChildren().add(imageVBox);

        addAmountPriceFields();

        addMinAmountBox();

        Tuple2<Button, Button> tuple = add2ButtonsAfterGroup(gridPane, ++gridRow, BSResources.get("createOffer.amountPriceBox.next"), BSResources.get("shared.cancel"));
        nextButton = tuple.first;
        nextButton.disableProperty().bind(model.isNextButtonDisabled);
        //UserThread.runAfter(() -> nextButton.requestFocus(), 100, TimeUnit.MILLISECONDS);
        cancelButton1 = tuple.second;
        cancelButton1.setDefaultButton(false);
        cancelButton1.setOnAction(e -> close());
        cancelButton1.setId("cancel-button");

        GridPane.setMargin(nextButton, new Insets(-35, 0, 0, 0));
        nextButton.setOnAction(e -> onShowPayFundsScreen());
    }

    private void addFundingGroup() {
        // don't increase gridRow as we removed button when this gets visible
        payFundsPane = addTitledGroupBg(gridPane, gridRow, 3, BSResources.get("createOffer.fundsBox.title"), Layout.GROUP_DISTANCE);
        payFundsPane.setVisible(false);

        totalToPayLabel = new Label(BSResources.get("createOffer.fundsBox.totalsNeeded"));
        totalToPayLabel.setVisible(false);
        totalToPayInfoIconLabel = new Label();
        totalToPayInfoIconLabel.setVisible(false);
        HBox totalToPayBox = new HBox();
        totalToPayBox.setSpacing(4);
        totalToPayBox.setAlignment(Pos.CENTER_RIGHT);
        totalToPayBox.getChildren().addAll(totalToPayLabel, totalToPayInfoIconLabel);
        GridPane.setMargin(totalToPayBox, new Insets(Layout.FIRST_ROW_AND_GROUP_DISTANCE, 0, 0, 0));
        GridPane.setRowIndex(totalToPayBox, gridRow);
        gridPane.getChildren().add(totalToPayBox);
        totalToPayTextField = new TextField();
        totalToPayTextField.setEditable(false);
        totalToPayTextField.setFocusTraversable(false);
        totalToPayTextField.setVisible(false);
        GridPane.setRowIndex(totalToPayTextField, gridRow);
        GridPane.setColumnIndex(totalToPayTextField, 1);
        GridPane.setMargin(totalToPayTextField, new Insets(Layout.FIRST_ROW_AND_GROUP_DISTANCE, 0, 0, 0));
        gridPane.getChildren().add(totalToPayTextField);

        Tuple2<Label, AddressTextField> addressTuple = addLabelAddressTextField(gridPane, ++gridRow, BSResources.get("createOffer.fundsBox.address"));
        addressLabel = addressTuple.first;
        addressLabel.setVisible(false);
        addressTextField = addressTuple.second;
        addressTextField.setVisible(false);

        Tuple2<Label, BalanceTextField> balanceTuple = addLabelBalanceTextField(gridPane, ++gridRow, BSResources.get("createOffer.fundsBox.balance"));
        balanceLabel = balanceTuple.first;
        balanceLabel.setVisible(false);
        balanceTextField = balanceTuple.second;
        balanceTextField.setVisible(false);

        Tuple3<Button, ProgressIndicator, Label> placeOfferTuple = addButtonWithStatusAfterGroup(gridPane, ++gridRow, "");
        placeOfferButton = placeOfferTuple.first;
        placeOfferButton.setVisible(false);
        placeOfferButton.setOnAction(e -> onPlaceOffer());
        placeOfferButton.setMinHeight(40);
        placeOfferButton.setPadding(new Insets(0, 20, 0, 20));

        spinner = placeOfferTuple.second;
        spinnerInfoLabel = placeOfferTuple.third;

        cancelButton2 = addButton(gridPane, ++gridRow, BSResources.get("shared.cancel"));
        cancelButton2.setOnAction(e -> {
            if (model.dataModel.isWalletFunded.get())
                new Popup().warning("You have already paid in the funds.\n" +
                        "Are you sure you want to cancel.")
                        .actionButtonText("No")
                        .closeButtonText("Yes, close")
                        .onClose(() -> close())
                        .show();
            else
                close();
        });
        cancelButton2.setDefaultButton(false);
        cancelButton2.setVisible(false);
        cancelButton2.setId("cancel-button");
    }

    private void addAmountPriceFields() {
        // amountBox
        Tuple3<HBox, InputTextField, Label> amountValueCurrencyBoxTuple = FormBuilder.getValueCurrencyBox(BSResources.get("createOffer.amount.prompt"));
        HBox amountValueCurrencyBox = amountValueCurrencyBoxTuple.first;
        amountTextField = amountValueCurrencyBoxTuple.second;
        amountBtcLabel = amountValueCurrencyBoxTuple.third;
        Tuple2<Label, VBox> amountInputBoxTuple = getTradeInputBox(amountValueCurrencyBox, model.getAmountDescription());
        amountDescriptionLabel = amountInputBoxTuple.first;
        VBox amountBox = amountInputBoxTuple.second;

        // x
        Label xLabel = new Label("x");
        xLabel.setFont(Font.font("Helvetica-Bold", 20));
        xLabel.setPadding(new Insets(14, 3, 0, 3));

        // price
        Tuple3<HBox, InputTextField, Label> priceValueCurrencyBoxTuple = FormBuilder.getValueCurrencyBox(BSResources.get("createOffer.price.prompt"));
        HBox priceValueCurrencyBox = priceValueCurrencyBoxTuple.first;
        priceTextField = priceValueCurrencyBoxTuple.second;
        priceCurrencyLabel = priceValueCurrencyBoxTuple.third;
        Tuple2<Label, VBox> priceInputBoxTuple = getTradeInputBox(priceValueCurrencyBox, BSResources.get("createOffer.amountPriceBox.priceDescription"));
        priceDescriptionLabel = priceInputBoxTuple.first;
        VBox priceBox = priceInputBoxTuple.second;

        // =
        Label resultLabel = new Label("=");
        resultLabel.setFont(Font.font("Helvetica-Bold", 20));
        resultLabel.setPadding(new Insets(14, 2, 0, 2));

        // volume
        Tuple3<HBox, InputTextField, Label> volumeValueCurrencyBoxTuple = FormBuilder.getValueCurrencyBox(BSResources.get("createOffer.volume.prompt"));
        HBox volumeValueCurrencyBox = volumeValueCurrencyBoxTuple.first;
        volumeTextField = volumeValueCurrencyBoxTuple.second;
        volumeCurrencyLabel = volumeValueCurrencyBoxTuple.third;
        Tuple2<Label, VBox> volumeInputBoxTuple = getTradeInputBox(volumeValueCurrencyBox, model.volumeDescriptionLabel.get());
        volumeDescriptionLabel = volumeInputBoxTuple.first;
        VBox volumeBox = volumeInputBoxTuple.second;

        HBox hBox = new HBox();
        hBox.setSpacing(5);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getChildren().addAll(amountBox, xLabel, priceBox, resultLabel, volumeBox);
        GridPane.setRowIndex(hBox, gridRow);
        GridPane.setColumnIndex(hBox, 1);
        GridPane.setMargin(hBox, new Insets(Layout.FIRST_ROW_AND_GROUP_DISTANCE, 10, 0, 0));
        gridPane.getChildren().add(hBox);
    }

    private void addMinAmountBox() {
        Tuple3<HBox, InputTextField, Label> amountValueCurrencyBoxTuple = getValueCurrencyBox(BSResources.get("createOffer.amount.prompt"));
        HBox amountValueCurrencyBox = amountValueCurrencyBoxTuple.first;
        minAmountTextField = amountValueCurrencyBoxTuple.second;
        minAmountBtcLabel = amountValueCurrencyBoxTuple.third;

        Tuple2<Label, VBox> amountInputBoxTuple = getTradeInputBox(amountValueCurrencyBox, BSResources.get("createOffer.amountPriceBox" +
                ".minAmountDescription"));
        VBox box = amountInputBoxTuple.second;
        GridPane.setRowIndex(box, ++gridRow);
        GridPane.setColumnIndex(box, 1);
        GridPane.setMargin(box, new Insets(5, 10, 5, 0));
        gridPane.getChildren().add(box);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PayInfo
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void setupTotalToPayInfoIconLabel() {
        totalToPayInfoIconLabel.setId("clickable-icon");
        AwesomeDude.setIcon(totalToPayInfoIconLabel, AwesomeIcon.QUESTION_SIGN);

        totalToPayInfoIconLabel.setOnMouseEntered(e -> createInfoPopover());
        totalToPayInfoIconLabel.setOnMouseExited(e -> {
            if (totalToPayInfoPopover != null)
                totalToPayInfoPopover.hide();
        });
    }

    // As we don't use binding here we need to recreate it on mouse over to reflect the current state
    private void createInfoPopover() {
        GridPane infoGridPane = new GridPane();
        infoGridPane.setHgap(5);
        infoGridPane.setVgap(5);
        infoGridPane.setPadding(new Insets(10, 10, 10, 10));

        int i = 0;
        if (model.isSellOffer())
            addPayInfoEntry(infoGridPane, i++, BSResources.get("createOffer.fundsBox.tradeAmount"), model.tradeAmount.get());

        addPayInfoEntry(infoGridPane, i++, BSResources.get("createOffer.fundsBox.securityDeposit"), model.getSecurityDeposit());
        addPayInfoEntry(infoGridPane, i++, BSResources.get("createOffer.fundsBox.offerFee"), model.getOfferFee());
        addPayInfoEntry(infoGridPane, i++, BSResources.get("createOffer.fundsBox.networkFee"), model.getNetworkFee());
        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        separator.setStyle("-fx-background: #666;");
        GridPane.setConstraints(separator, 1, i++);
        infoGridPane.getChildren().add(separator);
        addPayInfoEntry(infoGridPane, i, BSResources.get("createOffer.fundsBox.total"), model.totalToPay.get());
        totalToPayInfoPopover = new PopOver(infoGridPane);
        if (totalToPayInfoIconLabel.getScene() != null) {
            totalToPayInfoPopover.setDetachable(false);
            totalToPayInfoPopover.setArrowIndent(5);
            totalToPayInfoPopover.show(totalToPayInfoIconLabel.getScene().getWindow(),
                    getPopupPosition().getX(),
                    getPopupPosition().getY());
        }
    }

    private void addPayInfoEntry(GridPane infoGridPane, int row, String labelText, String value) {
        Label label = new Label(labelText);
        TextField textField = new TextField(value);
        textField.setEditable(false);
        textField.setFocusTraversable(false);
        textField.setId("payment-info");
        GridPane.setConstraints(label, 0, row, 1, 1, HPos.RIGHT, VPos.CENTER);
        GridPane.setConstraints(textField, 1, row);
        infoGridPane.getChildren().addAll(label, textField);
    }

    private Point2D getPopupPosition() {
        Window window = totalToPayInfoIconLabel.getScene().getWindow();
        Point2D point = totalToPayInfoIconLabel.localToScene(0, 0);
        double x = point.getX() + window.getX() + totalToPayInfoIconLabel.getWidth() + 2;
        double y = point.getY() + window.getY() + Math.floor(totalToPayInfoIconLabel.getHeight() / 2) - 9;
        return new Point2D(x, y);
    }
}

