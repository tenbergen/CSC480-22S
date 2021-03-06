package edu.oswego.cs.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.and;

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

    public List<Document> getAllCourses(SecurityContext securityContext) {
        String professorID = securityContext.getUserPrincipal().getName().split("@")[0];
        MongoCursor<Document> query = courseCollection.find(eq("professor_id", professorID)).iterator();
        List<Document> courses = new ArrayList<>();
        while (query.hasNext()) {
            Document document = query.next();
            courses.add(document);
        }
        return courses;
    }

    public Document getCourse(SecurityContext securityContext, String courseID) {
        String professorID = securityContext.getUserPrincipal().getName().split("@")[0];
        Document document = courseCollection.find(and(eq("course_id", courseID), eq("professor_id", professorID))).first();
        if (document == null)
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("This course does not exist.").build());
        return document;
    }

    public List<Document> getStudentCourses(String studentID) {
        Document studentDocument = studentCollection.find(eq("student_id", studentID)).first();
        if (studentDocument == null)
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("This student does not exist.").build());

        List<String> courses = studentDocument.getList("courses", String.class);
        List<Document> courseDocuments = new ArrayList<>();
        for (String course : courses) {
            Document courseDocument = courseCollection.find(eq("course_id", course)).first();
            courseDocuments.add(courseDocument);
        }
        return courseDocuments;
    }

    public List<Document> getAllStudents() {
        MongoCursor<Document> query = studentCollection.find().iterator();
        List<Document> students = new ArrayList<>();
        while (query.hasNext()) {
            Document document = query.next();
            students.add(document);
        }
        query.close();
        return students;
    }

    public Document getStudent(SecurityContext securityContext, String studentID) {
        Document document = studentCollection.find(eq("student_id", studentID)).first();
        if (document == null)
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("This student does not exist.").build());
        if (securityContext.isUserInRole("student")) document.remove("courses");
        return document;
    }

    

    public List<Document> getStudentsInCourse(SecurityContext securityContext, String courseID) {
        String professorID = securityContext.getUserPrincipal().getName().split("@")[0];
        Document courseDocument = courseCollection.find(and(eq("course_id", courseID), eq("professor_id", professorID))).first();
        if (courseDocument == null) throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        List<String> studentIDs = courseDocument.getList("students", String.class);
        return studentIDs.stream()
                .map(id -> studentCollection.find(eq("student_id", id)).first())
                .collect(Collectors.toList());
    }
}