(ns status-im.contexts.chat.messenger.messages.transport.events
  (:require
    [clojure.set :as set]
    [clojure.string :as string]
    [legacy.status-im.browser.core :as browser]
    [legacy.status-im.chat.models.message :as models.message]
    [legacy.status-im.data-store.activities :as data-store.activities]
    [legacy.status-im.data-store.chats :as data-store.chats]
    [legacy.status-im.data-store.communities :as data-store.communities]
    [legacy.status-im.data-store.invitations :as data-store.invitations]
    [legacy.status-im.data-store.messages :as data-store.messages]
    [legacy.status-im.group-chats.core :as models.group]
    [legacy.status-im.multiaccounts.update.core :as update.core]
    [legacy.status-im.pairing.core :as models.pairing]
    [legacy.status-im.utils.deprecated-types :as types]
    [legacy.status-im.visibility-status-updates.core :as models.visibility-status-updates]
    [status-im.constants :as constants]
    [status-im.contexts.chat.contacts.events :as models.contact]
    [status-im.contexts.chat.events :as chat.events]
    [status-im.contexts.chat.messenger.messages.content.reactions.events :as reactions]
    [status-im.contexts.chat.messenger.messages.pin.events :as messages.pin]
    [status-im.contexts.communities.events :as communities]
    [status-im.contexts.shell.activity-center.events :as activity-center]
    [status-im.contexts.wallet.data-store :as wallet.data-store]
    [taoensso.timbre :as log]
    [utils.re-frame :as rf]))

(rf/defn process-next
  {:events [:transport/process-next]}
  [cofx ^js response-js sync-handler]
  (if sync-handler
    (sync-handler cofx response-js true)
    {:utils/dispatch-later [{:ms 20 :dispatch [:process-response response-js]}]}))

