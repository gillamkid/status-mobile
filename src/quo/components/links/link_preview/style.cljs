(ns quo.components.links.link-preview.style
  (:require
    [quo.foundations.colors :as colors]))

(defn container
  [preview-enabled? theme]
  (merge {:border-width       1
          :border-color       (colors/theme-colors colors/neutral-20 colors/neutral-80 theme)
          :background-color   (colors/theme-colors colors/white colors/neutral-80-opa-40 theme)
          :border-radius      16
          :padding-horizontal 12
          :padding-top        10
          :padding-bottom     12}
         (when-not preview-enabled?
           {:height          139
            :align-items     :center
            :justify-content :center})))

(def header-container
  {:flex-direction :row
   :align-items    :center})

(def title
  {:flex          1
   :margin-bottom 2})

(defn link
  [theme]
  {:margin-top 8
   :color      (colors/theme-colors colors/neutral-50 colors/neutral-40 theme)})

(defn thumbnail
  [size]
  {:width         "100%"
   :height        (if (= size :large) 271 139)
   :margin-top    12
   :border-radius 12})

(def logo
  {:margin-right  6
   :width         16
   :height        16
   :border-radius 8})
