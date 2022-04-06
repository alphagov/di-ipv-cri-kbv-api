package uk.gov.di.ipv.cri.kbv.api.library.domain;

public class QuestionsRequest {

    private String urn;
    private String strategy;
    private PersonIdentity personIdentity;

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public PersonIdentity getPersonIdentity() {
        return personIdentity;
    }

    public void setPersonIdentity(PersonIdentity personIdentity) {
        this.personIdentity = personIdentity;
    }
}