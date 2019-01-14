package uk.gov.hmcts.reform.sscs.services.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.models.deserialize.gaps2.AppealCase;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.services.date.DateHelper;
import uk.gov.hmcts.reform.sscs.services.refdata.ReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class TransformAppealCaseToCaseDataTest {

    private static final String expectedRegionName = "region-name";

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private CaseDataEventBuilder caseDataEventBuilder;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    private TransformAppealCaseToCaseData transformAppealCaseToCaseData;


    private final RegionalProcessingCenter expectedRegionalProcessingCentre = RegionalProcessingCenter.builder()
        .name(expectedRegionName).build();

    @Before
    public void setUp() {
        final CaseDataBuilder caseDataBuilder =
            new CaseDataBuilder(referenceDataService, caseDataEventBuilder, regionalProcessingCenterService);

        transformAppealCaseToCaseData =
            new TransformAppealCaseToCaseData(caseDataBuilder);
        ReflectionTestUtils.setField(transformAppealCaseToCaseData, "lookupRpcByVenueId", true);

        when(regionalProcessingCenterService.getByVenueId("68")).thenReturn(expectedRegionalProcessingCentre);
        when(referenceDataService.getTbtCode("2")).thenReturn("O");
    }

    @Test
    public void givenACaseData_shouldBeTransformToCaseDataWithSubscriptionsAndAppealNumber() throws Exception {

        AppealCase appealCase = getAppealCase("AppealCase.json");

        SscsCaseData caseData = transformAppealCaseToCaseData.transform(appealCase);

        String appealNumber = caseData.getSubscriptions().getAppellantSubscription().getTya();
        assertEquals("appealNumber length is not 10 digits", 10, appealNumber.length());
        assertEquals("Appeal references are mapped (SC Reference)", "SC068/17/00013", caseData.getCaseReference());
        assertEquals("Appeal references are mapped (CCD ID)", "1111222233334444", caseData.getCcdCaseId());
        assertThat(caseData.getRegionalProcessingCenter(), is(expectedRegionalProcessingCentre));
        assertThat(caseData.getRegion(), is(expectedRegionName));
        assertThat(caseData.getAppeal().getHearingType(), is("oral"));
        assertThat(caseData.getGeneratedNino(), is(caseData.getAppeal().getAppellant().getIdentity().getNino()));
        assertThat(caseData.getGeneratedDob(), is(caseData.getAppeal().getAppellant().getIdentity().getDob()));
        assertThat(caseData.getGeneratedEmail(), is(caseData.getAppeal().getAppellant().getContact().getEmail()));
        assertThat(caseData.getGeneratedMobile(), is(caseData.getAppeal().getAppellant().getContact().getMobile()));
        assertThat(caseData.getGeneratedSurname(), is(caseData.getAppeal().getAppellant().getName().getLastName()));

        String dob = DateHelper.getValidDateOrTime(appealCase.getParties().get(0).getDob(), true);

        assertThat(caseData.getGeneratedDob(), is(dob));
    }

    @Test
    public void givenACaseDataWithRepresentative_shouldTransformToCaseDataWithRepresentativeSubscription()
        throws Exception {
        final AppealCase appealCase = getAppealCase("AppealCaseWithRepresentative.json");
        final SscsCaseData caseData = transformAppealCaseToCaseData.transform(appealCase);
        assertEquals("tya field should have a length of 10",
            10, caseData.getSubscriptions().getRepresentativeSubscription().getTya().length());
        final Subscription expectedRepresentativeSubscription = Subscription.builder()
            .email("john@example.com")
            .subscribeEmail("No")
            .mobile("07123456789")
            .reason("")
            .subscribeSms("No")
            .tya(caseData.getSubscriptions().getRepresentativeSubscription().getTya())
            .build();
        final Contact expectedContact = Contact.builder().email("john@example.com").mobile("07123456789").build();
        assertEquals("Representative Subscriptions is not as expected",
            expectedRepresentativeSubscription, caseData.getSubscriptions().getRepresentativeSubscription());
        assertNotNull("Representative must not be null", caseData.getAppeal().getRep());
        assertEquals("Contact should be equal",
            expectedContact, caseData.getAppeal().getRep().getContact());
    }

    private AppealCase getAppealCase(String filename) throws Exception {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder
            .json()
            .indentOutput(true)
            .build();

        String appealCaseJson =
            IOUtils.toString(
                TransformAppealCaseToCaseDataTest.class
                    .getClassLoader()
                    .getResourceAsStream(filename),
                StandardCharsets.UTF_8.name()
            );

        return mapper.readerFor(AppealCase.class).readValue(appealCaseJson);
    }
}
