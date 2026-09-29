package com.biztalkmigration.contentenricher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.xmlunit.assertj3.XmlAssert;

@SpringBootTest
@AutoConfigureMockMvc
class EnrichControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enrichesUploadedUsersAndAddresses() throws Exception {
        var users = new MockMultipartFile("users", "InuputUsers.xml", MediaType.APPLICATION_XML_VALUE,
                Files.readAllBytes(SampleFiles.TEST_FILES.resolve("InuputUsers.xml")));
        var addresses = new MockMultipartFile("addresses", "InputAddresses.xml", MediaType.APPLICATION_XML_VALUE,
                Files.readAllBytes(SampleFiles.TEST_FILES.resolve("InputAddresses.xml")));

        String body = mockMvc.perform(multipart("/api/enrich").file(users).file(addresses))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn().getResponse().getContentAsString();

        XmlAssert.assertThat(body).and(SampleFiles.expected("users-addresses.expected.xml"))
                .ignoreWhitespace().areIdentical();
    }

    @Test
    void enrichesAggregatedMessageBody() throws Exception {
        String body = mockMvc.perform(post("/api/enrich/aggregated").contentType(MediaType.APPLICATION_XML)
                        .content(Files.readAllBytes(SampleFiles.TEST_FILES.resolve("inputfilemultiple.xml"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        XmlAssert.assertThat(body).and(SampleFiles.expected("inputfilemultiple.expected.xml"))
                .ignoreWhitespace().areIdentical();
    }

    @Test
    void rejectsWrongRootElement() throws Exception {
        var users = new MockMultipartFile("users", "a.xml", MediaType.APPLICATION_XML_VALUE,
                Files.readAllBytes(SampleFiles.TEST_FILES.resolve("InuputUsers.xml")));
        var notAddresses = new MockMultipartFile("addresses", "b.xml", MediaType.APPLICATION_XML_VALUE,
                Files.readAllBytes(SampleFiles.TEST_FILES.resolve("InuputUsers.xml")));

        mockMvc.perform(multipart("/api/enrich").file(users).file(notAddresses))
                .andExpect(status().isBadRequest());
    }
}
