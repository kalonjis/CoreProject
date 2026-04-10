package be.steby.CoreProject.bll.domains.crm.tag.exceptions;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(String publicId) {
        super("Tag not found: " + publicId);
    }
}
