(ns status-im.subs.communities-test
  (:require
    [cljs.test :refer [is testing]]
    matcher-combinators.test
    [re-frame.db :as rf-db]
    [status-im.constants :as constants]
    status-im.subs.communities
    [test-helpers.unit :as h]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(def community-id "0x1")

(h/deftest-sub :communities
  [sub-name]
  (testing "returns raw communities"
    (let [raw-communities {"0x1" {:id "0x1"}}]
      (swap! rf-db/app-db assoc
        :communities
        raw-communities)
      (is (match? raw-communities (rf/sub [sub-name]))))))

(h/deftest-sub :communities/section-list
  [sub-name]
  (testing "builds sections using the first community name char (uppercased)"
    (swap! rf-db/app-db assoc
      :communities
      {"0x1" {:name "civilized monkeys"}
       "0x2" {:name "Civilized rats"}})
    (is (match? [{:title "C"
                  :data  [{:name "civilized monkeys"}
                          {:name "Civilized rats"}]}]
                (rf/sub [sub-name]))))

  (testing "sorts by section ascending"
    (swap! rf-db/app-db assoc
      :communities
      {"0x3" {:name "Memorable"}
       "0x1" {:name "Civilized monkeys"}})
    (is (match? [{:title "C" :data [{:name "Civilized monkeys"}]}
                 {:title "M" :data [{:name "Memorable"}]}]
                (rf/sub [sub-name]))))

  (testing "builds default section for communities without a name"
    (swap! rf-db/app-db assoc
      :communities
      {"0x2" {:id "0x2"}
       "0x1" {:id "0x1"}})
    (is (match? [{:title ""
                  :data  [{:id "0x2"}
                          {:id "0x1"}]}]
                (rf/sub [sub-name])))))

(h/deftest-sub :communities/unviewed-counts
  [sub-name]
  (testing "sums counts for a particular community"
    (swap! rf-db/app-db assoc
      :chats
      {"0x100" {:community-id            community-id
                :unviewed-mentions-count 3
                :unviewed-messages-count 2}
       "0x101" {:community-id            "0x2"
                :unviewed-mentions-count 7
                :unviewed-messages-count 9}
       "0x102" {:community-id            community-id
                :unviewed-mentions-count 5
                :unviewed-messages-count 1}})
    (is (match? {:unviewed-messages-count 3
                 :unviewed-mentions-count 8}
                (rf/sub [sub-name community-id]))))

  (testing "defaults to zero when count keys are not present"
    (swap! rf-db/app-db assoc
      :chats
      {"0x100" {:community-id community-id}})
    (is (match? {:unviewed-messages-count 0
                 :unviewed-mentions-count 0}
                (rf/sub [sub-name community-id])))))

(h/deftest-sub :communities/categorized-channels
  [sub-name]
  (testing "Channels with categories"
    (swap! rf-db/app-db assoc
      :communities
      {"0x1" {:id         "0x1"
              :chats      {"0x1"
                           {:id         "0x1"
                            :position   1
                            :name       "chat1"
                            :muted?     nil
                            :categoryID "1"
                            :can-post?  true}
                           "0x2"
                           {:id         "0x2"
                            :position   2
                            :name       "chat2"
                            :muted?     nil
                            :categoryID "1"
                            :can-post?  false}
                           "0x3"
                           {:id         "0x3"
                            :position   3
                            :name       "chat3"
                            :muted?     nil
                            :categoryID "2"
                            :can-post?  true}}
              :categories {"1" {:id       "1"
                                :position 2
                                :name     "category1"}
                           "2" {:id       "2"
                                :position 1
                                :name     "category2"}}
              :joined     true}})
    (is
     (= [["2"
          {:id         "2"
           :name       "category2"
           :collapsed? nil
           :position   1
           :chats      [{:name             "chat3"
                         :position         3
                         :emoji            nil
                         :muted?           nil
                         :locked?          nil
                         :id               "0x3"
                         :unread-messages? false
                         :mentions-count   0}]}]
         ["1"
          {:id         "1"
           :name       "category1"
           :collapsed? nil
           :position   2
           :chats      [{:name             "chat1"
                         :emoji            nil
                         :position         1
                         :muted?           nil
                         :locked?          nil
                         :id               "0x1"
                         :unread-messages? false
                         :mentions-count   0}
                        {:name             "chat2"
                         :emoji            nil
                         :position         2
                         :muted?           nil
                         :locked?          nil
                         :id               "0x2"
                         :unread-messages? false
                         :mentions-count   0}]}]]
        (rf/sub [sub-name "0x1"]))))

  (testing "Channels with categories and token permissions"
    (swap! rf-db/app-db assoc
      :community-channels-permissions
      {community-id
       {(keyword (str community-id "0x100"))
        {:view-only     {:satisfied?  false
                         :permissions {:token-permission-id-01 {:criteria [false]}}}
         :view-and-post {:satisfied? true :permissions {}}}
        (keyword (str community-id "0x200"))
        {:view-only     {:satisfied? true :permissions {}}
         :view-and-post {:satisfied? true :permissions {}}}
        (keyword (str community-id "0x300"))
        {:view-only     {:satisfied? false :permissions {}}
         :view-and-post {:satisfied?  true
                         :permissions {:token-permission-id-03 {:criteria [true]}}}}
        (keyword (str community-id "0x400"))
        {:view-only     {:satisfied?  true
                         :permissions {}}
         :view-and-post {:satisfied?  false
                         :permissions {:token-permission-id-04 {:criteria [false]}}}}}}

      :communities
      {community-id {:id         community-id
                     :chats      {"0x100" {:id         "0x100"
                                           :position   1
                                           :name       "chat1"
                                           :muted?     nil
                                           :categoryID "1"
                                           :can-post?  false}
                                  "0x200" {:id         "0x200"
                                           :position   2
                                           :name       "chat2"
                                           :muted?     nil
                                           :categoryID "1"
                                           :can-post?  false}
                                  "0x300" {:id         "0x300"
                                           :position   3
                                           :name       "chat3"
                                           :muted?     nil
                                           :categoryID "2"
                                           :can-post?  true}
                                  "0x400" {:id         "0x400"
                                           :position   4
                                           :name       "chat4"
                                           :muted?     nil
                                           :categoryID "2"
                                           :can-post?  true}}
                     :categories {"1" {:id       "1"
                                       :position 2
                                       :name     "category1"}
                                  "2" {:id       "2"
                                       :position 1
                                       :name     "category2"}}
                     :joined     true}})
    (is
     (= [["2"
          {:id         "2"
           :name       "category2"
           :collapsed? nil
           :position   1
           :chats      [{:name             "chat3"
                         :position         3
                         :emoji            nil
                         :muted?           nil
                         :locked?          false
                         :id               "0x300"
                         :unread-messages? false
                         :mentions-count   0}
                        {:name             "chat4"
                         :position         4
                         :emoji            nil
                         :muted?           nil
                         :locked?          true
                         :id               "0x400"
                         :unread-messages? false
                         :mentions-count   0}]}]
         ["1"
          {:id         "1"
           :name       "category1"
           :collapsed? nil
           :position   2
           :chats      [{:name             "chat1"
                         :emoji            nil
                         :position         1
                         :muted?           nil
                         :locked?          true
                         :id               "0x100"
                         :unread-messages? false
                         :mentions-count   0}
                        {:name             "chat2"
                         :emoji            nil
                         :position         2
                         :muted?           nil
                         :locked?          nil
                         :id               "0x200"
                         :unread-messages? false
                         :mentions-count   0}]}]]
        (rf/sub [sub-name "0x1"]))))

  (testing "Channels without categories"
    (swap! rf-db/app-db assoc
      :communities
      {"0x1" {:id         "0x1"
              :chats      {"0x1"
                           {:id         "0x1"
                            :position   1
                            :name       "chat1"
                            :categoryID "1"
                            :can-post?  true
                            :muted?     nil}
                           "0x2"
                           {:id         "0x2"
                            :position   2
                            :name       "chat2"
                            :categoryID "1"
                            :can-post?  false
                            :muted?     nil}
                           "0x3" {:id "0x3" :position 3 :name "chat3" :can-post? true :muted? nil}}
              :categories {"1" {:id       "1"
                                :position 1
                                :name     "category1"}
                           "2" {:id       "2"
                                :position 2
                                :name     "category2"}}
              :joined     true}})
    (is
     (=
      [[constants/empty-category-id
        {:name       (i18n/label :t/none)
         :collapsed? nil
         :chats      [{:name             "chat3"
                       :emoji            nil
                       :position         3
                       :locked?          nil
                       :muted?           nil
                       :id               "0x3"
                       :unread-messages? false
                       :mentions-count   0}]}]
       ["1"
        {:name       "category1"
         :id         "1"
         :position   1
         :collapsed? nil
         :chats      [{:name             "chat1"
                       :emoji            nil
                       :position         1
                       :locked?          nil
                       :muted?           nil
                       :id               "0x1"
                       :unread-messages? false
                       :mentions-count   0}
                      {:name             "chat2"
                       :emoji            nil
                       :position         2
                       :locked?          nil
                       :id               "0x2"
                       :muted?           nil
                       :unread-messages? false
                       :mentions-count   0}]}]]
      (rf/sub [sub-name "0x1"]))))

  (testing "Unread messages"
    (swap! rf-db/app-db assoc
      :communities
      {"0x1" {:id         "0x1"
              :chats      {"0x1"
                           {:id "0x1" :position 1 :name "chat1" :categoryID "1" :can-post? true}
                           "0x2"
                           {:id "0x2" :position 2 :name "chat2" :categoryID "1" :can-post? false}}
              :categories {"1" {:id "1" :name "category1"}}
              :joined     true}}
      :chats
      {"0x10x1" {:unviewed-messages-count 1 :unviewed-mentions-count 2}
       "0x10x2" {:unviewed-messages-count 0 :unviewed-mentions-count 0}})
    (is
     (= [["1"
          {:name       "category1"
           :id         "1"
           :collapsed? nil
           :chats      [{:name             "chat1"
                         :emoji            nil
                         :position         1
                         :locked?          nil
                         :id               "0x1"
                         :muted?           nil
                         :unread-messages? true
                         :mentions-count   2}
                        {:name             "chat2"
                         :emoji            nil
                         :position         2
                         :locked?          nil
                         :muted?           nil
                         :id               "0x2"
                         :unread-messages? false
                         :mentions-count   0}]}]]
        (rf/sub [sub-name "0x1"])))))

(h/deftest-sub :communities/my-pending-requests-to-join
  [sub-name]
  (testing "no requests"
    (swap! rf-db/app-db assoc
      :communities/my-pending-requests-to-join
      {})
    (is (match? {}
                (rf/sub [sub-name]))))
  (testing "users requests to join different communities"
    (swap! rf-db/app-db assoc
      :communities/my-pending-requests-to-join
      {:community-id-1 {:id :request-id-1}
       :community-id-2 {:id :request-id-2}})
    (is (match? {:community-id-1 {:id :request-id-1}
                 :community-id-2 {:id :request-id-2}}
                (rf/sub [sub-name])))))

(h/deftest-sub :communities/my-pending-request-to-join
  [sub-name]
  (testing "no request for community"
    (swap! rf-db/app-db assoc
      :communities/my-pending-requests-to-join
      {})
    (is (match? nil
                (rf/sub [sub-name :community-id-1]))))
  (testing "users request to join a specific communities"
    (swap! rf-db/app-db assoc
      :communities/my-pending-requests-to-join
      {:community-id-1 {:id :request-id-1}
       :community-id-2 {:id :request-id-2}})
    (is (match? :request-id-1
                (rf/sub [sub-name :community-id-1])))))

(h/deftest-sub :community/token-gated-overview
  [sub-name]
  (let
    [checking-permissions? true
     token-image-eth       "data:image/jpeg;base64,/9j/2w"
     checks                {:checking? checking-permissions?
                            :check
                            {:satisfied true
                             :highestRole {:type     constants/community-token-permission-become-admin
                                           :criteria [{:tokenRequirement [{:satisfied true
                                                                           :criteria {:contract_addresses
                                                                                      {:5 "0x0"}
                                                                                      :type 1
                                                                                      :symbol "DAI"
                                                                                      :amount "5.0"
                                                                                      :decimals 18}}]}
                                                      {:tokenRequirement [{:satisfied false
                                                                           :criteria  {:type     1
                                                                                       :symbol   "ETH"
                                                                                       :amount   "0.002"
                                                                                       :decimals 18}}]}]}

                             :permissions
                             {:a3dd5b6b-d93b-452c-b22a-09a8f42ec566 {:criteria [true false
                                                                                true]}}
                             :validCombinations
                             [{:address  "0xd722eaa60dc73e334b588d34ba66a3b27e537783"
                               :chainIds nil}
                              {:address  "0x738d3146831c5871fa15872b409e8f360e341784"
                               :chainIds [5 420]}]}}
     community             {:id                      community-id
                            :checking-permissions?   checking-permissions?
                            :permissions             {:access 3}
                            :highest-permission-role constants/community-token-permission-become-admin
                            :token-images            {"ETH" token-image-eth}
                            :name                    "Community super name"
                            :chats                   {"89f98a1e-6776-4e5f-8626-8ab9f855253f"
                                                      {:description "x"
                                                       :emoji "🎲"
                                                       :permissions {:access 1}
                                                       :color "#88B0FF"
                                                       :name "random"
                                                       :categoryID "0c3c64e7-d56e-439b-a3fb-a946d83cb056"
                                                       :id "89f98a1e-6776-4e5f-8626-8ab9f855253f"
                                                       :position 4
                                                       :can-post? false
                                                       :members {"0x04" {"roles" [1]}}}
                                                      "a076358e-4638-470e-a3fb-584d0a542ce6"
                                                      {:description "General channel for the community"
                                                       :emoji "🐷 "
                                                       :permissions {:access 1}
                                                       :color "#4360DF"
                                                       :name "general"
                                                       :categoryID "0c3c64e7-d56e-439b-a3fb-a946d83cb056"
                                                       :id "a076358e-4638-470e-a3fb-584d0a542ce6"
                                                       :position 0
                                                       :can-post? false
                                                       :members {"0x04" {"roles" [1]}}}}
                            :members                 {"0x04" {"roles" [1]}}
                            :can-request-access?     false
                            :outroMessage            "bla"
                            :verified                false}]
    (swap! rf-db/app-db assoc-in [:communities community-id] community)
    (swap! rf-db/app-db assoc-in [:communities/permissions-check community-id] checks)
    (is (match? {:can-request-access?     true
                 :networks-not-supported? nil
                 :tokens                  [[{:symbol      "DAI"
                                             :amount      "5"
                                             :sufficient? true
                                             :loading?    checking-permissions?}]
                                           [{:symbol      "ETH"
                                             :amount      "0.002"
                                             :sufficient? false
                                             :loading?    checking-permissions?
                                             :img-src     token-image-eth}]]}
                (rf/sub [sub-name community-id])))))

(h/deftest-sub :communities/airdrop-account
  [sub-name]
  (testing "returns airdrop account"
    (swap! rf-db/app-db assoc-in [:communities community-id :airdrop-address] "0x1")
    (swap! rf-db/app-db assoc
      :wallet
      {:accounts {"0x1" {:address "0x1"
                         :color   :blue
                         :name    "account1"}
                  "0x2" {:address "0x2"
                         :color   :orange
                         :name    "account2"}}})
    (is (match? {:address                   "0x1"
                 :network-preferences-names #{}
                 :name                      "account1"
                 :color                     :blue
                 :customization-color       :blue}
                (rf/sub [sub-name community-id])))))

(h/deftest-sub :communities/selected-permission-accounts
  [sub-name]
  (testing "returns selected permission accounts"
    (swap! rf-db/app-db assoc-in
      [:communities community-id :selected-permission-addresses]
      #{"0x1" "0x3"})
    (swap! rf-db/app-db assoc
      :wallet
      {:accounts {"0x1" {:address "0x1" :color :blue :name "account1"}
                  "0x2" {:address "0x2" :color :orange :name "account2"}
                  "0x3" {:address "0x3" :color :purple :name "account3"}}})
    (is (match? [{:address                   "0x1"
                  :color                     :blue
                  :customization-color       :blue
                  :network-preferences-names #{}
                  :name                      "account1"}
                 {:address                   "0x3"
                  :color                     :purple
                  :customization-color       :purple
                  :network-preferences-names #{}
                  :name                      "account3"}]
                (rf/sub [sub-name community-id])))))

;; (h/deftest-sub :communities/token-permissions
;;   [sub-name]
;;   (testing "returns token permissions grouped by role"
;;     (swap! rf-db/app-db assoc
;;       :communities
;;       {"0x1" {:token-permissions [[:1cfb5618-5e6a-4695-a91c-19e7e4f0af3a
;;                                    {:id "1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
;;                                     :type 2
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:5        "0x3d6afaa395c31fcd391fe3d562e75fe9e8ec7e6a"
;;                                        :11155111 "0xe452027cdef746c7cd3db31cb700428b16cd8e51"}
;;                                       :type 1
;;                                       :symbol "STT"
;;                                       :amount "1.0000000000000000"
;;                                       :decimals 18}]}]
;;                                   [:6872cfb4-9122-4d39-b58c-5dff441f94d4
;;                                    {:id "6872cfb4-9122-4d39-b58c-5dff441f94d4"
;;                                     :type 6
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:420 "0x98C28243BeFD84f5B83BA49fE843e6Cbd9a55AD4"}
;;                                       :type 2
;;                                       :symbol "OWNPER"
;;                                       :name "Owner-Permission Drawer"
;;                                       :amount "1"}]
;;                                     :is_private true}]
;;                                   [:730c3fd3-3fcf-4d48-934a-b175b571c5fe
;;                                    {:id "730c3fd3-3fcf-4d48-934a-b175b571c5fe"
;;                                     :type 2
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:5        "0x0000000000000000000000000000000000000000"
;;                                        :420      "0x0000000000000000000000000000000000000000"
;;                                        :421613   "0x0000000000000000000000000000000000000000"
;;                                        :421614   "0x0000000000000000000000000000000000000000"
;;                                        :11155111 "0x0000000000000000000000000000000000000000"}
;;                                       :type 1
;;                                       :symbol "ETH"
;;                                       :amount "1.0000000000000000"
;;                                       :decimals 18}]}]
;;                                   [:75a6c876-7be4-4830-a82b-b672986329da
;;                                    {:id "75a6c876-7be4-4830-a82b-b672986329da"
;;                                     :type 5
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:420 "0x2334ab535D0146d4a78C24894Cdc0C466cF3159e"}
;;                                       :type 2
;;                                       :symbol "TMPER"
;;                                       :name "TMaster-Permission Drawer"
;;                                       :amount "1"}]
;;                                     :is_private true}]]}})
;;     (is
;;      (match?
;;       {2 [[{:symbol "STT" :amount "1" :sufficient? true :loading? false :img-src nil}]
;;           [{:symbol "ETH" :amount "1" :sufficient? true :loading? false :img-src nil}]]
;;        5
;;        [[{:symbol "TMPER"
;;           :amount "1"
;;           :sufficient? true
;;           :loading? false
;;           :img-src
;;           "data:image/jpeg;base64,/path/to/jpeg"}]]
;;        6
;;        [[{:symbol "OWNPER"
;;           :amount "1"
;;           :sufficient? true
;;           :loading? false
;;           :img-src
;;           "data:image/jpeg;base64,/path/to/jpeg"}]]}
;;       (rf/sub [sub-name])))))

