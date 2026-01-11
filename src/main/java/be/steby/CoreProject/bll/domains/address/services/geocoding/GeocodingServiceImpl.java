package be.steby.CoreProject.bll.domains.address.services.geocoding;


import be.steby.CoreProject.bll.domains.address.models.GeocodingResult;
import be.steby.CoreProject.bll.domains.address.services.AddressService;
import be.steby.CoreProject.bll.domains.address.services.geocoding.providers.GeocodingProvider;
import be.steby.CoreProject.dl.entities.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of GeocodingService.
 * 
 * Orchestrates the geocoding workflow:
 * 1. Retrieves address from database
 * 2. Calls external geocoding provider
 * 3. Updates address with coordinates
 * 4. Marks address as validated
 * 
 * This service is called asynchronously by AddressGeocodingListener
 * to avoid blocking address creation operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingServiceImpl implements GeocodingService {

    private final AddressService addressService;
    private final GeocodingProvider geocodingProvider;

    @Override
    @Transactional
    public void geocodeAddress(Long addressId) {
        log.debug("Starting geocoding process for address ID: {}", addressId);

        // Validate input
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null");
        }

        // Fetch address from database
        Address address = addressService.getById(addressId);

        // Skip if already geocoded
        if (address.getLatitude() != null && address.getLongitude() != null) {
            log.debug("Address ID {} already has coordinates, skipping geocoding", addressId);
            return;
        }

        // Call geocoding provider
        log.debug("Calling geocoding provider: {}", geocodingProvider.getProviderName());
        Optional<GeocodingResult> result = geocodingProvider.geocode(address);

        // Process result
        if (result.isPresent()) {
            GeocodingResult geocodingResult = result.get();
            
            log.info("Geocoding successful for address ID {}: lat={}, lon={}",
                    addressId, geocodingResult.latitude(), geocodingResult.longitude());

            // Update address with coordinates
            addressService.setCoordinates(
                    address,
                    geocodingResult.latitude(),
                    geocodingResult.longitude()
            );

            // Mark as validated
            addressService.markAsValidated(address, geocodingResult.source());

            // Set formatted address
            addressService.setFormattedAddress(address, geocodingResult.formattedAddress());

            log.debug("Address ID {} updated successfully with geocoding data", addressId);

        } else {
            log.warn("Geocoding returned no results for address ID: {}", addressId);
            // Address remains valid but without coordinates
        }
    }
}