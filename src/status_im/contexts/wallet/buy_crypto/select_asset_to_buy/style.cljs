(ns status-im.contexts.wallet.buy-crypto.select-asset-to-buy.style
  (:require [react-native.navigation :as navigation]
            [react-native.platform :as platform]))

(def container
  {:flex-grow   1
   :padding-top (when platform/android? (navigation/status-bar-height))})

(def search-input-container
  {:padding-horizontal 20
   :padding-vertical   8})
