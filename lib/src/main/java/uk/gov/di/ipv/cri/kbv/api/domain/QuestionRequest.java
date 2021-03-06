package uk.gov.di.ipv.cri.kbv.api.domain;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;

public class QuestionRequest {
    private String urn;

    private String strategy;

    private String iiqOperatorId;

    private PersonIdentity personIdentity;

    public String getIiqOperatorId() {
        return iiqOperatorId;
    }

    public void setIiqOperatorId(String iiqOperatorId) {
        this.iiqOperatorId = iiqOperatorId;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public void setPersonIdentity(PersonIdentity personIdentity) {
        this.personIdentity = personIdentity;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getUrn() {
        return urn;
    }

    public PersonIdentity getPersonIdentity() {
        return personIdentity;
    }
}
