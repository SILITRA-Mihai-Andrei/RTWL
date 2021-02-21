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

import com.example.realtimeweatherlocationtrafficsystem.models.Utils;

/**
 * This activity will define and store some of the important variables.
 * It also allow the user to modify them.
 */
public class SettingsActivity extends AppCompatActivity {

    // Define default variables
    public static final String DEFAULT_BLUETOOTH_DEVICE = "HC-05";                  // the most common Bluetooth device used for this kind of application
    public static final String DEFAULT_BLUETOOTH_RECEIVE_DELIMITER = "\r";          // the Bluetooth receive messages delimiter - separator between two messages
    public static final String DEFAULT_BLUETOOTH_RECEIVE_STATE_DELIMITER = "\r";    // the Bluetooth receive state messages delimiter - separator between two messages of state
    public static final String DEFAULT_START_ZOOM = "15f";                          // the zoom value used by GoogleMapsActivity to start the map view
    public static final String DEFAULT_MAX_ZOOM_REGION = "16f";                     // the zoom value at which a weather icon of a region can't be seen

    // Define the types used for summary provider
    public static final int TYPE_EDIT_TEXT_PREF = 1;
    public static final int TYPE_LIST_PREF = 2;

    // Define the preferences values
    public static final int PREF_DEFAULT_DEVICE = 1;
    public static final int PREF_VALUE_STRING = 2;
    public static final int PREF_VALUE_STRING_SLASH = 3;

    // Define the SharedPreferences objects for each settings category
    private static SharedPreferences bluetooth_settings;    // settings for Bluetooth
    private static SharedPreferences google_maps_settings;  // settings for GoogleMaps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        // Initialize the settings fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsFrame, new SettingsFragment())
                .commit();
        // Get action bar support
        ActionBar actionBar = getSupportActionBar();
        // Check if action bar support is available
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Initialize the SharedPreferences object using the keys
        bluetooth_settings = getSharedPreferences(getString(R.string.preference_bluetooth_key), MODE_PRIVATE);
        google_maps_settings = getSharedPreferences(getString(R.string.preference_google_maps_key), MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the variable that indicates the app is running (not stopped)
        Utils.APP_ACTIVE = true;
    }

    @Override
    protected void onDestroy() {
        // Update the variable that indicates the app is running (not stopped)
        Utils.APP_ACTIVE = false;
        super.onDestroy();
    }

