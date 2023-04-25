(ns status-im2.contexts.chat.bottom-sheet-composer.actions.style
  (:require
    [quo2.foundations.colors :as colors]
    [react-native.reanimated :as reanimated]
    [status-im2.contexts.chat.bottom-sheet-composer.constants :as constants]))

(def actions-container
  {:height          constants/actions-container-height
   :justify-content :space-between
   :align-items     :center
   :z-index         2
   :flex-direction  :row})

(defn send-button
  [opacity z-index]
  (reanimated/apply-animations-to-style
   {:opacity opacity}
   {:position         :absolute
    :right            0
    :z-index          z-index
    :background-color (colors/theme-colors colors/white colors/neutral-95)}))