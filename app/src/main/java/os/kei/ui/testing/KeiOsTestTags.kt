@file:Suppress("PropertyName")

package os.kei.ui.testing

object KeiOsTestTags {
    const val SettingsTabInterface = "settings_tab_2"
    const val SettingsThemeModeSelector = "settings_theme_mode_selector"
    const val MainBottomTabHome = "main_bottom_tab_home"
    const val MainBottomTabOs = "main_bottom_tab_os"
    const val MainBottomTabMcp = "main_bottom_tab_mcp"
    const val MainBottomTabGitHub = "main_bottom_tab_github"
    const val MainBottomTabBa = "main_bottom_tab_ba"

    /**
     * The button that converts the tab bar into a sidebar, and back.
     *
     * Tagged so the baseline profile can reach the sidebar at all: it is the only entry point, and the rail's
     * composables would otherwise never be compiled into the profile even on a tablet-shaped window.
     */
    const val MainSidebarToggle = "main_sidebar_toggle"
    const val MainSidebarRowHome = "main_sidebar_row_home"
    const val MainSidebarRowOs = "main_sidebar_row_os"
    const val MainSidebarRowMcp = "main_sidebar_row_mcp"
    const val MainSidebarRowGitHub = "main_sidebar_row_github"
    const val MainSidebarRowBa = "main_sidebar_row_ba"
    const val MainPagerSettledHome = "main_pager_settled_home"
    const val MainPagerSettledOs = "main_pager_settled_os"
    const val MainPagerSettledMcp = "main_pager_settled_mcp"
    const val MainPagerSettledGitHub = "main_pager_settled_github"
    const val MainPagerSettledBa = "main_pager_settled_ba"
    const val HomePageRoot = "home_page_root"
    const val HomeSettingsButton = "home_settings_button"
    const val HomeAboutButton = "home_about_button"
    const val HomeWebDavCard = "home_webdav_card"
    const val SettingsPageRoot = "settings_page_root"
    const val AboutPageRoot = "about_page_root"
    const val WebDavSyncPageRoot = "webdav_sync_page_root"
    const val WebDavTabConnection = "webdav_sync_tab_0"
    const val WebDavTabData = "webdav_sync_tab_1"
    const val WebDavTabHistory = "webdav_sync_tab_2"
    const val WebDavTabAdvanced = "webdav_sync_tab_3"
    const val OsPageRoot = "os_page_root"
    const val OsShellRunnerButton = "os_shell_runner_button"
    const val OsShellRunnerPageRoot = "os_shell_runner_page_root"
    const val McpPageRoot = "mcp_page_root"
    const val McpSkillButton = "mcp_skill_button"
    const val McpSkillPageRoot = "mcp_skill_page_root"
    const val BaPageRoot = "ba_page_root"
    const val BaAccountManagementButton = "ba_account_management_button"
    const val BaCraftCardHeader = "ba_craft_card_header"

    /** The release list page's root, and the handles a journey needs on it. */
    const val GitHubReleasePageRoot = "github_release_page_root"
    const val GitHubReleaseCardFirst = "github_release_card_first"
    const val GitHubReleaseNextPageButton = "github_release_next_page_button"
    const val GitHubReleasePageJumpField = "github_release_page_jump_field"

    /** A tracked card's overflow trigger, and the release entry inside it. */
    const val GitHubTrackedItemMoreButton = "github_tracked_item_more_button"
    const val GitHubTrackedItemPinMenuItem = "github_tracked_item_pin_menu_item"
    const val GitHubReleaseMenuItem = "github_release_menu_item"

    /**
     * The overflow entry that opens the F-Droid detail sheet, which only an F-Droid track offers.
     *
     * Tagged as a finder as much as a handle: a journey cannot tell which tracked card is the F-Droid
     * one from the outside, so it opens overflows until this appears.
     */
    const val GitHubFdroidDetailMenuItem = "github_fdroid_detail_menu_item"
    const val GitHubReleaseTagFilterButton = "github_release_tag_filter_button"

