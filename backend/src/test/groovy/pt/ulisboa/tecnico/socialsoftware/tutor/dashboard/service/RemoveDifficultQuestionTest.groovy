package pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.SpockTest
import pt.ulisboa.tecnico.socialsoftware.tutor.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.domain.Dashboard
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.domain.DifficultQuestion
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.domain.SameDifficulty
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.MultipleChoiceQuestion
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Option
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question
import pt.ulisboa.tecnico.socialsoftware.tutor.user.domain.Student
import pt.ulisboa.tecnico.socialsoftware.tutor.utils.DateHandler
import spock.lang.Unroll

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.CANNOT_REMOVE_DIFFICULT_QUESTION
import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.DIFFICULT_QUESTION_NOT_FOUND

@DataJpaTest
class RemoveDifficultQuestionTest extends SpockTest {
    def student
    def dashboard
    def question
    def optionOK
    def optionKO

    def setup() {
        createExternalCourseAndExecution()

        student = new Student(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, false, AuthUser.Type.EXTERNAL)
        student.addCourse(externalCourseExecution)
        userRepository.save(student)

        question = new Question()
        question.setKey(1)
        question.setTitle(QUESTION_1_TITLE)
        question.setContent(QUESTION_1_CONTENT)
        question.setStatus(Question.Status.AVAILABLE)
        question.setNumberOfAnswers(2)
        question.setNumberOfCorrect(1)
        question.setCourse(externalCourse)
        def questionDetails = new MultipleChoiceQuestion()
        question.setQuestionDetails(questionDetails)
        questionDetailsRepository.save(questionDetails)
        questionRepository.save(question)

        optionOK = new Option()
        optionOK.setContent(OPTION_1_CONTENT)
        optionOK.setCorrect(true)
        optionOK.setSequence(0)
        optionOK.setQuestionDetails(questionDetails)
        optionRepository.save(optionOK)

        optionKO = new Option()
        optionKO.setContent(OPTION_1_CONTENT)
        optionKO.setCorrect(false)
        optionKO.setSequence(1)
        optionKO.setQuestionDetails(questionDetails)
        optionRepository.save(optionKO)

        dashboard = new Dashboard(externalCourseExecution, student)
        dashboardRepository.save(dashboard)
    }

    def "student removes a difficult question from the dashboard with daysSince=#daysSince"() {
        given:
        def difficultQuestion = new DifficultQuestion()
        difficultQuestion.setDashboard(dashboard)
        difficultQuestion.setQuestion(question)
        difficultQuestion.setPercentage(24)
        difficultQuestion.setRemovedDate(DateHandler.now().minusDays(daysSince))
        difficultQuestion.setRemoved(true)
        difficultQuestion.setSameDifficulty(new SameDifficulty(difficultQuestion))
        difficultQuestionRepository.save(difficultQuestion)

        when:
        difficultQuestionService.removeDifficultQuestion(difficultQuestion.getId())

        then:
        difficultQuestionRepository.count() == 0
        and:
        def dashboard = dashboardRepository.getById(dashboard.getId())
        dashboard.getDifficultQuestions().size() == 0

        where:
        daysSince << [0, 1, 6]
    }

    @Unroll
    def "the difficult question cannot be deleted days before #daysSince or not removed #removed"() {
        given: "a difficult question"
        def difficultQuestion = new DifficultQuestion()
        difficultQuestion.setDashboard(dashboard)
        difficultQuestion.setQuestion(question)
        difficultQuestion.setPercentage(24)
        difficultQuestion.setRemovedDate(DateHandler.now().minusDays(daysSince))
        difficultQuestion.setRemoved(removed)
        difficultQuestion.setSameDifficulty(new SameDifficulty(difficultQuestion))
        difficultQuestionRepository.save(difficultQuestion)

        when:
        difficultQuestionService.removeDifficultQuestion(difficultQuestion.getId())

        then:
        def exception = thrown(TutorException)
        exception.getErrorMessage() == errorMessage
        and:
        difficultQuestionRepository.count() == 1

        where:
        removed | daysSince || errorMessage
        false   | 0         || CANNOT_REMOVE_DIFFICULT_QUESTION
        true    | 7         || CANNOT_REMOVE_DIFFICULT_QUESTION
        true    | 8         || CANNOT_REMOVE_DIFFICULT_QUESTION
    }

    @Unroll
    def "the difficult question cannot be deleted because invalid difficultQuestionId #id"() {
        when:
        difficultQuestionService.removeDifficultQuestion(id)

        then: "an exception is thrown"
        def exception = thrown(TutorException)
        exception.getErrorMessage() == errorMessage

        where:
        id  || errorMessage
        0   || DIFFICULT_QUESTION_NOT_FOUND
        100 || DIFFICULT_QUESTION_NOT_FOUND
    }

    @Unroll
    def "delete a question from a set of #numQuestions difficult questions with the same percentage"(){

        given:
        def difficultQuestions = []
        for (int i in 0..numQuestions - 1){
            def question = new Question()
            question.setTitle(QUESTION_1_TITLE)
            question.setContent(QUESTION_1_CONTENT)
            question.setStatus(Question.Status.AVAILABLE)
            question.setNumberOfAnswers(2 * (i + 1))
            question.setNumberOfCorrect(i + 1)
            question.setCourse(externalCourse)
            question.setQuestionDetails(new MultipleChoiceQuestion())
            questionRepository.save(question)

            difficultQuestions[i] = new DifficultQuestion()
            difficultQuestions[i].setQuestion(question)
            difficultQuestions[i].setDashboard(dashboard)
            difficultQuestions[i].setPercentage(24)
            difficultQuestions[i].setRemovedDate(DateHandler.now().minusDays(0))
            difficultQuestions[i].setRemoved(true)
            difficultQuestions[i].setSameDifficulty(new SameDifficulty(difficultQuestions[i]))
            difficultQuestionRepository.save(difficultQuestions[i])
        }
        for (int i in 0..numQuestions - 1) {
            for (int j in 0..numQuestions - 1) {
                if (i != j) {
                    difficultQuestions[i].getSameDifficulty().addSameDifficultyQuestion(difficultQuestions[j])
                }
            }
        }

        when:
        difficultQuestionService.removeDifficultQuestion(difficultQuestions[0].getId())

        then:
        difficultQuestionRepository.count() == (long) (numQuestions - 1)
        def results = []
        for (int i in 0..numQuestions - 2){
            results.add(difficultQuestionRepository.findAll().get(i))
        }

        for (int i in 0..numQuestions - 2){
            results[i].getSameDifficulty().getSameDifficultyQuestions().size() == (long) (numQuestions - 2)
            for (int j in 0..numQuestions - 2) {
                if (i != j) {
                    results[j] in results[i].getSameDifficulty().getSameDifficultyQuestions()
                }
            }
            !(difficultQuestions[0] in results[i].getSameDifficulty().getSameDifficultyQuestions())
        }

        where:
        numQuestions << [2, 10]

    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}