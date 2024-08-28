(ns status-im.contexts.wallet.swap.utils
  (:require
   [status-im.contexts.wallet.common.utils :as utils]
   [status-im.contexts.wallet.common.utils.networks :as network-utils]))

(defn select-asset-to-receive
  [wallet profile asset-to-pay]
  (let [wallet-address         (get-in wallet [:current-viewing-account-address])
        account                (-> wallet
                                   :accounts
                                   vals
                                   (utils/get-account-by-address wallet-address))
        test-networks-enabled? (get profile :test-networks-enabled?)
        networks               (-> (get-in wallet
                                           [:wallet :networks (if test-networks-enabled? :test :prod)])
                                   (network-utils/sorted-networks-with-details))]
    (->> (utils/tokens-with-balance (:tokens account) networks nil)
         (remove #(= (:symbol %) (:symbol asset-to-pay)))
         first)))

(defn select-network
  [{:keys [networks]}]
  (when (= (count networks) 1)
    (first networks)))