(rf/defn process-response
  {:events [:process-response]}
  [{:keys [db] :as cofx} ^js response-js process-async]
  (let [^js communities                (.-communities response-js)
        ^js requests-to-join-community (.-requestsToJoinCommunity response-js)
        ^js chats                      (.-chats response-js)
        ^js contacts                   (.-contacts response-js)
        ^js installations              (.-installations response-js)
        ^js messages                   (.-messages response-js)
        ^js emoji-reactions            (.-emojiReactions response-js)
        ^js invitations                (.-invitations response-js)
        ^js removed-chats              (.-removedChats response-js)
        ^js activity-notifications     (.-activityCenterNotifications response-js)
        ^js activity-center-state      (.-activityCenterState response-js)
        ^js pin-messages-js            (.-pinMessages response-js)
        ^js removed-messages           (.-removedMessages response-js)
        ^js visibility-status-updates  (.-statusUpdates response-js)
        ^js current-visibility-status  (.-currentStatus response-js)
        ^js bookmarks                  (.-bookmarks response-js)
        ^js settings                   (remove nil? (.-settings response-js))
        ^js cleared-histories          (.-clearedHistories response-js)
        ^js identity-images            (.-identityImages response-js)
        ^js accounts                   (.-accounts response-js)
        ^js keypairs                   (.-keypairs response-js)
        ^js ens-username-details-js    (.-ensUsernameDetails response-js)
        ^js customization-color-js     (.-customizationColor response-js)
        ^js saved-addresses-js         (.-savedAddresses response-js)
        ^js watch-only-accounts        (.-watchOnlyAccounts response-js)
        sync-handler                   (when-not process-async process-response)]
    (cond

      (seq chats)
      (do
        (js-delete response-js "chats")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  #(chat.events/ensure-chats % [(map data-store.chats/<-rpc (types/js->clj chats))])))

      (seq messages)
      (models.message/receive-many cofx response-js)

      (seq activity-notifications)
      (let [notifications (->> activity-notifications
                               types/js->clj
                               (map data-store.activities/<-rpc))]
        (js-delete response-js "activityCenterNotifications")
        (rf/merge cofx
                  (activity-center/notifications-reconcile notifications)
                  (activity-center/show-toasts notifications)
                  (process-next response-js sync-handler)))

      (some? activity-center-state)
      (let [seen? (-> activity-center-state
                      types/js->clj
                      data-store.activities/<-rpc-seen-state)]
        (js-delete response-js "activityCenterState")
        (rf/merge cofx
                  (activity-center/reconcile-seen-state seen?)
                  (process-next response-js sync-handler)))

      (seq installations)
      (let [installations-clj (types/js->clj installations)]
        (js-delete response-js "installations")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (models.pairing/handle-installations installations-clj)))

      (seq contacts)
      (models.contact/process-js-contacts cofx response-js)

      (seq communities)
      (do
        (js-delete response-js "communities")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (communities/handle-communities communities)))

      (seq bookmarks)
      (let [bookmarks-clj (types/js->clj bookmarks)]
        (js-delete response-js "bookmarks")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (browser/handle-bookmarks bookmarks-clj)))

      (seq pin-messages-js)
      (do
        (js-delete response-js "pinMessages")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (messages.pin/receive-signal pin-messages-js)))

      (seq removed-chats)
      (let [removed-chats-clj (types/js->clj removed-chats)]
        (js-delete response-js "removedChats")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (communities/handle-removed-chats removed-chats-clj)))

      (seq requests-to-join-community)
      (let [requests (->> requests-to-join-community
                          types/js->clj
                          (map data-store.communities/<-request-to-join-community-rpc))]
        (js-delete response-js "requestsToJoinCommunity")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (communities/handle-requests-to-join requests)))

      (seq emoji-reactions)
      (let [reactions (types/js->clj emoji-reactions)]
        (js-delete response-js "emojiReactions")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (reactions/receive-signal (map reactions/<-rpc reactions))))

      (seq invitations)
      (let [invitations (types/js->clj invitations)]
        (js-delete response-js "invitations")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (models.group/handle-invitations (map data-store.invitations/<-rpc invitations))))

      (seq removed-messages)
      (let [removed-messages-clj (types/js->clj removed-messages)]
        (js-delete response-js "removedMessages")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (models.message/handle-removed-messages removed-messages-clj)))

      (seq cleared-histories)
      (let [cleared-histories-clj (types/js->clj cleared-histories)]
        (js-delete response-js "clearedHistories")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (models.message/handle-cleared-histories-messages cleared-histories-clj)))

      (seq visibility-status-updates)
      (let [visibility-status-updates-clj (types/js->clj visibility-status-updates)]
        (js-delete response-js "statusUpdates")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (models.visibility-status-updates/handle-visibility-status-updates
                   visibility-status-updates-clj)))

      (seq accounts)
      (do
        (js-delete response-js "accounts")
        (rf/merge cofx
                  (process-next response-js sync-handler)))

      (seq watch-only-accounts)
      (do
        (js-delete response-js "watchOnlyAccounts")
        (rf/merge cofx
                  {:fx [[:dispatch
                         [:wallet/reconcile-watch-only-accounts
                          (types/js->clj watch-only-accounts)]]]}
                  (process-next response-js sync-handler)))

      (seq keypairs)
      (do
        (js-delete response-js "keypairs")
        (rf/merge cofx
                  {:fx [[:dispatch [:wallet/reconcile-keypairs (types/js->clj keypairs)]]]}
                  (process-next response-js sync-handler)))

      (seq settings)
      (do
        (js-delete response-js "settings")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (update.core/set-many-js settings)))

      (seq identity-images)
      (let [images-clj (map types/js->clj identity-images)]
        (js-delete response-js "identityImages")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (update.core/optimistic :images images-clj)))

      (some? current-visibility-status)
      (let [current-visibility-status-clj (types/js->clj current-visibility-status)]
        (js-delete response-js "currentStatus")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (models.visibility-status-updates/sync-visibility-status-update
                   current-visibility-status-clj)))

      (seq saved-addresses-js)
      (let [saved-addresses (-> saved-addresses-js types/js->clj wallet.data-store/rpc->saved-addresses)]
        (js-delete response-js "savedAddresses")
        (rf/merge cofx
                  {:fx [[:dispatch [:wallet/reconcile-saved-addresses saved-addresses]]]}
                  (process-next response-js sync-handler)))

      (seq ens-username-details-js)
      (let [ens-username-details-clj (types/js->clj ens-username-details-js)]
        (js-delete response-js "ensUsernameDetails")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (rf/dispatch [:ens/update-usernames ens-username-details-clj])))

      (seq customization-color-js)
      (let [customization-color (types/js->clj customization-color-js)]
        (js-delete response-js "customizationColor")
        (rf/merge cofx
                  (process-next response-js sync-handler)
                  (rf/dispatch [:profile/save-local-accent-color (keyword customization-color)]))))))

