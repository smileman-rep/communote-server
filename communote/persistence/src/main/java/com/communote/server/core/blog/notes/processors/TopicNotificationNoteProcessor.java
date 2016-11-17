package com.communote.server.core.blog.notes.processors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.communote.common.converter.CollectionConverter;
import com.communote.common.converter.IdentityConverter;
import com.communote.common.util.PageableList;
import com.communote.server.api.core.blog.BlogRightsManagement;
import com.communote.server.api.core.config.type.ClientProperty;
import com.communote.server.api.core.note.NoteStoringTO;
import com.communote.server.api.core.note.processor.NoteStoringPostProcessorContext;
import com.communote.server.api.core.property.PropertyManagement;
import com.communote.server.api.core.user.UserData;
import com.communote.server.core.blog.TooManyMentionedUsersNoteManagementException;
import com.communote.server.core.query.QueryManagement;
import com.communote.server.core.user.UserManagement;
import com.communote.server.core.vo.query.TaggingCoreItemUTPExtension;
import com.communote.server.core.vo.query.blog.TopicAccessLevel;
import com.communote.server.core.vo.query.user.UserTaggingCoreQuery;
import com.communote.server.core.vo.query.user.UserTaggingCoreQueryParameters;
import com.communote.server.model.blog.BlogRole;
import com.communote.server.model.blog.UserToBlogRoleMapping;
import com.communote.server.model.note.Note;
import com.communote.server.model.note.NoteStatus;
import com.communote.server.model.user.User;
import com.communote.server.model.user.UserRole;
import com.communote.server.model.user.UserStatus;

