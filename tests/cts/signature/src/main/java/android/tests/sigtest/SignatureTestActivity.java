/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tests.sigtest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used for Signature Test. It will started by the Instrumentation class, and send
 * back the test result via IBinder.
 */
public class SignatureTestActivity extends Activity {
    static final String BUNDLE_KEY_MISSING_CLASS = "missing_class";
    static final String BUNDLE_KEY_MISSING_INTERFACE = "missing_interface";
    static final String BUNDLE_KEY_MISSING_METHOD = "missing_method";
    static final String BUNDLE_KEY_MISSING_FIELD = "missing_field";
    static final String BUNDLE_KEY_MISMATCH_CLASS = "mismatch_class_signature";
    static final String BUNDLE_KEY_MISMATCH_INTERFACE = "mismatch_interface_signature";
    static final String BUNDLE_KEY_MISMATCH_METHOD = "mismatch_method_signature";
    static final String BUNDLE_KEY_MISMATCH_FIELD = "mismatch_field_signature";
    static final String BUNDLE_KEY_CAUGHT_EXCEPTION = "caught_exception";

    private DeviceResultObserver mResultObserver;

    private Bundle mBundle;

    private List<RcsApiSignatureTestResult> mFailedItems = new ArrayList<>();
    private ListView mListView;

    private static final String CURRENT_RELEASE = "crane_1_6_1";

    // @formatter:off
    private static final Map<String,Integer> sRcsApiReleases = new HashMap<>();

    static {
        sRcsApiReleases.put("albatros",R.xml.albatros);
        sRcsApiReleases.put("blackbird_1_0",R.xml.blackbird_1_0);
        sRcsApiReleases.put("blackbird_1_5_1", R.xml.blackbird_1_5_1);
        sRcsApiReleases.put(CURRENT_RELEASE, R.xml.crane_1_6_1);
    }
    // @formatter:on

    private ArrayAdapter<String> mSpinnerAdapter;
    private int mSelectedResource;
    private TestSigResultArrayAdapter mAdpater;

    /**
     * Define the type of the signature check failures.
     */
    public enum FAILURE_TYPE {
        MISSING_CLASS, MISSING_INTERFACE, MISSING_METHOD, MISSING_FIELD, MISMATCH_CLASS, MISMATCH_INTERFACE, MISMATCH_METHOD, MISMATCH_FIELD, CAUGHT_EXCEPTION,
    }

    static final HashMap<FAILURE_TYPE, String> FAILURE_TYPE_TO_KEY = new HashMap<>();

