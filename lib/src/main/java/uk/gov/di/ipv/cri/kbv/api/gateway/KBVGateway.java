package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;

import java.util.Objects;

public class KBVGateway {
    private final StartAuthnAttemptRequestMapper saaRequestMapper;
    private final ResponseToQuestionMapper responseToQuestionMapper;
    private final IdentityIQWebServiceSoap identityIQWebServiceSoap;

    KBVGateway(
            StartAuthnAttemptRequestMapper saaRequestMapper,
            ResponseToQuestionMapper responseToQuestionMapper,
            IdentityIQWebServiceSoap identityIQWebServiceSoap) {
        this.identityIQWebServiceSoap =
                Objects.requireNonNull(
                        identityIQWebServiceSoap, "identityIQWebServiceSoap must not be null");
        this.saaRequestMapper =
                Objects.requireNonNull(saaRequestMapper, "saaRequestMapper must not be null");
        this.responseToQuestionMapper =
                Objects.requireNonNull(
                        responseToQuestionMapper, "rtqRequestMapper must not be null");
    }

    @Tracing
    public QuestionsResponse getQuestions(QuestionRequest questionRequest) {
        SAARequest saaRequest = saaRequestMapper.mapQuestionRequest(questionRequest);
        SAAResponse2 saaResponse2 = identityIQWebServiceSoap.saa(saaRequest);
        return saaRequestMapper.mapSAAResponse2ToQuestionsResponse(saaResponse2);
    }

    @Tracing
    public QuestionsResponse submitAnswers(QuestionAnswerRequest questionAnswerRequest) {
        RTQRequest rtqRequest =
                responseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest);
        RTQResponse2 rtqResponse2 = identityIQWebServiceSoap.rtq(rtqRequest);
        return responseToQuestionMapper.mapRTQResponse2ToMapQuestionsResponse(rtqResponse2);
    }
}
