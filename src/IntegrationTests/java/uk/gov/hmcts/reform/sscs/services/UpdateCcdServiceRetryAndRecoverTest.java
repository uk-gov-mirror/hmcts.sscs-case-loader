package uk.gov.hmcts.reform.sscs.services;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.models.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.models.serialize.ccd.CaseData;
import uk.gov.hmcts.reform.sscs.services.ccd.UpdateCcdService;
import uk.gov.hmcts.reform.sscs.services.idam.IdamService;
import uk.gov.hmcts.reform.sscs.services.sftp.SftpChannelAdapter;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("development")
public class UpdateCcdServiceRetryAndRecoverTest {

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private SftpChannelAdapter channelAdapter;

    @MockBean
    private IdamService idamService;

    @Autowired
    private UpdateCcdService updateCcdService;

    private CaseData caseData;
    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        caseData = CaseData.builder()
            .caseReference("SC068/17/00004")
            .build();
        idamTokens = IdamTokens.builder()
            .authenticationService("serviceAuthorization")
            .idamOauth2Token("authorization")
            .build();
    }

    @Test
    public void givenUpdateCcdApiFailsWhenStartEvent_shouldRetryAndRecover() {
        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException());

        when(idamService.getIdamOauth2Token()).thenReturn("authorization2");
        when(idamService.generateServiceAuthorization()).thenReturn("serviceAuthorization2");

        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenReturn(CaseDetails.builder().build());

        updateCcdService.update(caseData, 1L, "appealReceived", idamTokens);

        verify(coreCaseDataApi, times(0)).submitEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class));
    }

    @Test
    public void givenUpdateCcdApiFailsWhenSubmittingEvent_shouldRetryAndRecover() {
        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
            eq("authorization"),
            eq("serviceAuthorization"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException())
            .thenThrow(new RuntimeException());

        when(idamService.getIdamOauth2Token()).thenReturn("authorization2");
        when(idamService.generateServiceAuthorization()).thenReturn("serviceAuthorization2");

        when(coreCaseDataApi.startEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
            .thenReturn(StartEventResponse.builder().build());

        when(coreCaseDataApi.submitEventForCaseWorker(
            eq("authorization2"),
            eq("serviceAuthorization2"),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(true),
            any(CaseDataContent.class)))
            .thenReturn(CaseDetails.builder().build());

        updateCcdService.update(caseData, 1L, "appealReceived", idamTokens);
    }
}
