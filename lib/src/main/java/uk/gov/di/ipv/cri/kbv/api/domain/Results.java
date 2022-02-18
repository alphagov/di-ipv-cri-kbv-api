package uk.gov.di.ipv.cri.kbv.api.domain;

public class Results {
    private String outcome;
    private String authenticationResult;
    private NextTransId nextTransId;
    private String questions;
    private String alerts;
    private String caseFoundFlag;
    private String confirmationCode;

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getAuthenticationResult() {
        return authenticationResult;
    }

    public void setAuthenticationResult(String authenticationResult) {
        this.authenticationResult = authenticationResult;
    }

    public NextTransId getNextTransId() {
        return nextTransId;
    }

    public void setNextTransId(NextTransId nextTransId) {
        this.nextTransId = nextTransId;
    }

    public String getQuestions() {
        return questions;
    }

    public void setQuestions(String questions) {
        this.questions = questions;
    }

    public String getAlerts() {
        return alerts;
    }

    public void setAlerts(String alerts) {
        this.alerts = alerts;
    }

    public String getCaseFoundFlag() {
        return caseFoundFlag;
    }

    public void setCaseFoundFlag(String caseFoundFlag) {
        this.caseFoundFlag = caseFoundFlag;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
}