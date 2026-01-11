package be.steby.CoreProject.bll.domains.address.exceptions;

/**
 * Exception thrown when attempting to delete an address that still has active links.
 */
public class AddressStillInUseException extends AddressDomainException {

    public AddressStillInUseException(String message) {
        super(message, 409);
    }

    public static AddressStillInUseException forAddress(String addressPublicId, long linkCount) {
        return new AddressStillInUseException(
                String.format("Cannot delete address %s: still linked to %d user(s)",
                        addressPublicId, linkCount)
        );
    }
}