;; (h/deftest-sub :communities/token-permissions
;;   [sub-name]
;;   (testing "returns token permissions grouped by role"
;;     (swap! rf-db/app-db assoc
;;       :communities
;;       {"0x1" {:token-permissions [[:1cfb5618-5e6a-4695-a91c-19e7e4f0af3a
;;                                    {:id "1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
;;                                     :type 2
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:5        "0x3d6afaa395c31fcd391fe3d562e75fe9e8ec7e6a"
;;                                        :11155111 "0xe452027cdef746c7cd3db31cb700428b16cd8e51"}
;;                                       :type 1
;;                                       :symbol "STT"
;;                                       :amount "1.0000000000000000"
;;                                       :decimals 18}]}]
;;                                   [:6872cfb4-9122-4d39-b58c-5dff441f94d4
;;                                    {:id "6872cfb4-9122-4d39-b58c-5dff441f94d4"
;;                                     :type 6
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:420 "0x98C28243BeFD84f5B83BA49fE843e6Cbd9a55AD4"}
;;                                       :type 2
;;                                       :symbol "OWNPER"
;;                                       :name "Owner-Permission Drawer"
;;                                       :amount "1"}]
;;                                     :is_private true}]
;;                                   [:730c3fd3-3fcf-4d48-934a-b175b571c5fe
;;                                    {:id "730c3fd3-3fcf-4d48-934a-b175b571c5fe"
;;                                     :type 2
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:5        "0x0000000000000000000000000000000000000000"
;;                                        :420      "0x0000000000000000000000000000000000000000"
;;                                        :421613   "0x0000000000000000000000000000000000000000"
;;                                        :421614   "0x0000000000000000000000000000000000000000"
;;                                        :11155111 "0x0000000000000000000000000000000000000000"}
;;                                       :type 1
;;                                       :symbol "ETH"
;;                                       :amount "1.0000000000000000"
;;                                       :decimals 18}]}]
;;                                   [:75a6c876-7be4-4830-a82b-b672986329da
;;                                    {:id "75a6c876-7be4-4830-a82b-b672986329da"
;;                                     :type 5
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:420 "0x2334ab535D0146d4a78C24894Cdc0C466cF3159e"}
;;                                       :type 2
;;                                       :symbol "TMPER"
;;                                       :name "TMaster-Permission Drawer"
;;                                       :amount "1"}]
;;                                     :is_private true}]]}})
;;     (swap! rf-db/app-db assoc
;;       :communities/checking-permissions-by-id
;;       {"0x1" {:check {:permissions {"1cfb5618-5e6a-4695-a91c-19e7e4f0af3a" {:tokenRequirement
;;                                                                             [{:satisfied true}]}
;;                                     "6872cfb4-9122-4d39-b58c-5dff441f94d4" {:tokenRequirement
;;                                                                             [{:satisfied true}]}
;;                                     "730c3fd3-3fcf-4d48-934a-b175b571c5fe" {:tokenRequirement
;;                                                                             [{:satisfied true}]}
;;                                     "75a6c876-7be4-4830-a82b-b672986329da" {:tokenRequirement
;;                                     [{:satisfied true}]}}}}})
;;     (is
;;      (match?
;;         {2 [[{:symbol      "STT"
;;               :amount      "1"
;;               :sufficient? true
;;               :loading?    false
;;               :img-src     nil}]
;;             [{:symbol      "ETH"
;;               :amount      "1"
;;               :sufficient? true
;;               :loading?    false
;;               :img-src     nil}]]
;;          5
;;          [[{:symbol      "TMPER"
;;             :amount      "1"
;;             :sufficient? true
;;             :loading?    false
;;             :img-src     "path/to/jpeg"}]]
;;          6
;;          [[{:symbol      "OWNPER"
;;             :amount      "1"
;;             :sufficient? true
;;             :loading?    false
;;             :img-src     "path/to/jpeg"}]]})
;;       (rf/sub [sub-name community-id]))))

