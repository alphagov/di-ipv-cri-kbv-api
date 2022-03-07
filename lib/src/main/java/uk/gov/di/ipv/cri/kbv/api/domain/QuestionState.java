package uk.gov.di.ipv.cri.kbv.api.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionState {
    private Integer skipsRemaining;
    private String skipWarning;
    private List<QuestionAnswerPair> qaPairs = new ArrayList<>();
    private NextQuestion nextQuestion;

    public QuestionState() {}

    public void setAnswer(QuestionAnswer questionAnswer) {
        this.getQaPairs().stream()
                .filter(
                        pair ->
                                pair.getQuestion()
                                        .getQuestionID()
                                        .equals(questionAnswer.getQuestionId()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Question not found for questionID: "
                                                + questionAnswer.getQuestionId()))
                .setAnswer(questionAnswer.getAnswer());
    }

    public boolean setQuestionsResponse(QuestionsResponse questionsResponse) {
        // setControl(questionsResponse.getControl());
        Questions questions = questionsResponse.getQuestions();
        boolean hasQuestions = questions != null && questions.getQuestion() != null;
        if (hasQuestions) {
            skipsRemaining = questions.getSkipsRemaining();
            skipWarning = questions.getSkipWarning();

            setQAPairs(questions);
        }
        return hasQuestions;
    }

    public void setQAPairs(Questions questions) {
        qaPairs =
                Arrays.stream(questions.getQuestion())
                        .map(QuestionAnswerPair::new)
                        .collect(Collectors.toList());
    }

    public List<QuestionAnswerPair> getQaPairs() {
        return qaPairs;
    }

    public List<QuestionAnswer> getAnswers() {
        return getQaPairs().stream()
                .map(
                        pair -> {
                            QuestionAnswer questionAnswer = new QuestionAnswer();
                            questionAnswer.setAnswer(pair.getAnswer());
                            questionAnswer.setQuestionId(pair.getQuestion().getQuestionID());
                            return questionAnswer;
                        })
                .collect(Collectors.toList());
    }

    public Optional<Question> getNextQuestion() {
        return qaPairs.stream()
                .filter(pair -> pair.getAnswer() == null)
                .map(pair -> pair.getQuestion())
                .findFirst();
    }

    public boolean questionsHaveAllBeenAnswered() {
        return qaPairs.stream().allMatch(qa -> qa.getAnswer() != null);
    }

    public boolean hasAtLeastOneUnAnswered() {
        return qaPairs.stream().anyMatch(qa -> qa.getAnswer() == null);
    }
}