(defn group-by-and-update-unviewed-counts
  "group messages by current chat, profile updates, transactions and update unviewed counters in db for not curent chats"
  [{:keys [current-chat-id db] :as acc} ^js message-js]
  (let [chat-id                 (.-localChatId message-js)
        message-type            (.-messageType message-js)
        from                    (.-from message-js)
        mentioned               (.-mentioned message-js)
        new-message             (.-new message-js)
        current                 (= current-chat-id chat-id)
        should-update-unviewed? (and (not current)
                                     new-message
                                     (not (= message-type
                                             constants/message-type-private-group-system-message))
                                     (not (= from (get-in db [:profile/profile :public-key]))))
        tx-hash                 (and (.-commandParameters message-js)
                                     (.-commandParameters.transactionHash message-js))]
    (cond-> acc
      current
      (update :messages conj message-js)

      ;;update counter
      should-update-unviewed?
      (update-in [:db :chats chat-id :unviewed-messages-count] inc)

      (and should-update-unviewed?
           mentioned)
      (update-in [:db :chats chat-id :unviewed-mentions-count] inc)

      ;;conj incoming transaction for :watch-tx
      (not (string/blank? tx-hash))
      (update :transactions conj tx-hash)

      :always
      (update :chats conj chat-id))))

(defn sort-js-messages!
  "sort messages, so we can start process latest first,in that case we only need to process frist 20 and drop others"
  [^js response-js messages]
  (if (seq messages)
    (set! (.-messages response-js)
      (.sort (to-array messages)
             (fn [^js a ^js b]
               (- (.-clock b) (.-clock a)))))
    (js-delete response-js "messages")))

(rf/defn sanitize-messages-and-process-response
  "before processing we want to filter and sort messages, so we can process first only messages which will be showed"
  {:events [:sanitize-messages-and-process-response]}
  [{:keys [db] :as cofx} ^js response-js process-async]
  (when response-js
    (let [current-chat-id (:current-chat-id db)
          {:keys [db messages transactions chats statuses]}
          (reduce group-by-and-update-unviewed-counts
                  {:db              db
                   :chats           #{}
                   :transactions    #{}
                   :statuses        []
                   :messages        []
                   :current-chat-id current-chat-id}
                  (.-messages response-js))]
      (sort-js-messages! response-js messages)
      (rf/merge cofx
                {:db                   db
                 :utils/dispatch-later (concat []
                                               (when (seq statuses)
                                                 [{:ms 100 :dispatch [:process-statuses statuses]}])
                                               (when (seq transactions)
                                                 (for [transaction-hash transactions]
                                                   {:ms       100
                                                    :dispatch [:watch-tx nil transaction-hash]})))}
                (process-response response-js process-async)))))

(rf/defn remove-hash
  [{:keys [db]} envelope-hash]
  {:db (update db :transport/message-envelopes dissoc envelope-hash)})

