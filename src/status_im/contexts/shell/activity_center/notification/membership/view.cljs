(ns status-im.contexts.shell.activity-center.notification.membership.view
  (:require
    [quo.core :as quo]
    [react-native.core :as rn]
    [react-native.gesture :as gesture]
    [status-im.contexts.shell.activity-center.notification.common.style :as common-style]
    [status-im.contexts.shell.activity-center.notification.common.view :as common]
    [utils.datetime :as datetime]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

(defn- pressable
  [{:keys [accepted chat-id]} child]
  (if accepted
    [gesture/touchable-without-feedback
     {:on-press (fn []
                  (rf/dispatch [:hide-popover])
                  (rf/dispatch [:chat/pop-to-root-and-navigate-to-chat chat-id]))}
     child]
    child))

(defn- swipe-button-accept
  [{:keys [style]} _]
  [common/swipe-button-container
   {:style (common-style/swipe-success-container style)
    :icon  :i/done
    :text  (i18n/label :t/accept)}])

(defn- swipe-button-decline
  [{:keys [style]} _]
  [common/swipe-button-container
   {:style (common-style/swipe-danger-container style)
    :icon  :i/decline
    :text  (i18n/label :t/decline)}])

(defn- swipeable
  [{:keys [notification extra-fn]} child]
  (let [{:keys [accepted dismissed
                id]} notification
        accept       (rn/use-callback
                      (fn [] (rf/dispatch [:activity-center.notifications/accept id]))
                      [id])
        dismiss      (rn/use-callback
                      (fn [] (rf/dispatch [:activity-center.notifications/dismiss id]))
                      [id])]
    (if (or accepted dismissed)
      [common/swipeable
       {:left-button    common/swipe-button-read-or-unread
        :left-on-press  common/swipe-on-press-toggle-read
        :right-button   common/swipe-button-delete
        :right-on-press common/swipe-on-press-delete
        :extra-fn       extra-fn}
       child]
      [common/swipeable
       {:left-button    swipe-button-accept
        :left-on-press  accept
        :right-button   swipe-button-decline
        :right-on-press dismiss
        :extra-fn       extra-fn}
       child])))

(defn view
  [{:keys [notification] :as props}]
  (let [{:keys [id accepted dismissed author read
                timestamp chat-name
                chat-id]}   notification
        customization-color (rf/sub [:profile/customization-color])
        accept              (rn/use-callback
                             (fn []
                               (rf/dispatch [:activity-center.notifications/accept id]))
                             [id])
        dismiss             (rn/use-callback
                             (fn []
                               (rf/dispatch [:activity-center.notifications/dismiss id]))
                             [id])]
    [swipeable props
     [pressable {:accepted accepted :chat-id chat-id}
      [quo/activity-log
       {:title               (i18n/label :t/added-to-group-chat)
        :customization-color customization-color
        :icon                :i/add-user
        :timestamp           (datetime/timestamp->relative timestamp)
        :unread?             (not read)
        :context             [[common/user-avatar-tag author]
                              (i18n/label :t/added-you-to)
                              [quo/context-tag
                               {:type       :group
                                :blur?      true
                                :size       24
                                :group-name chat-name}]]
        :items               (when-not (or accepted dismissed)
                               [{:type                :button
                                 :subtype             :positive
                                 :key                 :button-accept
                                 :label               (i18n/label :t/accept)
                                 :accessibility-label :accept-group-chat-invitation
                                 :on-press            accept}
                                {:type                :button
                                 :subtype             :danger
                                 :key                 :button-decline
                                 :label               (i18n/label :t/decline)
                                 :accessibility-label :decline-group-chat-invitation
                                 :on-press            dismiss}])}]]]))
