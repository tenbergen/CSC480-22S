package edu.oswego.cs.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.oswego.cs.daos.CourseDAO;
import edu.oswego.cs.daos.StudentDAO;
import org.bson.Document;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class CourseInterface {
    private final MongoCollection<Document> studentCollection;
    private final MongoCollection<Document> courseCollection;

    public CourseInterface() {
        DatabaseManager databaseManager = new DatabaseManager();
        try {
            MongoDatabase studentDB = databaseManager.getStudentDB();
            MongoDatabase courseDB = databaseManager.getCourseDB();
            studentCollection = studentDB.getCollection("students");
            courseCollection = courseDB.getCollection("courses");
        } catch (WebApplicationException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Failed to retrieve collections.").build());
        }
    }

    public List<CourseDAO> getAllCourses() {
        List<CourseDAO> courses = new ArrayList<>();
        for (Document document : courseCollection.find()) {
            CourseDAO courseDAO = new CourseDAO(
                    document.getString("abbreviation"),
                    document.getString("course_name"),
                    document.getString("course_section"),
                    document.getString("crn"),
                    document.getString("semester"),
                    document.getString("year")
            );
            courses.add(courseDAO);
        }
        return courses;
    }

    public CourseDAO getCourse(String courseID) {
        Document document = courseCollection.find(eq("course_id", courseID)).first();
        if (document == null) throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This course does not exist.").build());

        CourseDAO courseDAO = new CourseDAO(
            document.getString("abbreviation"),
            document.getString("course_name"),
            document.getString("course_section"),
            document.getString("crn"),
            document.getString("semester"),
            document.getString("year")
        );
        courseDAO.students = document.getList("students", String.class);
        return courseDAO;
    }

    public List<StudentDAO> getAllStudents() {
        List<StudentDAO> students = new ArrayList<>();
        for (Document document : studentCollection.find()) {
            StudentDAO studentDAO = new StudentDAO((String) document.get("student_id"));
            studentDAO.courses = document.getList("courses", String.class);
            students.add(studentDAO);
        }
        return students;
    }

    public StudentDAO getStudent(String studentID) {
        Document document = studentCollection.find(eq("student_id", studentID)).first();
        if (document == null) throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This student does not exist.").build());

        StudentDAO studentDAO = new StudentDAO((String) document.get("student_id"));
        studentDAO.courses = document.getList("courses", String.class);
        return studentDAO;
    }
}