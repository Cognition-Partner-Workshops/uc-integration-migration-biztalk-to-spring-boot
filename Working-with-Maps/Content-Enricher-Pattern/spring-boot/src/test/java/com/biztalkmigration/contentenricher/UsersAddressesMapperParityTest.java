package com.biztalkmigration.contentenricher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

/**
 * Replays the sample messages shipped with the BizTalk solution and compares the Java mapper
 * with the output of the original map's XSLT (src/test/resources/expected, generated with xsltproc).
 */
class UsersAddressesMapperParityTest {

    private final UsersAddressesMapper mapper = new UsersAddressesMapper();

    @Test
    void enrichesTheTwoOrchestrationMessages() throws IOException {
        String actual = XmlSupport.serialize(
                mapper.enrich(SampleFiles.sample("InuputUsers.xml"), SampleFiles.sample("InputAddresses.xml")));

        XmlAssert.assertThat(actual).and(SampleFiles.expected("users-addresses.expected.xml"))
                .ignoreWhitespace().areIdentical();
    }

    @Test
    void enrichesTheAggregatedTransformShapeInput() throws IOException {
        String single = XmlSupport.serialize(mapper.enrichAggregated(SampleFiles.sample("inputfile.xml")));
        String multiple = XmlSupport.serialize(mapper.enrichAggregated(SampleFiles.sample("inputfilemultiple.xml")));

        XmlAssert.assertThat(single).and(SampleFiles.expected("inputfile.expected.xml"))
                .ignoreWhitespace().areIdentical();
        XmlAssert.assertThat(multiple).and(SampleFiles.expected("inputfilemultiple.expected.xml"))
                .ignoreWhitespace().areIdentical();
    }

    /**
     * TestFiles/OutputMessage.xml was hand-written upstream and contains one typo
     * ("18 avec Gerard Majax" vs the input's "18 avenue Gerard Majax"); apart from that
     * text node the Java output matches the sample byte for byte.
     */
    @Test
    void matchesUpstreamOutputMessageExceptForItsKnownTypo() throws IOException {
        String actual = XmlSupport.serialize(
                mapper.enrich(SampleFiles.sample("InuputUsers.xml"), SampleFiles.sample("InputAddresses.xml")));

        Diff diff = DiffBuilder.compare(SampleFiles.sampleText("OutputMessage.xml")).withTest(actual)
                .ignoreWhitespace().checkForIdentical().build();
        List<Difference> differences = new java.util.ArrayList<>();
        diff.getDifferences().forEach(differences::add);

        assertThat(differences).hasSize(1);
        assertThat(differences.get(0).getComparison().getControlDetails().getValue()).isEqualTo("18 avec Gerard Majax");
        assertThat(differences.get(0).getComparison().getTestDetails().getValue()).isEqualTo("18 avenue Gerard Majax");
    }

    @Test
    void userWithoutAddressesGetsAnEmptyAddressesElement() {
        var users = List.of(new User("7", "Doe", "Jane"));
        var addresses = List.of(new Address("8", "Elsewhere", "0000", "Nowhere"));

        String actual = XmlSupport.serialize(mapper.buildInformations(users, addresses));

        XmlAssert.assertThat(actual).and("""
                <ns0:Informations xmlns:ns0="http://BizTalk_Server_GenerationFichiersReleve.Test3">
                  <Users><User><UserID>7</UserID><LastName>Doe</LastName><FirstName>Jane</FirstName><Addresses/></User></Users>
                </ns0:Informations>
                """).ignoreWhitespace().areIdentical();
    }

    @Test
    void rejectsMessagesOfTheWrongSchema() throws IOException {
        var users = SampleFiles.sample("InuputUsers.xml");

        assertThatThrownBy(() -> mapper.enrich(users, users))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Addresses");
    }
}
