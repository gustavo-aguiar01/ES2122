package pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.webservice

import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.HttpStatus
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import pt.ulisboa.tecnico.socialsoftware.tutor.SpockTest
import pt.ulisboa.tecnico.socialsoftware.tutor.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.domain.Dashboard
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.MultipleChoiceQuestion
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Option
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question
import pt.ulisboa.tecnico.socialsoftware.tutor.user.domain.Student

import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetDifficultQuestionWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def response
    def student
    def dashboard
    def question
    def optionOK
    def optionKO

    def setup() {
        given:
        restClient = new RESTClient("http://localhost:" + port)
        and:
        createExternalCourseAndExecution()
        and:
        student = new Student(USER_1_NAME,  USER_1_USERNAME, USER_1_EMAIL, false, AuthUser.Type.EXTERNAL)
        student.authUser.setPassword(passwordEncoder.encode(USER_1_PASSWORD))
        student.addCourse(externalCourseExecution)
        userRepository.save(student)
        and:
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
        and:
        optionOK = new Option()
        optionOK.setContent(OPTION_1_CONTENT)
        optionOK.setCorrect(true)
        optionOK.setSequence(0)
        optionOK.setQuestionDetails(questionDetails)
        optionRepository.save(optionOK)
        and:
        optionKO = new Option()
        optionKO.setContent(OPTION_1_CONTENT)
        optionKO.setCorrect(false)
        optionKO.setSequence(1)
        optionKO.setQuestionDetails(questionDetails)
        optionRepository.save(optionKO)
        and:
        dashboard = new Dashboard(externalCourseExecution, student)
        dashboardRepository.save(dashboard)
    }

    def "student gets difficult questions"() {
        given: '2 difficult questions in a students dashboard'
        createdUserLogin(USER_1_USERNAME, USER_1_PASSWORD)
        def question2 = new Question()
        question2.setKey(2)
        question2.setTitle(QUESTION_2_TITLE)
        question2.setContent(QUESTION_2_CONTENT)
        question2.setStatus(Question.Status.AVAILABLE)
        question2.setNumberOfAnswers(3)
        question2.setNumberOfCorrect(2)
        question2.setCourse(externalCourse)
        def questionDetails2 = new MultipleChoiceQuestion()
        question2.setQuestionDetails(questionDetails2)
        questionDetailsRepository.save(questionDetails2)
        questionRepository.save(question2)

        def difficultQuestionDto1 = DifficultQuestionService.createDifficultQuestion(dashboard.getId(), question.getId(), 24)
        def difficultQuestionDto2 = DifficultQuestionService.createDifficultQuestion(dashboard.getId(), question2.getId(), 20)


        when: 'Get web service is invoked'
        response = restClient.get(
                path: '/students/dashboards/' + dashboard.getId() + '/difficultquestions',
                requestContentType: 'application/json'
        )

        then: "the request returns status code 200"
        response != null
        response.status == 200

        and: "both questions are in the repository"
        difficultQuestionRepository.findAll().size() == 2

        and: "the difficult questions are returned"
        def info = response.data
        info.size() == 2
        (info.get(0).questionDto.id == question.getId()) || (info.get(0).questionDto.id == question2.getId())
        (info.get(1).questionDto.id == question.getId()) || (info.get(1).questionDto.id == question2.getId())

        cleanup:
        difficultQuestionRepository.deleteAll()
        questionDetailsRepository.deleteById(questionDetails2.getId())
        questionRepository.deleteAll()
    }

    def "teacher can't get student's difficult questions"() {
        given: "a demo teacher"
        demoTeacherLogin()

        when: "Get web service is invoked"
        response = restClient.get(
                path: '/students/dashboards/' + dashboard.getId() + '/difficultquestions',
                requestContentType: 'application/json'
        )

        then: "the request returns status code 403"
        def error = thrown(HttpResponseException)
        error.response.status == HttpStatus.SC_FORBIDDEN
    }

    def "student can't get another student's difficult questions"() {
        given: "a second student"
        def student2 = new Student(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, false, AuthUser.Type.EXTERNAL)
        student2.authUser.setPassword(passwordEncoder.encode(USER_2_PASSWORD))
        student2.addCourse(externalCourseExecution)
        userRepository.save(student2)
        createdUserLogin(USER_2_USERNAME, USER_2_PASSWORD)

        and: "a second dashboard"
        def dashboard2 = new Dashboard(externalCourseExecution, student2)
        dashboardRepository.save(dashboard2)

        when: "get web service is invoked"
        response = restClient.get(
                path: '/students/dashboards/' + dashboard.getId() + '/difficultquestions',
                requestContentType: 'application/json'
        )

        then: "the request returns status code 403"
        def error = thrown(HttpResponseException)
        error.response.status == HttpStatus.SC_FORBIDDEN

        cleanup:
        userRepository.deleteById(student2.getId())
    }

    def cleanup() {
        userRepository.deleteById(student.getId())
        courseExecutionRepository.deleteById(externalCourseExecution.getId())
        courseRepository.deleteById(externalCourseExecution.getCourse().getId())
        questionRepository.deleteAll()
        questionDetailsRepository.deleteAll()
        optionRepository.deleteAll()
        dashboardRepository.deleteAll()
    }
}