(rf/defn check-confirmations
  [{:keys [db] :as cofx} status chat-id message-id]
  (when-let [{:keys [pending-confirmations not-sent]}
             (get-in db [:transport/message-ids->confirmations message-id])]
    (if (zero? (dec pending-confirmations))
      (rf/merge cofx
                {:db (update db
                             :transport/message-ids->confirmations
                             dissoc
                             message-id)}
                (models.message/update-message-status chat-id
                                                      message-id
                                                      (if not-sent
                                                        :not-sent
                                                        status))
                (remove-hash message-id))
      (let [confirmations {:pending-confirmations (dec pending-confirmations)
                           :not-sent              (or not-sent
                                                      (= :not-sent status))}]
        {:db (assoc-in db
              [:transport/message-ids->confirmations message-id]
              confirmations)}))))

(rf/defn update-envelope-status
  [{:keys [db] :as cofx} message-id status]
  (if-let [{:keys [chat-id]}
           (get-in db [:transport/message-envelopes message-id])]
    (when-let [{:keys [from]} (get-in db [:messages chat-id message-id])]
      (check-confirmations cofx status chat-id message-id))
    ;; We don't have a message-envelope for this, might be that the confirmation came too early
    {:db (update-in db [:transport/message-confirmations message-id] conj status)}))

(rf/defn update-envelopes-status
  [{:keys [db] :as cofx} message-id status]
  (when (or (not= status :not-sent)
            (= :online (:network db)))
    (apply rf/merge cofx (map #(update-envelope-status % status) message-id))))

(rf/defn set-message-envelope-hash
  "message-type is used for tracking"
  [{:keys [db] :as cofx} chat-id message-id message-type]
  ;; Check first if the confirmation has already arrived
  (let [statuses               (get-in db [:transport/message-confirmations message-id])
        check-confirmations-fx (map
                                #(check-confirmations % chat-id message-id)
                                statuses)

        add-envelope-data      (fn [{:keys [db]}]
                                 {:db (-> db
                                          (update :transport/message-confirmations dissoc message-id)
                                          (assoc-in [:transport/message-envelopes message-id]
                                                    {:chat-id      chat-id
                                                     :message-type message-type})
                                          (update-in [:transport/message-ids->confirmations message-id]
                                                     #(or % {:pending-confirmations 1})))})]
    (apply rf/merge cofx (conj check-confirmations-fx add-envelope-data))))

(rf/defn transport-message-sent
  {:events [:transport/message-sent]}
  [cofx ^js response-js]
  (let [set-hash-fxs (map (fn [{:keys [localChatId id messageType]}]
                            (set-message-envelope-hash localChatId id messageType))
                          (types/js->clj (.-messages response-js)))]
    (apply rf/merge
           cofx
           (conj set-hash-fxs
                 #(sanitize-messages-and-process-response % response-js false)))))

(defn- image->rpc [image] (set/rename-keys image {:data-uri :dataUri}))

(defn- link-preview->rpc
  [preview]
  (-> preview
      (update :thumbnail image->rpc)
      (update :favicon image->rpc)))

(defn build-message
  [msg]
  (-> msg
      (update :link-previews #(map link-preview->rpc %))
      (update :status-link-previews #(set (map data-store.messages/->status-link-previews-rpc %)))
      (set/rename-keys
       {:album-id             :albumId
        :audio-duration-ms    :audioDurationMs
        :audio-path           :audioPath
        :chat-id              :chatId
        :community-id         :communityId
        :content-type         :contentType
        :ens-name             :ensName
        :image-height         :imageHeight
        :image-path           :imagePath
        :image-width          :imageWidth
        :link-previews        :linkPreviews
        :status-link-previews :statusLinkPreviews
        :response-to          :responseTo
        :sticker              :sticker
        :text                 :text})))

(rf/defn send-chat-messages
  [_ messages]
  {:json-rpc/call [{:method      "wakuext_sendChatMessages"
                    :params      [(mapv build-message messages)]
                    :js-response true
                    :on-success  #(rf/dispatch [:transport/message-sent %])
                    :on-error    #(do
                                    (log/warn "failed to send a message" %)
                                    (js/alert (str "failed to send a message: " %)))}]})
