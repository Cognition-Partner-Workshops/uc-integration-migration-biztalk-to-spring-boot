package com.biztalkmigration.contentenricher;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

/**
 * HTTP replacement for the JoinMultipleMessagesDemoOrchestration receive ports: the two
 * FILE receive locations become multipart parts and the FILE send port becomes the response body.
 */
@RestController
@RequestMapping(path = "/api/enrich", produces = MediaType.APPLICATION_XML_VALUE)
public class EnrichController {

    private final UsersAddressesMapper mapper;

    public EnrichController(UsersAddressesMapper mapper) {
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String enrich(@RequestPart("users") MultipartFile users,
                         @RequestPart("addresses") MultipartFile addresses) throws IOException {
        try (InputStream usersIn = users.getInputStream(); InputStream addressesIn = addresses.getInputStream()) {
            Document output = mapper.enrich(XmlSupport.parse(usersIn), XmlSupport.parse(addressesIn));
            return XmlSupport.serialize(output);
        }
    }

    @PostMapping(path = "/aggregated", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public String enrichAggregated(InputStream body) {
        return XmlSupport.serialize(mapper.enrichAggregated(XmlSupport.parse(body)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException e) {
        return "<error>" + e.getMessage() + "</error>";
    }
}