    /**
     * The F-Droid build-history page, and the handles a journey needs on it.
     *
     * [FdroidVersionListMenuItem] doubles as the finder for an F-Droid track, the same way
     * [GitHubFdroidDetailMenuItem] does — an overflow is the only place either appears.
     */
    const val FdroidVersionListMenuItem = "fdroid_version_list_menu_item"
    const val FdroidVersionPageRoot = "fdroid_version_page_root"
    const val FdroidVersionCardFirst = "fdroid_version_card_first"
    const val FdroidVersionFilterField = "fdroid_version_filter_field"
    const val FdroidVersionCompatibleFilterButton = "fdroid_version_compatible_filter_button"
    const val FdroidVersionRefreshButton = "fdroid_version_refresh_button"

    /**
     * The first tracked card itself, so a journey can open one.
     *
     * A collapsed card composes its header and nothing else, so the asset panel, the asset rows and the
     * version sections underneath it — the densest part of the page a user actually looks at — were
     * reachable only by expanding a card, which no journey did.
     */
    const val GitHubTrackedItemCardFirst = "github_tracked_item_card_first"

    /** The Actions entry in that overflow, which opens the workflow-runs sheet. */
    const val GitHubActionsMenuItem = "github_actions_menu_item"

    /** The empty-state and overview button that opens the track editor for a new track. */
    const val GitHubAddTrackedButton = "github_add_tracked_button"

    /** The two top-bar toolbar actions that open sheets of their own. */
    const val GitHubStrategySheetButton = "github_strategy_sheet_button"
    const val GitHubCheckLogicSheetButton = "github_check_logic_sheet_button"
    const val GitHubCheckPrereleaseSwitch = "github_check_prerelease_switch"

    /**
     * The first cooldown card, the cafe's headpat.
     *
     * The three cooldowns are cards in the list now, so the profile has something to expand that is not
     * a craft slot — and the two card kinds animate the same accordion but compose different bodies.
     */
    const val BaCooldownCardFirst = "ba_cooldown_card_first"

    /** The cooldown editor's entry point, inside an expanded cooldown card. */
    const val BaCooldownAdjustButton = "ba_cooldown_adjust_button"

    /**
     * The first craft slot's card.
     *
     * The baseline profile's "the craft section is open" signal, and its way in: the slots are one card
     * each now, so there is no single container whose presence says the section is showing.
     */
    const val BaCraftSlotCardFirst = "ba_craft_slot_card_first"

    /** The configure button inside that card, which opens the craft sheet. */
    const val BaCraftSlotFirst = "ba_craft_slot_first"
    /**
     * The dock's first action, which pushes the merged calendar-and-banner route.
     *
     * One tag where there were two: the two lists became two tabs of one page, which is what freed a dock
     * slot. A journey that wants the banner half taps this and then the page's second category tab.
     */
    const val BaDockOpenCalendarPool = "ba_dock_open_calendar_pool"
    const val BaCalendarPoolPageRoot = "ba_calendar_pool_page_root"

    /**
     * The merged route's two category tabs.
     *
     * A second spelling of `tabbedPageCategoryTabTestTag("ba_calendar_pool", index)`, which the chrome
     * derives rather than being handed — see the contract test that pins the two together. The banner half
     * is only reachable through its tab now, so a journey that wants it needs this.
     */
    const val BaCalendarPoolTabCalendar = "ba_calendar_pool_tab_0"
    const val BaCalendarPoolTabPool = "ba_calendar_pool_tab_1"

    /**
     * The dock's third action, which pushes the guide catalog.
     *
     * Untagged until the profile went looking for it: the catalog and the student guide it opens are two
     * nav routes and about a hundred composable files, and not one of them had a single rule in the
     * shipped profile, because nothing could reach them.
     */
    const val BaDockOpenGuideCatalog = "ba_dock_open_guide_catalog"

    /**
     * The dock's fourth action, which opens the daily-done template as a page sheet.
     *
     * The same sheet [BaDailyTemplateHeadpatSwitch] is tagged inside, reached without a tile: the tile
     * long-press route needs a tile added first, and a teacher who never added one had no way in.
     */
    const val BaDockDailyDone = "ba_dock_daily_done"
    const val BaGuideCatalogPageRoot = "ba_guide_catalog_page_root"

    /** The catalog's first entry card, which is the only way into the student guide. */
    const val BaGuideCatalogEntryFirst = "ba_guide_catalog_entry_first"
    const val BaStudentGuidePageRoot = "ba_student_guide_page_root"

