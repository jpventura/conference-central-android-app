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
import com.google.devrel.training.conference.domain.Session;
import com.googlecode.objectify.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for putting announcements in memcache.
 * The announcement announces speakers who present more than one session in the same conference
 */
@SuppressWarnings("serial")
public class SetSpeakerAnnouncementServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, List<String>> sessionNamesMap = new HashMap<>();

        for (Conference conference : ofy().load().type(Conference.class).list()) {
            List<Key<Session>> sessionKeys = new ArrayList<>();
            for (String webSafeSessionKey : conference.getSessionsKeys()) {
                sessionKeys.add(Key.<Session>create(webSafeSessionKey));
            }

            for (Session session : ofy().load().keys(sessionKeys).values()) {
                String speaker = session.getSpeaker();
                if (!sessionNamesMap.containsKey(speaker)) {
                    sessionNamesMap.put(speaker, new ArrayList<String>());
                }
                sessionNamesMap.get(speaker).add(session.getName());
            }
        }

        for (String speaker : sessionNamesMap.keySet()) {
            List<String> names = sessionNamesMap.get(speaker);
            if (names.size() > 1) {
                // Build a String that announces conference sessions with the same speaker
                StringBuilder announcementStringBuilder = new StringBuilder(
                        speaker + " is presenting the following sessions: ");
                Joiner joiner = Joiner.on(", ").skipNulls();
                announcementStringBuilder.append(joiner.join(names));

                MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();

                String announcementKey = Constants.MEMCACHE_FEATURED_SPEAKER_KEY;
                String announcementText = announcementStringBuilder.toString();

                memcacheService.put(announcementKey, announcementText);
            }
        }

        // Set the response status to 204 which means
        // the request was successful but there's no data to send back
        // Browser stays on the same page if the get came from the browser
        response.setStatus(204);
    }
}