import time

from appium.webdriver.common.mobileby import MobileBy
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from typing_extensions import Literal

from tests import test_dapp_url
from views.base_element import Button, Text, BaseElement, SilentButton, CheckBox, EditBox
from views.base_view import BaseView, UnreadMessagesCountText


class ChatButton(Button):
    def __init__(self, driver, **kwargs):
        super().__init__(driver, **kwargs)

    def navigate(self):
        from views.chat_view import ChatView
        return ChatView(self.driver)


class ActivityTabButton(Button):
    def __init__(self, driver, **kwargs):
        super().__init__(driver, **kwargs)

    @property
    def counter(self):
        return BaseElement(self.driver,
                           xpath='//*[@content-desc="%s"]//*[@content-desc="notification-dot"]' % self.accessibility_id)


class ChatElement(SilentButton):
    def __init__(self, driver, username_part, community=False, community_channel=False):
        self.username = username_part
        self.community = community
        self.community_channel = community_channel
        if self.community_channel:
            super().__init__(
                driver,
                xpath="//*[@content-desc='channel-list-item']//*[starts-with(@text,'# %s')]/.." % username_part)
        elif community:
            super().__init__(
                driver,
                xpath="//*[@content-desc='chat-name-text'][starts-with(@text,'%s')]/.." % username_part)
        else:
            super().__init__(
                driver,
                xpath="//*[@content-desc='author-primary-name'][starts-with(@text,'%s')]/.." % username_part)

    def navigate(self):
        if self.community:
            from views.chat_view import CommunityView
            return CommunityView(self.driver)
        else:
            from views.chat_view import ChatView
            return ChatView(self.driver)

    def click(self):
        if self.community:
            from views.chat_view import CommunityView
            desired_element = CommunityView(self.driver).community_description_text
        else:
            from views.chat_view import ChatView
            desired_element = ChatView(self.driver).chat_message_input
        self.click_until_presence_of_element(desired_element=desired_element)

        return self.navigate()

    def find_element(self):
        for i in range(2):
            try:
                return super(ChatElement, self).find_element()
            except NoSuchElementException as e:
                if i == 0:
                    self.wait_for_visibility_of_element(20)
                else:
                    e.msg = 'Device %s: Unable to find chat with name %s' % (self.driver.number, self.username)
                    raise e

    @property
    def new_messages_counter(self):
        if self.community:
            return UnreadMessagesCountText(self.driver, self.locator)

        class NewMessageCounterText(Text):
            def __init__(self, driver, parent_locator: str):
                super().__init__(
                    driver,
                    xpath="%s//*[@content-desc='new-message-counter']/android.widget.TextView" % parent_locator)

        return NewMessageCounterText(self.driver, self.locator)

    @property
    def chat_preview(self):
        class PreveiewMessageText(Text):
            def __init__(self, driver, parent_locator: str):
                super().__init__(driver, xpath="%s//*[@content-desc='chat-message-text']" % parent_locator)

        return PreveiewMessageText(self.driver, self.locator)

    @property
    def no_message_preview(self):
        class NoMessageText(Text):
            def __init__(self, driver, parent_locator: str):
                super().__init__(driver, xpath="%s//*[@content-desc='no-messages-text']" % parent_locator)

        return NoMessageText(self.driver, self.locator)

    @property
    def new_messages_grey_dot(self):
        class UnreadMessagesPublicChat(BaseElement):
            def __init__(self, driver, parent_locator):
                super().__init__(driver, xpath="%s/*[@content-desc='unviewed-messages-public']" % parent_locator)

        return UnreadMessagesPublicChat(self.driver, self.locator)

    @property
    def chat_image(self):
        class ChatImage(BaseElement):
            def __init__(self, driver):
                super().__init__(driver, xpath="//*[@content-desc='chat-icon']")

        return ChatImage(self.driver)

    @property
    def profile_unblock_button(self):
        return Button(self.driver, xpath=self.locator + "/*[@content-desc='Unblock']")


