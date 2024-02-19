(ns status-im.common.standard-authentication.standard-auth.slide-button.view
  (:require
    [quo.core :as quo]
    [quo.theme :as quo.theme]
    [react-native.core :as rn]
    [reagent.core :as reagent]
    [status-im.common.standard-authentication.standard-auth.authorize :as authorize]
    [status-im.constants :as constants]
    [utils.re-frame :as rf]))

(defn- view-internal
  [_]
  (let [reset-slider?   (reagent/atom false)
        reset-slider    (fn []
                          (js/setTimeout
                           #(reset! reset-slider? true)
                           200))
        auth-method     (rf/sub [:auth-method])
        biometric-auth? (= auth-method constants/auth-method-biometric)]
    (fn [{:keys [track-text
                 customization-color
                 auth-button-label
                 on-auth-success
                 on-auth-fail
                 auth-button-icon-left
                 size
                 theme
                 blur?
                 on-close
                 container-style]
          :or   {container-style {:flex 1}
                 on-close        #()}}]
      [rn/view {:style container-style}
       [quo/slide-button
        {:size                size
         :customization-color customization-color
         :on-reset            (when @reset-slider? #(reset! reset-slider? false))
         :on-complete         #(authorize/authorize {:on-close              (fn []
                                                                              (on-close)
                                                                              (reset-slider))
                                                     :auth-button-icon-left auth-button-icon-left
                                                     :theme                 theme
                                                     :blur?                 blur?
                                                     :biometric-auth?       biometric-auth?
                                                     :on-auth-success       on-auth-success
                                                     :on-auth-fail          on-auth-fail
                                                     :auth-button-label     auth-button-label})
         :track-icon          (if biometric-auth? :i/face-id :password)
         :track-text          track-text}]])))

(def view (quo.theme/with-theme view-internal))
