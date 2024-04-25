(ns status-im.contexts.wallet.add-account.add-address.confirm-address.style)

(defn container
  [purpose]
  {:flex       1
   :margin-top (when (= purpose :save) -39)})

(def data-item
  {:margin-horizontal  20
   :padding-vertical   8
   :padding-horizontal 12})

(def save-address-drawer-bar-container
  {:position :absolute
   :left     "50%"
   :right    "50%"})