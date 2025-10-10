package be.steby.CoreProject.bll.domains.admin.events.password;

import be.steby.CoreProject.dl.entities.User;

public record AdminTemporaryPasswordSentEvent (
    User target,
    User admin,
    String temporaryPassword,
    String reason){

}