/**
 * Processor which handles topic mention flags.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class TopicNotificationNoteProcessor extends NotificationNoteProcessor {

    private static final String PROPERTY_KEY_TOPIC_MENTIONS = PropertyManagement.KEY_GROUP
            + ".notification.topicMentionsToSkip";
    private static final String TOPIC_MENTION_READERS = "reader";
    private static final String TOPIC_MENTION_AUTHORS = "author";
    private static final String TOPIC_MENTION_MANAGERS = "manager";
    private final BlogRightsManagement topicRightsManagement;
    private final UserManagement userManagement;
    private final QueryManagement queryManagement;

    /**
     * Constructor.
     *
     * @param topicRightsManagement
     *            Used to get users to notify.
     * @param userManagement
     *            Used to retrieve concrete users.
     * @param queryManagement
     *            Needed to execute queries.
     */
    public TopicNotificationNoteProcessor(BlogRightsManagement topicRightsManagement,
            UserManagement userManagement, QueryManagement queryManagement) {
        this.topicRightsManagement = topicRightsManagement;
        this.userManagement = userManagement;
        this.queryManagement = queryManagement;
    }

    private String encodeMentionPropertyValue(boolean includeReaders, boolean includeAuthors,
            boolean includeManagers) {
        StringBuilder value = new StringBuilder();
        String separator = "";
        if (!includeReaders) {
            value.append(TOPIC_MENTION_READERS);
            separator = ",";
        }
        if (!includeAuthors) {
            value.append(separator);
            value.append(TOPIC_MENTION_AUTHORS);
            separator = ",";
        }

        if (!includeManagers) {
            value.append(separator);
            value.append(TOPIC_MENTION_MANAGERS);
        }
        return value.toString();
    }

    /**
     * @return 0
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<User> getUsersToNotify(Note note, NoteStoringPostProcessorContext context,
            Set<Long> userIdsToSkip) {
        if (note.isDirect()) {
            return null;
        }
        boolean includeAuthors;
        boolean includeManagers;
        boolean includeReaders;
        String value = context.getProperties().get(PROPERTY_KEY_TOPIC_MENTIONS);
        if (value == null) {
            includeAuthors = note.isMentionTopicAuthors();
            includeManagers = note.isMentionTopicManagers();
            includeReaders = note.isMentionTopicReaders();
        } else {
            includeAuthors = !value.contains(TOPIC_MENTION_AUTHORS);
            includeManagers = !value.contains(TOPIC_MENTION_MANAGERS);
            includeReaders = !value.contains(TOPIC_MENTION_READERS);
        }
        Collection<User> usersToNotify = internalGetUsersToNotify(note, includeReaders,
                includeAuthors, includeManagers);
        return usersToNotify;
    }

    /**
     * Method to get all users to be notified.
     *
     * @param note
     *            The note to check.
     * @return Collection of users to be notified.
     */
    private Collection<User> internalGetUsersToNotify(Note note, boolean includeReaders,
            boolean includeAuthors, boolean includeManagers) {
        Collection<User> usersToNotify = new HashSet<User>();
        Set<BlogRole> roles = new HashSet<BlogRole>();
        if (includeManagers) {
            roles.add(BlogRole.MANAGER);
        }
        if (includeReaders) {
            if (note.getBlog().isAllCanRead()) {
                return userManagement.findUsersByRole(UserRole.ROLE_KENMEI_USER, UserStatus.ACTIVE);
            }
            roles.add(BlogRole.VIEWER);
            roles.add(BlogRole.MEMBER);
            roles.add(BlogRole.MANAGER);
        }
        if (roles.size() > 0) {
            Collection<User> mappedUsers = topicRightsManagement.getMappedUsers(note.getBlog()
                    .getId(), new CollectionConverter<UserToBlogRoleMapping, User>() {
                @Override
                public User convert(UserToBlogRoleMapping source) {
                    return userManagement.getUserById(source.getUserId(),
                            new IdentityConverter<User>());
                }
            }, roles.toArray(new BlogRole[roles.size()]));
            usersToNotify.addAll(mappedUsers);
        }
        if (includeAuthors) {
            UserTaggingCoreQuery query = new UserTaggingCoreQuery();
            UserTaggingCoreQueryParameters parameters = new UserTaggingCoreQueryParameters(query);
            parameters.setLimitResultSet(false);
            parameters.setExcludeNoteStatus(new NoteStatus[] { NoteStatus.AUTOSAVED });
            parameters.setTypeSpecificExtension(new TaggingCoreItemUTPExtension());
            parameters.getTypeSpecificExtension().setBlogId(note.getBlog().getId());
            parameters.getTypeSpecificExtension().setTopicAccessLevel(TopicAccessLevel.READ);
            parameters.getTypeSpecificExtension().setUserId(note.getUser().getId());
            parameters.setIncludeStatusFilter(new UserStatus[] { UserStatus.ACTIVE });
            PageableList<UserData> authors = queryManagement.query(query, parameters);
            for (UserData author : authors) {
                if (topicRightsManagement.userHasReadAccess(note.getBlog().getId(), author.getId(),
                        false)) {
                    User userToNotify = userManagement.getUserById(author.getId(),
                            new IdentityConverter<User>());
                    if (userToNotify != null) {
                        usersToNotify.add(userToNotify);
                    }
                }
            }
        }
        return usersToNotify;
    }

    @Override
    protected boolean sendNotifications(Note note, NoteStoringTO orginalNoteStoringTO,
            Map<String, String> properties, NoteNotificationDetails resendDetails) {
        if (!note.isDirect()) {
            boolean includeAuthors;
            boolean includeManagers;
            boolean includeReaders;
            int usersToSkip;
            if (resendDetails != null) {
                includeAuthors = note.isMentionTopicAuthors()
                        && !resendDetails.isMentionTopicAuthors();
                includeManagers = note.isMentionTopicManagers()
                        && !resendDetails.isMentionTopicManagers();
                includeReaders = note.isMentionTopicReaders()
                        && !resendDetails.isMentionTopicReaders();
                usersToSkip = resendDetails.getMentionedUserIds().size();
            } else {
                includeAuthors = note.isMentionTopicAuthors();
                includeManagers = note.isMentionTopicManagers();
                includeReaders = note.isMentionTopicReaders();
                usersToSkip = 0;
            }
            if (includeAuthors || includeManagers || includeReaders) {
                int maxUsers = ClientProperty.MAX_NUMBER_OF_MENTIONED_USERS
                        .getValue(ClientProperty.DEFAULT_MAX_NUMBER_OF_MENTIONED_USERS);
                // Shortcut for readers and allCanRead via active user count
                if (note.isMentionTopicReaders() && note.getBlog().isAllCanRead()
                        && maxUsers < userManagement.getActiveUserCount() - usersToSkip) {
                    throw new TooManyMentionedUsersNoteManagementException();
                }
                // TODO how to get information about users to skip at check time?
                if (maxUsers > 0
                        && maxUsers < internalGetUsersToNotify(note, includeReaders,
                                includeAuthors, includeManagers).size()) {
                    throw new TooManyMentionedUsersNoteManagementException();
                }
                properties
                .put(PROPERTY_KEY_TOPIC_MENTIONS,
                        encodeMentionPropertyValue(includeReaders, includeAuthors,
                                includeManagers));
                return true;
            }
        }
        return false;
    }
}
