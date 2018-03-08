package uk.gov.hmcts.reform.sscs.services;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.models.GapsInputStream;
import uk.gov.hmcts.reform.sscs.models.serialize.ccd.CaseData;
import uk.gov.hmcts.reform.sscs.services.ccd.CcdCasesSender;
import uk.gov.hmcts.reform.sscs.services.ccd.CreateCoreCaseDataService;
import uk.gov.hmcts.reform.sscs.services.mapper.TransformJsonCasesToCaseData;
import uk.gov.hmcts.reform.sscs.services.mapper.TransformXmlFilesToJsonFiles;
import uk.gov.hmcts.reform.sscs.services.sftp.SftpSshService;
import uk.gov.hmcts.reform.sscs.services.xml.XmlValidator;

public class CaseLoaderServiceTest {

    @Mock
    private SftpSshService sftpSshService;
    @Mock
    private XmlValidator xmlValidator;
    @Mock
    private TransformXmlFilesToJsonFiles transformXmlFilesToJsonFiles;
    @Mock
    private TransformJsonCasesToCaseData transformJsonCasesToCaseData;
    @Mock
    private CreateCoreCaseDataService createCoreCaseDataService;
    @Mock
    private CcdCasesSender ccdCasesSender;

    private CaseLoaderService caseLoaderService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        caseLoaderService = new CaseLoaderService(sftpSshService, xmlValidator, transformXmlFilesToJsonFiles,
            transformJsonCasesToCaseData,
            ccdCasesSender);
    }

    @Test
    public void givenDeltaAndRefFiles_shouldProcessBothCorrectly() throws IOException {
        when(sftpSshService.readExtractFiles()).thenReturn(buildGapsInputStreams());
        doNothing().when(xmlValidator).validateXml(anyString(), anyString());
        when(transformXmlFilesToJsonFiles.transform(anyString())).thenReturn(mock(JSONObject.class));
        List<CaseData> caseDataList = Collections.singletonList(CaseData.builder().build());
        when(transformJsonCasesToCaseData.transformCreateCases(anyString())).thenReturn(caseDataList);
        when(createCoreCaseDataService.createCcdCase(any(CaseData.class)))
            .thenReturn(CaseDetails.builder().build());
        caseLoaderService.process();
    }

    private List<GapsInputStream> buildGapsInputStreams() throws IOException {
        GapsInputStream refStream = GapsInputStream.builder()
            .isDelta(false)
            .isReference(true)
            .inputStream(IOUtils.toInputStream("Reference", StandardCharsets.UTF_8.name()))
            .build();
        GapsInputStream deltaStream = GapsInputStream.builder()
            .isDelta(true)
            .isReference(false)
            .inputStream(IOUtils.toInputStream("Delta", StandardCharsets.UTF_8.name()))
            .build();
        return ImmutableList.of(refStream, deltaStream);
    }


}
