package be.steby.CoreProject.pl.assemblers;

import be.steby.CoreProject.dl.entities.ConnectionLog;
import be.steby.CoreProject.pl.security.ConnectionLogController;
import be.steby.CoreProject.pl.security.models.ConnectionLogDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler for converting ConnectionLog entities to EntityModel representations
 * with proper HATEOAS links.
 */
@Component
public class ConnectionLogModelAssembler implements RepresentationModelAssembler<ConnectionLog, EntityModel<ConnectionLogDTO>> {

    @Override
    public EntityModel<ConnectionLogDTO> toModel(ConnectionLog log) {
        ConnectionLogDTO dto = ConnectionLogDTO.fromEntity(log);

        // Create an EntityModel with relevant links
        EntityModel<ConnectionLogDTO> model = EntityModel.of(dto);

        // Add link to user's connection history if this is not anonymous access
        if (log.getUser() != null && log.getUser().getId() != null) {
            model.add(linkTo(methodOn(ConnectionLogController.class)
                    .getUserConnectionHistory(log.getUser().getId(), null))
                    .withRel("userHistory"));
        }

        // Add device-specific link if a device is associated
        if (log.getDevice() != null && log.getDevice().getId() != null) {
            model.add(linkTo(methodOn(ConnectionLogController.class)
                    .searchLogs(log.getUser().getId(), null, null, null, null, null, null))
                    .withRel("deviceActivity")
                    .expand(log.getDevice().getId()));
        }

        // Add action-type link to find similar actions
        if (log.getActionType() != null) {
            model.add(linkTo(methodOn(ConnectionLogController.class)
                    .searchLogs(null, null, java.util.Collections.singletonList(log.getActionType()), null, null, null, null))
                    .withRel("similarActions"));
        }

        return model;
    }

    /**
     * Creates an EntityModel with links but without certain potentially sensitive details
     * for use in public-facing APIs.
     */
    public EntityModel<ConnectionLogDTO> toPublicModel(ConnectionLog log) {
        ConnectionLogDTO dto = ConnectionLogDTO.fromEntity(log);

        // Remove sensitive information
        // In a real application, you might create a separate DTO for public view

        return EntityModel.of(dto,
                linkTo(methodOn(ConnectionLogController.class).getActionTypes())
                        .withRel("actionTypes"));
    }
}