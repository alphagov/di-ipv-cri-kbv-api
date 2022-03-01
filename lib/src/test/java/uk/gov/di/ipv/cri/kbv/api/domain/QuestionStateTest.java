package uk.gov.di.ipv.cri.kbv.api.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    void shouldEvaluateToTrueWhenNoQuestionsRemainToAnswer() {
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getAnswer()).thenReturn("answer-1");
        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock2.getAnswer()).thenReturn("answer-2");
        questionState
                .getQaPairs()
                .addAll(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        boolean allQuestionsAnswered = questionState.canSubmitAnswers();
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

        boolean allQuestionsAnswered = questionState.canSubmitAnswers();
        assertFalse(allQuestionsAnswered);
    }

    @Test
    void shouldEvaluateToTrueWhenMoreQuestionsAreReturnedFromExperianAPI() {
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        Questions questionsMock = mock(Questions.class);
        when(questionsMock.getQuestion()).thenReturn(new Question[0]);
        when(questionsResponseMock.getQuestions()).thenReturn(questionsMock);

        boolean hasMoreQuestions = questionState.setQuestionsResponse(questionsResponseMock);
        assertTrue(hasMoreQuestions);
    }

    @Test
    void shouldEvaluateToFalseWhenNoMoreQuestionsReturnedFromExperianAPI() {
        QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
        Questions questionsMock = mock(Questions.class);
        when(questionsMock.getQuestion()).thenReturn(null);
        when(questionsResponse.getQuestions()).thenReturn(questionsMock);

        boolean hasMoreQuestions = questionState.setQuestionsResponse(questionsResponse);
        assertFalse(hasMoreQuestions);
    }
}