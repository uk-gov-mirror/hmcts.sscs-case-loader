package uk.gov.hmcts.reform.sscs.services.mapper;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.models.deserialize.gaps2.AppealCase;
import uk.gov.hmcts.reform.sscs.models.serialize.ccd.CaseData;
import uk.gov.hmcts.reform.sscs.services.refdata.ReferenceDataService;

public class TransformAppealCaseToCaseDataTest {
    @Test
    public void givenACaseData_shouldBeTransformToCaseDataWithSubscriptionsAndAppealNumber() throws Exception {
        ReferenceDataService referenceDataService = mock(ReferenceDataService.class);
        CaseDataBuilder caseDataBuilder = new CaseDataBuilder(referenceDataService);
        TransformAppealCaseToCaseData transformAppealCaseToCaseData =
            new TransformAppealCaseToCaseData(caseDataBuilder);

        AppealCase appealCase = getAppealCase();

        CaseData caseData = transformAppealCaseToCaseData.transform(appealCase);

        String appealNumber = caseData.getSubscriptions().getAppellantSubscription().getTya();
        assertTrue("appealNumber length is not 10 digits", appealNumber.length() == 10);
    }

    private AppealCase getAppealCase() throws Exception {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder
            .json()
            .indentOutput(true)
            .build();

        String appealCaseJson = FileUtils.readFileToString(new File("src/test/resources/AppealCase.json"),
            StandardCharsets.UTF_8.name());
        return mapper.readerFor(AppealCase.class).readValue(appealCaseJson);
    }
}
