(ns status-im.contexts.wallet.add-account.add-address.component-spec
  (:require
    [status-im.contexts.wallet.add-account.add-address.view :as add-address-to-watch]
    status-im.contexts.wallet.events
    [test-helpers.component :as h]))

(h/describe "select address for watch only account"
  (h/setup-restorable-re-frame)

  (h/before-each
   (fn []
     (h/setup-subs
      {:wallet/scanned-address              nil
       :wallet/lowercased-addresses         #{"0x12e838ae1f769147b12956485dc56e57138f3ac8"
                                              "0x22e838ae1f769147b12956485dc56e57138f3ac8"}
       :alert-banners/top-margin            0
       :wallet/watch-address-activity-state nil
       :profile/customization-color         :blue
       :wallet/currently-added-address      {:title :t/add-address-to-watch
                                             :description :t/enter-eth
                                             :input-title :t/eth-or-ens
                                             :screen :screen/wallet.add-address
                                             :confirm-screen :screen/wallet.confirm-address
                                             :ens? false
                                             :confirm-screen-props
                                             {:button-label :t/add-watched-address
                                              :address-type :t/watched-address
                                              :placeholder  :t/default-watched-address-placeholder}
                                             :adding-address-purpose :watch}})))

  (h/test "validation messages show for already used addressed"
    (h/render-with-theme-provider [add-address-to-watch/view] :dark)
    (h/is-falsy (h/query-by-label-text :error-message))
    (h/fire-event :change-text
                  (h/get-by-label-text :add-address-to-watch)
                  "0x12E838Ae1f769147b12956485dc56e57138f3AC8")
    (-> (h/wait-for #(h/get-by-translation-text :t/address-already-in-use))
        (.then (fn []
                 (h/is-truthy (h/get-by-translation-text :t/address-already-in-use))))))

  (h/test "validation messages show for invalid address"
    (h/render-with-theme-provider [add-address-to-watch/view] :dark)
    (h/is-falsy (h/query-by-label-text :error-message))
    (h/fire-event :change-text (h/get-by-label-text :add-address-to-watch) "0x12E838Ae1f769147b")
    (-> (h/wait-for #(h/get-by-translation-text :t/invalid-address))
        (.then (fn []
                 (h/is-truthy (h/get-by-translation-text :t/invalid-address)))))))
