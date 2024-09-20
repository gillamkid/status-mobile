(ns status-im.contexts.wallet.send.send-amount.view
  (:require
    [quo.theme]
    [status-im.contexts.wallet.send.input-amount.view :as input-amount]
    [status-im.setup.hot-reload :as hot-reload]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(defn view
  []
  (hot-reload/use-safe-unmount #(rf/dispatch [:wallet/suggested-routes-cleanup]))
  [input-amount/view
   {:current-screen-id      :screen/wallet.send-input-amount
    :button-one-label       (i18n/label :t/review-send)
    :enabled-from-chain-ids (rf/sub
                             [:wallet/wallet-send-enabled-from-chain-ids])
    :on-confirm             (fn [amount]
                              (rf/dispatch [:wallet/stop-get-suggested-routes])
                              (rf/dispatch [:wallet/set-token-amount-to-send
                                            {:amount   amount
                                             :stack-id :screen/wallet.send-input-amount}]))
    :from-enabled-networks  (rf/sub [:wallet/wallet-send-enabled-networks])
    :on-navigate-back       (fn []
                              (rf/dispatch [:wallet/clean-disabled-from-networks])
                              (rf/dispatch [:wallet/clean-from-locked-amounts])
                              (rf/dispatch [:wallet/clean-send-amount]))}])
