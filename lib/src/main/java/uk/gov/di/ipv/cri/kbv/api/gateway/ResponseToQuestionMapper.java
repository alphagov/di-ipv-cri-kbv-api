package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Response;
import com.experian.uk.schema.experian.identityiq.services.webservice.Responses;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResponseToQuestionMapper {

    private MetricsService metricsService;

    public ResponseToQuestionMapper(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public RTQRequest mapQuestionAnswersRtqRequest(QuestionAnswerRequest questionAnswers) {
        Objects.requireNonNull(questionAnswers, "The QuestionAnswerRequest must not be null");

        var rtqRequest = new RTQRequest();
        var responses = new Responses();

        responses.getResponse().addAll(getResponses(questionAnswers.getQuestionAnswers()));

        rtqRequest.setControl(getControl(questionAnswers));
        rtqRequest.setResponses(responses);
        return rtqRequest;
    }

    public QuestionsResponse mapRTQResponse2ToMapQuestionsResponse(RTQResponse2 response) {
        QuestionsResponse questionAnswerResponse = new QuestionsResponse();

        questionAnswerResponse.setQuestions(response.getQuestions());
        Results results = response.getResults();
        Error error = response.getError();
        questionAnswerResponse.setResults(results);
        questionAnswerResponse.setControl(response.getControl());
        questionAnswerResponse.setError(error);

        metricsService.sendResultMetric(results, "submit_questions_response");
        metricsService.sendErrorMetric(error, "submit_questions_response_error");

        return questionAnswerResponse;
    }

    private Control getControl(QuestionAnswerRequest questionAnswers) {
        var control = new Control();
        control.setAuthRefNo(questionAnswers.getAuthRefNo());
        control.setURN(questionAnswers.getUrn());
        return control;
    }

    private List<Response> getResponses(List<QuestionAnswer> questionAnswers) {
        return questionAnswers.stream()
                .map(
                        element -> {
                            Response response = new Response();
                            response.setQuestionID(element.getQuestionId());
                            response.setAnswerGiven(element.getAnswer());
                            response.setCustResponseFlag(0);
                            response.setAnswerActionFlag("U");
                            return response;
                        })
                .collect(Collectors.toList());
    }
}