    /**
     * The student guide's six bottom tabs.
     *
     * Its tab bar is a bar, not a swipeable pager — verified on the AVD, where two horizontal swipes over
     * the content left the page on Profile — and its buttons published neither a resource id nor a
     * content description, so a journey could reach the guide and then only ever see the tab it landed on.
     * That left `student.section` and `student.tabcontent`, about 290KB between them, with no rules.
     */
    const val BaStudentGuideTabArchive = "ba_student_guide_tab_archive"
    const val BaStudentGuideTabSkills = "ba_student_guide_tab_skills"

    /**
     * The guide's convert-between-shapes button, in whichever shape it is currently in.
     *
     * One tag for the top row's copy and the rail's, because it is one control in two positions --
     * the same arrangement the pager's has. They are never on screen together: the rail carries it
     * once the bar is gone.
     */
    const val BaStudentGuideSidebarToggle = "ba_student_guide_sidebar_toggle"

    /** One row of the guide's rail. Derived by `guideSidebarRowTestTag`; pinned by the tag contract. */
    const val BaStudentGuideSidebarRowSkills = "ba_student_guide_tab_skills_sidebar_row"
    const val BaStudentGuideTabProfile = "ba_student_guide_tab_profile"
    const val BaStudentGuideTabVoice = "ba_student_guide_tab_voice"
    const val BaStudentGuideTabGallery = "ba_student_guide_tab_gallery"
    const val BaStudentGuideTabSimulate = "ba_student_guide_tab_simulate"

    /**
     * The catalog's own dock: students, memory lobby, student BGM, favourite BGM.
     *
     * The same bar-not-a-pager trap as the guide's tabs, and the reason the previous capture reached
     * none of the three tabs past the first. It is also where `androidx.media3` comes from — the BGM
     * interface composes on every tab because the mini player is always there, but nothing loads a
     * track until someone opens the library and plays one.
     */
    const val BaGuideCatalogDockStudent = "ba_guide_catalog_dock_student"
    const val BaGuideCatalogDockMemoryLobby = "ba_guide_catalog_dock_memory_lobby"
    const val BaGuideCatalogDockStudentBgm = "ba_guide_catalog_dock_student_bgm"
    const val BaGuideCatalogDockFavoriteBgm = "ba_guide_catalog_dock_favorite_bgm"

    /** The first student-BGM row, whose whole card is a play button. Tapping it is what loads media3. */
    const val BaGuideCatalogStudentBgmFirst = "ba_guide_catalog_student_bgm_first"

    /**
     * On the catalog's mini player only while a track is actually playing.
     *
     * Tapping a row is not enough to load media3: on a fresh install the row first resolves its audio over
     * the network, and a journey that moved on straight after the tap could leave before playback began —
     * of two captures from a fresh install, one came back with 9 media3 rules and the other with 4,992.
     * Waiting for this is waiting for the player to exist.
     */
    const val BaGuideCatalogBgmPlaying = "ba_guide_catalog_bgm_playing"

    /** Whatever collapsed bottom dock is currently on screen. Tapping it puts the tabs back. */
    const val CompactBottomBarDock = "compact_bottom_bar_dock"

    /**
     * The component lab, and the Liquid catalogue behind it.
     *
     * Both activities are `exported="false"`, so a journey cannot start either by name and has to walk
     * in: About, the lab's entry row, then the card that opens the catalogue. The catalogue is where
     * almost all of the weight is — a dozen sample cards on one lazy list.
     */
    /**
     * About's fourth category tab, which is where the component lab card lives.
     *
     * The card is not in About's three overview cards, so before this the only way to it was the page's
     * search — and driving a text field from a profile journey is focus- and IME-dependent in a way a
     * tab tap is not. Value must match `tabbedPageCategoryTabTestTag("about", 3)`.
     */
    const val AboutTabLab = "about_tab_3"
    const val AboutComponentLabButton = "about_component_lab_button"
    const val DebugComponentLabPageRoot = "debug_component_lab_page_root"
    const val DebugLiquidCatalogButton = "debug_liquid_catalog_button"
    const val DebugLiquidCatalogPageRoot = "debug_liquid_catalog_page_root"

    /**
     * The Actions-history route's four category tabs.
     *
     * Spelled out rather than derived, because `BaselineProfileTestTagContractTest` checks the
     * generator's constants against literals declared here. The values must match
     * `tabbedPageCategoryTabTestTag("github_history", index)`, which is what the chrome applies.
     */
    const val GitHubHistoryTabRefresh = "github_history_tab_0"
    const val GitHubHistoryTabActions = "github_history_tab_1"
    const val GitHubHistoryTabTracking = "github_history_tab_2"
    const val GitHubHistoryTabApps = "github_history_tab_3"

