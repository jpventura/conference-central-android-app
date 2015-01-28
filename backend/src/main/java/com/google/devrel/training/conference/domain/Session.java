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
package com.google.devrel.training.conference.domain;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.base.Preconditions;
import com.google.devrel.training.conference.form.SessionForm;
import com.google.devrel.training.conference.form.SessionForm.TypeOfSession;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.regex.Pattern;

import static com.google.devrel.training.conference.service.OfyService.ofy;

/**
 * Session class stores conference session information.
 */
@Entity
@Cache
public class Session {
    @Id
    private Long id;

    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Conference> conferenceKey;

    @Index
    private String name;

    private String highlights;

    @Index
    private String speaker;

    private String duration;

    @Index
    private TypeOfSession typeOfSession;

    private Date date;

    @Index
    private String startTime;

    private Session() {
    }

    public Session(final long id, String webSafeConferenceKey, final SessionForm sessionForm) {
        this.id = id;
        this.conferenceKey = Key.create(webSafeConferenceKey);
        updateWithSessionForm(sessionForm);
    }

    public Session(final long id, String userId, final long conferenceId, final SessionForm sessionForm) {
        this.id = id;
        Key<Profile> profileKey = Key.create(Profile.class, userId);
        this.conferenceKey = Key.create(profileKey, Conference.class, conferenceId);
        updateWithSessionForm(sessionForm);
    }

    public long getId() {
        return id;
    }

    /**
     * Get the conference key to which this session belongs.
     *
     * @return The ancestor conference key
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Conference> getConferenceKey() {
        return conferenceKey;
    }

    /**
     * Get a web safe key of the session.
     *
     * @return A String web safe key of the session.
     */
    public String getWebSafeKey() {
        return Key.create(conferenceKey, Session.class, id).getString();
    }

    public String getName() {
        return name;
    }

    public String getHighlights() {
        return highlights;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getDuration() {
        return duration;
    }

    public TypeOfSession getTypeOfSession () {
        return typeOfSession;
    }

    public Date getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    /**
     * Updates the Session with SessionForm.
     * This method is used upon object creation as well as updating existing Sessions.
     *
     * @param sessionForm contains form data sent from the client.
     */
    public void updateWithSessionForm(SessionForm sessionForm) {
        name = sessionForm.getName();

        highlights = sessionForm.getHighlights();

        speaker = sessionForm.getSpeaker();

        if (isTimeFormat(sessionForm.getDuration()) == false) {
            String message = "Invalid duration time format: " + sessionForm.getDuration();
            throw new IllegalArgumentException(message);
        } else {
            duration = sessionForm.getDuration();
        }

        typeOfSession = sessionForm.getTypeOfSession();

        date = sessionForm.getDate();

        if (isTimeFormat(sessionForm.getStartTime()) == false) {
            String message = "Invalid start time format: " + sessionForm.getStartTime();
            throw new IllegalArgumentException(message);
        } else {
            startTime = sessionForm.getStartTime();
        }
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    /**
     * Check if String time has a valid 24 hours format.
     *
     * @param time String representing time
     * @return true if string has correct format, false otherwise
     */
    private boolean isTimeFormat(final String time) {
        String TIME_PATTERN = "^(0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])$";
        return Pattern.matches(TIME_PATTERN, time);
    }
}
