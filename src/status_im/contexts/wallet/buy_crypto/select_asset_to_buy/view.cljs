(ns status-im.contexts.wallet.buy-crypto.select-asset-to-buy.view
  (:require
    [quo.core :as quo]
    [react-native.core :as rn]
    [status-im.contexts.wallet.buy-crypto.select-asset-to-buy.style :as style]
    [status-im.contexts.wallet.common.asset-list.view :as asset-list]
    [utils.i18n :as i18n]
    [utils.navigation :as navigation]
    [utils.re-frame :as rf]))

(defn- search-input
  [search-text on-change-text]
  [rn/view {:style style/search-input-container}
   [quo/input
    {:small?         true
     :placeholder    (i18n/label :t/search-assets)
     :icon-name      :i/search
     :value          search-text
     :on-change-text on-change-text}]])

(defn- assets-view
  [chain-ids search-text on-change-text]
  (let [on-token-press (fn [token]
                         (rf/dispatch [:wallet/get-crypto-on-ramp-url {:token token}]))]
    [:<>
     [search-input search-text on-change-text]
     [asset-list/view
      {:chain-ids      chain-ids
       :search-text    search-text
       :on-token-press on-token-press}]]))

(defn view
  []
  (let [network                       (rf/sub [:wallet/wallet-buy-crypto-network])
        [search-text set-search-text] (rn/use-state "")
        on-change-text                #(set-search-text %)]
    (rn/use-unmount (fn []
                      (rf/dispatch [:wallet.buy-crypto/clean-all])))
    [rn/safe-area-view {:style style/container}
     [quo/page-nav
      {:icon-name           :i/close
       :on-press            navigation/navigate-back
       :accessibility-label :top-bar}]
     [quo/page-top
      {:title                     (i18n/label :t/select-asset-to-buy)
       :title-accessibility-label :title-label}]
     [assets-view (mapv #(:chain-id %) network) search-text on-change-text]]))
