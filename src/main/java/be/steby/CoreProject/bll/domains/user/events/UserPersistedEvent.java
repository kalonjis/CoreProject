package be.steby.CoreProject.bll.domains.user.events;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user is persisted to the database.
 *
 * This event follows the same pattern as DevicePersistedEvent,
 * containing the full user entity for efficient cache updates.
 *
 * Used by UserCacheEventListener to update the user cache
 * without requiring additional database calls.
 */
public record UserPersistedEvent(User user) {
}