;; (h/deftest-sub :community/token-permissions
;;   [sub-name]
;;   (testing "returns token permissions grouped by type with correct attributes"
;;     (js/console.log "We here guys")
;;       (swap! rf-db/app-db assoc
;;              :communities
;;              {"0x1" {:token-permissions [[:1cfb5618-5e6a-4695-a91c-19e7e4f0af3a
;;                                             {:id "1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
;;                                              :type 2
;;                                              :token_criteria
;;                                              [{:contract_addresses
;;                                                {:5
;;                                                "0x3d6afaa395c31fcd391fe3d562e75fe9e8ec7e6a"
;;                                                 :11155111
;;                                                 "0xe452027cdef746c7cd3db31cb700428b16cd8e51"}
;;                                                :type 1
;;                                                :symbol "STT"
;;                                                :amount "1.0000000000000000"
;;                                                :decimals 18}]}]]}})

;;     (swap! rf-db/app-db assoc
;;            :communities/checking-permissions-by-id
;;            {:checking? false
;;             :check     {:satisfied   true
;;                         :permissions {"1cfb5618-5e6a-4695-a91c-19e7e4f0af3a" {:tokenRequirement
;;                                                                               [{:satisfied
;;                                                                               true}]}}}})
;;     (is (match?
;;          {2 [[{:symbol      "STT"
;;                :amount      "1"
;;                :sufficient? true
;;                :loading?    ""
;;                :img-src     ""}]]}
;;          (rf/sub [sub-name community-id])))))

