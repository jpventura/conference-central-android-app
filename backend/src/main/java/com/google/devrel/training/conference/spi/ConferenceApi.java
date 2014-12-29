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

package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.factory;
import static com.google.devrel.training.conference.service.OfyService.ofy;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

import java.util.Collection;
import java.util.List;

/**
 * Defines conference APIs.
 */
@Api(
        name = "conference",
        version = "v1",
        scopes = {
                Constants.EMAIL_SCOPE
        },
        clientIds = {
                Constants.ANDROID_CLIENT_ID,
                Constants.API_EXPLORER_CLIENT_ID,
                Constants.WEB_CLIENT_ID
        },
        audiences = {
                Constants.ANDROID_AUDIENCE
        },
        description = "Conference Central API for creating and querying conferences, and for " +
                "creating and getting user Profiles",
        namespace = @ApiNamespace(
                ownerDomain = Constants.OWNER_DOMAIN,
                ownerName = Constants.OWNER_DOMAIN,
                packagePath = ""
        )
)
public class ConferenceApi {
    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
     *
     * @param user A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getProfile",
            path = "profile",
            httpMethod = HttpMethod.GET
    )
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        String userId = user.getUserId();
        Key key = Key.create(Profile.class, userId);
        Profile profile = (Profile) ofy().load().key(key).now();
        return profile;
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user A User object injected by the cloud endpoints.
     * @param profileForm A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "saveProfile",
            path = "profile",
            httpMethod = HttpMethod.POST
    )
    public Profile saveProfile(final User user, final ProfileForm profileForm)
            throws UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required");
        }

        // Set the teeShirtSize to the value sent by the ProfileForm, if sent
        // otherwise leave it as the default value
        TeeShirtSize teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
        if (profileForm.getTeeShirtSize() != null) {
            teeShirtSize = profileForm.getTeeShirtSize();
        }

        // Set the displayName to the value sent by the ProfileForm, if sent
        // otherwise set it to null
        String displayName = profileForm.getDisplayName();

        // Get the userId and mainEmail
        String userId = user.getUserId();
        String mainEmail = user.getEmail();

        // Get the Profile from the datastore if it exists
        // otherwise create a new one
        Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

        if (null == profile) {
            // Populate the displayName and teeShirtSize with default values
            // if not sent in request
            displayName = (null == displayName) ? extractDefaultDisplayNameFromEmail(mainEmail) : displayName;
            teeShirtSize = (null == teeShirtSize) ? TeeShirtSize.NOT_SPECIFIED : teeShirtSize;

            // Create a new Profile entity from the
            // userId, displayName, mainEmail and teeShirtSize
            profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
        } else {
            // The Profile entity already exists
            // Update the Profile entity
            profile.update(displayName, teeShirtSize);
        }

        // Save the entity in the database
        ofy().save().entity(profile).now();

        // Return the profile
        return profile;
    }

    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
            throws UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required");
        }

        String userId = user.getUserId();

        Key<Profile> profileKey = Key.create(Profile.class, userId);

        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        final long conferenceId = conferenceKey.getId();

        final Profile profile = getProfileFromUser(user);

        Conference conference = new Conference(conferenceId, userId, conferenceForm);

        ofy().save().entities(conference, profile).now();

        return conference;
    }

    /**
     * Queries against the datastore with the given filters and returns the result.
     *
     * Normally this kind of method is supposed to get invoked by a GET HTTP method,
     * but we do it with POST, in order to receive conferenceQueryForm Object via the POST body.
     *
     * @param conferenceQueryForm A form object representing the query.
     * @return A List of Conferences that match the query.
     */
    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {
        Query<Conference> query = ofy().load().type(Conference.class).order("name");

        return query.list();
    }

    public List<Conference> filterPlayground() {
        Query<Conference> query = ofy().load().type(Conference.class).order("name");
        query = query.filter("city =", "London");
        query = query.filter("topics =", "Medical Innovations");

        return query.list();
    }

    /**
     * Returns a list of Conferences that the user created.
     * In order to receive the websafeConferenceKey via the JSON params, uses a POST method.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @return a list of Conferences that the user created.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getConferencesCreated",
            path = "getConferencesCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user) throws UnauthorizedException {
        // If not signed in, throw a 401 error.
        if (null == user) {
            throw new UnauthorizedException("Authorization required");
        }

        Key<Profile> userKey = Key.create(Profile.class, user.getUserId());

        return ofy().load().type(Conference.class).ancestor(userKey).order("name").list();
    }

    /**
     * Returns a collection of Conference Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Conferences that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getConferencesToAttend",
            path = "getConferencesToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Conference> getConferencesToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO
        // Get the Profile entity for the user
        Profile profile = null;

        // TODO
        // Get the value of the Profile's conferenceKeysToAttend property
        List<String> keyStringsToAttend = null;

        // TODO
        // Iterate over keyStringsToAttend
        // and return a Collection of the
        // Conference entities that the user has registered to attend
        return null;
    }
}
