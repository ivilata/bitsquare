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

package io.bitsquare.gui.main.account;

import io.bitsquare.gui.Navigation;
import io.bitsquare.gui.common.view.*;
import io.bitsquare.gui.main.MainView;
import io.bitsquare.gui.main.account.arbitratorregistration.ArbitratorRegistrationView;
import io.bitsquare.gui.main.account.content.fiataccounts.FiatAccountsView;
import io.bitsquare.gui.main.account.settings.AccountSettingsView;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import javax.inject.Inject;

@FxmlView
public class AccountView extends ActivatableView<TabPane, AccountViewModel> {

    @FXML
    Tab accountSettingsTab;

    private Navigation.Listener navigationListener;
    private ChangeListener<Tab> tabChangeListener;

    private final ViewLoader viewLoader;
    private final Navigation navigation;
    private Tab selectedTab;
    Tab arbitratorRegistrationTab;
    private ArbitratorRegistrationView arbitratorRegistrationView;
    private AccountSettingsView accountSettingsView;
    private Scene scene;
    private EventHandler<KeyEvent> keyEventEventHandler;


    @Inject
    private AccountView(AccountViewModel model, CachingViewLoader viewLoader, Navigation navigation) {
        super(model);
        this.viewLoader = viewLoader;
        this.navigation = navigation;
    }

    @Override
    public void initialize() {
        navigationListener = viewPath -> {
            if (viewPath.size() == 3 && viewPath.indexOf(AccountView.class) == 1) {
                if (arbitratorRegistrationTab == null && viewPath.get(2).equals(ArbitratorRegistrationView.class))
                    navigation.navigateTo(MainView.class, AccountView.class, AccountSettingsView.class, FiatAccountsView.class);
                else
                    loadView(viewPath.tip());
            }
        };

        keyEventEventHandler = event -> {
            if (new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN).match(event) &&
                    arbitratorRegistrationTab == null) {
                arbitratorRegistrationTab = new Tab("Arbitrator registration");
                arbitratorRegistrationTab.setClosable(false);
                root.getTabs().add(arbitratorRegistrationTab);
            }
        };

        tabChangeListener = (ov, oldValue, newValue) -> {
            if (newValue == accountSettingsTab) {
                Class<? extends View> selectedViewClass = accountSettingsView.getSelectedViewClass();
                if (selectedViewClass == null)
                    navigation.navigateTo(MainView.class, AccountView.class, AccountSettingsView.class, FiatAccountsView.class);
                else
                    navigation.navigateTo(MainView.class, AccountView.class, AccountSettingsView.class, selectedViewClass);
            } else if (arbitratorRegistrationTab != null) {
                navigation.navigateTo(MainView.class, AccountView.class, ArbitratorRegistrationView.class);
            } else {
                navigation.navigateTo(MainView.class, AccountView.class, AccountSettingsView.class);
            }
        };
    }

    @Override
    protected void activate() {
        navigation.addListener(navigationListener);
        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
        scene = root.getScene();
        if (scene != null)
            scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEventEventHandler);

        if (navigation.getCurrentPath().size() == 2 && navigation.getCurrentPath().get(1) == AccountView.class) {
            if (root.getSelectionModel().getSelectedItem() == accountSettingsTab)
                navigation.navigateTo(MainView.class, AccountView.class, AccountSettingsView.class);
            else if (arbitratorRegistrationTab != null)
                navigation.navigateTo(MainView.class, AccountView.class, ArbitratorRegistrationView.class);
            else
                navigation.navigateTo(MainView.class, AccountView.class, AccountSettingsView.class);
        }
    }

    @Override
    protected void deactivate() {
        navigation.removeListener(navigationListener);
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);

        if (scene != null)
            scene.removeEventHandler(KeyEvent.KEY_RELEASED, keyEventEventHandler);
    }

    private void loadView(Class<? extends View> viewClass) {
        View view = viewLoader.load(viewClass);

        if (view instanceof AccountSettingsView) {
            selectedTab = accountSettingsTab;
            accountSettingsView = (AccountSettingsView) view;
            selectedTab.setText("Account");
            if (arbitratorRegistrationTab != null) {
                arbitratorRegistrationTab.setDisable(false);
                if (arbitratorRegistrationView != null)
                    arbitratorRegistrationView.onTabSelection(false);
            }
        } else if (view instanceof ArbitratorRegistrationView) {
            if (arbitratorRegistrationTab != null) {
                selectedTab = arbitratorRegistrationTab;
                arbitratorRegistrationView = (ArbitratorRegistrationView) view;
                arbitratorRegistrationView.onTabSelection(true);
            }
        } else {
            throw new IllegalArgumentException("View not supported: " + view);
        }

        selectedTab.setContent(view.getRoot());
        root.getSelectionModel().select(selectedTab);
    }
}
