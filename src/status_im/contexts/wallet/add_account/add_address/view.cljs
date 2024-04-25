(ns status-im.contexts.wallet.add-account.add-address.view
  (:require
    [clojure.string :as string]
    [quo.core :as quo]
    [react-native.clipboard :as clipboard]
    [react-native.core :as rn]
    [reagent.core :as reagent]
    [status-im.common.floating-button-page.view :as floating-button-page]
    [status-im.constants :as constants]
    [status-im.contexts.wallet.add-account.add-address.style :as style]
    [status-im.contexts.wallet.common.validation :as validation]
    [status-im.subs.wallet.add-account.address-to-watch]
    [utils.debounce :as debounce]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(def ^:private adding-addresses-purposes
  {:watch {:title       :t/add-address-to-watch
           :description :t/enter-eth
           :input-title :t/eth-or-ens}
   :save  {:title       :t/add-address
           :description :t/save-address-description
           :input-title :t/address-or-end-name}})

(defn- validate-address
  [known-addresses user-input purpose]
  (cond
    (or (nil? user-input) (= user-input ""))     nil
    ;; Allow adding existing address if saving, As it'll upsert
    (and (not= purpose :save)
         (contains? known-addresses user-input)) (i18n/label :t/address-already-in-use)
    (not
     (or (validation/eth-address? user-input)
         (validation/ens-name? user-input)))     (i18n/label :t/invalid-address)))

(defn- extract-address
  [scanned-text]
  (re-find constants/regx-address-contains scanned-text))

(defn- address-input
  [{:keys [input-value validation-msg validate clear-input purpose]}]
  (let [scanned-address (rf/sub [:wallet/scanned-address])
        empty-input?    (and (string/blank? @input-value)
                             (string/blank? scanned-address))
        on-change-text  (fn [new-text]
                          (reset! validation-msg (validate new-text))
                          (reset! input-value new-text)
                          (reagent/flush)
                          (if (and (not-empty new-text) (nil? (validate new-text)))
                            (debounce/debounce-and-dispatch [:wallet/get-address-details new-text]
                                                            500)
                            (rf/dispatch [:wallet/clear-address-activity]))
                          (when (and scanned-address (not= scanned-address new-text))
                            (rf/dispatch [:wallet/clear-address-activity])
                            (rf/dispatch [:wallet/clean-scanned-address])))
        paste-on-input  #(clipboard/get-string
                          (fn [clipboard-text]
                            (on-change-text clipboard-text)))]
    (rn/use-effect (fn []
                     (when-not (string/blank? scanned-address)
                       (on-change-text scanned-address)))
                   [scanned-address])
    [rn/view {:style style/input-container}
     [quo/input
      {:accessibility-label (if (= :watch purpose)
                              :add-address-to-watch
                              :add-address-to-save)
       :placeholder         (i18n/label :t/address-placeholder)
       :container-style     style/input
       :label               (-> adding-addresses-purposes
                                (get-in [purpose :input-title])
                                i18n/label)
       :auto-capitalize     :none
       :multiline?          true
       :on-clear            clear-input
       :return-key-type     :done
       :clearable?          (not empty-input?)
       :on-change-text      on-change-text
       :button              (when empty-input?
                              {:on-press paste-on-input
                               :text     (i18n/label :t/paste)})
       :value               (or scanned-address @input-value)}]
     [quo/button
      {:type            :outline
       :on-press        (fn []
                          (rn/dismiss-keyboard!)
                          (rf/dispatch [:open-modal :screen/wallet.scan-address]))
       :container-style style/scan
       :size            40
       :icon-only?      true}
      :i/scan]]))

(defn activity-indicator
  [activity-state]
  (let [{:keys [message]
         :as   props} (case activity-state
                        :has-activity               {:accessibility-label :account-has-activity
                                                     :icon                :i/done
                                                     :type                :success
                                                     :message             :t/address-activity}
                        :no-activity                {:accessibility-label :account-has-no-activity
                                                     :icon :i/info
                                                     :type :warning
                                                     :message :t/this-address-has-no-activity}
                        :invalid-ens                {:accessibility-label :error-message
                                                     :icon                :i/info
                                                     :type                :error
                                                     :message             :t/invalid-address}
                        :address-already-registered {:accessibility-label :error-message
                                                     :icon                :i/info
                                                     :type                :error
                                                     :message             :t/address-already-in-use}
                        {:accessibility-label :searching-for-activity
                         :icon                :i/pending-state
                         :type                :default
                         :message             :t/searching-for-activity})]
    (when activity-state
      [quo/info-message
       (assoc props
              :style style/info-message
              :size  :default)
       (i18n/label message)])))

(defn view
  []
  (let [addresses            (rf/sub [:wallet/addresses])
        lowercased-addresses (map string/lower-case addresses)
        input-value          (reagent/atom nil)
        {:keys [purpose]}    (rf/sub [:get-screen-params])
        validate             #(validate-address (set lowercased-addresses) (string/lower-case %) purpose)
        validation-msg       (reagent/atom nil)
        clear-input          (fn []
                               (reset! input-value nil)
                               (reset! validation-msg nil)
                               (rf/dispatch [:wallet/clear-address-activity])
                               (rf/dispatch [:wallet/clean-scanned-address]))
        customization-color  (rf/sub [:profile/customization-color])]

    (rf/dispatch [:wallet/clean-scanned-address])
    (rf/dispatch [:wallet/clear-address-activity])
    (fn []
      (let [activity-state    (rf/sub [:wallet/watch-address-activity-state])
            validated-address (rf/sub [:wallet/watch-address-validated-address])]
        [rn/view
         {:style {:flex 1}}
         [quo/drawer-bar]
         [floating-button-page/view
          {:header [quo/page-nav
                    {:type      :no-title
                     :icon-name :i/close
                     :on-press  (fn []
                                  (rf/dispatch [:wallet/clean-scanned-address])
                                  (rf/dispatch [:wallet/clear-address-activity])
                                  (rf/dispatch [:navigate-back]))}]
           :footer [quo/button
                    {:customization-color customization-color
                     :disabled?           (or (string/blank? @input-value)
                                              (some? (validate @input-value))
                                              (= activity-state :invalid-ens)
                                              (= activity-state :scanning)
                                              (not validated-address))
                     :on-press            (fn []
                                            (condp = purpose
                                              :watch (rf/dispatch
                                                      [:navigate-to
                                                       :screen/wallet.confirm-address
                                                       {:purpose purpose
                                                        :address (extract-address
                                                                  validated-address)}])
                                              :save  (rf/dispatch
                                                      [:open-modal
                                                       :screen/wallet.confirm-address-to-save
                                                       {:purpose purpose
                                                        :address (extract-address
                                                                  validated-address)
                                                        :ens?    (and
                                                                  (not (validation/eth-address?
                                                                        validated-address))
                                                                  (validation/ens-name?
                                                                   validated-address))}]))
                                            (clear-input))
                     :container-style     {:z-index 2}}
                    (i18n/label :t/continue)]}
          [quo/page-top
           {:container-style  style/header-container
            :title            (-> adding-addresses-purposes
                                  (get-in [purpose :title])
                                  i18n/label)
            :description      :text
            :description-text (-> adding-addresses-purposes
                                  (get-in [purpose :description])
                                  i18n/label)}]
          [address-input
           {:input-value    input-value
            :validate       validate
            :validation-msg validation-msg
            :clear-input    clear-input
            :purpose        purpose}]
          (if @validation-msg
            [quo/info-message
             {:accessibility-label :error-message
              :size                :default
              :icon                :i/info
              :type                :error
              :style               style/info-message}
             @validation-msg]
            [activity-indicator activity-state])]]))))