;; (h/deftest-sub :community/token-permissions
;;   [sub-name]
;;   (testing "returns token permissions grouped by type with correct attributes"
;;     (swap! rf-db/app-db assoc
;;       :communities
;;       {"0x1" {:token-permissions [[:1cfb5618-5e6a-4695-a91c-19e7e4f0af3a
;;                                    {:id "1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
;;                                     :type 2
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:5       "0x3d6afaa395c31fcd391fe3d562e75fe9e8ec7e6a"
;;                                        11155111 "0xe452027cdef746c7cd3db31cb700428b16cd8e51"}
;;                                       :type 1
;;                                       :symbol "STT"
;;                                       :amount "1.0000000000000000"
;;                                       :decimals 18}]}]
;;                                   [:6872cfb4-9122-4d39-b58c-5dff441f94d4
;;                                    {:id "6872cfb4-9122-4d39-b58c-5dff441f94d4"
;;                                     :type 6
;;                                     :token_criteria
;;                                     [{:contract_addresses
;;                                       {:420 "0x98C28243BeFD84f5B83BA49fE843e6Cbd9a55AD4"}
;;                                       :type 2
;;                                       :symbol "OWNPER"
;;                                       :name "Owner-Permission Drawer"
;;                                       :amount "1"}]
;;                                     :is_private true}]]
;;               :token-images      {"OWNPER" "path/to/jpeg"}}})

