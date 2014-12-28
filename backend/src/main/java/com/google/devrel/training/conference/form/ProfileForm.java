/* Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devrel.training.conference.form;

/**
 * Pojo representing a profile form on the client side.
 */
public class ProfileForm {
    /**
     * Any string user wants us to display him/her on this system.
     */
    private String displayName;

    /**
     * User's tee shirt size
     */
    private TeeShirtSize teeShirtSize;

    private ProfileForm () {}

    /**
     * Constructor for ProfileForm, solely for unit test.
     * @param displayName A String for displaying the user on this system.
     * @param teeShirtSize User's tee shirt size
     */
    public ProfileForm(String displayName, TeeShirtSize teeShirtSize) {
        this.displayName = displayName;
        this.teeShirtSize = teeShirtSize;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TeeShirtSize getTeeShirtSize() {
        return teeShirtSize;
    }

    /**
     * Enum representing T shirt size.
     */
    public static enum TeeShirtSize {
        NOT_SPECIFIED,
        XS,
        S,
        M,
        L,
        XL,
        XXL,
        XXXL
    }
}
