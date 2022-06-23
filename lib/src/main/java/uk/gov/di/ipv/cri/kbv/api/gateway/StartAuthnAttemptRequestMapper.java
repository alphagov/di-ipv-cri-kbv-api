package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Applicant;
import com.experian.uk.schema.experian.identityiq.services.webservice.ApplicantDateOfBirth;
import com.experian.uk.schema.experian.identityiq.services.webservice.ApplicantName;
import com.experian.uk.schema.experian.identityiq.services.webservice.ApplicationData;
import com.experian.uk.schema.experian.identityiq.services.webservice.ArrayOfString;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.LocationDetails;
import com.experian.uk.schema.experian.identityiq.services.webservice.LocationDetailsUKLocation;
import com.experian.uk.schema.experian.identityiq.services.webservice.Parameters;
import com.experian.uk.schema.experian.identityiq.services.webservice.Residency;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StartAuthnAttemptRequestMapper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String DEFAULT_STRATEGY = "3 out of 4";
    public static final String DEFAULT_TITLE = "MR";

    public SAARequest mapQuestionRequest(QuestionRequest questionRequest) {
        Objects.requireNonNull(questionRequest, "The QuestionRequest must not be null");

        return createRequest(questionRequest);
    }

    public QuestionsResponse mapSAAResponse2ToQuestionsResponse(SAAResponse2 sAAResponse2) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        questionsResponse.setQuestions(sAAResponse2.getQuestions());
        questionsResponse.setControl(sAAResponse2.getControl());
        questionsResponse.setError(sAAResponse2.getError());
        Results results = sAAResponse2.getResults();
        if (results != null) {
            String outcome = results.getOutcome();
            LOGGER.info("question response outcome: " + outcome);
            String authenticationResult = results.getAuthenticationResult();
            LOGGER.info("question response authenticationResult: " + authenticationResult);
            ArrayOfString nextTransId = results.getNextTransId();
            if (nextTransId != null) {
                List<String> transId = nextTransId.getString();
                if (transId != null) {
                    LOGGER.info(
                            "question response transIds: "
                                    + transId.stream().collect(Collectors.joining(",")));
                }
            }
        }
        questionsResponse.setResults(results);
        return questionsResponse;
    }

    private SAARequest createRequest(QuestionRequest questionRequest) {
        SAARequest saaRequest = new SAARequest();
        setApplicant(saaRequest, questionRequest.getPersonIdentity());
        setApplicationData(saaRequest, questionRequest.getStrategy());
        setControl(saaRequest, questionRequest);
        setLocationDetails(saaRequest, questionRequest.getPersonIdentity());
        setResidency(saaRequest);
        return saaRequest;
    }

    private void setResidency(SAARequest saaRequest) {
        Residency residency = new Residency();
        residency.setApplicantIdentifier(1);
        residency.setLocationIdentifier(1);
        saaRequest.getResidency().add(residency);
    }

    private void setApplicationData(SAARequest saaRequest, String strategy) {
        ApplicationData applicationData = new ApplicationData();
        applicationData.setApplicationType("IG");
        applicationData.setChannel("IN");
        applicationData.setSearchConsent("Y");
        applicationData.setProduct(StringUtils.isNotBlank(strategy) ? strategy : DEFAULT_STRATEGY);
        saaRequest.setApplicationData(applicationData);
    }

    private void setControl(SAARequest saaRequest, QuestionRequest questionRequest) {
        saaRequest.setControl(createControl(questionRequest));
    }

    private Control createControl(QuestionRequest questionRequest) {
        Control control = new Control();
        control.setTestDatabase("A");
        Parameters parameters = new Parameters();
        parameters.setOneShotAuthentication("N");
        parameters.setStoreCaseData("P");
        control.setParameters(parameters);
        control.setURN(
                StringUtils.isNotBlank(questionRequest.getUrn())
                        ? questionRequest.getUrn()
                        : UUID.randomUUID().toString());
        control.setOperatorID("GDSCABINETUIIQ01U");
        return control;
    }

    private void setApplicant(SAARequest saaRequest, PersonIdentity personIdentity) {
        Applicant applicant = new Applicant();
        applicant.setApplicantIdentifier("1");
        ApplicantName name = new ApplicantName();

        if (StringUtils.isNotBlank(personIdentity.getFirstName())) {
            name.setForename(personIdentity.getFirstName());
        }

        if (StringUtils.isNotBlank(personIdentity.getSurname())) {
            name.setSurname(personIdentity.getSurname());
        }

        name.setTitle(DEFAULT_TITLE);
        applicant.setName(name);
        ApplicantDateOfBirth dateOfBirth = new ApplicantDateOfBirth();

        if (personIdentity.getDateOfBirth() != null) {
            dateOfBirth.setDD(personIdentity.getDateOfBirth().getDayOfMonth());
            dateOfBirth.setMM(personIdentity.getDateOfBirth().getMonth().getValue());
            dateOfBirth.setCCYY(personIdentity.getDateOfBirth().getYear());
        }
        applicant.setDateOfBirth(dateOfBirth);

        saaRequest.setApplicant(applicant);
    }

    private void setLocationDetails(SAARequest saaRequest, PersonIdentity personIdentity) {
        if (Objects.nonNull(personIdentity.getAddresses())
                && !personIdentity.getAddresses().isEmpty()) {
            AtomicInteger idCounter = new AtomicInteger(1);
            List<LocationDetails> locations =
                    personIdentity.getAddresses().stream()
                            .map(
                                    personAddress -> {
                                        LocationDetailsUKLocation ukLocation =
                                                new LocationDetailsUKLocation();

                                        if (StringUtils.isNotBlank(
                                                personAddress.getBuildingName())) {
                                            ukLocation.setHouseName(
                                                    personAddress.getBuildingName());
                                        }

                                        if (StringUtils.isNotBlank(
                                                personAddress.getBuildingNumber())) {
                                            ukLocation.setHouseNumber(
                                                    personAddress.getBuildingNumber());
                                        }

                                        ukLocation.setPostcode(personAddress.getPostalCode());
                                        ukLocation.setPostTown(personAddress.getAddressLocality());
                                        ukLocation.setStreet(personAddress.getStreetName());

                                        LocationDetails locationDetails = new LocationDetails();
                                        locationDetails.setLocationIdentifier(
                                                idCounter.getAndIncrement());
                                        locationDetails.setUKLocation(ukLocation);

                                        return locationDetails;

                                        // TODO: examine the edge cases, specifically when the
                                        // following fields are populated:
                                        // personAddress.getDependentAddressLocality() &
                                        // personAddress.getDoubleDependentAddressLocality()

                                    })
                            .collect(Collectors.toList());

            // locationDetails.setClientLocationID("1");
            saaRequest.getLocationDetails().addAll(locations);
        }
    }
}
