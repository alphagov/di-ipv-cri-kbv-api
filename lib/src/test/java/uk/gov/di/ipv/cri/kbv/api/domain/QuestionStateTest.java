package uk.gov.di.ipv.cri.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionStateTest {

    private QuestionState questionState;

    @BeforeEach
    void setUp() {
        questionState = new QuestionState();
    }

    @Test
    void shouldEvaluateToTrueWhenAllQuestionsHaveAnswers() {
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getAnswer()).thenReturn("answer-1");
        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock2.getAnswer()).thenReturn("answer-2");
        questionState
                .getQaPairs()
                .addAll(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        boolean allQuestionsAnswered = questionState.questionsHaveAllBeenAnswered();
        assertTrue(allQuestionsAnswered);
    }

    @Test
    void shouldEvaluateToFalseWhenAtLeastOneQuestionsIsUnAnswered() {
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getAnswer()).thenReturn("answer-1");
        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);
        questionState
                .getQaPairs()
                .addAll(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        boolean allQuestionsAnswered = questionState.questionsHaveAllBeenAnswered();
        assertFalse(allQuestionsAnswered);
    }

    @Test
    void shouldEvaluateToTrueWhenQuestionsResponseHasQuestions() {
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        Questions questionsMock = mock(Questions.class);
        when(questionsMock.getQuestion()).thenReturn(List.of(new Question[0]));
        when(questionsResponseMock.getQuestions()).thenReturn(questionsMock);

        boolean hasMoreQuestions = questionState.setQuestionsResponse(questionsResponseMock);
        assertTrue(hasMoreQuestions);
    }

    @Test
    void shouldEvaluateToFalseWhenQuestionResponseHasNoQuestion() {
        QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
        Questions questionsMock = mock(Questions.class);
        when(questionsMock.getQuestion()).thenReturn(null);
        when(questionsResponse.getQuestions()).thenReturn(questionsMock);

        boolean hasMoreQuestions = questionState.setQuestionsResponse(questionsResponse);
        assertFalse(hasMoreQuestions);
    }

    @Test
    void shouldEvaluateToFalseWhenQuestionResponseHasNoQuestions() {
        QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
        when(questionsResponse.getQuestions()).thenReturn(null);

        boolean hasMoreQuestions = questionState.setQuestionsResponse(questionsResponse);
        assertFalse(hasMoreQuestions);
    }

    @Test
    void shouldReduceArray() {
        List<String> sample = List.of("END");
        String result = sample.stream().reduce("", String::concat);

        assertEquals("END", result);
    }
}
