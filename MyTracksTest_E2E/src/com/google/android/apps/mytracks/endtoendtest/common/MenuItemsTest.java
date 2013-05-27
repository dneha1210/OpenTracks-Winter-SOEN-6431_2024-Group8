/*
 * Copyright 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.apps.mytracks.endtoendtest.common;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.endtoendtest.GoogleUtils;
import com.google.android.apps.mytracks.endtoendtest.sync.SyncTestUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.services.drive.Drive;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Tests some menu items of MyTracks.
 * 
 * @author Youtao Liu
 */
public class MenuItemsTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;
  private final static String SHARE_ITEM_PARENT_VIEW_NAME = "RecycleListView";

  public MenuItemsTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
  }

  /**
   * Tests following items in More menu.
   * <ul>
   * <li>Tests the aggregated statistics activity.</li>
   * <li>Tests the Sensor state activity.</li>
   * <li>Tests the help menu.</li>
   * </ul>
   */
  public void testSomeMenuItems() {
    // Menu in TrackListActivity.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_aggregated_statistics),
        true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.stats_distance));
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();
    // Menu in TrackDetailActivity.
    // When there is no sensor connected this menu will be hidden.
    if (EndToEndTestUtils
        .findMenuItem(activityMyTracks.getString(R.string.menu_sensor_state), true)) {
      EndToEndTestUtils.SOLO.waitForText(activityMyTracks
          .getString(R.string.sensor_state_last_sensor_time));
    }

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_help), true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.help_about), true, true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
  }

  /**
   * Tests search menu item. Checks the display and hide of record controller
   * during search.
   */
  public void testSearch() {
    EndToEndTestUtils.createSimpleTrack(0, true);
    assertTrue(isControllerShown());
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_search), true);
    assertFalse(isControllerShown());
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, EndToEndTestUtils.trackName);
    sendKeys(KeyEvent.KEYCODE_ENTER);
    instrumentation.waitForIdleSync();
    assertEquals(1, EndToEndTestUtils.SOLO.getCurrentListViews().size());
    EndToEndTestUtils.SOLO.goBack();
    assertTrue(isControllerShown());
  }

  /**
   * Gets the status whether is controller is shown.
   * 
   * @return true mean the controller is display and false mean it is disappear
   */
  private boolean isControllerShown() {
    return activityMyTracks.findViewById(R.id.track_controler_container).isShown();
  }

  /**
   * Tests sharing a track with Google Drive.
   * 
   * @throws GoogleAuthException
   * @throws IOException
   */
  public void testShareActivity_withDrive() throws IOException, GoogleAuthException {
    // Prepare test environment.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    Drive drive1 = SyncTestUtils.getGoogleDrive(EndToEndTestUtils.activityMytracks
        .getApplicationContext());
    SyncTestUtils.removeKMLFiles(drive1);
    EndToEndTestUtils.deleteAllTracks();
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    Drive drive2 = SyncTestUtils.getGoogleDrive(EndToEndTestUtils.activityMytracks
        .getApplicationContext());
    SyncTestUtils.removeKMLFiles(drive2);
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.resetAllSettings(activityMyTracks, false);
    
    EndToEndTestUtils.createSimpleTrack(0, false);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sharing_share_track));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sharing_share_track_drive));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    // Click share, check message and cancel the share.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_share), true);
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.share_track_drive_confirm_message)));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_no));
    assertFalse(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.share_track_add_emails_title), 1,
        EndToEndTestUtils.VERY_SHORT_WAIT_TIME));

    // Click share again and confirm the share.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_share), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_yes));

    boolean isAccount2Bound = false;
    // If Choose account dialog prompt, choose the first account.
    if (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.send_google_choose_account_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      // Whether can found account2.
      if (EndToEndTestUtils.SOLO.waitForText(GoogleUtils.ACCOUNT_NAME_2, 1,
          EndToEndTestUtils.VERY_SHORT_WAIT_TIME)) {
        isAccount2Bound = true;
      }
      EndToEndTestUtils.SOLO.clickOnText(GoogleUtils.ACCOUNT_NAME_1);
      EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true,
          true);
    }

    // Input account to share and click OK button.
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.share_track_add_emails_title)));
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Waiting the send is finish.
    while (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.generic_progress_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {}

    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.SOLO
        .getString(R.string.generic_success_title)));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.SOLO.getString(R.string.generic_ok));

    // Make more checks if the second account is also bound with this device.
    if (isAccount2Bound) {
      EndToEndTestUtils.SOLO.goBack();
      SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
      EndToEndTestUtils.findMenuItem(
          EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);

      assertTrue(EndToEndTestUtils.SOLO.waitForText(
          activityMyTracks.getString(R.string.track_list_shared_with_me), 1,
          EndToEndTestUtils.SUPER_LONG_WAIT_TIME));
      assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName, 1,
          EndToEndTestUtils.SUPER_LONG_WAIT_TIME));
    }
  }

  /**
   * Tests the share a track with Google Maps.
   */
  public void testShareActivity_withMaps() {
    EndToEndTestUtils.createTrackIfEmpty(0, false);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sharing_share_track));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sharing_share_track_maps));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    // Click share, check message and cancel the share.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_share), true);
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(
        R.string.share_track_maps_confirm_message).split("\\%")[0]));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_no));
    assertFalse(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.share_track_add_emails_title), 1,
        EndToEndTestUtils.VERY_SHORT_WAIT_TIME));

    // Try all share items.
    for (int i = 0;; i++) {
      EndToEndTestUtils.createTrackIfEmpty(0, false);
      View oneItemView = findShareItem(i);
      if (oneItemView == null) {
        break;
      }
      EndToEndTestUtils.SOLO.clickOnView(oneItemView);
      EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true,
          true);
      if (!GoogleUtils.isAccountAvailable()) {
        break;
      }
      // Waiting the send is finish.
      while (EndToEndTestUtils.SOLO.waitForText(
          activityMyTracks.getString(R.string.generic_progress_title), 1,
          EndToEndTestUtils.SHORT_WAIT_TIME)) {}

      // Check whether data is correct on Google Map and then delete it.
      assertTrue(GoogleUtils.deleteMap(EndToEndTestUtils.trackName, activityMyTracks));

      // Display the MyTracks activity for the share item may startup other
      // applications.
      Intent intent = new Intent();
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setClass(activityMyTracks.getApplicationContext(), TrackListActivity.class);
      activityMyTracks.getApplicationContext().startActivity(intent);
      EndToEndTestUtils.sleep(EndToEndTestUtils.NORMAL_WAIT_TIME);
    }
  }

  /**
   * Gets the view to click the share item by item index.
   * 
   * @param index of a share item
   * @return null when no such item
   */
  private View findShareItem(int index) {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_share), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_yes));
    ArrayList<View> views = EndToEndTestUtils.SOLO.getViews();
    int i = 0;
    for (View view : views) {
      String name = view.getParent().getClass().getName();
      // Each share item is a child of a "RecycleListView"
      if (name.indexOf(SHARE_ITEM_PARENT_VIEW_NAME) > 0) {
        if (index == i) {
          return view;
        }
        i++;
      }
    }
    return null;
  }

  /**
   * Checks the voice frequency and split frequency menus during recording. When
   * recording, they should be in both the menu and the recording settings. When
   * not recording, they should only be in the recording settings.
   */
  public void testFrequencyMenu() {
    EndToEndTestUtils.startRecording();

    assertTrue(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_voice_frequency), false));
    assertTrue(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_split_frequency), false));

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_recording));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_voice_frequency), 1, true, true));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_split_frequency), 1, true, true));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.stopRecording(true);

    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_voice_frequency), false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_split_frequency), false));

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_recording));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_voice_frequency), 1, true, true));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_split_frequency), 1, true, true));
  }

  /**
   * Tests starting and stopping GPS.
   */
  public void testGPSMenu() {
    boolean GPSStatus = EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_gps), false);

    // Following starting/stopping or stopping/starting GPS.
    EndToEndTestUtils.findMenuItem(GPSStatus ? activityMyTracks.getString(R.string.menu_stop_gps)
        : activityMyTracks.getString(R.string.menu_start_gps), true);
    GPSStatus = !GPSStatus;
    EndToEndTestUtils.waitTextToDisappear(GPSStatus ? activityMyTracks
        .getString(R.string.menu_start_gps) : activityMyTracks.getString(R.string.menu_stop_gps));
    assertEquals(GPSStatus,
        EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_stop_gps), false));

    EndToEndTestUtils.findMenuItem(GPSStatus ? activityMyTracks.getString(R.string.menu_stop_gps)
        : activityMyTracks.getString(R.string.menu_start_gps), true);
    GPSStatus = !GPSStatus;
    EndToEndTestUtils.waitTextToDisappear(GPSStatus ? activityMyTracks
        .getString(R.string.menu_start_gps) : activityMyTracks.getString(R.string.menu_stop_gps));
    assertEquals(GPSStatus,
        EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_stop_gps), false));
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