    /**
     * One switch in the daily-done template editor, tagged so a journey can make an edit it knows about.
     *
     * A switch rather than one of the four dropdowns: a toggle always changes the draft, so the
     * unsaved-changes path it is there to reach cannot silently not happen because a picker landed back
     * on the value it started from.
     */
    const val BaDailyTemplateHeadpatSwitch = "ba_daily_template_headpat_switch"
    const val GitHubPageRoot = "github_page_root"
    const val GitHubImportMenuButton = "github_import_menu_button"
    const val GitHubImportTracks = "github_import_tracks"
    const val GitHubImportStars = "github_import_stars"
    const val GitHubShareImportCancel = "github_share_import_cancel"
    const val GitHubShareImportConfirm = "github_share_import_confirm"
    const val GitHubShareImportPendingClose = "github_share_import_pending_close"
    const val GitHubShareImportPendingCancel = "github_share_import_pending_cancel"
    const val GitHubShareImportAttachClose = "github_share_import_attach_close"
    const val GitHubShareImportAttachCancel = "github_share_import_attach_cancel"
    const val GitHubShareImportAttachConfirm = "github_share_import_attach_confirm"
    /**
     * The overview's progress ring, tagged only while a refresh is actually running.
     *
     * The ring is drawn for every non-idle state -- cached and completed included -- so the tag is
     * applied conditionally rather than to the composable. What needs to be observable is not "there is
     * a ring" but "the batch is still going", because leaving the GitHub page cancels it: a baseline
     * profile journey that navigated away a second after pulling turned a refresh of three items into a
     * record reading `0/3 done, failed 0`, which is a record with nothing to diagnose.
     */
    const val GitHubOverviewRefreshing = "github_overview_refreshing"
    const val GitHubActionsHistoryButton = "github_actions_history_button"
    const val GitHubActionsHistoryPageRoot = "github_actions_history_page_root"

    /**
     * Every refresh history record card, all carrying the same tag rather than one "first" tag.
     *
     * The other card tags in this file name a single card because a journey only ever wants that one.
     * This one is different: what the baseline profile is after is a record whose refresh *failed*, and
     * which record that is depends on what the device did last. Tagging them all lets a journey walk
     * the visible cards until [GitHubRefreshHistoryDiagnostics] appears instead of betting on position.
     */
    const val GitHubRefreshHistoryCard = "github_refresh_history_card"

    /**
     * The diagnostic pill row inside an expanded refresh record, which composes only when that record
     * has something to diagnose -- a failure, a slow item, or a scheduler stop.
     *
     * Tagged as proof rather than as a handle. Nothing here is clicked; the baseline profile journey
     * waits for it to decide whether its failing-refresh fixture actually took, because a journey that
     * expands a clean record collects nothing and still passes.
     */
    const val GitHubRefreshHistoryDiagnostics = "github_refresh_history_diagnostics"

    /**
     * The other two history tabs' record cards, and the detail bodies only an expanded card draws.
     *
     * Same shape as the refresh pair above and for the same reason. A collapsed card composes its
     * header and stops, so the body -- which is most of both files -- needs a handle that exists only
     * once it is open. Waiting on the card alone would let a missed header tap read as success.
     */
    const val GitHubTrackChangeHistoryCard = "github_track_change_history_card"
    const val GitHubTrackChangeHistoryDetails = "github_track_change_history_details"
    const val GitHubAppInstallHistoryCard = "github_app_install_history_card"
    const val GitHubAppInstallHistoryDetails = "github_app_install_history_details"

    /**
     * The JSON import window's root, and its confirm button in the one state where it applies the file.
     *
     * The root is here for the reason `pageRootTestTag` exists: this window had no tags at all, so
     * nothing had ever set `testTagsAsResourceId` on it, and the confirm tag below was composed and
     * invisible to UiAutomator until the root started publishing the subtree.
     */
    const val JsonImportPageRoot = "json_import_page_root"
    const val JsonImportConfirm = "json_import_confirm"
    const val GitHubShareImportAttachConfirmOpenGitHub =
        "github_share_import_attach_confirm_open_github"
}
