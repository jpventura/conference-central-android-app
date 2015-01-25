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
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Announcement;
import com.google.devrel.training.conference.domain.AppEngineUser;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.domain.Session;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.form.SessionForm;
import com.google.devrel.training.conference.form.SessionForm.TypeOfSession;
import com.google.devrel.training.conference.form.SessionQueryForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Work;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(ConferenceApi.class.getName());

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    private static Profile getProfileFromUser(User user, String userId) {
        // First fetch it from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, userId)).now();
        if (profile == null) {
            // Create a new Profile if not exist.
            String email = user.getEmail();
            profile = new Profile(userId,
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
        }
        return profile;
    }

    /**
     * @param user A User object injected by the cloud endpoints.
     * @return the App Engine userId for the user.
     */
    private static String getUserId(User user) {
        String userId = user.getUserId();
        if (null == userId) {
            AppEngineUser appEngineUser = new AppEngineUser(user);
            ofy().save().entity(appEngineUser).now();
            // Begin new session for not using session cache.
            Objectify objectify = ofy().factory().begin();
            AppEngineUser savedUser = objectify.load().key(appEngineUser.getKey()).now();
            userId = savedUser.getUser().getUserId();
        }
        return userId;
    }

    /**
     * Just a wrapper for Boolean.
     * We need this wrapped Boolean because endpoints functions must return
     * an object instance, they can't return a Type class such as
     * String or Integer or Boolean
     */
    public static class WrappedBoolean {

        private final Boolean result;
        private final String reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * A wrapper class that can embrace a generic result or some kind of exception.
     *
     * Use this wrapper class for the return type of objectify transaction.
     * <pre>
     * {@code
     * // The transaction that returns Conference object.
     * TxResult<Conference> result = ofy().transact(new Work<TxResult<Conference>>() {
     *     public TxResult<Conference> run() {
     *         // Code here.
     *         // To throw 404
     *         return new TxResult<>(new NotFoundException("No such conference"));
     *         // To return a conference.
     *         Conference conference = somehow.getConference();
     *         return new TxResult<>(conference);
     *     }
     * }
     * // Actually the NotFoundException will be thrown here.
     * return result.getResult();
     * </pre>
     *
     * @param <ResultType> The type of the actual return object.
     */
    private static class TxResult<ResultType> {
        private ResultType result;

        private Throwable exception;

        private TxResult(ResultType result) {
            this.result = result;
        }

        private TxResult(Throwable exception) {
            if (exception instanceof ConflictException) {
                this.exception = exception;
            } else if (exception instanceof ForbiddenException) {
                this.exception = exception;
            } else if (exception instanceof NotFoundException) {
                this.exception = exception;
            } else {
                throw new IllegalArgumentException("Exception not supported.");
            }
        }

        private ResultType getResult()
                throws ConflictException, ForbiddenException, NotFoundException {
            if (exception instanceof ConflictException) {
                throw (ConflictException) exception;
            } else if (exception instanceof ForbiddenException) {
                throw (ForbiddenException) exception;
            } else if (exception instanceof NotFoundException) {
                throw (NotFoundException) exception;
            } else {
                return result;
            }
        }
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
        return ofy().load().key(Key.create(Profile.class, getUserId(user))).now();
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
        String userId = getUserId(user);
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
            profile = new Profile(getUserId(user), displayName, mainEmail, teeShirtSize);
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
    @ApiMethod(
            name = "createConference",
            path = "conference",
            httpMethod = HttpMethod.POST
    )
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
            throws UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required");
        }

        final String userId = getUserId(user);

        Key<Profile> profileKey = Key.create(Profile.class, userId);

        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        final long conferenceId = conferenceKey.getId();

        final Queue queue = QueueFactory.getQueue("default");

        // Start a transaction
        Conference conference = ofy().transact(new Work<Conference>() {
            @Override
            public Conference run() {
                // Fetch user's profile.
                Profile profile = getProfileFromUser(user, userId);
                Conference conference = new Conference(conferenceId, userId, conferenceForm);
                // Save Conference and Profile.
                ofy().save().entities(conference, profile).now();
                queue.add(ofy().getTransaction(),
                        TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                        .param("email", profile.getMainEmail())
                        .param("conferenceInfo", conference.toString()));
                return conference;
            }
        });

        return conference;
    }

    /**
     * Updates the existing Conference with the given conferenceId.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @param websafeConferenceKey The String representation of the Conference key.
     * @return Updated Conference object.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     * @throws ForbiddenException when the user is not the owner of the Conference.
     */
    @ApiMethod(
            name = "updateConference",
            path = "conference/{websafeConferenceKey}",
            httpMethod = HttpMethod.PUT
    )
    public Conference updateConference(final User user, final ConferenceForm conferenceForm,
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the user ID.
        final String userId = getUserId(user);

        TxResult<Conference> result = ofy().transact(new Work<TxResult<Conference>>() {
            @Override
            public TxResult<Conference> run() {
                // Get the conference key -- you can get it from websafeConferenceKey
                // Will throw ForbiddenException if the key cannot be created
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                // Get the Conference entity from the datastore
                Conference conference = ofy().load().key(conferenceKey).now();
                if (conference == null) {
                    String message = "No Conference found with the key: " + websafeConferenceKey;
                    return new TxResult<>(new NotFoundException(message));
                }

                // Get the user's Profile entity
                Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();
                if (null == profile) {
                    String message = "Profile does not exist.";
                    return new TxResult<>(new ForbiddenException(message));
                }

                // Check if user is the conference owner.
                if (!conference.getOrganizerUserId().equals(userId)) {
                    String message = "Only the owner can update the conference.";
                    return new TxResult<>(new ForbiddenException(message));
                }

                // Update the conference with the conferenceForm sent from the client.
                conference.updateWithConferenceForm(conferenceForm);
                ofy().save().entity(conference).now();
                return new TxResult<>(conference);
            }
        });

        return result.getResult();
    }

    @ApiMethod(
            name = "getAnnouncement",
            path = "announcement",
            httpMethod = HttpMethod.GET
    )
    public Announcement getAnnouncement() {
        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
        String announcementKey = Constants.MEMCACHE_ANNOUNCEMENTS_KEY;
        Object message = memcacheService.get(announcementKey);

        if (null != message) {
            return new Announcement(message.toString());
        }

        return null;
    }

    /**
     * Returns a Conference object with the given conferenceId.
     *
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return a Conference object with the given conferenceId.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "getConference",
            path = "conference/{websafeConferenceKey}",
            httpMethod = HttpMethod.GET
    )
    public Conference getConference(
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws NotFoundException {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (null == conference) {
            throw new NotFoundException("No Conference found with key: " + websafeConferenceKey);
        }
        return conference;
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

        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }

        List<String> keyStringsToAttend = profile.getConferenceKeysToAttend();

        List<Key<Conference>> keysToAttend = new ArrayList<>();
        for (String keyString : keyStringsToAttend) {
            keysToAttend.add(Key.<Conference>create(keyString));
        }
        return ofy().load().keys(keysToAttend).values();
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
        Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery();
        List<Conference> result = new ArrayList<>(0);
        List<Key<Profile>> organizersKeyList = new ArrayList<>(0);

        for (Conference conference : conferenceIterable) {
            organizersKeyList.add(Key.create(Profile.class, conference.getOrganizerUserId()));
            result.add(conference);
        }

        // To avoid separate datastore gets for each Conference, pre-fetch the Profiles.
        ofy().load().keys(organizersKeyList);
        return result;
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

        Key<Profile> userKey = Key.create(Profile.class, getUserId(user));

        return ofy().load().type(Conference.class).ancestor(userKey).order("name").list();
    }

    /**
     * Register to attend the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "registerForConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.POST
    )
    public WrappedBoolean registerForConference(final User user,
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws ConflictException, ForbiddenException, NotFoundException, UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        final String userId = getUserId(user);

        TxResult<Boolean> result = ofy().transact(new Work<TxResult<Boolean>>() {
            @Override
            public TxResult<Boolean> run() {
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                Conference conference = ofy().load().key(conferenceKey).now();

                Profile profile = getProfileFromUser(user, userId);

                if (null == conference) {
                    String message = "No Conference found with key: " + websafeConferenceKey;
                    return new TxResult<>(new NotFoundException(message));
                } else if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                    String message = "You have already registered for this conference";
                    return new TxResult<>(new ConflictException(message));
                } else if (conference.getSeatsAvailable() <= 0) {
                    String message = "There are no seats available.";
                    return new TxResult<>(new ConflictException(message));
                } else {
                    // All looks good, go ahead and book the seat

                    profile.addToConferenceKeysToAttend(websafeConferenceKey);

                    conference.bookSeats(1);

                    ofy().save().entities(profile, conference).now();

                    // We are booked!
                    return new TxResult<>(true);
                }
            }
        });

        return new WrappedBoolean(result.getResult());
    }

    /**
     * Unregister from the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key
     * to unregister from.
     * @return Boolean true when success, otherwise false.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "unregisterFromConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromConference(final User user,
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        final String userId = getUserId(user);
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
                Conference conference = ofy().load().key(conferenceKey).now();
                // 404 when there is no Conference with the given conferenceId.
                if (conference == null) {
                    return new  WrappedBoolean(false,
                            "No Conference found with key: " + websafeConferenceKey);
                }

                // Un-registering from the Conference.
                Profile profile = getProfileFromUser(user, userId);
                if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                    profile.unregisterFromConference(websafeConferenceKey);
                    conference.giveBackSeats(1);
                    ofy().save().entities(profile, conference).now();
                    return new WrappedBoolean(true);
                } else {
                    return new WrappedBoolean(false, "You are not registered for this conference");
                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else {
                throw new ForbiddenException(result.getReason());
            }
        }
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }

    /**
     * Given a conference, return all its sessions objects
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param webSafeConferenceKey The String representation of the Conference Key.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     * @return a list of Sessions of the given conference.
     */
    @ApiMethod(
            name = "getConferenceSessions",
            path = "conference/{webSafeConferenceKey}/session",
            httpMethod = HttpMethod.GET
    )
    public Collection<Session> getConferenceSessions(final User user,
            @Named("webSafeConferenceKey") final String webSafeConferenceKey)
            throws NotFoundException, UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Unauthorized.");
        }

        Key<Conference> conferenceKey = Key.create(webSafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (null == conference) {
            String message = "No Conference found with the key: " + webSafeConferenceKey;
            throw new NotFoundException(message);
        }

        return ofy().load().type(Session.class).ancestor(conferenceKey).list();
    }

    @ApiMethod(
            name = "getConferenceSessionsByType",
            path = "conference/{webSafeConferenceKey}/type_of_session/{typeOfSession}",
            httpMethod = HttpMethod.GET
    )
    public Collection<Session> getConferenceSessionsByType(final User user,
            @Named("webSafeConferenceKey") final String webSafeConferenceKey,
            @Named("typeOfSession") TypeOfSession typeOfSession)
            throws NotFoundException, UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Unauthorized.");
        }

        Key<Conference> conferenceKey = Key.create(webSafeConferenceKey);

        // If conference is null, throw NotFoundException
        Conference conference = ofy().load().key(conferenceKey).now();
        if (null == conference) {
            throw new NotFoundException("Not found");
        }

        return ofy().load().type(Session.class)
              .ancestor(Key.create(webSafeConferenceKey))
              .filter("typeOfSession =", typeOfSession).list();
    }

    @ApiMethod(
            name = "getConferenceSessionsBySpeaker",
            path = "getConferenceSessionsBySpeaker",
            httpMethod = HttpMethod.GET
    )
    public Collection<Session> getConferenceSessionsBySpeaker(final User user,
            @Named("speaker") String speaker) throws UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Unauthorized");
        }

        return ofy().load().type(Session.class).filter("speaker =", speaker).list();
    }

    /**
     * Returns a new conference session.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return A conference session.
     * @throws ForbiddenException when the requesting user is not the conference organizer.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "createSession",
            path = "conference/{webSafeConferenceKey}/session",
            httpMethod = HttpMethod.POST
    )
    public Session createSession(final User user,
        @Named("webSafeConferenceKey") final String webSafeConferenceKey,
        final SessionForm sessionForm) throws ConflictException, ForbiddenException,
        NotFoundException, UnauthorizedException {

        TxResult<Session> result = ofy().transact(new Work<TxResult<Session>>() {
            @Override
            public TxResult<Session> run() {
                // If user is not authorized, throws UnauthorizedException
                if (null == user) {
                    return new TxResult<>(new UnauthorizedException("Unauthorized"));
                }

                Profile profile = getProfileFromUser(user, getUserId(user));
                if (null == profile) {
                    return new TxResult<>(new NotFoundException("Profile doesn't exist."));
                }

                // If conference does not exist, throws NotFoundException
                Key<Conference> conferenceKey = Key.create(webSafeConferenceKey);
                Conference conference = ofy().load().key(conferenceKey).now();
                if (null == conference) {
                    return new TxResult<>(new NotFoundException("Not found"));
                }

                // If user is not the conference organizer, throws ForbiddenException
                if (!conference.getOrganizerUserId().equals(getUserId(user))) {
                    return new TxResult<>(new ForbiddenException("Forbidden"));
                }

                // Create session and add to its conference list
                Key<Session> sessionKey = factory().allocateId(conferenceKey, Session.class);
                Session session = new Session(sessionKey.getId(), webSafeConferenceKey, sessionForm);
                conference.addSessionsKeys(session.getWebSafeKey());
                ofy().save().entities(session, conference).now();

                return new TxResult<>(session);
            }
        });

        return result.getResult();
    }

    /**
     * Add session to user wishlist.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param webSafeSessionKey The String representation of the Session Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Session with the given web safe Session key.
     */
    @ApiMethod(
            name = "addSessionToWishlist",
            path = "session/{webSafeSessionKey}/wishlist",
            httpMethod = HttpMethod.POST
    )
    public WrappedBoolean addSessionToWishlist(final User user,
            @Named("webSafeSessionKey") final String webSafeSessionKey)
            throws ConflictException, ForbiddenException, NotFoundException, UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required.");
        }

        final Profile profile = getProfileFromUser(user, getUserId(user));
        if (null == profile) {
            throw new NotFoundException("Profile doesn't exist.");
        }

        TxResult<Boolean> result = ofy().transact(new Work<TxResult<Boolean>>() {
            @Override
            public TxResult<Boolean> run() {
                Key<Session> sessionKey = Key.create(webSafeSessionKey);
                Session session = ofy().load().key(sessionKey).now();

                if (null == session) {
                    String message = "No session found with key: " + webSafeSessionKey;
                    return new TxResult<>(new NotFoundException(message));
                }
                try {
                    profile.addSessionToWishlist(webSafeSessionKey);
                    ofy().save().entity(profile).now();
                    return new TxResult(true);
                } catch (IllegalArgumentException e) {
                    String message = "You have already add for this session to wishlist";
                    return new TxResult<>(new ConflictException(message));
                }
            }
        });

        return new WrappedBoolean(result.getResult());
    }

    /**
     * Remove session from user wishlist.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param webSafeSessionKey The String representation of the Session Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Session with the given web safe Session key.
     */
    @ApiMethod(
            name = "removeSessionFromWishlist",
            path = "session/{webSafeSessionKey}/wishlist",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean removeSessionFromWishlist(final User user,
            @Named("webSafeSessionKey") final String webSafeSessionKey)
            throws ConflictException, ForbiddenException, NotFoundException, UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required.");
        }

        final Profile profile = getProfileFromUser(user, getUserId(user));
        if (null == profile) {
            throw new NotFoundException("Profile doesn't exist.");
        }

        TxResult<Boolean> result = ofy().transact(new Work<TxResult<Boolean>>() {
            @Override
            public TxResult<Boolean> run() {
                Key<Session> sessionKey = Key.create(webSafeSessionKey);
                Session session = ofy().load().key(sessionKey).now();

                if (null == session) {
                    String message = "No session found with key: " + webSafeSessionKey;
                    return new TxResult<>(new NotFoundException(message));
                }
                try {
                    profile.removeSessionFromWishlist(webSafeSessionKey);
                    ofy().save().entity(profile).now();
                    return new TxResult(true);
                } catch (IllegalArgumentException e) {
                    String message = "You have not added this session";
                    return new TxResult<>(new ConflictException(message));
                }
            }
        });

        return new WrappedBoolean(result.getResult());
    }

    /**
     * Returns a collection of Session Object that the user is wish to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of sessions that the user is wish to attend.
     * @throws NotFoundException when the Profile object of the user is null.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getSessionsInWishlist",
            path = "session/wishlist",
            httpMethod = HttpMethod.GET
    )
    public Collection<Session> getSessionsInWishlist(final User user)
            throws NotFoundException, UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required.");
        }

        Profile profile = getProfileFromUser(user, getUserId(user));
        if (null == profile) {
            throw new NotFoundException("Profile doesn't exist.");
        }

        List<Key<Session>> sessionKeysToAttend = new ArrayList<>();
        for (String webSafeSessionKey : profile.getSessionsInWishlist()) {
            sessionKeysToAttend.add(Key.<Session>create(webSafeSessionKey));
        }

        return ofy().load().keys(sessionKeysToAttend).values();
    }

    /**
     * Get the conference sessions that happen a given day of date interval.
     *
     * @param startDate Start date interval.
     * @param endDate End date interval.
     *
     * @return List of Session object that happen on given interval.
     */
    @ApiMethod(
            name = "getConferenceSessionsByInterval",
            path = "getConferenceSessionsByInterval",
            httpMethod = HttpMethod.GET
    )
    public Collection<Session> getConferenceSessionsByInterval(@Named("startDate") Date startDate,
            @Named("endDate") Date endDate) throws NotFoundException{
        List<Session> sessionsStartingAfter = ofy().load().type(Session.class)
                .order("date")
                .filter("date >=", startDate)
                .list();

        List<Session> sessionsEndingAfter = ofy().load().type(Session.class)
                .order("date")
                .filter("date <=", endDate).list();

        List<Session> sessionsInterval = new ArrayList<>();
        for (Session session : sessionsStartingAfter) {
            if (sessionsEndingAfter.contains(session)) {
                sessionsInterval.add(session);
            }
        }

        return sessionsInterval;
    }

    @ApiMethod(
            name = "querySessions",
            path = "querySessions",
            httpMethod = HttpMethod.POST
    )
    public Collection<Session> querySessions(SessionQueryForm sessionQueryForm) {
        List<Session> sessionList = new ArrayList<>(0);
        for (Session session : sessionQueryForm.getQuery()) {
            sessionList.add(session);
        }

        return sessionList;
    }

    /**
     * Returns a collection of Session Object that the user is wish to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of sessions that the user is wish to attend.
     * @throws NotFoundException when the Profile object of the user is null.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "queryRelatedProblem",
            path = "queryRelatedProblem",
            httpMethod = HttpMethod.GET
    )
    public Collection<Session> queryRelatedProblem(final User user,
            @Named("excludeTypeOfSession") final TypeOfSession excludeTypeOfSession,
            @Named("beforeTime") final String startTime) throws UnauthorizedException {
        if (null == user) {
            throw new UnauthorizedException("Authorization required.");
        }

        List<Session> sessionsByTime = ofy().load().type(Session.class)
                .order("startTime")
                .filter("startTime <", startTime)
                .list();

        for (Session session : sessionsByTime) {
            if (session.getTypeOfSession().equals(excludeTypeOfSession)) {
                sessionsByTime.remove(session);
            }
        }

        return sessionsByTime;
    }

    /**
     * If a new session is added to a conference and it has a already present speaker, add a new
     * Memcache entry with the speaker and session names.
     *
     * @return Announcement with speaker and session names.
     */
    @ApiMethod(
            name = "getFeaturedSpeaker",
            path = "featured_speaker",
            httpMethod = HttpMethod.GET
    )
    public Announcement getFeaturedSpeaker() {
        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
        String featuredSpeakerKey = Constants.MEMCACHE_FEATURED_SPEAKER_KEY;
        Object message = memcacheService.get(featuredSpeakerKey);

        if (null != message) {
            return new Announcement(message.toString());
        }

        return null;
    }
}
