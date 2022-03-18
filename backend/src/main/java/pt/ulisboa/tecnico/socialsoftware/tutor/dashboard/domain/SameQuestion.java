package pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.domain;

import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.DomainEntity;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.Visitor;
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.domain.FailedAnswer;

import java.util.Set;
import java.util.HashSet;

import javax.persistence.*;

@Entity
public class SameQuestion implements DomainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "same_question_id")
    private Set<FailedAnswer> sameQuestions = new HashSet<>();

    @OneToOne
    private FailedAnswer failedAnswer;

    public SameQuestion() {
    }

    public SameQuestion(FailedAnswer failedAnswer, Set<FailedAnswer> sameQuestions) {
        /* add verifications */
        setFailedAnswer(failedAnswer);
        setSameQuestions(sameQuestions);
    }

    public void setFailedAnswer(FailedAnswer failedAnswer) {
        this.failedAnswer = failedAnswer;
    }

    public void setSameQuestions(Set<FailedAnswer> sameQuestion) {
        this.sameQuestions = sameQuestion;
    }

    public Set<FailedAnswer> getSameQuestions() {
        return sameQuestions;
    }

    public void addSameQuestion(FailedAnswer failedAnswer){
        this.sameQuestions.add(failedAnswer);
    }

    @Override
    public void accept(Visitor visitor) {
        // TODO Auto-generated method stub
    }

    @Override
    public String toString() {
        return "SameQuestion{" +
                "id=" + id +
                ", sameQuestion=" + sameQuestions +
                ", failedAnswer=" + failedAnswer +
                '}';
    }
}