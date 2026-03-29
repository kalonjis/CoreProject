package be.steby.CoreProject.pl.domains.tag.models.responses;

import be.steby.CoreProject.dl.entities.crm.Tag;

public record TagResponse(String publicId, String name, String color) {
    public static TagResponse fromEntity(Tag tag) {
        return new TagResponse(tag.getPublicId(), tag.getName(), tag.getColor());
    }
}
