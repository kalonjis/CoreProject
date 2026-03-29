package be.steby.CoreProject.bll.domains.tag.services;

import be.steby.CoreProject.dl.entities.crm.Tag;
import be.steby.CoreProject.pl.domains.tag.models.requests.CreateTagRequest;
import be.steby.CoreProject.pl.domains.tag.models.requests.UpdateTagRequest;

import java.util.List;

public interface TagService {
    List<Tag> findAll();
    Tag create(CreateTagRequest request);
    Tag update(String publicId, UpdateTagRequest request);
    void delete(String publicId);
    void addToContact(String tagPublicId, String contactPublicId);
    void removeFromContact(String tagPublicId, String contactPublicId);
    void addToDeal(String tagPublicId, String dealPublicId);
    void removeFromDeal(String tagPublicId, String dealPublicId);
    void addToOrganisation(String tagPublicId, String organisationPublicId);
    void removeFromOrganisation(String tagPublicId, String organisationPublicId);
}
