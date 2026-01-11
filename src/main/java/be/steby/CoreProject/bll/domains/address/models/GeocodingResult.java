package be.steby.CoreProject.bll.domains.address.models;

/**
     * Result of a geocoding operation.
     * 
     * Contains:
     * - latitude: decimal degrees (-90 to 90)
     * - longitude: decimal degrees (-180 to 180)
     * - formattedAddress: standardized address string from provider
     * - source: provider name for traceability
     * 
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     * @param formattedAddress the formatted address string from the provider
     * @param source the geocoding provider name
     */
    public record GeocodingResult(
            Double latitude,
            Double longitude,
            String formattedAddress,
            String source
    ) {
    }