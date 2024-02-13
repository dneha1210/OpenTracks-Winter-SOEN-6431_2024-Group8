/*
 * Copyright 2008 Google Inc.
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

package de.dennisguse.opentracks.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordedActivity;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.StatisticsRecordedBinding;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * A fragment to display track statistics to the user for a recorded
 * {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordedFragment extends Fragment {

    private static final String TAG = StatisticsRecordedFragment.class.getSimpleName();

    private static final String TRACK_ID_KEY = "trackId";

    public static StatisticsRecordedFragment newInstance(Track.Id trackId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(TRACK_ID_KEY, trackId);

        StatisticsRecordedFragment fragment = new StatisticsRecordedFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private SensorStatistics sensorStatistics;

    private Track.Id trackId;
    @Nullable // Lazily loaded.
    private Track track;

    private ContentProviderUtils contentProviderUtils;

    private StatisticsRecordedBinding viewBinding;

    private UnitSystem unitSystem = UnitSystem.defaultUnitSystem();
    private boolean preferenceReportSpeed;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (
            sharedPreferences, key) -> {
        boolean updateUInecessary = false;

        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            updateUInecessary = true;
            unitSystem = PreferencesUtils.getUnitSystem();
        }

        if (PreferencesUtils.isKey(R.string.stats_rate_key, key) && track != null) {
            updateUInecessary = true;
            preferenceReportSpeed = PreferencesUtils.isReportSpeed(track);
        }

        if (key != null && updateUInecessary && isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    updateUI();
                }
            });
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackId = getArguments().getParcelable(TRACK_ID_KEY);
        contentProviderUtils = new ContentProviderUtils(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordedBinding.inflate(inflater, container, false);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        loadStatistics();
    }

    @Override
    public void onPause() {
        super.onPause();

        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    public void loadStatistics() {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    Track trackWithIds = contentProviderUtils.getTrack(trackId);
                    if (trackWithIds == null) {
                        Log.e(TAG, "trackWithIds cannot be null");
                        getActivity().finish();
                        return;
                    }

                    sensorStatistics = contentProviderUtils.getSensorStats(trackId);

                    boolean prefsChanged = this.track == null
                            || (!this.track.getActivityTypeLocalized().equals(trackWithIds.getActivityTypeLocalized()));
                    this.track = trackWithIds;
                    if (prefsChanged) {
                        sharedPreferenceChangeListener.onSharedPreferenceChanged(null,
                                getString(R.string.stats_rate_key));
                    }

                    loadTrackDescription(trackWithIds);
                    updateUI();
                    updateSensorUI();

                    ((TrackRecordedActivity) getActivity())
                            .startPostponedEnterTransitionWith(viewBinding.statsActivityTypeIcon);
                }
            });
        }
    }

    private void loadTrackDescription(@NonNull Track track) {
        viewBinding.statsNameValue.setText(track.getName());
        viewBinding.statsDescriptionValue.setText(track.getDescription());
        viewBinding.statsStartDatetimeValue
                .setText(StringUtils.formatDateTimeWithOffsetIfDifferent(track.getStartTime()));
    }

    private void updateUISetTotalDistance(TrackStatistics trackStatistics) {
        Pair<String, String> parts = DistanceFormatter.Builder()
                .setUnit(unitSystem)
                .build(getContext()).getDistanceParts(trackStatistics.getTotalDistance());

        viewBinding.statsDistanceValue.setText(parts.first);
        viewBinding.statsDistanceUnit.setText(parts.second);

    }

    private void updateUISetActivityType(TrackStatistics trackStatistics) {
        Context context = getContext();
        String localizedActivityType = track.getActivityTypeLocalized();
        int iconDrawableId = ActivityType.findByLocalizedString(context, localizedActivityType)
                .getIconDrawableId();
        viewBinding.statsActivityTypeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), iconDrawableId));
    }

    private void updatedUISetTimeAndStartDatetime(TrackStatistics trackStatistics) {
        viewBinding.statsMovingTimeValue.setText(StringUtils.formatElapsedTime(trackStatistics.getMovingTime()));
        viewBinding.statsTotalTimeValue.setText(StringUtils.formatElapsedTime(trackStatistics.getTotalTime()));
    }

    private void updatedAverageSpeed(TrackStatistics trackStatistics) {
        viewBinding.statsAverageSpeedLabel
                .setText(preferenceReportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

        Pair<String, String> parts = formatter.getSpeedParts(trackStatistics.getAverageSpeed());
        viewBinding.statsAverageSpeedValue.setText(parts.first);
        viewBinding.statsAverageSpeedUnit.setText(parts.second);
    }

    private void updatedMaxSpeed(TrackStatistics trackStatistics) {
        viewBinding.statsMaxSpeedLabel
                .setText(preferenceReportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

        Pair<String, String> parts = formatter.getSpeedParts(trackStatistics.getMaxSpeed());
        viewBinding.statsMaxSpeedValue.setText(parts.first);
        viewBinding.statsMaxSpeedUnit.setText(parts.second);
    }

    private void updatedSetMovingSpeed(TrackStatistics trackStatistics) {
        viewBinding.statsMovingSpeedLabel.setText(
                preferenceReportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

        Pair<String, String> parts = formatter.getSpeedParts(trackStatistics.getAverageMovingSpeed());
        viewBinding.statsMovingSpeedValue.setText(parts.first);
        viewBinding.statsMovingSpeedUnit.setText(parts.second);
    }

    private void updatedSetAltitudeGainAndLoss(TrackStatistics trackStatistics) {
        Float altitudeGain = trackStatistics.getTotalAltitudeGain();
        Float altitudeLoss_m = trackStatistics.getTotalAltitudeLoss();

        Pair<String, String> parts;

        parts = StringUtils.getAltitudeParts(getContext(), altitudeGain, unitSystem);
        viewBinding.statsAltitudeGainValue.setText(parts.first);
        viewBinding.statsAltitudeGainUnit.setText(parts.second);

        parts = StringUtils.getAltitudeParts(getContext(), altitudeLoss_m, unitSystem);
        viewBinding.statsAltitudeLossValue.setText(parts.first);
        viewBinding.statsAltitudeLossUnit.setText(parts.second);

        boolean show = altitudeGain != null && altitudeLoss_m != null;
        viewBinding.statsAltitudeGroup.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateUI() {
        TrackStatistics trackStatistics = track.getTrackStatistics();
        // Set total distance
        updateUISetTotalDistance(trackStatistics);

        // Set activity type
        updateUISetActivityType(trackStatistics);

        // Set time and start datetime
        updatedUISetTimeAndStartDatetime(trackStatistics);

        SpeedFormatter formatter = SpeedFormatter.Builder().setUnit(unitSystem)
                .setReportSpeedOrPace(preferenceReportSpeed).build(getContext());
        // Set average speed/pace
        updatedAverageSpeed(trackStatistics);

        // Set max speed/pace
        updatedMaxSpeed(trackStatistics);

        // Set moving speed/pace
        updatedSetMovingSpeed(trackStatistics);

        // Set altitude gain and loss
        updatedSetAltitudeGainAndLoss(trackStatistics);
    }

    private void updateSensorUI() {
        if (sensorStatistics == null) {
            return;
        }

        if (sensorStatistics.hasHeartRate()) {
            String maxBPM = String.valueOf(Math.round(sensorStatistics.maxHeartRate().getBPM()));
            String avgBPM = String.valueOf(Math.round(sensorStatistics.avgHeartRate().getBPM()));

            viewBinding.statsHeartRateGroup.setVisibility(View.VISIBLE);
            viewBinding.statsMaxHeartRateValue.setText(maxBPM);
            viewBinding.statsAvgHeartRateValue.setText(avgBPM);
        }
        if (sensorStatistics.hasCadence()) {
            String maxRPM = String.valueOf(Math.round(sensorStatistics.maxCadence().getRPM()));
            String avgRPM = String.valueOf(Math.round(sensorStatistics.avgCadence().getRPM()));

            viewBinding.statsCadenceGroup.setVisibility(View.VISIBLE);
            viewBinding.statsMaxCadenceValue.setText(maxRPM);
            viewBinding.statsAvgCadenceValue.setText(avgRPM);
        }
        if (sensorStatistics.hasPower()) {
            String maxW = String.valueOf(Math.round(sensorStatistics.maxPower().getW()));
            String avgW = String.valueOf(Math.round(sensorStatistics.avgPower().getW()));

            viewBinding.statsPowerGroup.setVisibility(View.VISIBLE);
            viewBinding.statsMaxPowerValue.setText(maxW);
            viewBinding.statsAvgPowerValue.setText(avgW);
        }
    }
}
