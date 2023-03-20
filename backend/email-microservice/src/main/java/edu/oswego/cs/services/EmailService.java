package edu.oswego.cs.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import edu.oswego.cs.daos.AssignmentDAO;
import edu.oswego.cs.daos.CourseDAO;
import edu.oswego.cs.daos.TeamDAO;
import edu.oswego.cs.database.AssignmentInterface;
import edu.oswego.cs.database.CourseInterface;
import edu.oswego.cs.database.DatabaseManager;
import edu.oswego.cs.util.DeadlineTimer;
import org.bson.Document;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.SecurityContext;
import java.io.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * This class contains all the methods that deal with sending emails. These methods are called in edu.oswego.cs.services.EmailService.
 *
 */
public class EmailService {
    static String from = "schmittsLaptop@patrick.com";
    static Properties props = new Properties();
    static Session session = Session.getDefaultInstance(props, null);

    /**
     * Sends an email to the professor informing them that all teams in the course have submitted an assignment.
     * The email will not be sent if the assignment deadline has passed.
     * This method is otherwise called every time a team submits an assignment, at which point
     * it checks every team in the course to see if all of them have submitted it. Sends the email if and only if
     * all teams have submitted.
     *
     * @param courseID ID of the course in which the assignment is assigned
     * @param assignmentID ID the assignment which has been submitted
     */

    public void allAssignmentsSubmittedEmail(String courseID, int assignmentID) throws IOException {
        Document assignment = new AssignmentInterface().getSpecifiedAssignment(courseID, assignmentID);
        //turns string date into unix timestamp and compares it to current time to tell if due date has passed.
        if(new SimpleDateFormat("yyyy-MM-dd").parse((assignment.getString("due_date")), new ParsePosition(0)).getTime() < new Date().getTime()){
            //overdue, don't send email.
            return;
        }
        if(!allSubmitted(assignment)){
            //not everyone has submitted
            return;
        }
        //prerequisites are met

        //load template
        String body = getTemplate("allAssignmentsSubmitted.html");

        String subject = "An assignment has been submitted by all students and is ready for grading.";
        Document course = new CourseInterface().getCourse(courseID);

        //fill in specifics
        body = body.replace("[Course Name]", course.getString("course_id"));
        body = body.replace("[Today's Date]", new Date().toString());
        body = body.replace("[Name of Instructor]", course.getString("professor_id"));
        body = body.replace("[Assignment Name]", assignment.getString("assignment_name"));

        System.out.println(body);
        //will throw error if professor doesn't have @oswego.edu email
        sendEmail(course.getString("professor_id") + "@oswego.edu", subject, body);
    }

    /**
     * Sends an email to the professor informing them that all teams in the course have submitted a peer review.
     * This method will not be called if the peer review deadline has passed.
     * This method is otherwise called every time a team submits a peer review, at which point
     * it checks every team in the course to see if all of them have submitted it. Sends the email if and only if
     * all teams have submitted.
     *
     * @param course the course in which the peer review is assigned
     * @param assignment the peer review which has been submitted
     */
    public void allPeerReviewsSubmittedEmail(CourseDAO course, AssignmentDAO assignment){
        //How to tell if peer review is submitted? Where is that stored?
    }

    /**
     * Sends an email to all the students in the course when an assignment is created by the professor.
     * Called when the professor finished posting the assignment.
     *
     * @param securityContext context from which professor ID can be obtained
     * @param courseID the course in which the peer review is assigned
     * @param assignmentID the assignment that has been created
     */
    public void assignmentCreatedEmail(SecurityContext securityContext, String courseID, int assignmentID) throws IOException {
        //read contents of template
        String template = getTemplate("assignmentCreatedEmail.html");

        //get a list of all the students in the course
        List<Document> students = new CourseInterface().getStudentsInCourse(securityContext, courseID);

        String subject = "A new assignment has been created in one of your classes.";

        for(Document student : students){
            String body = "" + template; //copy template
            //Fill in specifics
            body = body.replace("[Name of Student]", student.getString("first_name") + " " + student.getString("last_name"));
            body = body.replace("[Today's Date]", new Date().toString());
            body = body.replace("[Course Name]", new CourseInterface().getCourse(securityContext, courseID).getString("course_name"));
            body = body.replace("[Assignment Name]", new AssignmentInterface().getSpecifiedAssignment(courseID, assignmentID).getString("assignment_name"));
            body = body.replace("[Due Date]", new AssignmentInterface().getSpecifiedAssignment(courseID, assignmentID).getString("due_date"));
            body = body.replace("[Instructor Name]", new CourseInterface().getProfessor(securityContext).getString("professor_id"));
            //print because I currently can't send mail
            System.out.println(body);
            //send email
            sendEmail(student.getString("student_id") + "@gmail.com", subject, body); //will throw an error for any student with a non-gmail email. This is a DB issue.
        }
    }

        /**
     * Sends an email to the professor when the deadline for an assignment has passed and not all teams have made a
     * submission for it. A timer is created when the assignment is created (See createDeadlineTimer). This method is
     * called when the timer reaches its end.
     *
     * @param course the course in which the assignment is assigned
     * @param assignment the assignment whose deadline has passed
     */
    public static void assignmentDeadlinePassed(CourseDAO course, AssignmentDAO assignment) throws IOException {

    }

