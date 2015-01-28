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

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import java.util.Date;

/**
 * Pojo representing a session form on the client side.
 */
public class SessionForm {
    public static final String TIME_PATTERN = "^((0[0-9])|(1[0-9])|(2[0-3])):([0-5][0-9])$";
    public static enum TypeOfSession {
        NOT_SPECIFIED(0),
        KEYNOTE(1),
        LECTURE(2),
        WORKSHOP(3);

        private int type;

        private TypeOfSession(int type) {
            this.type = type;
        }

        public boolean equals(final int type) {
            return type == this.type;
        }
        public boolean equals(String type) {
            return toString().equals(type);
        }
    }

    private String name;

    private String highlights;

    private String speaker;

    private String duration;

    private TypeOfSession typeOfSession;

    private Date date;

    private String startTime;

    private SessionForm() {
    }

    public SessionForm(String name, String highlights, String speaker, String duration,
            TypeOfSession typeOfSession, Date date, String startTime) {
        Preconditions.checkNotNull(name, "The name is required");

        this.name = name;

        this.highlights = highlights;

        this.speaker = speaker;

        this.duration = duration;

        this.typeOfSession = (null == typeOfSession) ? TypeOfSession.NOT_SPECIFIED : typeOfSession;

        this.date = (null == date) ? this.date : new Date(date.getTime());

        this.startTime = startTime;
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

    public TypeOfSession getTypeOfSession() {
        return typeOfSession;
    }

    public Date getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
