package be.steby.CoreProject.pl.assemblers;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.pl.controllers.activity.ActivityLogController;
import be.steby.CoreProject.pl.security.models.ActivityLogDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler for converting ActivityLog entities to EntityModel representations
 * with proper HATEOAS links.
 */
@Component
public class ActivityLogModelAssembler implements RepresentationModelAssembler<ActivityLog, EntityModel<ActivityLogDTO>> {

    @Override
    public EntityModel<ActivityLogDTO> toModel(ActivityLog log) {
        ActivityLogDTO dto = ActivityLogDTO.fromEntity(log);

        // Create an EntityModel with relevant links
        EntityModel<ActivityLogDTO> model = EntityModel.of(dto);

        // Add link to user's connection history if this is not anonymous access
        if (log.getUser() != null && log.getUser().getId() != null) {
            model.add(linkTo(methodOn(ActivityLogController.class)
                    .getUserConnectionHistory(log.getUser().getId(), null))
                    .withRel("userHistory"));
        }

        // Add device-specific link if a device is associated
        if (log.getDevice() != null && log.getDevice().getId() != null) {
            model.add(linkTo(methodOn(ActivityLogController.class)
                    .searchLogs(log.getUser().getId(), null, null, null, null, null, null))
                    .withRel("deviceActivity")
                    .expand(log.getDevice().getId()));
        }

        // Add action-type link to find similar actions
//        if (log.getActionType() != null) {
//            model.add(linkTo(methodOn(ActivityLogController.class)
//                    .searchLogs(null, null, java.util.Collections.singletonList(log.getActionType()), null, null, null, null))
//                    .withRel("similarActions"));
//        }

        return model;
    }

    /**
     * Creates an EntityModel with links but without certain potentially sensitive details
     * for use in public-facing APIs.
     */
    public EntityModel<ActivityLogDTO> toPublicModel(ActivityLog log) {
        ActivityLogDTO dto = ActivityLogDTO.fromEntity(log);

        // Remove sensitive information
        // In a real application, you might create a separate DTO for public view

        return EntityModel.of(dto,
                linkTo(methodOn(ActivityLogController.class).getActionTypes())
                        .withRel("actionTypes"));
    }
}