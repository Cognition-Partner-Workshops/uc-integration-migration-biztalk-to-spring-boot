package com.biztalkmigration.contentenricher;

import static com.biztalkmigration.contentenricher.XmlSupport.appendElement;
import static com.biztalkmigration.contentenricher.XmlSupport.childElements;
import static com.biztalkmigration.contentenricher.XmlSupport.childText;
import static com.biztalkmigration.contentenricher.XmlSupport.requireChild;
import static com.biztalkmigration.contentenricher.XmlSupport.requireRoot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Java port of the BizTalk map {@code 3-UsingXSLTTemplate/TransformUserAndAddressToOutput.btm}:
 * every {@code User} is copied to the output and enriched with the {@code Address} records
 * whose {@code UserID} matches, in source document order.
 */
@Service
public class UsersAddressesMapper {

    /**
     * Enriches the two orchestration messages ({@code ns0:Users} and {@code ns0:Addresses})
     * into one {@code ns0:Informations} message.
     */
    public Document enrich(Document usersMessage, Document addressesMessage) {
        Element usersRoot = requireRoot(usersMessage, Namespaces.USERS, "Users");
        Element addressesRoot = requireRoot(addressesMessage, Namespaces.ADDRESSES, "Addresses");
        return buildInformations(readUsers(usersRoot), readAddresses(addressesRoot));
    }

    /**
     * Accepts the aggregated form BizTalk hands to a multi-input transform shape
     * ({@code aggschema Root/InputMessagePart_0|1}), i.e. the sample {@code inputfile*.xml} messages.
     */
    public Document enrichAggregated(Document aggregatedMessage) {
        Element root = requireRoot(aggregatedMessage, Namespaces.AGGREGATE, "Root");
        Element usersRoot = requireChild(requireChild(root, "InputMessagePart_0"), "Users");
        Element addressesRoot = requireChild(requireChild(root, "InputMessagePart_1"), "Addresses");
        return buildInformations(readUsers(usersRoot), readAddresses(addressesRoot));
    }

    List<User> readUsers(Element usersRoot) {
        List<User> users = new ArrayList<>();
        for (Element user : childElements(usersRoot, "User")) {
            users.add(new User(childText(user, "UserID"), childText(user, "LastName"), childText(user, "FirstName")));
        }
        return users;
    }

    List<Address> readAddresses(Element addressesRoot) {
        List<Address> addresses = new ArrayList<>();
        for (Element address : childElements(addressesRoot, "Address")) {
            addresses.add(new Address(childText(address, "UserID"), childText(address, "AddressLine1"),
                    childText(address, "PostCode"), childText(address, "Town")));
        }
        return addresses;
    }

    Document buildInformations(List<User> users, List<Address> addresses) {
        Map<String, List<Address>> addressesByUserId = new LinkedHashMap<>();
        for (Address address : addresses) {
            addressesByUserId.computeIfAbsent(address.userId(), id -> new ArrayList<>()).add(address);
        }

        Document output = XmlSupport.newDocument();
        Element informations = output.createElementNS(Namespaces.INFORMATIONS, "ns0:Informations");
        output.appendChild(informations);
        Element usersElement = output.createElement("Users");
        informations.appendChild(usersElement);

        for (User user : users) {
            Element userElement = output.createElement("User");
            usersElement.appendChild(userElement);
            appendElement(userElement, "UserID", user.userId());
            appendElement(userElement, "LastName", user.lastName());
            appendElement(userElement, "FirstName", user.firstName());

            Element addressesElement = output.createElement("Addresses");
            userElement.appendChild(addressesElement);
            for (Address address : addressesByUserId.getOrDefault(user.userId(), List.of())) {
                Element addressElement = output.createElement("Address");
                addressesElement.appendChild(addressElement);
                appendElement(addressElement, "UserID", address.userId());
                appendElement(addressElement, "AddressLine1", address.addressLine1());
                appendElement(addressElement, "PostCode", address.postCode());
                appendElement(addressElement, "Town", address.town());
            }
        }
        return output;
    }
}
