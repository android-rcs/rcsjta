/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.tools.http.provisioning;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This activity creates a provisioning template by querying the DM server. The current MSISDN is
 * replaced by a token to be reused for manual provisioning.
 * 
 * @author yplo6403
 */
public class ProvisioningTemplateActivity extends Activity {
    private static final String PARAM_TERMINAL_MODEL = "terminal_model";
    private static final String TOKEN_MSISDN = "__s__MSISDN__e__";
    private static final String PROVISIONING_FILENAME = "provisioning_template.xml";

    private Button mButton;

    private static String REGEXP_EXTRACT_URI = "<parm name=\"Public_user_Identity\" value=\"sip:(.*)@(.*)\"/>";
    /**
     * Pattern to extract Uri from SIP header
     */
    private final static Pattern PATTERN_EXTRACT_URI = Pattern.compile(REGEXP_EXTRACT_URI);

    /**
     * get URI from SIP identity header
     *
     * @param header the SIP header
     * @return the Uri
     */
    public static String extractMsisdnFromProvisioningResponse(String header) {
        Matcher matcher = PATTERN_EXTRACT_URI.matcher(header);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new NoSuchElementException("Cannot found MSISDN!");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.connect);
        mButton.setOnClickListener(btnConnectListener);
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    private OnClickListener btnConnectListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            TextView textView = (TextView) findViewById(R.id.result);
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.pBAsync);
            try {
                URL url = new URL("http" + "://" + buildProvisioningAddress());
                HttpProvisioningClient client = new HttpProvisioningClient(url, progressBar,
                        textView, mButton);
                client.execute();
            } catch (MalformedURLException e) {
                textView.setText("MalformedURLException occurred: " + e.getMessage() + "!");
            }
        }
    };

    private class HttpProvisioningClient extends AsyncTask<Object, Integer, String> {

        private static final String PARAM_RCS_VERSION = "rcs_version";
        private static final String PARAM_RCS_PROFILE = "rcs_profile";

        private URL mUrl;
        private final TextView mTextView;
        private final ProgressBar mProgress;
        private final Button mGenerateButton;

        public HttpProvisioningClient(URL url, final ProgressBar progress, final TextView textview,
                final Button generateButton) {
            mUrl = url;
            mProgress = progress;
            mTextView = textview;
            mGenerateButton = generateButton;
        }

        @Override
        protected void onPreExecute() {
            mTextView.setText("");
            mProgress.setProgress(0);
            mProgress.setMax(100);
            mGenerateButton.setEnabled(false);
        }

        @Override
        protected String doInBackground(Object... arg0) {
            return getConfig();
        }

        protected void onProgressUpdate(Integer... progress) {
            mProgress.setProgress(progress[0]);
        }

        protected void onPostExecute(String result) {
            mTextView.setText(result);
            mGenerateButton.setEnabled(true);
        }

        private String getConfig() {
            publishProgress(0);
            HttpURLConnection urlConnection = null;
            try {
                /*
                 * Firstly, we query the DM server with HTTP protocol to get the current MSISDN and
                 * a cookie.
                 */
                urlConnection = (HttpURLConnection) mUrl.openConnection();
                int respCode = urlConnection.getResponseCode();
                String message = urlConnection.getResponseMessage();
                if (HttpURLConnection.HTTP_OK == respCode) {
                    mUrl = getHttpsRequestArguments();
                    urlConnection.disconnect();
                    urlConnection = null;
                    publishProgress(50);
                    /*
                     * Secondly, we query the DM server with HTTPs to get the provisioning file.
                     */
                    urlConnection = (HttpURLConnection) mUrl.openConnection();
                    respCode = urlConnection.getResponseCode();
                    message = urlConnection.getResponseMessage();
                    if (HttpURLConnection.HTTP_OK == respCode) {
                        publishProgress(100);
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        String content = readStream(in);
                        try {
                            String msisdn = extractMsisdnFromProvisioningResponse(content);
                            saveProvisioningTemplate(content, msisdn);
                        } catch (NoSuchElementException e) {
                            return "Second request failed: code=" + respCode + " message='" + e.getMessage()
                                    + "'!";
                        }

                        return "Success! /sdcard/provisioning_template.xml is available";
                    } else {
                        return "Second request failed: code=" + respCode + " message='" + message
                                + "'!";
                    }
                } else {
                    publishProgress(0);
                    return "First request failed: code=" + respCode + " message='" + message + "'!";
                }
            } catch (FileNotFoundException e) {
                publishProgress(0);
                return "FileNotFoundException occurred: '" + e.getMessage() + "'!";
            } catch (MalformedURLException e) {
                publishProgress(0);
                return "MalformedURLException occurred: '" + e.getMessage() + "'!";
            } catch (IOException e) {
                publishProgress(0);
                return "IOException occurred: '" + e.getMessage() + "'!";
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        private String readStream(InputStream in) throws IOException {
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
                for (String line = r.readLine(); line != null; line = r.readLine()) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) {

                }
            }
        }

        private URL getHttpsRequestArguments() throws MalformedURLException {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.scheme("https");
            uriBuilder.authority(buildProvisioningAddress());
            uriBuilder.appendQueryParameter(PARAM_RCS_VERSION, "5.1B");
            uriBuilder.appendQueryParameter(PARAM_RCS_PROFILE, "joyn_blackbird");
            uriBuilder.appendQueryParameter(PARAM_TERMINAL_MODEL, Build.DEVICE );
            return new URL(uriBuilder.build().toString());
        }

        private void saveProvisioningTemplate(String provisioning, String msisdn) throws FileNotFoundException {
            File dirProvTemplate = Environment.getExternalStorageDirectory();
            PrintWriter out = null;
            try {
                File file = new File(dirProvTemplate, PROVISIONING_FILENAME);
                out = new PrintWriter(file);
                if (msisdn.startsWith("+")) {
                    msisdn = msisdn.substring(1);
                }
                String template = provisioning.replaceAll(msisdn, TOKEN_MSISDN);
                out.println(template);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private String buildProvisioningAddress() {
        String mnc = String.format("%03d", getMobileNetworkCode());
        String mcc = String.format("%03d", getMobileCountryCode());
        return "config.rcs.mnc" + mnc + ".mcc" + mcc + ".pub.3gppnetwork.org";
    }

    private int getMobileCountryCode() {
        Configuration config = getResources().getConfiguration();
        return config.mcc;
    }

    private int getMobileNetworkCode() {
        Configuration config = getResources().getConfiguration();
        return config.mnc;
    }
}
