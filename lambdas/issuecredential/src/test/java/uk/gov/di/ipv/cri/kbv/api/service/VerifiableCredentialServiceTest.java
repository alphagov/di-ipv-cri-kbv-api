package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonAddress;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.*;

@ExtendWith(MockitoExtension.class)
class VerifiableCredentialServiceTest implements TestFixtures {
    private static final String SUBJECT = "subject";
    @Mock private ObjectMapper objectMapper;
    @Mock private ConfigurationService mockConfigurationService;

    @BeforeEach
    void setUp() {
        when(mockConfigurationService.getVerifiableCredentialIssuer())
                .thenReturn("https://kbv-cri.account.gov.uk.TBC");
    }

    @Test
    void shouldReturnAVerifiedCredentialWhenGivenCanonicalAddresses() throws JOSEException {
        SignedJWTFactory mockSignedClaimSetJwt = mock(SignedJWTFactory.class);
        var verifiableCredentialService =
                new VerifiableCredentialService(
                        mockSignedClaimSetJwt, mockConfigurationService, objectMapper);

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("kbv-cri-issue");
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(342L);

        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setExpiryDate(Instant.now().plusSeconds(342).getEpochSecond());
        kbvItem.setStatus("VALID");

        PersonAddress address = new PersonAddress();
        address.setBuildingNumber("114");
        address.setStreet("Wellington Street");
        address.setPostcode("LS1 1BA");
        List<PersonAddress> addresses = List.of(address);

        PersonIdentity personIdentity = new PersonIdentity();
        personIdentity.setFirstName("Joe");
        personIdentity.setSurname("Bloggs");
        personIdentity.setDateOfBirth(LocalDate.of(1980, 1, 1));
        personIdentity.setAddresses(addresses);

        verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                SUBJECT, personIdentity, kbvItem);

        verify(mockSignedClaimSetJwt).createSignedJwt(any());
    }

    @Test
    void shouldCreateValidSignedJWT()
            throws InvalidKeySpecException, NoSuchAlgorithmException, JOSEException, ParseException,
                    JsonProcessingException {

        SignedJWTFactory signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));
        var verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJwtFactory, mockConfigurationService, objectMapper);

        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("VALID");

        PersonAddress address = new PersonAddress();
        address.setBuildingNumber("114");
        address.setStreet("Wellington Street");
        address.setPostcode("LS1 1BA");
        List<PersonAddress> addresses = List.of(address);

        PersonIdentity personIdentity = new PersonIdentity();
        personIdentity.setFirstName("Joe");
        personIdentity.setSurname("Bloggs");
        personIdentity.setDateOfBirth(LocalDate.of(1980, 1, 1));
        personIdentity.setAddresses(addresses);

        SignedJWT signedJWT =
                verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, personIdentity, kbvItem);
        JWTClaimsSet generatedClaims = signedJWT.getJWTClaimsSet();
        assertTrue(signedJWT.verify(new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1))));

        ObjectMapper claimSetMapper = new ObjectMapper();
        JsonNode claimsSet = claimSetMapper.readTree(generatedClaims.toString());

        assertEquals(5, claimsSet.size());

        assertAll(
                () -> {
                    assertEquals(
                            personIdentity.getDateOfBirth().format(DateTimeFormatter.ISO_DATE),
                            claimsSet
                                    .get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_BIRTHDATE_KEY)
                                    .get("value")
                                    .asText());
                });
        ECDSAVerifier ecVerifier = new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1));
        assertTrue(signedJWT.verify(ecVerifier));
    }
}
