/*
 * Copyright (C) 2014 GSM Association
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.gsma.iariauth.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that processes a package containing one or more IARI Authorization documents.
 */
public class PackageProcessor {

    /*******************************
     * Public API
     *******************************/

    /**
     * Construct a PackageProcessor given the known trusted certificates and relevant package
     * parameters.
     * 
     * @param trustStore: a keystore containing the known and enabled IARI range certificates
     * @param packageName: the package id
     * @param packageSigner: a byte array containing the serialised package signing certificate as
     *            returned by PackageManager.getPackageInfo().signatures[0]
     */
    public PackageProcessor(String packageName, String packageSigner) {
        this.packageName = packageName;
        this.packageSigner = packageSigner;
    }

    /**
     * Process an IARI Authorization document presented as an InputStream. It is the caller's
     * responsibility to close the stream after this method has returned.
     * 
     * @param is
     * @return a ProcessingResult, containing details of the processing.
     */
    public ProcessingResult processIARIauthorization(InputStream is) {
        IARIAuthProcessor processor = new IARIAuthProcessor();
        processor.process(is);
        if (processor.getStatus() == ProcessingResult.STATUS_OK) {
            IARIAuthDocument authDocument = processor.getAuthDocument();
            Artifact error = checkPackageDetails(authDocument);
            if (error != null) {
                return new ErrorResult(error);
            }
            authorizedIARIs.put(authDocument.iari, authDocument);
        }
        return processor;
    }

    /**
     * Process an IARI Authorization document presented as an File.
     * 
     * @param f
     * @return a ProcessingResult, containing details of the processing.
     */
    public ProcessingResult processIARIauthorization(File f) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            return processIARIauthorization(fis);
        } catch (final FileNotFoundException e) {
            return new ErrorResult(new Artifact("Unable to read IARI Authorization: "
                    + e.getLocalizedMessage()));
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (Throwable t) {
            }
        }
    }

    /*******************************
     * Internal
     *******************************/

    private static class ErrorResult implements ProcessingResult {

        @Override
        public int getStatus() {
            return error.status;
        };

        @Override
        public Artifact getError() {
            return error;
        }

        @Override
        public IARIAuthDocument getAuthDocument() {
            return null;
        }

        private ErrorResult(Artifact error) {
            this.error = error;
        }

        private final Artifact error;
    }

    private Artifact checkPackageDetails(IARIAuthDocument authDocument) {
        if (authDocument.packageName != null) {
            if (!authDocument.packageName.equals(packageName)) {
                return new Artifact("Mismatched package name");
            }
        }
        if (!authDocument.packageSigner.equals(packageSigner)) {
            return new Artifact("Mismatched package signer");
        }
        return null;
    }

    private final String packageName;
    private final String packageSigner;
    private final Map<String, IARIAuthDocument> authorizedIARIs = new HashMap<String, IARIAuthDocument>();
}
