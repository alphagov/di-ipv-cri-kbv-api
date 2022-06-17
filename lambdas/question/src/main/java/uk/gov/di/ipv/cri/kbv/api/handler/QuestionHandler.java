package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String GET_QUESTION = "get_question";
    public static final String ERROR_KEY = "\"error\"";
    private final ObjectMapper objectMapper;
    private final KBVStorageService kbvStorageService;
    private final PersonIdentityService personIdentityService;
    private final EventProbe eventProbe;
    private final KBVService kbvService;
    private final AuditService auditService;
    private final ConfigurationService configurationService;

    @ExcludeFromGeneratedCoverageReport
    public QuestionHandler() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.personIdentityService = new PersonIdentityService();
        this.configurationService = new ConfigurationService();
        this.kbvService =
                new KBVService(new KBVGatewayFactory(this.configurationService).getKbvGateway());
        this.kbvStorageService = new KBVStorageService(this.configurationService);
        this.eventProbe = new EventProbe();
        this.auditService = new AuditService();
    }

    public QuestionHandler(
            ObjectMapper objectMapper,
            KBVStorageService kbvStorageService,
            PersonIdentityService personIdentityService,
            KBVService kbvService,
            ConfigurationService configurationService,
            EventProbe eventProbe,
            AuditService auditService) {
        this.objectMapper = objectMapper;
        this.kbvStorageService = kbvStorageService;
        this.personIdentityService = personIdentityService;
        this.eventProbe = eventProbe;
        this.auditService = auditService;
        this.kbvService = kbvService;
        this.configurationService = configurationService;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            processQuestionRequest(input, response);
            eventProbe.counterMetric(GET_QUESTION);
        } catch (JsonProcessingException jsonProcessingException) {
            eventProbe.log(ERROR, jsonProcessingException).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ " + ERROR_KEY + ":\"Failed to parse object using ObjectMapper.\" }");
        } catch (NullPointerException npe) {
            eventProbe.log(ERROR, npe).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.BAD_REQUEST);
            response.withBody("{ " + ERROR_KEY + ":\"" + npe + "\" }");
        } catch (IOException e) {
            eventProbe.log(ERROR, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"Retrieving questions failed.\" }");
        } catch (Exception e) {
            eventProbe.log(ERROR, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"AWS Server error occurred.\" }");
        }
        return response;
    }

    public void processQuestionRequest(
            APIGatewayProxyRequestEvent input, APIGatewayProxyResponseEvent response)
            throws IOException, SqsException {
        response.withHeaders(Map.of("Content-Type", "application/json"));
        UUID sessionId = UUID.fromString(input.getHeaders().get(HEADER_SESSION_ID));
        KBVItem kbvItem = kbvStorageService.getKBVItem(sessionId);
        QuestionState questionState = new QuestionState();
        if (kbvItem != null) {
            questionState = objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        } else {
            kbvItem = new KBVItem();
            kbvItem.setSessionId(sessionId);
        }

        if (respondWithQuestionFromDbStore(questionState, response)) return;
        respondWithQuestionFromExperianThenStoreInDb(kbvItem, questionState, response);
    }

    private boolean respondWithQuestionFromDbStore(
            QuestionState questionState, APIGatewayProxyResponseEvent response)
            throws JsonProcessingException {
        // TODO Handle scenario when no questions are available
        Optional<Question> nextQuestion = questionState.getNextQuestion();
        if (nextQuestion.isPresent()) {
            response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));
            response.withStatusCode(HttpStatusCode.OK);
            return true;
        }
        return false;
    }

    private void respondWithQuestionFromExperianThenStoreInDb(
            KBVItem kbvItem, QuestionState questionState, APIGatewayProxyResponseEvent response)
            throws IOException, SqsException {
        // we should fall in this block once only
        // fetch a batch of questions from experian kbv wrapper
        if (Objects.nonNull(kbvItem.getStatus())) {
            response.withStatusCode(HttpStatusCode.NO_CONTENT);
            return;
        }
        QuestionsResponse questionsResponse = getQuestionRequest(kbvItem);
        if (questionsResponse != null && questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            Optional<Question> nextQuestion = questionState.getNextQuestion();
            response.withStatusCode(HttpStatusCode.OK);
            response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));

            String state = objectMapper.writeValueAsString(questionState);
            kbvItem.setQuestionState(state);
            kbvItem.setAuthRefNo(questionsResponse.getControl().getAuthRefNo());
            kbvItem.setUrn(questionsResponse.getControl().getURN());
            kbvItem.setExpiryDate(this.configurationService.getSessionExpirationEpoch());
            auditService.sendAuditEvent(AuditEventType.REQUEST_SENT);

            kbvStorageService.save(kbvItem);
        } else { // Alternate flow when first request does not return questions
            response.withStatusCode(HttpStatusCode.BAD_REQUEST);
            response.withBody(objectMapper.writeValueAsString(questionsResponse));
        }
    }

    private QuestionsResponse getQuestionRequest(KBVItem kbvItem) throws JsonProcessingException {
        if (kbvItem.getExpiryDate() == 0L) { // first request for questions for a given session
            QuestionRequest questionRequest = new QuestionRequest();
            questionRequest.setPersonIdentity(
                    personIdentityService.getPersonIdentity(kbvItem.getSessionId()));
            return this.kbvService.getQuestions(questionRequest);
        }
        QuestionState questionState =
                objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        questionAnswerRequest.setUrn(kbvItem.getUrn());
        questionAnswerRequest.setAuthRefNo(kbvItem.getAuthRefNo());
        questionAnswerRequest.setQuestionAnswers(questionState.getAnswers());
        return this.kbvService.submitAnswers(questionAnswerRequest);
    }
}