    /**
     * Inner class used to collect, edit and store the settings values.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {

        // Define the editors for each settings category
        SharedPreferences.Editor bluetooth_settings_editor;
        SharedPreferences.Editor google_maps_settings_editor;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            // Initialize the editor from the SharedPreferences objects
            bluetooth_settings_editor = bluetooth_settings.edit();
            google_maps_settings_editor = google_maps_settings.edit();
            // Find the stored values
            // Preferences for Bluetooth
            final EditTextPreference bluetoothMainDevice = findPreference(getString(R.string.bluetooth_main_device_key));
            final ListPreference bluetoothReceiveDelimiter = findPreference(getString(R.string.bluetooth_receive_delimiter_key));
            final ListPreference bluetoothReceiveStateDelimiter = findPreference(getString(R.string.bluetooth_receive_state_delimiter_key));
            // Preferences for Google Maps
            final EditTextPreference mapsDefaultZoom = findPreference(getString(R.string.maps_default_zoom_key));
            final EditTextPreference mapsMaxRegionZoom = findPreference(getString(R.string.maps_max_zoom_region_key));

            // Check if there is a Bluetooth device value defined
            if (bluetoothMainDevice != null) {
                // Get the key for the main Bluetooth device
                final String key = getString(R.string.bluetooth_main_device_key);
                bluetoothMainDevice.setOnBindEditTextListener(getOnBindEditText(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS));
                // Get the value stored in preferences using the key
                String value = bluetooth_settings.getString(key, null);
                // Check if the value exists
                if (value == null) {
                    // The value doesn't exists, so store the default value
                    value = DEFAULT_BLUETOOTH_DEVICE;
                    bluetooth_settings_editor.putString(key, value);
                }
                // Store the value in preferences
                bluetoothMainDevice.setSummaryProvider(getSummary(key, value, TYPE_EDIT_TEXT_PREF, PREF_DEFAULT_DEVICE));
                // Set listener on changing the value
                bluetoothMainDevice.setOnPreferenceChangeListener(getPreferenceChangeListener(bluetoothMainDevice, bluetooth_settings_editor, key, PREF_DEFAULT_DEVICE));
            }

            // Check if the value of Bluetooth receive delimiter is defined in preferences
            if (bluetoothReceiveDelimiter != null) {
                // Get the key for Bluetooth receive delimiter
                final String key = getString(R.string.bluetooth_receive_delimiter_key);
                // Get the value stored in preferences using the key
                String value = bluetooth_settings.getString(key, null);
                // Check if the value exists
                if (value == null) {
                    // The value doesn't exists, so store the default value
                    value = DEFAULT_BLUETOOTH_RECEIVE_DELIMITER;
                    bluetooth_settings_editor.putString(key, value);
                }
                // Store the value in preferences
                bluetoothReceiveDelimiter.setSummaryProvider(getSummary(key, value, TYPE_LIST_PREF, PREF_VALUE_STRING_SLASH));
                // Set listener on changing the value
                bluetoothReceiveDelimiter.setOnPreferenceChangeListener(getPreferenceChangeListener(bluetoothReceiveDelimiter, bluetooth_settings_editor, key, PREF_VALUE_STRING_SLASH));
            }

            // Check if the value of Bluetooth receive state delimiter is defined in preferences
            if (bluetoothReceiveStateDelimiter != null) {
                // Get the key for Bluetooth receive state delimiter
                final String key = getString(R.string.bluetooth_receive_state_delimiter_key);
                // Get the value stored in preferences using the key
                String value = bluetooth_settings.getString(key, null);
                // Check if the value exists
                if (value == null) {
                    // The value doesn't exists, so store the default value
                    value = DEFAULT_BLUETOOTH_RECEIVE_STATE_DELIMITER;
                    bluetooth_settings_editor.putString(key, value);
                }
                // Store the value in preferences
                bluetoothReceiveStateDelimiter.setSummaryProvider(getSummary(key, value, TYPE_LIST_PREF, PREF_VALUE_STRING));
                // Set listener on changing the value
                bluetoothReceiveStateDelimiter.setOnPreferenceChangeListener(getPreferenceChangeListener(bluetoothReceiveStateDelimiter, bluetooth_settings_editor, key, PREF_VALUE_STRING));
            }

            // Check if the value of default map zoom is defined in preferences
            if (mapsDefaultZoom != null) {
                // Get the key for default map zoom
                final String key = getString(R.string.maps_default_zoom_key);
                mapsDefaultZoom.setOnBindEditTextListener(getOnBindEditText(InputType.TYPE_NUMBER_FLAG_DECIMAL));
                // Get the value stored in preferences using the key
                String value = google_maps_settings.getString(key, null);
                // Check if the value exists
                if (value == null) {
                    // The value doesn't exists, so store the default value
                    value = DEFAULT_START_ZOOM;
                    google_maps_settings_editor.putString(key, value);
                }
                // Store the value in preferences
                mapsDefaultZoom.setSummaryProvider(getSummary(key, value, TYPE_EDIT_TEXT_PREF, PREF_VALUE_STRING));
                // Set listener on changing the value
                mapsDefaultZoom.setOnPreferenceChangeListener(getPreferenceChangeListener(mapsDefaultZoom, google_maps_settings_editor, key, PREF_VALUE_STRING));
            }

            // Check if the value of max region zoom is defined in preferences
            if (mapsMaxRegionZoom != null) {
                // Get the key for max region zoom
                final String key = getString(R.string.maps_max_zoom_region_key);
                mapsMaxRegionZoom.setOnBindEditTextListener(getOnBindEditText(InputType.TYPE_NUMBER_FLAG_DECIMAL));
                // Get the value stored in preferences using the key
                String value = google_maps_settings.getString(key, null);
                // Check if the value exists
                if (value == null) {
                    // The value doesn't exists, so store the default value
                    value = DEFAULT_MAX_ZOOM_REGION;
                    google_maps_settings_editor.putString(key, value);
                }
                // Store the value in preferences
                mapsMaxRegionZoom.setSummaryProvider(getSummary(key, value, TYPE_EDIT_TEXT_PREF, PREF_VALUE_STRING));
                // Set listener on changing the value
                mapsMaxRegionZoom.setOnPreferenceChangeListener(getPreferenceChangeListener(mapsMaxRegionZoom, google_maps_settings_editor, key, PREF_VALUE_STRING));
            }

            // Apply the editors configured above
            bluetooth_settings_editor.apply();
            google_maps_settings_editor.apply();
        }

        /**
         * Get summary from SharedPreferences using the key.
         *
         * @param key   is the key used to locate the setting object.
         * @param value is the value that will be stored.
         * @param type  is the type of preferences.
         * @param order is value used to order the received strings from preferences.
         * @return the configured summary provider.
         */
        private Preference.SummaryProvider<?> getSummary(final String key, final Object value, int type, final int order) {
            // Check if the key and value exists
            if (key == null || value == null) return null;
            // Get the value stored in preferences with the @key
            final String string = getPreferenceStringByOrder(order);
            // Check if the value exists
            if (string == null) return null;
            // Check which type of preferences is
            if (type == TYPE_EDIT_TEXT_PREF) {
                return new Preference.SummaryProvider<EditTextPreference>() {
                    @Override
                    public CharSequence provideSummary(EditTextPreference preference) {
                        // Check the type of @value object
                        if (value.getClass() == Integer.class) {
                            if ((int) value == -1) return null;                 // invalid value
                            else
                                return String.format(string, (int) value);     // convert the Integer value to String
                        } else if (value.getClass() == Float.class) {
                            if ((float) value == -1f) return null;              // invalid value
                            else
                                return String.format(string, (float) value);   // convert the Float value to String
                        } else if (value.getClass() == String.class) {
                            return String.format(string, (String) value);       // make sure the value is returned as String
                        }
                        return null;
                    }
                };
            } else if (type == TYPE_LIST_PREF) {
                return new Preference.SummaryProvider<ListPreference>() {
                    @Override
                    public CharSequence provideSummary(ListPreference preference) {
                        // Check the type of @value object
                        if (value.getClass() == String.class) {
                            // Make sure the value is a String
                            String valueStr = (String) value;
                            // Format the value to display property
                            // Ex: '\n' and '\r' will not be displayed property in a text view
                            if (valueStr.equals("\n")) valueStr = "n";
                            else if (valueStr.equals("\t")) valueStr = "t";
                            return String.format(string, valueStr);             // make sure that the returned value is String
                        }
                        return null;
                    }
                };
            }
            return null;
        }

