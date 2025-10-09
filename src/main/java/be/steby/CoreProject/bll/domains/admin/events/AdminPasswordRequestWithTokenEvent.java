package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.dl.entities.User;

public record AdminPasswordRequestWithTokenEvent(
        User user,
        String tokenPublicId
) {
}
