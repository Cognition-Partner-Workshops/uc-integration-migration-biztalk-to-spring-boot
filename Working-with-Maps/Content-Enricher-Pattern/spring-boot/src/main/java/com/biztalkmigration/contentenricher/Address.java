package com.biztalkmigration.contentenricher;

/** One {@code Address} record of InputAddresses.xsd. */
public record Address(String userId, String addressLine1, String postCode, String town) {
}
