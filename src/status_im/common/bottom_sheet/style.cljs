(ns status-im.common.bottom-sheet.style
  (:require
    [quo.foundations.colors :as colors]
    [react-native.platform :as platform]))

(def ^:private sheet-border-radius 20)

(defn sheet
  [{:keys [max-height]}]
  {:position                :absolute
   :bottom                  0
   :left                    0
   :right                   0
   :z-index                 1
   :max-height              max-height
   :border-top-left-radius  sheet-border-radius
   :border-top-right-radius sheet-border-radius})

(def gradient-bg
  {:position :absolute
   :top      0
   :left     0
   :right    0})

(def shell-bg
  {:position         :absolute
   :background-color (if platform/ios? colors/white-opa-5 colors/neutral-100-opa-90)
   :left             0
   :right            0
   :top              0
   :bottom           0})

(defn sheet-content
  [{:keys [theme padding-bottom shell?]}]
  {:overflow                :scroll
   :padding-bottom          padding-bottom
   :border-top-left-radius  sheet-border-radius
   :border-top-right-radius sheet-border-radius
   :background-color        (if shell?
                              :transparent
                              (colors/theme-colors colors/white colors/neutral-95 theme))})

(defn selected-item
  [theme top bottom selected-item-smaller-than-sheet? border-radius]
  {:position          :absolute
   :top               (when-not selected-item-smaller-than-sheet? (- 0 top))
   :bottom            bottom
   :overflow          :hidden
   :left              0
   :right             0
   :border-radius     border-radius
   :margin-horizontal 8
   :background-color  (colors/theme-colors colors/white colors/neutral-90 theme)})