class ActivityCenterElement(SilentButton):
    def __init__(self, driver, username):
        self.chat_name = username
        super().__init__(driver,
                         xpath="//*[contains(@text, '%s')]/ancestor::*[@content-desc='activity']" % username)

    @property
    def title(self):
        return Text(self.driver, xpath=self.locator + '//*[@content-desc="activity-title"]')

    @property
    def unread_indicator(self):
        return Button(self.driver, xpath=self.locator + '//*[@content-desc="activity-unread-indicator"]')

    @property
    def message_body(self):
        return Button(self.driver, xpath=self.locator + '//*[@content-desc="activity-message-body"]')

    @property
    def context_tag_text(self):
        return Text(self.driver, xpath=self.locator + '//*[@content-desc="context-tag"]/android.widget.TextView').text

    @property
    def pending_status_tag(self):
        return Text(self.driver, xpath=self.locator + '//*[@content-desc="status-tag-pending"]')

    def handle_cr(self, element_accessibility: str):
        Button(
            self.driver,
            xpath=self.locator + '/*[@content-desc="%s"]' % element_accessibility
        ).wait_for_rendering_ended_and_click()

    def accept_contact_request(self):
        self.handle_cr("accept-contact-request")

    def decline_contact_request(self):
        self.handle_cr("decline-contact-request")

    def cancel_contact_request(self):
        self.handle_cr("cancel-contact-request")


class PushNotificationElement(SilentButton):
    def __init__(self, driver, pn_text):
        self.pn_text = pn_text
        super().__init__(driver, xpath="//*[@text='%s']" % pn_text)

    @property
    def icon(self):
        class PnIconElement(BaseElement):
            def __init__(self, driver, parent_locator):
                super().__init__(driver,
                                 xpath="%s/../../../../*/*[@resource-id='android:id/message_icon']" % parent_locator)

        return PnIconElement(self.driver, self.locator)

    @property
    def username(self):
        class PnUsername(BaseElement):
            def __init__(self, driver, parent_locator):
                super().__init__(driver,
                                 xpath="%s/../../*[@resource-id='android:id/message_name']" % parent_locator)

        return PnUsername(self.driver, self.locator).text

    @property
    def group_chat_icon(self):
        class GroupChatIconElement(BaseElement):
            def __init__(self, driver, parent_locator):
                super().__init__(driver,
                                 xpath="%s/../../../../*[@resource-id='android:id/right_icon_container']" % parent_locator)

        return GroupChatIconElement(self.driver, self.locator)


class ContactDetailsRow(BaseElement):
    def __init__(self, driver, username=None, index=None):
        main_locator = "//*[@content-desc='user-list']"
        if username:
            xpath_locator = "%s[*[contains(@text,'%s')]]" % (main_locator, username)
        elif index:
            xpath_locator = "%s[%s]" % (main_locator, index)
        else:
            xpath_locator = main_locator
        super().__init__(driver, xpath=xpath_locator)

        self.options_button = Button(self.driver, xpath="(%s//android.widget.ImageView)[2]" % xpath_locator)
        self.username_text = Text(self.driver, xpath="(%s//android.widget.TextView)[2]" % xpath_locator)


class MuteButton(Button):
    def __init__(self, driver, accessibility_id):
        super().__init__(driver=driver, accessibility_id=accessibility_id)

    @property
    def text(self):
        return self.find_element().find_element(by=MobileBy.CLASS_NAME, value="android.widget.TextView").text

    @property
    def unmute_caption_text(self):
        return self.find_element().find_element(by=MobileBy.XPATH, value="//android.widget.TextView[2]").text


class ShareQRCodeInfoText(Text):
    def __init__(self, driver):
        super().__init__(driver, accessibility_id="share-qr-code-info-text")

    @property
    def text(self):
        return self.find_element().find_element(by=MobileBy.XPATH, value="/android.widget.TextView").text


