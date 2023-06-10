(ns status-im2.contexts.chat.home.style
  (:require [react-native.platform :as platform]))

(def tabs
  {:padding-horizontal 20
   :padding-top        16
   :padding-bottom     12})

(def blur
  {:position :absolute
   :top      0
   :right    0
   :left     0
   :bottom   0})

(defn blur-container
  [top]
  {:overflow    (if platform/ios? :visible :hidden)
   :position    :absolute
   :z-index     1
   :top         0
   :right       0
   :left        0
   :padding-top top})

(def header-height 245)

(defn header-space
  [top]
  {:height (+ header-height top)})

(defn empty-content-container
  [top]
  {:flex            1
   :margin-top      (+ header-height top)
   :margin-bottom   44
   :justify-content :center})
