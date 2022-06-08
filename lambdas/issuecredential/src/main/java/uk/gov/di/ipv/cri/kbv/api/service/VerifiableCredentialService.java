package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.KMSSigner;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.EvidenceType;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.*;

public class VerifiableCredentialService {

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
                                        VC_CONTEXT,
                                        new String[] {W3_BASE_CONTEXT, DI_CONTEXT},
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

        var logger = LogManager.getLogger();

        try {
            logger.error(objectMapper.writeValueAsString(calculateEvidence(kbvItem)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return signedJwtFactory.createSignedJwt(claimsSet);
    }

    private Object[] convertAddresses(List<Address> addresses) {
        return addresses.stream()
                .map(address -> objectMapper.convertValue(address, Map.class))
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

    private Map[] calculateEvidence(KBVItem kbvItem) {

        Evidence evidence = new Evidence();
        evidence.setType(EvidenceType.IDENTITY_CHECK);
        evidence.setTxn(kbvItem.getAuthRefNo());

        if (kbvItem.getStatus() == null) {
            throw new IllegalArgumentException("KBV item status is null");
        }

        switch (kbvItem.getStatus().toUpperCase()) {
            case VC_THIRD_PARTY_KBV_CHECK_PASS:
                evidence.setVerificationScore(VC_PASS_EVIDENCE_SCORE);
                break;
            case VC_THIRD_PARTY_KBV_CHECK_FAIL:
                evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
                break;
            default:
                throw new IllegalArgumentException("KBV item status is unknown");
        }

        return new Map[] {objectMapper.convertValue(evidence, Map.class)};
    }
}