;;     (swap! rf-db/app-db assoc
;;       :communities/checking-permissions-by-id
;;       {:checking? false
;;        :check     {:satisfied   true
;;                    :permissions {"1cfb5618-5e6a-4695-a91c-19e7e4f0af3a" {:tokenRequirement
;;                                                                          [{:satisfied true}]}}}})
;;     (is (match? {2 [[{:symbol      "STT"
;;                       :amount      "1"
;;                       :sufficient? true
;;                       :img-src     nil}]]
;;                  6 [[{:symbol      "OWNPER"
;;                       :amount      "1"
;;                       :sufficient? true
;;                       :img-src     "path/to/jpeg"}]]}
;;                 (rf/sub [sub-name community-id])))))

(h/deftest-sub :community/token-permissions
  [sub-name]
  (testing "returns token permissions grouped by type with correct attributes"
    (swap! rf-db/app-db assoc
      :communities
      {community-id
       {:token-permissions [[:1cfb5618-5e6a-4695-a91c-19e7e4f0af3a
                             {:id "1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
                              :type 2
                              :token_criteria
                              [{:contract_addresses
                                {:5       "0x3d6afaa395c31fcd391fe3d562e75fe9e8ec7e6a"
                                 11155111 "0xe452027cdef746c7cd3db31cb700428b16cd8e51"}
                                :type 1
                                :symbol "STT"
                                :amount "1.0000000000000000"
                                :decimals 18}]}]
                            [:6872cfb4-9122-4d39-b58c-5dff441f94d4
                             {:id "6872cfb4-9122-4d39-b58c-5dff441f94d4"
                              :type 6
                              :token_criteria
                              [{:contract_addresses
                                {:420 "0x98C28243BeFD84f5B83BA49fE843e6Cbd9a55AD4"}
                                :type 2
                                :symbol "OWNPER"
                                :name "Owner-Permission Drawer"
                                :amount "1"}]
                              :is_private true}]]
        :token-images      {"OWNPER" "path/to/jpeg"}}})

    (swap! rf-db/app-db assoc
      :communities/permissions-check
      {community-id {:checking? false
                     :check     {:satisfied   true
                                 :permissions {"1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
                                               {:tokenRequirement
                                                [{:satisfied true}]}}}}})

    (is (match? {2 [[{:symbol          "STT"
                      :amount          "1"
                      :permission-type 2
                      :sufficient?     true
                      :img-src         nil}]]
                 6 [[{:symbol          "OWNPER"
                      :permission-type 6
                      :amount          "1"
                      :sufficient?     false
                      :img-src         "path/to/jpeg"}]]}
                (rf/sub [sub-name community-id]))))

  (testing "returns empty because theres no token permissions setted"
    (swap! rf-db/app-db assoc
      :communities
      {community-id
       {:token-permissions nil
        :token-images      nil}})

    (swap! rf-db/app-db assoc
      :communities/permissions-check
      {community-id {:checking? false
                     :check     {:satisfied   true
                                 :permissions {"1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
                                               {:tokenRequirement
                                                [{:satisfied true}]}}}}})

    (is (match? {}
                (rf/sub [sub-name community-id]))))


  (testing "returns tokens and images when no collectibles exist, with 'satisfied' set to true"
    (swap! rf-db/app-db assoc
      :communities
      {community-id
       {:token-permissions [[:1cfb5618-5e6a-4695-a91c-19e7e4f0af3a
                             {:id "1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
                              :type 2
                              :token_criteria
                              [{:contract_addresses
                                {:5       "0x3d6afaa395c31fcd391fe3d562e75fe9e8ec7e6a"
                                 11155111 "0xe452027cdef746c7cd3db31cb700428b16cd8e51"}
                                :type 1
                                :symbol "STT"
                                :amount "1.0000000000000000"
                                :decimals 18}]}]
                            [:730c3fd3-3fcf-4d48-934a-b175b571c5fe
                             {:id "730c3fd3-3fcf-4d48-934a-b175b571c5fe"
                              :type 2
                              :token_criteria
                              [{:contract_addresses
                                {:5        "0x0000000000000000000000000000000000000000"
                                 :420      "0x0000000000000000000000000000000000000000"
                                 :421613   "0x0000000000000000000000000000000000000000"
                                 :421614   "0x0000000000000000000000000000000000000000"
                                 :11155111 "0x0000000000000000000000000000000000000000"}
                                :type 1
                                :symbol "ETH"
                                :amount "1.0000000000000000"
                                :decimals 18}]}]]
        :token-images      {"STT" "path/to/jpeg" "ETH" "path/to/jpeg"}}})

    (swap! rf-db/app-db assoc
      :communities/permissions-check
      {community-id {:checking? false
                     :check     {:satisfied   true
                                 :permissions {"1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
                                               {:tokenRequirement
                                                [{:satisfied true}]}
                                               "730c3fd3-3fcf-4d48-934a-b175b571c5fe"
                                               {:tokenRequirement
                                                [{:satisfied true}]}}}}})

    (is (match?
         {2 [[{:symbol          "STT"
               :amount          "1"
               :permission-type 2
               :sufficient?     true
               :img-src         "path/to/jpeg"}]
             [{:symbol          "ETH"
               :amount          "1"
               :sufficient?     true
               :permission-type 2
               :img-src         "path/to/jpeg"}]]}
         (rf/sub [sub-name community-id]))))

  (testing "returns tokens and images when no collectibles exist, with 'satisfied' set to false"
    (swap! rf-db/app-db assoc
      :communities
      {community-id
       {:token-permissions [[:1cfb5618-5e6a-4695-a91c-19e7e4f0af3a
                             {:id "1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
                              :type 2
                              :token_criteria
                              [{:contract_addresses
                                {:5       "0x3d6afaa395c31fcd391fe3d562e75fe9e8ec7e6a"
                                 11155111 "0xe452027cdef746c7cd3db31cb700428b16cd8e51"}
                                :type 1
                                :symbol "STT"
                                :amount "1.0000000000000000"
                                :decimals 18}]}]
                            [:730c3fd3-3fcf-4d48-934a-b175b571c5fe
                             {:id "730c3fd3-3fcf-4d48-934a-b175b571c5fe"
                              :type 2
                              :token_criteria
                              [{:contract_addresses
                                {:5        "0x0000000000000000000000000000000000000000"
                                 :420      "0x0000000000000000000000000000000000000000"
                                 :421613   "0x0000000000000000000000000000000000000000"
                                 :421614   "0x0000000000000000000000000000000000000000"
                                 :11155111 "0x0000000000000000000000000000000000000000"}
                                :type 1
                                :symbol "ETH"
                                :amount "1.0000000000000000"
                                :decimals 18}]}]]
        :token-images      {"STT" "path/to/jpeg" "ETH" "path/to/jpeg"}}})

    (swap! rf-db/app-db assoc
      :communities/permissions-check
      {community-id {:checking? false
                     :check     {:satisfied   true
                                 :permissions {"1cfb5618-5e6a-4695-a91c-19e7e4f0af3a"
                                               {:tokenRequirement
                                                [{:satisfied false}]}
                                               "730c3fd3-3fcf-4d48-934a-b175b571c5fe"
                                               {:tokenRequirement
                                                [{:satisfied false}]}}}}})

    (is (match?
         {2 [[{:symbol          "STT"
               :amount          "1"
               :permission-type 2
               :sufficient?     false
               :img-src         "path/to/jpeg"}]
             [{:symbol          "ETH"
               :amount          "1"
               :sufficient?     false
               :permission-type 2
               :img-src         "path/to/jpeg"}]]}
         (rf/sub [sub-name community-id])))))

(comment
  (require 'cljs.test)
  (cljs.test/test-var #'status-im.subs.communities-test/community-token-permissions-test))
