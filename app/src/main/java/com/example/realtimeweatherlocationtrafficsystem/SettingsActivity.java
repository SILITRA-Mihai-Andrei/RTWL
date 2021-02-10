package com.example.realtimeweatherlocationtrafficsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    public static final String DEFAULT_BLUETOOTH_DEVICE = "HC-05";
    public static final String DEFAULT_ZOOM_KEY = "15f";
    public static final String DEFAULT_MAX_ZOOM_REGION = "16f";

    public static final int TYPE_EDIT_TEXT_PREF = 1;
    public static final int TYPE_LIST_PREF = 2;

    public static final int PREF_DEFAULT_DEVICE = 1;
    public static final int PREF_VALUE_STRING = 2;
    public static final int PREF_VALUE_STRING_SLASH = 3;

    private static SharedPreferences bluetooth_settings;
    private static SharedPreferences google_maps_settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsFrame, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        bluetooth_settings = getSharedPreferences(getString(R.string.preference_bluetooth_key), MODE_PRIVATE);
        google_maps_settings = getSharedPreferences(getString(R.string.preference_google_maps_key), MODE_PRIVATE);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        SharedPreferences.Editor bluetooth_settings_editor;
        SharedPreferences.Editor google_maps_settings_editor;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            bluetooth_settings_editor = bluetooth_settings.edit();
            google_maps_settings_editor = google_maps_settings.edit();
            final EditTextPreference bluetoothMainDevice = findPreference(getString(R.string.bluetooth_main_device_key));
            final ListPreference bluetoothReceiveDelimiter = findPreference(getString(R.string.bluetooth_receive_delimiter_key));
            final ListPreference bluetoothReceiveStateDelimiter = findPreference(getString(R.string.bluetooth_receive_state_delimiter_key));
            // Preferences for Google Maps
            final EditTextPreference mapsDefaultZoom = findPreference(getString(R.string.maps_default_zoom_key));
            final EditTextPreference mapsMaxRegionZoom = findPreference(getString(R.string.maps_max_zoom_region_key));

            if (bluetoothMainDevice != null) {
                final String key = getString(R.string.bluetooth_main_device_key);
                bluetoothMainDevice.setOnBindEditTextListener(getOnBindEditText(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS));
                String value = bluetooth_settings.getString(key, null);
                if (value == null) {
                    value = DEFAULT_BLUETOOTH_DEVICE;
                    bluetooth_settings_editor.putString(key, value);
                }
                bluetoothMainDevice.setSummaryProvider(getSummary(key, value, TYPE_EDIT_TEXT_PREF, PREF_DEFAULT_DEVICE));
                bluetoothMainDevice.setOnPreferenceChangeListener(getPreferenceChangeListener(bluetoothMainDevice, bluetooth_settings_editor, key, PREF_DEFAULT_DEVICE));
            }

            if (bluetoothReceiveDelimiter != null) {
                final String key = getString(R.string.bluetooth_receive_delimiter_key);
                String value = bluetooth_settings.getString(key, null);
                if (value == null) {
                    value = "\r";
                    bluetooth_settings_editor.putString(key, value);
                }
                bluetoothReceiveDelimiter.setSummaryProvider(getSummary(key, value, TYPE_LIST_PREF, PREF_VALUE_STRING_SLASH));
                bluetoothReceiveDelimiter.setOnPreferenceChangeListener(getPreferenceChangeListener(bluetoothReceiveDelimiter, bluetooth_settings_editor, key, PREF_VALUE_STRING_SLASH));
            }

            if (bluetoothReceiveStateDelimiter != null) {
                final String key = getString(R.string.bluetooth_receive_state_delimiter_key);
                String value = bluetooth_settings.getString(key, null);
                if (value == null) {
                    value = ":";
                    bluetooth_settings_editor.putString(key, value);
                }
                bluetoothReceiveStateDelimiter.setSummaryProvider(getSummary(key, value, TYPE_LIST_PREF, PREF_VALUE_STRING));
                bluetoothReceiveStateDelimiter.setOnPreferenceChangeListener(getPreferenceChangeListener(bluetoothReceiveStateDelimiter, bluetooth_settings_editor, key, PREF_VALUE_STRING));
            }

            if (mapsDefaultZoom != null) {
                final String key = getString(R.string.maps_default_zoom_key);
                mapsDefaultZoom.setOnBindEditTextListener(getOnBindEditText(InputType.TYPE_NUMBER_FLAG_DECIMAL));
                String value = google_maps_settings.getString(key, null);
                if (value == null) {
                    value = "15f";
                    google_maps_settings_editor.putString(key, value);
                }
                mapsDefaultZoom.setSummaryProvider(getSummary(key, value, TYPE_EDIT_TEXT_PREF, PREF_VALUE_STRING));
                mapsDefaultZoom.setOnPreferenceChangeListener(getPreferenceChangeListener(mapsDefaultZoom, google_maps_settings_editor, key, PREF_VALUE_STRING));
            }

            if (mapsMaxRegionZoom != null) {
                final String key = getString(R.string.maps_max_zoom_region_key);
                mapsMaxRegionZoom.setOnBindEditTextListener(getOnBindEditText(InputType.TYPE_NUMBER_FLAG_DECIMAL));
                String value = google_maps_settings.getString(key, null);
                if (value == null) {
                    value = "16f";
                    google_maps_settings_editor.putString(key, value);
                }
                mapsMaxRegionZoom.setSummaryProvider(getSummary(key, value, TYPE_EDIT_TEXT_PREF, PREF_VALUE_STRING));
                mapsMaxRegionZoom.setOnPreferenceChangeListener(getPreferenceChangeListener(mapsMaxRegionZoom, google_maps_settings_editor, key, PREF_VALUE_STRING));
            }

            bluetooth_settings_editor.apply();
            google_maps_settings_editor.apply();
        }

        private Preference.SummaryProvider<?> getSummary(final String key, final Object value, int type, final int order) {
            if (key == null || value == null) return null;
            final String string = getPreferenceStringByOrder(order);
            if (string == null) return null;
            if (type == TYPE_EDIT_TEXT_PREF) {
                return new Preference.SummaryProvider<EditTextPreference>() {
                    @Override
                    public CharSequence provideSummary(EditTextPreference preference) {
                        if (value.getClass() == Integer.class) {
                            if ((int) value == -1) return null;
                            else return String.format(string, (int) value);
                        } else if (value.getClass() == Float.class) {
                            if ((float) value == -1f) return null;
                            else return String.format(string, (float) value);
                        } else if (value.getClass() == String.class) {
                            return String.format(string, (String) value);
                        }
                        return null;
                    }
                };
            } else if (type == TYPE_LIST_PREF) {
                return new Preference.SummaryProvider<ListPreference>() {
                    @Override
                    public CharSequence provideSummary(ListPreference preference) {
                        if (value.getClass() == String.class) {
                            String valueStr = (String) value;
                            if (valueStr.equals("\n")) valueStr = "n";
                            else if (valueStr.equals("\t")) valueStr = "t";
                            return String.format(string, valueStr);
                        }
                        return null;
                    }
                };
            }
            return null;
        }

        private Preference.OnPreferenceChangeListener getPreferenceChangeListener(final Object field, final SharedPreferences.Editor editor, final String key, final int order) {
            return new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (editor == null || google_maps_settings_editor == null) return false;
                    if (field.getClass() == EditTextPreference.class) {
                        EditTextPreference editTextPreference = (EditTextPreference) field;
                        if (newValue.getClass() == Integer.class) {
                            editor.putInt(key, (Integer) newValue);
                            editTextPreference.setSummaryProvider(getSummary(key, (Integer) newValue, TYPE_EDIT_TEXT_PREF, order));
                        } else if (newValue.getClass() == String.class) {
                            editor.putString(key, (String) newValue);
                            editTextPreference.setSummaryProvider(getSummary(key, (String) newValue, TYPE_EDIT_TEXT_PREF, order));
                        }
                    } else if (field.getClass() == ListPreference.class) {
                        ListPreference listPreference = (ListPreference) field;
                        if (newValue.getClass() == String.class) {
                            editor.putString(key, (String) newValue);
                            listPreference.setSummaryProvider(getSummary(key, (String) newValue, TYPE_LIST_PREF, order));
                        }
                    }
                    editor.apply();
                    return false;
                }
            };
        }

        private EditTextPreference.OnBindEditTextListener getOnBindEditText(final int inputType) {
            return new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(inputType);
                }
            };
        }

        private String getPreferenceStringByOrder(int order) {
            if (order == PREF_DEFAULT_DEVICE)
                return getString(R.string.preference_default_device_placeholder);
            else if (order == PREF_VALUE_STRING)
                return getString(R.string.preference_value_string_placeholder);
            else if (order == PREF_VALUE_STRING_SLASH)
                return getString(R.string.preference_value_string_slash_placeholder);
            return null;
        }
    }
}