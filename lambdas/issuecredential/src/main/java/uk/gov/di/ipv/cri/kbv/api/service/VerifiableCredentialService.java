package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.KMSSigner;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.*;

public class VerifiableCredentialService {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String METRIC_DIMENSION_KBV_VERIFICATION = "kbv_verification";
    private final SignedJWTFactory signedJwtFactory;
    private final ConfigurationService configurationService;

    private final ObjectMapper objectMapper;

    public VerifiableCredentialService() {
        this.configurationService = new ConfigurationService();
        this.signedJwtFactory =
                new SignedJWTFactory(
                        new KMSSigner(
                                configurationService.getVerifiableCredentialKmsSigningKeyId()));
        this.objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());
    }

    public VerifiableCredentialService(
            SignedJWTFactory signedClaimSetJwt,
            ConfigurationService configurationService,
            ObjectMapper objectMapper) {
        this.signedJwtFactory = signedClaimSetJwt;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    @Tracing
    public SignedJWT generateSignedVerifiableCredentialJwt(
            String subject, PersonIdentityDetailed personIdentity, KBVItem kbvItem)
            throws JOSEException {
        var now = Instant.now();

        var claimsSet =
                new JWTClaimsSet.Builder()
                        .claim(SUBJECT, subject)
                        .claim(ISSUER, configurationService.getVerifiableCredentialIssuer())
                        .claim(NOT_BEFORE, now.getEpochSecond())
                        .claim(
                                EXPIRATION_TIME,
                                now.plusSeconds(configurationService.getMaxJwtTtl())
                                        .getEpochSecond())
                        .claim(
                                VC_CLAIM,
                                Map.of(
                                        VC_TYPE,
                                        new String[] {
                                            VERIFIABLE_CREDENTIAL_TYPE, KBV_CREDENTIAL_TYPE
                                        },
                                        VC_CREDENTIAL_SUBJECT,
                                        Map.of(
                                                VC_ADDRESS_KEY,
                                                        convertAddresses(
                                                                personIdentity.getAddresses()),
                                                VC_NAME_KEY, personIdentity.getNames(),
                                                VC_BIRTHDATE_KEY,
                                                        convertBirthDates(
                                                                personIdentity.getBirthDates())),
                                        VC_EVIDENCE_KEY,
                                        calculateEvidence(kbvItem)))
                        .build();

        return signedJwtFactory.createSignedJwt(claimsSet);
    }

    public Map<String, Object> getAuditEventExtensions(KBVItem kbvItem) {
        return Map.of(
                ISSUER,
                configurationService.getVerifiableCredentialIssuer(),
                VC_EVIDENCE_KEY,
                calculateEvidence(kbvItem));
    }

    @SuppressWarnings("unchecked")
    private Object[] convertAddresses(List<Address> addresses) {
        return addresses.stream()
                .map(
                        address -> {
                            var mappedAddress = objectMapper.convertValue(address, Map.class);
                            // Skip superfluous address type from the map to match RFC
                            HashMap<String, Object> addressMap = new HashMap<>();
                            if (mappedAddress != null) {
                                mappedAddress.forEach(
                                        (key, value) -> {
                                            if (!key.equals("addressType")) {
                                                addressMap.put(key.toString(), value);
                                            }
                                        });
                            }

                            return addressMap;
                        })
                .toArray();
    }

    private Object[] convertBirthDates(List<BirthDate> birthDates) {
        return birthDates.stream()
                .map(
                        birthDate ->
                                Map.of(
                                        "value",
                                        birthDate
                                                .getValue()
                                                .format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .toArray();
    }

    private Object[] calculateEvidence(KBVItem kbvItem) {

        Evidence evidence = new Evidence();
        evidence.setTxn(kbvItem.getAuthRefNo());

        EventProbe eventProbe = new EventProbe();

        if (VC_THIRD_PARTY_KBV_CHECK_PASS.equalsIgnoreCase(kbvItem.getStatus())) {
            evidence.setVerificationScore(VC_PASS_EVIDENCE_SCORE);
            eventProbe.addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
            LOGGER.info("kbv pass");

        } else {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            eventProbe.addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
            LOGGER.info("kbv fail");
        }

        return new Map[] {objectMapper.convertValue(evidence, Map.class)};
    }
}