    static {
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISSING_CLASS, BUNDLE_KEY_MISSING_CLASS);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISSING_INTERFACE, BUNDLE_KEY_MISSING_INTERFACE);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISSING_METHOD, BUNDLE_KEY_MISSING_METHOD);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISSING_FIELD, BUNDLE_KEY_MISSING_FIELD);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISMATCH_CLASS, BUNDLE_KEY_MISMATCH_CLASS);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISMATCH_INTERFACE, BUNDLE_KEY_MISMATCH_INTERFACE);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISMATCH_METHOD, BUNDLE_KEY_MISMATCH_METHOD);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.MISMATCH_FIELD, BUNDLE_KEY_MISMATCH_FIELD);
        FAILURE_TYPE_TO_KEY.put(FAILURE_TYPE.CAUGHT_EXCEPTION, BUNDLE_KEY_CAUGHT_EXCEPTION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_api_signature);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVersion = mSpinnerAdapter.getItem(position);
                Log.w("[RCS]", "Selected version=" + selectedVersion);
                Integer resourceId = sRcsApiReleases.get(selectedVersion);
                if (resourceId != null) {
                    mSelectedResource = resourceId;
                    mFailedItems.clear();
                    mListView.setAdapter(null);
                    CheckRcsApiSignatureTask mCheckRcsApiSignatureTask = new CheckRcsApiSignatureTask();
                    mCheckRcsApiSignatureTask.execute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
        Spinner spinner = (Spinner) findViewById(R.id.spinner_rcs_version);
        String[] versions = sRcsApiReleases.keySet().toArray(new String[sRcsApiReleases.size()]);
        Arrays.sort(versions);
        mSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, versions);
        spinner.setAdapter(mSpinnerAdapter);
        int defaultSpinnerPosition = mSpinnerAdapter.getPosition(CURRENT_RELEASE);
        spinner.setSelection(defaultSpinnerPosition);
        spinner.setOnItemSelectedListener(listener);
        mSelectedResource = ToTest.VERSION;

        // Set list adapter
        mListView = (ListView) findViewById(R.id.report_list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyView);
    }

    private class CheckRcsApiSignatureTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Log.w("[RCS]", "Starts verifying RCS signatures");
            mResultObserver = new DeviceResultObserver();
            start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mResultObserver.sendResult(getIntent());
            Log.w("[RCS]", "Ends verifying RCS signatures");
        }
    }

    private class TestSigResultArrayAdapter extends ArrayAdapter<RcsApiSignatureTestResult> {

        private final Context mCtx;
        private final int mResourceRowLayout;
        private final LayoutInflater mInflater;

        public TestSigResultArrayAdapter(Context ctx, int resourceRowLayout,
                List<RcsApiSignatureTestResult> items) {
            super(ctx, 0, items);
            mCtx = ctx;
            mResourceRowLayout = resourceRowLayout;
            mInflater = LayoutInflater.from(mCtx);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final RcsApiSignatureTestResultViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(mResourceRowLayout, parent, false);
                holder = new RcsApiSignatureTestResultViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (RcsApiSignatureTestResultViewHolder) convertView.getTag();
            }
            RcsApiSignatureTestResult item = getItem(position);
            if (item != null) {
                holder.typeTextView.setText(item.getType().toString());
                holder.classTextView.setText(item.getClassName());
                holder.reasonTextView.setText(item.getReason());
            }
            return convertView;
        }
    }

    /**
     * Start the signature test.
     */
    @SuppressWarnings("unchecked")
    private void start() {
        SignatureTest sigTest = new SignatureTest(mResultObserver);
        Resources r = getResources();
        try {
            // TODO allow user to select version within available XML files
            sigTest.start(r.getXml(mSelectedResource));
        } catch (Exception e) {
            mResultObserver.notifyFailure(FAILURE_TYPE.CAUGHT_EXCEPTION, e.getMessage(),
                    e.getMessage());
        }
    }

    /**
     * This class is an implementation of the ResultObserver. And it aims to record the result in
     * the Bundle, and send back to the Instrumentation class after all results has been recorded.
     */
    final public class DeviceResultObserver implements ResultObserver {
        public DeviceResultObserver() {
            mBundle = new Bundle();
            mBundle.putStringArrayList(BUNDLE_KEY_MISSING_FIELD, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_MISSING_METHOD, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_MISMATCH_CLASS, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_MISMATCH_FIELD, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_MISMATCH_METHOD, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_MISSING_CLASS, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_MISSING_INTERFACE, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_MISMATCH_INTERFACE, new ArrayList<String>());
            mBundle.putStringArrayList(BUNDLE_KEY_CAUGHT_EXCEPTION, new ArrayList<String>());
        }

        private int getFailedClassesNumber(String keyTestType) {
            ArrayList<String> failedClasses = mBundle.getStringArrayList(keyTestType);
            if (failedClasses != null) {
                return failedClasses.size();
            }
            throw new IllegalStateException("Invalid bundle for test type " + keyTestType);
        }

        /**
         * This method is called when all the results has been recorded. And this method will save
         * the results in IBinder and send back to the Instrumentation class.
         *
         * @param intent The intent to carry the result.
         */
        @SuppressWarnings("deprecation")
        public void sendResult(Intent intent) {
            SignatureTestLog.d("Send result");
            int failedClassesNumber = 0;
            if (mBundle == null) {
                throw new IllegalStateException("Invalid bundle");
            }
            for (String testType : mBundle.keySet()) {
                failedClassesNumber += getFailedClassesNumber(testType);
            }
            TextView result = (TextView) findViewById(R.id.test_result);
            if (failedClassesNumber == 0) {
                SignatureTestLog.d("PASS");
                result.setText(R.string.test_passed);
            } else {
                SignatureTestLog.d("FAIL: " + failedClassesNumber);
                result.setText(getString(R.string.test_failed, failedClassesNumber));
            }
            mAdpater = new TestSigResultArrayAdapter(
                    SignatureTestActivity.this, R.layout.rcs_api_signature_item, mFailedItems);
            mListView.setAdapter(mAdpater);
        }

        public void notifyFailure(FAILURE_TYPE type, String name, String errorMessage) {
            SignatureTestLog.d("Failure: ");
            SignatureTestLog.d("   Type: " + type);
            SignatureTestLog.d("   Name: " + name);
            SignatureTestLog.d("   Why : " + errorMessage);
            mBundle.getStringArrayList(SignatureTestActivity.FAILURE_TYPE_TO_KEY.get(type)).add(
                    name);
            mFailedItems.add(new RcsApiSignatureTestResult(type, name, errorMessage));
        }
    }

    private class RcsApiSignatureTestResultViewHolder {
        public final TextView typeTextView;
        public final TextView classTextView;
        public final TextView reasonTextView;

        public RcsApiSignatureTestResultViewHolder(View base) {
            typeTextView = (TextView) base.findViewById(R.id.test_type);
            classTextView = (TextView) base.findViewById(R.id.test_class_name);
            reasonTextView = (TextView) base.findViewById(R.id.test_why);
        }
    }
}
