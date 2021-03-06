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

package com.google.devrel.training.conference.servlet;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Joiner;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for putting announcements in memcache.
 * The announcement announces conferences that are nearly sold out
 * (defined as having 1 - 5 seats left)
 */
@SuppressWarnings("serial")
public class SetAnnouncementServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Query for conferences with less than 5 seats left
        Iterable<Conference> iterable = ofy().load().type(Conference.class)
                .filter("seatsAvailable <", 5)
                .filter("seatsAvailable >", 0);

        List<String> conferenceNames = new ArrayList<>(0);
        for (Conference conference : iterable) {
            conferenceNames.add(conference.getName());
        }

        if (conferenceNames.size() > 0) {

            // Build a String that announces the nearly sold-out conferences
            StringBuilder announcementStringBuilder = new StringBuilder(
                    "Last chance to attend! The following conferences are nearly sold out: ");
            Joiner joiner = Joiner.on(", ").skipNulls();
            announcementStringBuilder.append(joiner.join(conferenceNames));

            MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();

            String announcementKey = Constants.MEMCACHE_ANNOUNCEMENTS_KEY;
            String announcementText = announcementStringBuilder.toString();

            memcacheService.put(announcementKey, announcementText);
        }

        // Set the response status to 204 which means
        // the request was successful but there's no data to send back
        // Browser stays on the same page if the get came from the browser
        response.setStatus(204);
    }
}
