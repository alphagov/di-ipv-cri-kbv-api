package uk.gov.di.ipv.cri.kbv.api.exception;

import uk.gov.di.ipv.cri.common.library.error.ErrorResponse;

public class CredentialRequestException extends Exception {
    public CredentialRequestException(ErrorResponse invalidRequestParam) {
        super(invalidRequestParam.getMessage());
    }

    public CredentialRequestException(String message, Exception e) {
        super(message, e);
    }
}
