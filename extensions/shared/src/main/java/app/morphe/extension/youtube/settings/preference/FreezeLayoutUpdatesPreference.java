package app.morphe.extension.youtube.settings.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "deprecation"})
public class FreezeLayoutUpdatesPreference extends SwitchPreference {
    {
        setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                SharedPreferences youtubePreferences =
                        getContext().getSharedPreferences("youtube", Context.MODE_PRIVATE);
                Settings.FROZEN_HOT_HASH_DATA.save(youtubePreferences.getString(
                        "com.google.android.libraries.youtube.innertube.hot_hash_data", ""));
                Settings.FROZEN_HOT_CONFIG_GROUP.save(youtubePreferences.getString(
                        "com.google.android.libraries.youtube.innertube.hot_config_group", ""));
                Settings.FROZEN_COLD_HASH_DATA.save(youtubePreferences.getString(
                        "com.google.android.libraries.youtube.innertube.cold_hash_data", ""));
                Settings.FROZEN_COLD_CONFIG_GROUP.save(youtubePreferences.getString(
                        "com.google.android.libraries.youtube.innertube.cold_config_group", ""));
            }
            return true;
        });
    }

    public FreezeLayoutUpdatesPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FreezeLayoutUpdatesPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FreezeLayoutUpdatesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FreezeLayoutUpdatesPreference(Context context) {
        super(context);
    }
}
