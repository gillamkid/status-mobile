(ns status-im.contexts.wallet.buy-crypto.events
  (:require [re-frame.core :as rf]
            [react-native.core :as rn]
            [status-im.contexts.wallet.sheets.buy-network-selection.view :as buy-network-selection]))

(rf/reg-event-fx :wallet.buy-crypto/select-provider
 (fn [{:keys [db]} [{:keys [account provider recurrent?]}]]
   {:db (-> db
            (assoc-in [:wallet :ui :buy-crypto :account] account)
            (assoc-in [:wallet :ui :buy-crypto :provider] provider)
            (assoc-in [:wallet :ui :buy-crypto :recurrent?] recurrent?))
    :fx [[:dispatch
          [:show-bottom-sheet
           {:content
            (fn []
              [buy-network-selection/view
               {:provider provider
                :account  account}])}]]]}))

(rf/reg-event-fx :wallet.buy-crypto/select-network
 (fn [{:keys [db]} network]
   {:db (assoc-in db [:wallet :ui :buy-crypto :network] network)
    :fx [[:dispatch [:open-modal :screen/wallet.buy-crypto-select-asset-to-buy]]]}))

(rf/reg-event-fx
 :wallet/get-crypto-on-ramp-url
 (fn [{:keys [db]} {:keys [token]}]
   (let [account     (get-in db [:wallet :ui :buy-crypto :account])
         provider    (get-in db [:wallet :ui :buy-crypto :provider])
         network     (get-in db [:wallet :ui :buy-crypto :network])
         recurrent?  (get-in db [:wallet :ui :buy-crypto :recurrent?])
         provider-id (:id provider)
         parameters  {:symbol       (:symbol token)
                      :dest-address (:address account)
                      :chain-id     (:chain-id network)
                      :is-recurrent recurrent?}]
     {:fx [[:json-rpc/call
            [{:method     "wallet_getCryptoOnRampURL"
              :params     [provider-id parameters]
              :on-success (fn [url]
                            (rf/dispatch [:navigate-back])
                            (rf/dispatch [:wallet.buy-crypto/clean-all])
                            (rn/open-url url))
              :on-error   [:wallet/log-rpc-error {:event :wallet/get-crypto-on-ramps}]}]]]})))

(rf/reg-event-fx :wallet.buy-crypto/clean-all
 (fn [{:keys [db]}]
   {:db (update-in db [:wallet :ui] dissoc :buy-crypto)}))