class HomeView(BaseView):
    def __init__(self, driver):
        super().__init__(driver)

        self.plus_button = Button(self.driver, accessibility_id="new-chat-button")
        self.plus_community_button = Button(self.driver, accessibility_id="new-communities-button")
        self.chat_name_text = Text(self.driver, accessibility_id="chat-name-text")
        self.start_new_chat_button = ChatButton(self.driver, accessibility_id="start-1-1-chat-button")
        self.new_group_chat_button = ChatButton(self.driver, accessibility_id="start-group-chat-button")
        self.join_public_chat_button = ChatButton(self.driver, accessibility_id="join-public-chat-button")
        self.universal_qr_scanner_button = Button(self.driver, accessibility_id="universal-qr-scanner")
        self.invite_friends_button = Button(self.driver, accessibility_id="invite-friends-button")
        self.stop_status_service_button = Button(self.driver, accessibility_id="STOP")
        self.my_profile_on_start_new_chat_button = Button(self.driver,
                                                          xpath="//*[@content-desc='current-account-photo']")
        self.communities_button = ChatButton(self.driver, accessibility_id="create-community")
        self.create_closed_community_button = ChatButton(self.driver, accessibility_id="create-closed-community")
        self.create_open_community_button = ChatButton(self.driver, accessibility_id="create-open-community")
        self.create_token_gated_community_button = ChatButton(self.driver,
                                                              accessibility_id="create-token-gated-community")
        self.ens_banner_close_button = Button(self.driver, accessibility_id=":ens-banner-close-button")
        self.user_name_text = Text(
            self.driver, xpath="//*[@text='User found']/following-sibling::*/android.widget.TextView[1]")

        # Notification centre
        self.notifications_button = Button(self.driver, accessibility_id="notifications-button")
        self.notifications_unread_badge = BaseElement(self.driver, accessibility_id="activity-center-unread-count")
        self.open_activity_center_button = Button(self.driver, accessibility_id="open-activity-center-button")
        self.close_activity_centre = Button(self.driver, accessibility_id="close-activity-center")

        self.notifications_select_button = Button(self.driver, translation_id="select")
        self.notifications_reject_and_delete_button = Button(self.driver, accessibility_id="reject-and-delete"
                                                                                           "-activity-center")
        self.notifications_accept_and_add_button = Button(self.driver,
                                                          accessibility_id="accept-and-add-activity-center")
        self.notifications_select_all = Button(self.driver, xpath="(//android.widget.CheckBox["
                                                                  "@content-desc='checkbox-off'])[1]")

        # Tabs and elements on messages home view
        self.recent_tab = Button(self.driver, accessibility_id="tab-recent")
        self.groups_tab = Button(self.driver, accessibility_id="tab-groups")
        self.contacts_tab = Button(self.driver, accessibility_id="tab-contacts")
        self.contact_new_badge = Button(self.driver, accessibility_id="notification-dot")
        self.pending_contact_request_button = Button(self.driver,
                                                     accessibility_id="open-activity-center-contact-requests")
        self.pending_contact_request_text = Text(
            self.driver,
            xpath='//*[@content-desc="pending-contact-requests-count"]/android.widget.TextView')

        # Tabs and elements on community home view
        self.pending_communities_tab = Button(self.driver, accessibility_id="pending-tab")
        self.joined_communities_tab = Button(self.driver, accessibility_id="joined-tab")
        self.opened_communities_tab = Button(self.driver, accessibility_id="opened-tab")

        # Options on long tap
        self.chats_menu_invite_friends_button = Button(self.driver, accessibility_id="chats-menu-invite-friends-button")
        self.close_chat_button = Button(self.driver, accessibility_id="close-chat")
        self.confirm_closing_chat_button = Button(self.driver, accessibility_id="Confirm")
        self.clear_history_button = Button(self.driver, accessibility_id="clear-history")
        self.mute_chat_button = MuteButton(self.driver, accessibility_id="mute-chat")
        self.mute_community_button = MuteButton(self.driver, accessibility_id="mute-community")
        self.unmute_community_button = MuteButton(self.driver, accessibility_id="unmute-community")
        self.mute_channel_button = MuteButton(self.driver, accessibility_id="chat-toggle-muted")
        self.mark_all_messages_as_read_button = Button(self.driver, accessibility_id="mark-as-read")

        # Connection icons
        self.mobile_connection_off_icon = Button(self.driver, accessibility_id="conn-button-mobile-sync-off")
        self.mobile_connection_on_icon = Button(self.driver, accessibility_id="conn-button-mobile-sync")
        self.connection_offline_icon = Button(self.driver, accessibility_id="conn-button-offline")

        # Sync using mobile data bottom sheet
        self.continue_syncing_button = Button(self.driver, accessibility_id="mobile-network-continue-syncing")
        self.stop_syncing_button = Button(self.driver, accessibility_id="mobile-network-stop-syncing")
        self.remember_my_choice_checkbox = CheckBox(self.driver, accessibility_id=":checkbox-on")

        # Connection status bottom sheet
        self.connected_to_n_peers_text = Text(self.driver, accessibility_id="connected-to-n-peers")
        self.connected_to_node_text = Text(self.driver, accessibility_id="connected-to-mailserver")
        self.waiting_for_wi_fi = Text(self.driver, accessibility_id="waiting-wi-fi")
        self.use_mobile_data_switch = Button(self.driver, accessibility_id="mobile-network-use-mobile")
        self.connection_settings_button = Button(self.driver, accessibility_id="settings")
        self.not_connected_to_node_text = Text(self.driver, accessibility_id="not-connected-nodes")
        self.not_connected_to_peers_text = Text(self.driver, accessibility_id="not-connected-to-peers")

        # New UI
        self.new_chat_button = Button(self.driver, accessibility_id="new-chat-button")
        self.discover_communities_button = Button(self.driver, accessibility_id="communities-home-discover-card")

        # New UI bottom sheet
        self.start_a_new_chat_bottom_sheet_button = Button(self.driver, accessibility_id="start-a-new-chat")
        self.add_a_contact_chat_bottom_sheet_button = Button(self.driver, accessibility_id="add-a-contact")

        # Activity centre
        self.all_activity_tab_button = ActivityTabButton(self.driver, translation_id="all")
        self.mention_activity_tab_button = ActivityTabButton(self.driver, accessibility_id="tab-mention")
        self.reply_activity_tab_button = ActivityTabButton(self.driver, accessibility_id="tab-reply")
        self.activity_notification_swipe_button = Button(self.driver, accessibility_id="notification-swipe")
        self.activity_unread_filter_button = Button(self.driver, accessibility_id="selector-filter")
        self.more_options_activity_button = Button(self.driver, accessibility_id="activity-center-open-more")
        self.mark_all_read_activity_button = Button(self.driver, translation_id="mark-all-notifications-as-read")

        # Share tab
        self.share_qr_code_info_text = ShareQRCodeInfoText(self.driver)
        self.link_to_profile_button = Button(self.driver, accessibility_id="link-to-profile")
        self.link_to_profile_text = Text(self.driver, accessibility_id="share-qr-code-info-text")
        self.close_share_tab_button = Button(self.driver, accessibility_id="close-shell-share-tab")
        self.qr_code_image_element = BaseElement(self.driver, accessibility_id='share-qr-code')
        self.share_profile_tab_button = Button(self.driver, accessibility_id="Profile")
        self.share_wallet_tab_button = Button(self.driver, accessibility_id="Wallet")
        self.account_avatar = BaseElement(self.driver, accessibility_id="account-avatar")
        self.account_name_text = Text(
            self.driver, xpath="//*[@content-desc='link-to-profile']/preceding-sibling::android.widget.TextView")
        self.share_link_to_profile_button = Button(self.driver, accessibility_id='link-to-profile')
        # Discover communities
        self.community_card_item = BaseElement(self.driver, accessibility_id="community-card-item")

    def wait_for_syncing_complete(self):
        self.driver.info('Waiting for syncing to complete')
        while True:
            try:
                sync = self.element_by_text_part('Syncing').wait_for_element(10)
                self.driver.info(sync.text)
            except TimeoutException:
                break

    def get_activity_center_element_by_text(self, text_part):
        return ActivityCenterElement(self.driver, text_part)

    def get_chat(self, username, community=False, community_channel=False, wait_time=10):
        if community:
            self.driver.info("Looking for community: '%s'" % username)
        else:
            self.driver.info("Looking for chat: '%s'" % username)
        chat_element = ChatElement(self.driver, username[:25], community=community, community_channel=community_channel)
        if not chat_element.is_element_displayed(wait_time) and community is False and community_channel is False:
            if self.notifications_unread_badge.is_element_displayed(30):
                chat_in_ac = self.get_activity_center_element_by_text(username[:25])
                self.open_activity_center_button.click_until_presence_of_element(chat_in_ac)
                chat_in_ac.wait_for_element(20)
                chat_in_ac.click()
        return chat_element

    def get_to_community_channel_from_home(self, community_name, channel_name='general'):
        community_view = self.get_community_view()
        self.get_chat(community_name, community=True).click()
        return community_view.get_channel(channel_name).click()

    def get_chat_from_home_view(self, username):
        self.driver.info("Looking for chat: '%s'" % username)
        chat_element = ChatElement(self.driver, username[:25])
        return chat_element

    def get_element_from_activity_center_view(self, message_body):
        self.driver.info("Looking for activity center element: '%s'" % message_body)
        chat_element = self.get_activity_center_element_by_text(message_body)
        return chat_element

    def handle_contact_request(self, username: str, action='accept'):
        if self.toast_content_element.is_element_displayed(10):
            self.toast_content_element.wait_for_invisibility_of_element()
        try:
            self.notifications_unread_badge.wait_for_visibility_of_element(30)
        except TimeoutException:
            pass
        self.open_activity_center_button.click_until_presence_of_element(self.close_activity_centre)
        chat_element = self.get_activity_center_element_by_text(username[:25])
        try:
            if action == 'accept':
                self.driver.info("Accepting incoming CR for %s" % username)
                chat_element.accept_contact_request()
            elif action == 'decline':
                self.driver.info("Rejecting incoming CR for %s" % username)
                chat_element.decline_contact_request()
            elif action == 'cancel':
                self.driver.info("Canceling outgoing CR for %s" % username)
                chat_element.cancel_contact_request()
            else:
                self.driver.fail("Illegal option for CR!")
        except NoSuchElementException:
            self.driver.fail("No contact request received from %s" % username)
        finally:
            self.close_activity_centre.wait_for_rendering_ended_and_click()
            self.chats_tab.wait_for_visibility_of_element()

    def get_username_below_start_new_chat_button(self, username_part):
        return Text(self.driver,
                    xpath="//*[@content-desc='enter-contact-code-input']/../..//*[starts-with(@text,'%s')]" % username_part)

    def add_contact(self, public_key, nickname='', remove_from_contacts=False):
        self.driver.info("Adding user to Contacts via chats > add new contact")
        self.new_chat_button.click_until_presence_of_element(self.add_a_contact_chat_bottom_sheet_button)
        self.add_a_contact_chat_bottom_sheet_button.click()

        chat = self.get_chat_view()
        chat.public_key_edit_box.click()
        chat.public_key_edit_box.send_keys(public_key)
        chat.element_by_translation_id("user-found").wait_for_visibility_of_element()
        if not chat.view_profile_new_contact_button.is_element_displayed():
            chat.click_system_back_button()
        chat.view_profile_new_contact_button.click_until_presence_of_element(chat.profile_block_contact_button)
        if remove_from_contacts and chat.profile_remove_from_contacts.is_element_displayed():
            chat.profile_remove_from_contacts.click()
        chat.profile_send_contact_request_button.click()
        chat.contact_request_message_input.send_keys("hi")
        chat.confirm_send_contact_request_button.click()
        if nickname:
            chat.set_nickname(nickname)
        chat.close_button.click_until_absense_of_element(chat.close_button)

    def create_group_chat(self, user_names_to_add: list, group_chat_name: str = 'new_group_chat'):
        self.driver.info("## Creating group chat '%s'" % group_chat_name, device=False)
        self.new_chat_button.click()
        chat = self.get_chat_view()
        self.start_a_new_chat_bottom_sheet_button.click()
        for user_name in user_names_to_add:
            check_box = chat.get_username_checkbox(user_name)
            if not check_box.is_element_displayed():
                raise NoSuchElementException(
                    "User with the name '%s' is not in contacts list so can't create a group chat" % user_name)
            check_box.click_until_presence_of_element(chat.get_username_checkbox(user_name, state_on=True))
        self.next_button.click()
        self.get_sign_in_view().profile_title_input.send_keys(group_chat_name)
        chat.create_group_chat_button.click()
        self.driver.info("## Group chat %s is created successfully!" % group_chat_name, device=False)
        return chat

    def create_community_e2e(self, name: str, description="some_description", set_image=False,
                             file_name='sauce_logo.png',
                             require_approval=True):
        self.driver.info("## Creating community '%s', set image is set to '%s'" % (name, str(set_image)), device=False)
        self.plus_community_button.click()
        chat_view = self.communities_button.click()
        chat_view.community_name_edit_box.send_keys(name)
        chat_view.community_description_edit_box.send_keys(description)
        if set_image:
            from views.profile_view import ProfileView
            set_picture_view = ProfileView(self.driver)
            set_picture_view.element_by_translation_id("community-thumbnail-upload").scroll_and_click()
            set_picture_view.element_by_translation_id("community-image-pick").scroll_and_click()
            set_picture_view.select_photo_from_gallery(file_name)
            set_picture_view.crop_photo_button.click()
        if require_approval:
            self.element_by_translation_id("membership-title").scroll_and_click()
            self.element_by_translation_id("membership-approval").click()
            self.done_button.click()

        chat_view.confirm_create_in_community_button.wait_and_click()
        self.driver.info("## Community is created successfully!", device=False)
        return self.get_community_view()

    def create_community(self, community_type: Literal["open", "closed", "token-gated"]):
        self.driver.info("## Creating %s community" % community_type)
        self.plus_community_button.click()
        if community_type == "open":
            self.create_open_community_button.click()
        elif community_type == "closed":
            self.create_closed_community_button.click()
        elif community_type == "token-gated":
            self.create_token_gated_community_button.click()
        else:
            raise ValueError("Incorrect community type is set")

    def import_community(self, key):
        self.driver.info("## Importing community")
        import_button = Button(self.driver, translation_id="import")
        self.plus_button.click()
        chat_view = self.communities_button.click()
        chat_view.chat_options.click()
        chat_view.element_by_translation_id("import-community").wait_and_click()
        EditBox(self.driver, xpath="//android.widget.EditText").send_keys(key)
        import_button.click_until_absense_of_element(import_button)

    def join_public_chat(self, chat_name: str):
        self.driver.info("## Creating public chat %s" % chat_name, device=False)
        self.plus_button.click_until_presence_of_element(self.join_public_chat_button, attempts=5)
        self.join_public_chat_button.wait_for_visibility_of_element(5)
        chat_view = self.join_public_chat_button.click()
        chat_view.chat_name_editbox.wait_for_visibility_of_element(20)
        chat_view.chat_name_editbox.click()
        chat_view.chat_name_editbox.send_keys(chat_name)
        time.sleep(2)
        self.confirm_until_presence_of_element(chat_view.chat_message_input)
        self.driver.info("## Public chat '%s' is created successfully!" % chat_name, device=False)
        return self.get_chat_view()

    def open_status_test_dapp(self, url=test_dapp_url, allow_all=True):
        self.driver.info("Opening dapp '%s', allow all:'%s'" % (test_dapp_url, str(allow_all)))
        dapp_view = self.dapp_tab_button.click()
        dapp_view.open_url(url)
        status_test_dapp = dapp_view.get_status_test_dapp_view()
        if allow_all:
            if status_test_dapp.allow_button.is_element_displayed(20):
                status_test_dapp.allow_button.click_until_absense_of_element(status_test_dapp.allow_button)
        else:
            status_test_dapp.deny_button.click_until_absense_of_element(status_test_dapp.deny_button)
        return status_test_dapp

    def delete_chat_long_press(self, username):
        self.driver.info("Deleting chat '%s' by long press" % username)
        self.get_chat(username).long_press_element()
        self.close_chat_button.click()
        self.confirm_closing_chat_button.click()

    def leave_chat_long_press(self, username):
        self.driver.info("Leaving chat '%s' by long press" % username)
        self.get_chat(username).long_press_element()
        from views.chat_view import ChatView
        ChatView(self.driver).leave_chat_button.click()
        ChatView(self.driver).leave_button.click()

    def clear_chat_long_press(self, username):
        self.driver.info("Clearing history in chat '%s' by long press" % username)
        self.get_chat(username).long_press_element()
        self.clear_history_button.click()
        from views.chat_view import ChatView
        ChatView(self.driver).clear_button.click()

    def mute_chat_long_press(self, chat_name, mute_period="mute-till-unmute", community=False, community_channel=False):
        self.driver.info("Muting chat with %s" % chat_name)
        self.get_chat(username=chat_name, community=community, community_channel=community_channel).long_press_element()
        if community:
            self.mute_community_button.click()
        elif community_channel:
            self.mute_channel_button.click()
        else:
            self.mute_chat_button.click()
        self.element_by_translation_id(mute_period).click()

    def get_pn(self, pn_text: str):
        self.driver.info("Getting PN by '%s'" % pn_text)
        expected_element = PushNotificationElement(self.driver, pn_text)
        return expected_element if expected_element.is_element_displayed(60) else False

    def contact_details_row(self, username=None, index=None):
        return ContactDetailsRow(self.driver, username=username, index=index)

    def get_contact_rows_count(self):
        return len(ContactDetailsRow(self.driver).find_elements())

    def get_link_to_profile(self):
        self.show_qr_code_button.click()
        self.share_profile_tab_button.click()
        self.link_to_profile_button.click()
        link_to_profile = self.sharing_text_native.text
        self.click_system_back_button()
        return link_to_profile

    def get_public_key(self):
        self.driver.info("Getting public key via Share tab")
        link_to_profile = self.get_link_to_profile()
        self.click_system_back_button()
        return link_to_profile.split("#")[-1]

    def copy_wallet_address(self):
        self.share_link_to_profile_button.click()
        address = self.sharing_text_native.text
        self.click_system_back_button()
        return address

    def get_wallet_address(self):
        self.show_qr_code_button.click()
        self.share_wallet_tab_button.click()
        self.account_avatar.wait_for_visibility_of_element()
        address = self.copy_wallet_address()
        self.click_system_back_button()
        return address

    def get_discover_community_card_by_name(self, community_name: str):
        return BaseElement(
            self.driver,
            xpath="//*[@content-desc='%s'][descendant::*[@content-desc='chat-name-text'][@text='%s']]" % (
                self.community_card_item.accessibility_id, community_name)
        )
