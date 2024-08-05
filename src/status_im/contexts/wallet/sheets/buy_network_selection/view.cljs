(ns status-im.contexts.wallet.sheets.buy-network-selection.view
  (:require [quo.core :as quo]
            [quo.foundations.resources :as quo.resources]
            [react-native.core :as rn]
            [status-im.constants :as constants]
            [status-im.contexts.wallet.sheets.network-selection.style :as style]
            [utils.i18n :as i18n]
            [utils.re-frame :as rf]))

(defn- network-item
  [{:keys [network on-select-network]}]
  (let [{:keys [network-name]} network
        mainnet?               (= network-name constants/mainnet-network-name)]
    [quo/network-list
     {:label           (name network-name)
      :network-image   (quo.resources/get-network network-name)
      :on-press        #(on-select-network network)
      :container-style (style/network-list-container mainnet?)}]))

(defn view
  [{:keys [provider account on-select-network]}]
  (let [network-details  (rf/sub [:wallet/network-details])
        mainnet-network  (first network-details)
        layer-2-networks (rest network-details)
        render-fn        (rn/use-callback (fn [network]
                                            [network-item
                                             {:network network
                                              :on-select-network
                                              on-select-network}]))]
    [:<>
     [rn/view {:style style/header-container}
      [quo/text
       {:size   :heading-2
        :weight :semi-bold}
       (i18n/label :t/select-network-for-buying)]
     ]
     (when mainnet-network
       [network-item
        {:network           mainnet-network
         :on-select-network on-select-network}])
     [quo/divider-label {:container-style style/divider-label}
      (i18n/label :t/layer-2)]
     [rn/flat-list
      {:data           (vec layer-2-networks)
       :render-fn      render-fn
       :scroll-enabled false}]]))
