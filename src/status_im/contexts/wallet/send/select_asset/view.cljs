(ns status-im.contexts.wallet.send.select-asset.view
  (:require
   [clojure.string :as string]
   [quo.core :as quo]
   [quo.theme :as quo.theme]
   [react-native.core :as rn]
   [reagent.core :as reagent]
   [status-im.contexts.wallet.common.account-switcher.view :as account-switcher]
   [status-im.contexts.wallet.common.collectibles-tab.view :as collectibles-tab]
   [status-im.contexts.wallet.send.select-asset.style :as style]
   [utils.i18n :as i18n]
   [utils.re-frame :as rf]))

(def tabs-data
  [{:id :tab/assets :label (i18n/label :t/assets) :accessibility-label :assets-tab}
   {:id :tab/collectibles :label (i18n/label :t/collectibles) :accessibility-label :collectibles-tab}])

(defn- asset-component
  []
  (fn [token _ _ _]
    (let [on-press                #(rf/dispatch [:wallet/send-select-token
                                                 {:token    token
                                                  :stack-id :wallet-select-asset}])
          total-balance-formatted (.toFixed (:total-balance token) 2)
          balance-fiat-formatted  (.toFixed (:total-balance-fiat token) 2)
          currency-symbol         (rf/sub [:profile/currency-symbol])]
      [quo/token-network
       {:token       (:symbol token)
        :label       (:name token)
        :token-value (str total-balance-formatted " " (:symbol token))
        :fiat-value  (str currency-symbol balance-fiat-formatted)
        :networks    (:networks token)
        :on-press    on-press}])))

(defn- asset-list
  [search-text]
  (let [filtered-tokens (rf/sub [:wallet/tokens-filtered search-text])]
    [rn/flat-list
     {:data                         filtered-tokens
      :style                        {:flex 1}
      :content-container-style      {:padding-horizontal 8}
      :keyboard-should-persist-taps :handled
      :key-fn                       :id
      :on-scroll-to-index-failed    identity
      :render-fn                    asset-component}]))

(defn- search-input
  [search-text on-change-text]
  [rn/view {:style style/search-input-container}
   [quo/input
    {:small?         true
     :placeholder    (i18n/label :t/search-assets)
     :icon-name      :i/search
     :value          search-text
     :on-change-text on-change-text}]])

(defn collectibles-grid
  [search-text]
  (let [collectibles      (rf/sub [:wallet/current-viewing-account-collectibles-filtered search-text])
        search-performed? (not (string/blank? search-text))]
    [collectibles-tab/view
     {:collectibles         collectibles
      :filtered?            search-performed?
      :on-collectible-press (fn [collectible-id]
                              (js/alert (str "Collectible to send: \n"
                                             collectible-id
                                             "\nNavigation not implemented yet")))}]))

(defn- tab-view
  [search-text selected-tab on-change-text]
  (let [unfiltered-collectibles (rf/sub [:wallet/current-viewing-account-collectibles])
        show-search-input?      (or (= selected-tab :tab/assets)
                                    (and (= selected-tab :tab/collectibles)
                                         (seq unfiltered-collectibles)))]
    [:<>
     (when show-search-input?
       [search-input search-text on-change-text])
     (case selected-tab
       :tab/assets       [asset-list search-text]
       :tab/collectibles [collectibles-grid search-text])]))


(defn- f-view-internal
  []
  (let [selected-tab   (reagent/atom (:id (first tabs-data)))
        search-text    (reagent/atom "")
        on-change-text #(reset! search-text %)
        on-change-tab  #(reset! selected-tab %)
        on-close       #(rf/dispatch [:navigate-back-within-stack :wallet-select-asset])]
    (fn []
      [rn/safe-area-view {:style style/container}
       [rn/scroll-view
        {:content-container-style      {:flex 1}
         :keyboard-should-persist-taps :handled
         :scroll-enabled               false}
        [account-switcher/view
         {:icon-name     :i/arrow-left
          :on-press      on-close
          :switcher-type :select-account}]
        [quo/text-combinations
         {:title                     (i18n/label :t/select-asset)
          :container-style           style/title-container
          :title-accessibility-label :title-label}]
        [quo/segmented-control
         {:size            32
          :blur?           false
          :symbol          false
          :default-active  :tab/assets
          :container-style {:margin-horizontal 20
                            :margin-vertical   8}
          :data            tabs-data
          :on-change       on-change-tab}]
        [tab-view @search-text @selected-tab on-change-text]]])))

(defn- view-internal
  []
  [:f> f-view-internal])

(def view (quo.theme/with-theme view-internal))
