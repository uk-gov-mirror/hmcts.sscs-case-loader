package uk.gov.hmcts.reform.sscs.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exceptions.TransformException;
import uk.gov.hmcts.reform.sscs.models.serialize.ccd.CaseData;
import uk.gov.hmcts.reform.sscs.services.ccd.CcdCasesSender;
import uk.gov.hmcts.reform.sscs.services.gaps2.files.Gaps2File;
import uk.gov.hmcts.reform.sscs.services.mapper.TransformJsonCasesToCaseData;
import uk.gov.hmcts.reform.sscs.services.mapper.TransformXmlFilesToJsonFiles;
import uk.gov.hmcts.reform.sscs.services.sftp.SftpSshService;
import uk.gov.hmcts.reform.sscs.services.xml.XmlValidator;

@Service
@Slf4j
public class CaseLoaderService {

    private final SftpSshService sftpSshService;
    private final XmlValidator xmlValidator;
    private final TransformXmlFilesToJsonFiles transformXmlFilesToJsonFiles;
    private final TransformJsonCasesToCaseData transformJsonCasesToCaseData;
    private final CcdCasesSender ccdCasesSender;

    @Autowired
    public CaseLoaderService(SftpSshService sftpSshService, XmlValidator xmlValidator,
                             TransformXmlFilesToJsonFiles transformXmlFilesToJsonFiles,
                             TransformJsonCasesToCaseData transformJsonCasesToCaseData,
                             CcdCasesSender ccdCasesSender) {
        this.sftpSshService = sftpSshService;
        this.xmlValidator = xmlValidator;
        this.transformXmlFilesToJsonFiles = transformXmlFilesToJsonFiles;
        this.transformJsonCasesToCaseData = transformJsonCasesToCaseData;
        this.ccdCasesSender = ccdCasesSender;
    }

    public void process() {
        log.info("*** case-loader *** Reading xml files from SFTP...");
        List<Gaps2File> files = sftpSshService.getFiles();
        log.info("*** case-loader *** Read xml files from SFTP successfully");
        for (Gaps2File file : files) {
            try (InputStream inputStream = sftpSshService.readExtractFile(file)) {
                String xmlAsString = fromInputStreamToString(inputStream);
                xmlValidator.validateXml(xmlAsString, file.isDelta());
                log.info("*** case-loader *** Validate delta xml file successfully");
                if (file.isDelta()) {
                    JSONObject jsonCases = transformXmlFilesToJsonFiles.transform(xmlAsString);
                    log.info("*** case-loader *** Transform XML to JSON successfully");
                    List<CaseData> casesToCreate = transformJsonCasesToCaseData
                        .transformCreateCases(jsonCases.toString());
                    log.info("*** case-loader *** Transform json to cases to create successfully");
                    List<CaseData> casesToUpdate = transformJsonCasesToCaseData
                        .transformUpdateCases(jsonCases.toString());
                    log.info("*** case-loader *** Transform json to cases to update successfully");
                    ccdCasesSender.sendCreateCcdCases(casesToCreate);
                    ccdCasesSender.sendUpdateCcdCases(casesToUpdate);
                }
            } catch (IOException e) {
                log.error("Error in processing gaps2 extract file : {}", file.getName(), e);
                throw new TransformException("Error processing delta file", e);
            }
        }
    }

    private String fromInputStreamToString(InputStream inputStream) {
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new TransformException("Oops...something went wrong...", e);
        }
    }

}
