(ns status-im.contexts.wallet.add-account.add-address.confirm-address.view
  (:require
    [clojure.string :as string]
    [quo.core :as quo]
    [quo.foundations.colors :as colors]
    [quo.theme]
    [react-native.core :as rn]
    [reagent.core :as reagent]
    [status-im.common.emoji-picker.utils :as emoji-picker.utils]
    [status-im.contexts.wallet.add-account.add-address.confirm-address.style :as style]
    [status-im.contexts.wallet.common.screen-base.create-or-edit-account.view :as
     create-or-edit-account]
    [utils.debounce :as debounce]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(def ^:const sheet-closing-delay 750)

(def ^:private confirming-addresses-purposes
  {:watch {:button-label :t/add-watched-address
           :address-type :t/watched-address
           :placeholder  :t/default-watched-address-placeholder}
   :save  {:button-label :t/save-address
           :address-type :t/address
           :placeholder  :t/saved-address}})

(defn view
  []
  (let [{:keys [address purpose ens?]} (rf/sub [:get-screen-params])
        placeholder                    (-> confirming-addresses-purposes
                                           (get-in [purpose :placeholder])
                                           i18n/label)
        account-name                   (reagent/atom "")
        account-color                  (reagent/atom (rand-nth colors/account-colors))
        account-emoji                  (reagent/atom (emoji-picker.utils/random-emoji))
        on-change-name                 #(reset! account-name %)
        on-change-color                #(reset! account-color %)
        on-change-emoji                #(reset! account-emoji %)
        theme                          (quo.theme/use-theme)]
    (fn []
      [:<>
       (when (= purpose :save)
         [rn/view {:style style/save-address-drawer-bar-container}
          [quo/drawer-bar]])
       [rn/view {:style (style/container purpose)}
        [create-or-edit-account/view
         {:placeholder         placeholder
          :account-name        @account-name
          :account-emoji       @account-emoji
          :account-color       @account-color
          :on-change-name      on-change-name
          :on-change-color     on-change-color
          :on-change-emoji     on-change-emoji
          :watch-only?         true
          :top-left-icon       :i/arrow-left
          :bottom-action-label (-> confirming-addresses-purposes
                                   (get-in [purpose :button-label]))
          :bottom-action-props {:customization-color @account-color
                                :disabled?           (string/blank? @account-name)
                                :accessibility-label :confirm-button-label
                                :on-press            (fn []
                                                       (condp = purpose
                                                         :watch (rf/dispatch [:wallet/add-account
                                                                              {:sha3-pwd nil
                                                                               :type :watch
                                                                               :account-name
                                                                               @account-name
                                                                               :emoji @account-emoji
                                                                               :color @account-color}
                                                                              {:address    address
                                                                               :public-key ""}])
                                                         :save  (rf/dispatch
                                                                 [:wallet/save-address
                                                                  {:address address
                                                                   :name @account-name
                                                                   :customization-color @account-color
                                                                   :ens (when ens? address)
                                                                   :on-success
                                                                   (fn []
                                                                     (rf/dispatch [:navigate-back])
                                                                     (debounce/debounce-and-dispatch
                                                                      [:navigate-back]
                                                                      sheet-closing-delay)
                                                                     (debounce/debounce-and-dispatch
                                                                      [:toasts/upsert
                                                                       {:type  :positive
                                                                        :theme theme
                                                                        :text  (i18n/label
                                                                                :t/address-saved)}]
                                                                      sheet-closing-delay))}])))}}
         [quo/data-item
          {:card?           true
           :right-icon      :i/advanced
           :icon-right?     true
           :emoji           @account-emoji
           :title           (-> confirming-addresses-purposes
                                (get-in [purpose :address-type])
                                i18n/label)
           :subtitle        address
           :status          :default
           :size            :default
           :subtitle-type   :default
           :custom-subtitle (fn [] [quo/text
                                    {:size   :paragraph-2
                                     ;; TODO: monospace font
                                     ;; https://github.com/status-im/status-mobile/issues/17009
                                     :weight :monospace}
                                    address])
           :container-style style/data-item
           :on-press        #(js/alert "To be implemented")}]]]])))