    /**
     * Sends an email to all the members of a team after they submit an assignment to act as a digital receipt.
     *
     * @param course course for which the assignment was submitted
     * @param team team that submitted the assignment
     * @param assignment assignment that was submitted
     * @param timestamp time at which the assignment was submitted
     */
    public void assignmentSubmittedEmail(CourseDAO course, TeamDAO team, AssignmentDAO assignment, Date timestamp) throws IOException {

    }

    /**
     * Method to create a new thread that acts as a timer for a deadline. This timer checks avery hour to see if
     * the deadline has passed, at which point it calls the appropriate method. The timer checks repeatedly instead of
     * just waiting the full time to account for when the assignment deadline changes or if the assignment is deleted.
     *
     * @param assignment The assignment whose deadline is being checked for.
     * @param isPeerReview a true value will call peerReviewDeadlinePassed instead of assignmentDeadlinePassed.
     */
    public void createDeadlineTimer(AssignmentDAO assignment, boolean isPeerReview){
        new DeadlineTimer(assignment.courseID, isPeerReview).start();
    }

    /**
     * Sends the student an email when the professor submits a grade for their assignment.
     *
     * @param course course for which the grade is given
     * @param assignment assignment for which the grade is given
     * @param team team receiving the email
     */
    public void gradeReceivedEmail(CourseDAO course, AssignmentDAO assignment, TeamDAO team) throws IOException {


    }

    /**
     * Sends the professor an email when an outlier is detected.
     */
    public void outlierDetectedEmail(){

    }

    /**
     * Sends all the students in a team an email when that team is assigned a peer review.
     *
     * @param course course in which the peer review is assigned
     * @param team team reviewing the submission
     * @param assignment submission being peer reviewed
     */
    public void peerReviewAssignedEmail(CourseDAO course, TeamDAO team, AssignmentDAO assignment){

    }

    /**
     * Sends an email to the professor when the deadline for a peer review has passed and not all teams have made a
     * submission for it. A timer is created when the peer review assignment is created (See createDeadlineTimer). This
     * method is called when the timer reaches its end.
     *
     * @param course the course in which the assignment is assigned
     * @param assignment the assignment whose deadline has passed
     */
    public void peerReviewDeadlinePassed(CourseDAO course, AssignmentDAO assignment){

    }

    /**
     * Sends an email to all the members of a team after they submit a peer review to act as a digital receipt.
     *
     * @param course course for which the peer review was submitted
     * @param team team that submitted the peer review
     * @param assignment peer review that was submitted
     * @param timestamp time at which the peer review was submitted
     */
    public void peerReviewSubmittedEmail(CourseDAO course, TeamDAO team, AssignmentDAO assignment, Date timestamp){

    }

    /**
     * Sends the professor an email when someone submits an assignment containing profanity.
     *
     * @param course course for which the assignment is submitted
     * @param team team that submitted the assignment
     * @param assignment assignment for which the submission contained profanity
     */
    public void profanityEmail(CourseDAO course, TeamDAO team, AssignmentDAO assignment){

    }

    /**
     * Reads a file in the templates folder and returns its contents as a string. Used for getting html templates of
     * email bodies.
     *
     * @param fileName the name of the file to be read. Do not pass the entire path, only the file's name.
     * @return the contents of the file as a string
     */
    public String getTemplate(String fileName) throws IOException {
        String path = getPath();
        BufferedReader in = new BufferedReader(new FileReader(path + "templates"
                + File.separator + fileName));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        String ls = System.getProperty("line.separator");
        while((line = in.readLine()) != null){
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        in.close();
        return stringBuilder.toString();
    }

    /**
     * Sends an email with the specified recipients, subject, and body. Called by all the email methods in this class.
     *
     * @param to recipient's email address
     * @param subject subject of the email to be sent
     * @param body body of the email to be sent
     */
    public void sendEmail(String to, String subject, String body){
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from, "NoReply"));
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);
            System.out.println("Email sent successfully...");
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
            System.out.println("Could not send an email to " + to);
        }
    }

    /**
     * gets the path from the working directory to "defaultserver"
     *
     * @return the path from the working directory to "defaultserver"
     */
    public String getPath() {
        String path = (System.getProperty("user.dir").contains("\\")) ? System.getProperty("user.dir").replace("\\", "/") : System.getProperty("user.dir");
        String[] slicedPath = path.split("/");
        String targetDir = "defaultServer";
        StringBuilder relativePathPrefix = new StringBuilder();

        for (int i = slicedPath.length - 1; !slicedPath[i].equals(targetDir); i--) {
            relativePathPrefix.append("../");
        }

        if (System.getProperty("user.dir").contains("\\")) {
            String reg = "//";
            relativePathPrefix = new StringBuilder(relativePathPrefix.toString().replace("/", "\\"));
        }

        return relativePathPrefix.toString();
    }

    /**
     * checks to see if every team has submitted a given assignment.
     *
     * @param assignment assignment being checked
     * @return true if every team in the course has submitted the assignment, false otherwise
     */
    public boolean allSubmitted(Document assignment) {
        String courseID = assignment.getString("course_id");
        List<Document> teams = new CourseInterface().getTeamsInCourse(courseID);
        for(Document team : teams){
            if(!new AssignmentInterface().hasTeamSubmitted(assignment.getInteger("assignment_id"), team.getString("team_id"))){
                //has not submitted
                return false;
            }
        }
        //everyone has submitted
        return true;
    }
}