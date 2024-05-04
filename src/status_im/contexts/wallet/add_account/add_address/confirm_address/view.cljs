(ns status-im.contexts.wallet.add-account.add-address.confirm-address.view
  (:require
    [clojure.string :as string]
    [quo.core :as quo]
    [quo.foundations.colors :as colors]
    [quo.theme]
    [react-native.core :as rn]
    [status-im.common.emoji-picker.utils :as emoji-picker.utils]
    [status-im.constants :as constants]
    [status-im.contexts.wallet.add-account.add-address.confirm-address.style :as style]
    [status-im.contexts.wallet.common.screen-base.create-or-edit-account.view :as
     create-or-edit-account]
    [utils.debounce :as debounce]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(def ^:const sheet-closing-delay 750)

(defn- on-press-confirm-address
  [{:keys [adding-address-purpose account-name account-emoji account-color address ens? theme]}]
  (condp = adding-address-purpose
    constants/add-address-to-watch-type (rf/dispatch [:wallet/add-account
                                                      {:sha3-pwd     nil
                                                       :type         constants/add-address-to-watch-type
                                                       :account-name account-name
                                                       :emoji        account-emoji
                                                       :color        account-color}
                                                      {:address    address
                                                       :public-key ""}])
    constants/add-address-to-save-type  (rf/dispatch
                                         [:wallet/save-address
                                          {:address             address
                                           :name                account-name
                                           :customization-color account-color
                                           :ens                 (when ens? address)
                                           :on-success          (fn []
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
                                                                   sheet-closing-delay))}])))

(defn to-be-implemented
  []
  (js/alert "To be implemented"))

(defn view
  []
  (let [theme                                          (quo.theme/use-theme)
        {:keys                  [adding-address-purpose
                                 ens? address]
         {:keys [placeholder
                 address-type
                 button-label]} :confirm-screen-props} (rf/sub [:wallet/currently-added-address])
        placeholder                                    (i18n/label placeholder)
        [account-name on-change-name]                  (rn/use-state "")
        [account-color on-change-color]                (rn/use-state (rand-nth colors/account-colors))
        [account-emoji on-change-emoji]                (rn/use-state (emoji-picker.utils/random-emoji))]
    [:<>
     (when (= adding-address-purpose constants/add-address-to-save-type)
       [rn/view {:style style/save-address-drawer-bar-container}
        [quo/drawer-bar]])
     [rn/view {:style (style/container adding-address-purpose)}
      [create-or-edit-account/view
       {:placeholder         placeholder
        :account-name        account-name
        :account-emoji       account-emoji
        :account-color       account-color
        :on-change-name      on-change-name
        :on-change-color     on-change-color
        :on-change-emoji     on-change-emoji
        :watch-only?         true
        :top-left-icon       :i/arrow-left
        :bottom-action-label button-label
        :bottom-action-props {:customization-color account-color
                              :disabled?           (string/blank? account-name)
                              :accessibility-label :confirm-button-label
                              :on-press            #(on-press-confirm-address
                                                     {:ens?                   ens?
                                                      :adding-address-purpose adding-address-purpose
                                                      :account-name           account-name
                                                      :account-emoji          account-emoji
                                                      :account-color          account-color
                                                      :address                address
                                                      :theme                  theme})}}
       [quo/data-item
        {:card?           true
         :right-icon      :i/advanced
         :icon-right?     true
         :emoji           account-emoji
         :title           (i18n/label address-type)
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
         :on-press        to-be-implemented}]]]]))
