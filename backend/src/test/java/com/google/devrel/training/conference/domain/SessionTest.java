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

import static com.google.devrel.training.conference.service.OfyService.ofy;
import static org.junit.Assert.*;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.SessionForm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionTest {
    private static final long SESSION_ID = 123456L;

    private static final long CONFERENCE_ID = 123456L;

    private static final String LECTURE_NAME = "GCP Lecture";

    private static final String LECTURE_HIGHLIGHTS = "Lecture about Google Cloud Platform";

    private Date startTime;

    private Date endTime;

    private SessionForm sessionForm;

    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()
                    .setDefaultHighRepJobPolicyUnappliedJobPercentage(100));

    @Before
    public void setUp() throws Exception {
        helper.setUp();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        startTime = dateFormat.parse("2015-06-01 13:00");
        endTime = dateFormat.parse("2015-06-01 14:00");
    }

    @After
    public void tearDown() throws Exception {
        helper.tearDown();
    }

//    @Test(expected = NullPointerException.class)
//    public void testNullName() throws Exception {
//        SessionForm nullSessionForm = new SessionForm(null, LECTURE_HIGHLIGHTS, "ME",
//                Session.TypeOfSession.LECTURE, startTime, endTime);
//    }
}