        /**
         * Get the PreferenceChangeListener object configured using the editor and key.
         *
         * @param field  is the preference type.
         * @param editor is the SharedPreferences object editor that will allow changing the setting value.
         * @param key    is the key used to find the preference.
         * @param order  is value used to order the received strings from preferences.
         * @return the PreferenceChangeListener object that will change the value from preference with the new value.
         */
        private Preference.OnPreferenceChangeListener getPreferenceChangeListener(final Object field, final SharedPreferences.Editor editor, final String key, final int order) {
            return new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Check if the editors are available
                    if (editor == null || google_maps_settings_editor == null) return false;
                    // Check the preference type
                    if (field.getClass() == EditTextPreference.class) {
                        // Get the preference of EditTextPreference type
                        EditTextPreference editTextPreference = (EditTextPreference) field;
                        // Check what type of object is the new value
                        if (newValue.getClass() == Integer.class) {
                            // Convert the new value to Integer and put the value into the editor using the corresponding key
                            editor.putInt(key, (Integer) newValue);
                            // Update the value from view with the new value
                            editTextPreference.setSummaryProvider(getSummary(key, (Integer) newValue, TYPE_EDIT_TEXT_PREF, order));
                        } else if (newValue.getClass() == String.class) {
                            // Convert the new value to String and put the value into the editor using the corresponding key
                            editor.putString(key, (String) newValue);
                            // Update the value from view with the new value
                            editTextPreference.setSummaryProvider(getSummary(key, (String) newValue, TYPE_EDIT_TEXT_PREF, order));
                        }
                    } else if (field.getClass() == ListPreference.class) {
                        // Get the preference of ListPreference type
                        ListPreference listPreference = (ListPreference) field;
                        // Check if the new value is a String object
                        if (newValue.getClass() == String.class) {
                            // Convert the new value to String and put the value into the editor using the key
                            editor.putString(key, (String) newValue);
                            // Update the value from view with the new value
                            listPreference.setSummaryProvider(getSummary(key, (String) newValue, TYPE_LIST_PREF, order));
                        }
                    }
                    // Apply all changes made to editor
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

        /**
         * Get the String from resources using the order value.
         *
         * @param order is the order value.
         * @return the String taken from resources.
         */
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