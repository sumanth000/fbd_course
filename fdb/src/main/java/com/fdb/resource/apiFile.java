package com.fdb.resource;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.Map;


import java.security.SecureRandom;
import java.math.BigInteger;


@RestController
@CrossOrigin(origins = "http://localhost:3000",allowCredentials = "true")

public class apiFile {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/check")
    public String getString() {
        String responseString = "running properly";
        return responseString;
    }
    @GetMapping("/getInstructorDetails")
    public List<Map<String, Object>> getInstructorDetails() {
        String sql ="select id,userid,password from person where studentStatus=false";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, Object> userDetailsMap = new HashMap<>();

            // Convert the id to a numeric type instead of treating it as a String
            userDetailsMap.put("id",String.valueOf( row.get("id")));
            userDetailsMap.put("userid", (String) row.get("userid"));
            userDetailsMap.put("password", (String) row.get("password"));



            // userDetailsMap.put("sessionExpiry", (Date) row.get("sessionExpiry"));

            result.add(userDetailsMap);
        }

        return result;
    }

    @GetMapping("/getStudentDetails")
    public  List<Map<String, Object>> getStudentDetails() {

        String sql ="select id,userid,password from person where studentStatus=true";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {


            Map<String, Object> userDetailsMap = new HashMap<>();
            userDetailsMap.put("id",String.valueOf( row.get("id")));
            userDetailsMap.put("userid", (String) row.get("userid"));
            userDetailsMap.put("password", (String) row.get("password"));

//            userDetailsMap.put("sessionExpiry", (Date) row.get("sessionExpiry"));




            result.add(userDetailsMap);
        }

        System.out.println(result);

        return result;
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login( @RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        Map<String, String> responseBody = new HashMap<>();
        SecureRandom random = new SecureRandom();

        Map<String,Object> finding= detailsExistInDb(loginRequest);

        System.out.println(finding);
        if ((Boolean)finding.get("exists")) {

            String sessionId = new BigInteger(130, random).toString(32);


            Cookie sessionCookie = new Cookie("sessionId", sessionId);

                responseBody.put("id",(String)finding.get("id"));

                sessionCookie.setPath("/");

            response.addCookie(sessionCookie);
            responseBody.put("status", "SUCCESS");
            responseBody.put("sessionId", sessionId);
            return ResponseEntity.ok(responseBody);
        } else {
            responseBody.put("status", "FAILURE");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
        }
    }

    public Map<String,Object> detailsExistInDb(LoginRequest loginRequest)
    {
        Map<String,Object> result=new HashMap<>();
        if(loginRequest.getWho().equalsIgnoreCase("student"))
        {
            List<Map<String, Object>> studentDetails=  getStudentDetails();

            for(Map<String,Object> i:studentDetails)

            {
//            System.out.println(i.get("password")+" "+loginRequest.getPassword());
//                System.out.println(i.get("userid")+" "+loginRequest.getUserid());


                if(i.get("password").equals(loginRequest.getPassword()) && i.get("userid").equals(loginRequest.getUserid())
                ){
                    result.put("exists",true);

                    result.put("id",i.get("id"));


                    return result;
                }
            }
        }
        else if(loginRequest.getWho().equalsIgnoreCase("instructor"))
        {
            List<Map<String, Object>> instructorDetails=  getInstructorDetails();

            for(Map<String,Object> i:instructorDetails)
            {
                if(i.get("password").equals(loginRequest.getPassword()) && i.get("userid").equals(loginRequest.getUserid())
                ){
                    result.put("id",i.get("id"));


                    result.put("exists",true);

                    return result;
                }
            }
        }
        result.put("exists",false);
        return result;

    }


    @GetMapping("/getCoursesDetailsBasisPerson")
    public Map<String, Object> getCoursesDetailsBasisPerson( @RequestParam int personId) {


        System.out.println(personId);

        Map<String, Object>result = new HashMap<>();

        result.put("status","FAILURE");
        result.put("programme_status",false);
        result.put("programme_id",null);
        result.put("programme_name",null);
        result.put("programme_level",null);
        result.put("major_status",false);
        result.put("major_id",null);
        result.put("major_name",null);
        result.put("courses_enrolled",false);
        result.put("courses",null);



        String sqlCountofPerson ="SELECT COUNT(*) AS row_count FROM programme_enrollment WHERE person_id = ?";
        int row_count_person_enrollment = jdbcTemplate.queryForObject(sqlCountofPerson, Integer.class,personId);
      System.out.println("row_count_person_enrollment"+row_count_person_enrollment);
       if(row_count_person_enrollment!=0)
       {
          result.put("status","SUCCESS");
           String sqlData= "SELECT pe.person_id,pe.programmeid,pe.major_id,p.programme_name,p.programme_level,m.major_name FROM programme_enrollment pe\n" +
                   " left join programmes p\n" +
                   "on pe.programmeid=p.programmeid\n" +
                   "left join major m\n" +
                   "on pe.major_id=m.major_id\n" +
                   "WHERE person_id = ?";
           List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlData,personId);
           if(rows.get(0).get("programmeid")!=null)
           {
               result.replace("programme_status",true);
               result.replace("programme_id",rows.get(0).get("programmeid"));
               result.replace("programme_name",rows.get(0).get("programme_name"));
               result.replace("programme_level",rows.get(0).get("programme_level"));



           }
           if(rows.get(0).get("major_id")!=null) {
               result.replace("major_status", true);
               result.replace("major_id", rows.get(0).get("major_id"));
               result.replace("major_name", rows.get(0).get("major_name"));


           }


           String sqlCountofCourses ="SELECT COUNT(*) AS row_count FROM student_enrollment WHERE student_id = ?";
           int row_count_batch_enrollment = jdbcTemplate.queryForObject(sqlCountofCourses, Integer.class,personId);

           if(row_count_batch_enrollment!=0)
           {
               result.put("courses_enrolled",true);


                 String sqlCourses= "select * from student_enrollment se\n" +
                         "left join semesters s\n" +
                         "on s.semester_batch_id=se.semester_batch_id\n" +
                         " left join batch b\n" +
                         " on b.batch_id=s.batch_id\n" +
                         " left join instructor_info i\n" +
                         "on i.instructor_id=s.instructor_id\n" +
                         "left join courses c\n" +
                         "on b.course_id=c.course_id\n" +
                         "left join major m\n" +
                         "on c.major_id=m.major_id\n" +
                         "left join programmes p\n" +
                         "on p.programmeid=m.programme_id\n" +
                         "left join academic_grades ag\n" +
                         "on ag.enrollment_id=se.enrollment_id\n" +
                         "where student_id=?";
               List<Map<String, Object>> courses = jdbcTemplate.queryForList(sqlCourses,personId);
               result.replace("courses",courses);


           }
       }


        return result;
    }


    @GetMapping("/majors")
    public List<Map<String, Object>> getMajorProgrammes() {
        String sqlQuery = "SELECT m.*, p.programme_name, p.programme_level " +
                "FROM Major m " +
                "LEFT JOIN Programmes p ON m.programme_id = p.programmeid";
        return jdbcTemplate.queryForList(sqlQuery);
    }
    @PostMapping("/programme-enrollment")
    public ResponseEntity<Object> addProgrammeEnrollment(@RequestBody Map<String, Object> payload) {
        try {
            // Extract data from the payload
            int personId = Integer.parseInt((String) payload.get("person_id"));
            int majorId = Integer.parseInt((String) payload.get("major_id"));
            int programmeId = Integer.parseInt((String) payload.get("programme_id"));

            // Check if the person is already enrolled
            String checkQuery = "SELECT COUNT(*) FROM programme_enrollment WHERE person_id=?";
            int count = jdbcTemplate.queryForObject(checkQuery, Integer.class, personId);
            if (count > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of("message", "Person is already enrolled", "status", HttpStatus.BAD_REQUEST)
                );
            }

            // Insert into the database
            String insertQuery = "INSERT INTO programme_enrollment (person_id, major_id, programmeid) " +
                    "VALUES (?, ?, ?)";
            jdbcTemplate.update(insertQuery, personId, majorId, programmeId);

            // Return success message
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of("message", "Programme enrollment added successfully", "status", HttpStatus.CREATED)
            );
        } catch (Exception e) {
            // Handle any exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Error: " + e.getMessage(), "status", HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @GetMapping("/batches")
    public ResponseEntity<List<Map<String, Object>>> getBatches() {
        try {
            String sqlQuery ="select * from semesters s\n" +
                    " left join batch b\n" +
                    " on b.batch_id=s.batch_id\n" +
                    " left join instructor_info i\n" +
                    "on i.instructor_id=s.instructor_id\n" +
                    " left join courses c\n" +
                    "on c.course_id=b.course_id\n" +
                    "left join major m\n" +
                    " on c.major_id=m.major_id";
            List<Map<String, Object>> batches = jdbcTemplate.queryForList(sqlQuery);
            return ResponseEntity.ok().body(batches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/course-enrollment")
    public ResponseEntity<Map<String, String>> addStudentEnrollment(@RequestBody Map<String, String> payload) {
        try {
            int studentId = Integer.parseInt(payload.get("student_id"));
            int semesterBatchId = Integer.parseInt(payload.get("semester_batch_id"));

            // Initialize response map
            Map<String, String> response = new HashMap<>();

            // Check if the student was in that batch ever before..but what if he fails?
            String countQuery = "SELECT count(*) FROM student_enrollment se\n" +
                    "left join academic_grades ag\n" +
                    "on ag.enrollment_id=se.enrollment_id\n"+
                    "left join semesters s\n" +
                    "on s.semester_batch_id=se.semester_batch_id\n" +
                    "left join batch b\n" +
                    "on b.batch_id=s.batch_id\n" +
                    "WHERE se.student_id =? AND grade!='F' AND se.enrollment_status = 'Enrolled' \n" +
                    "AND s.batch_id in(select batch_id from semesters where semester_batch_id= ?)";
            int count = jdbcTemplate.queryForObject(countQuery, Integer.class, studentId, semesterBatchId);
            if (count > 0) {
                response.put("message", "Student is already enrolled, yet to complete course");
                response.put("status", "BAD_REQUEST");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Get details of the old course(s) that the student is enrolled in
            String oldCourseDetailsQuery = "select semester_name,shift_detail,shift_days,grade from student_enrollment  se\n" +
                    "left join semesters s\n" +
                    "on s.semester_batch_id=se.semester_batch_id\n" +
                    "left join batch b\n" +
                    "on b.batch_id=s.batch_id\n" +
                    "left join academic_grades ag\n" +
                    "on ag.enrollment_id=se.enrollment_id\n" +
                    "where   student_id=? and  \n" +
                    "((se.enrollment_status='Enrolled' and grade!='F') or (se.enrollment_status='Enrolled' and grade is null))";
            List<Map<String, Object>> oldCourseDetails = jdbcTemplate.queryForList(oldCourseDetailsQuery, studentId);

            // Get details of the new course being enrolled
            String newCourseDetailsQuery = "SELECT semester_name, shift_detail, shift_days FROM semesters s " +
                    "LEFT JOIN batch b ON b.batch_id = s.batch_id " +
                    "WHERE semester_batch_id = ?";
            Map<String, Object> newCourseDetails = jdbcTemplate.queryForMap(newCourseDetailsQuery, semesterBatchId);

            // Check for clashes in the schedule
            for (Map<String, Object> oldCourse : oldCourseDetails) {
                if (oldCourse.get("semester_name").equals(newCourseDetails.get("semester_name")) &&
                        oldCourse.get("shift_detail").equals(newCourseDetails.get("shift_detail")) &&
                        oldCourse.get("shift_days").equals(newCourseDetails.get("shift_days"))) {
                    response.put("message", "Clash in schedule: New course schedule matches with existing course");
                    response.put("status", "BAD_REQUEST");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }

            // If no clashes, proceed with enrollment
            String insertQuery = "INSERT INTO student_enrollment (student_id, semester_batch_id, enrollment_status) VALUES (?, ?, 'Enrolled')";
            jdbcTemplate.update(insertQuery, studentId, semesterBatchId);

            response.put("message", "Student enrollment added successfully");
            response.put("status", "CREATED");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: " + e.getMessage());
            response.put("status", "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/update-ratings")
    public ResponseEntity<Map<String, String>> updateRatings(@RequestBody Map<String, String> payload) {
        try {
            // Extract data from the payload
            int instructorId = Integer.parseInt(payload.get("instructor_id"));

            int enrollmentId = Integer.parseInt(payload.get("enrollment_id"));
            double courseRating = Double.parseDouble(payload.get("course_rating"));
            double instructorRating = Double.parseDouble(payload.get("instructor_rating"));

            // Check if the enrollment ID exists
            String enrollmentCheckQuery = "SELECT COUNT(*) FROM student_enrollment WHERE enrollment_id = ?";
            int enrollmentCount = jdbcTemplate.queryForObject(enrollmentCheckQuery, Integer.class, enrollmentId);
            if (enrollmentCount == 0) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Enrollment ID not found");
                response.put("status", "BAD_REQUEST");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Insert into the course_rating table
            String insertCourseRatingQuery = "INSERT INTO course_rating (enrollment_id, course_rating) VALUES (?, ?)";
            jdbcTemplate.update(insertCourseRatingQuery, enrollmentId, courseRating);

            // Insert into the instructor_rating table
            String insertInstructorRatingQuery = "INSERT INTO instructor_rating (enrollment_id, instructor_id,instructor_rating) VALUES (?, ?,?)";
            jdbcTemplate.update(insertInstructorRatingQuery, enrollmentId,instructorId, instructorRating);

            // Return success message
            Map<String, String> response = new HashMap<>();
            response.put("message", "Ratings inserted successfully");
            response.put("status", "CREATED");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NumberFormatException e) {
            // Handle invalid number format exception
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invalid rating format");
            response.put("status", "BAD_REQUEST");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            // Handle other exceptions
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: " + "Already Rated");
            response.put("status", "INTERNAL_SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/check-instructor-enrollment")
    public ResponseEntity<Map<String, String>> checkInstructorEnrollment(@RequestParam int instructorId) {
        Map<String, String> response = new HashMap<>();
        try {
            // Query to check if the instructor_id exists in the instructor_enrollment table
            String countQuery = "SELECT COUNT(*) FROM instructor_enrollment WHERE instructor_id = ?";
            int count = jdbcTemplate.queryForObject(countQuery, Integer.class, instructorId);

            if (count > 0) {
                // Query to fetch the programme_id associated with the instructor_id
                String programmeQuery = "SELECT programme_id FROM instructor_enrollment WHERE instructor_id = ?";
                int programmeId = jdbcTemplate.queryForObject(programmeQuery, Integer.class, instructorId);

                // Populate response map with the programme_id
                response.put("programme_id", String.valueOf(programmeId));
                response.put("status", "true");
            } else {
                response.put("status", "false");
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            // Handle any exceptions
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/instructor-students")
    public ResponseEntity<List<Map<String, Object>>> getInstructorStudents(@RequestParam int instructorId) {
        try {
            // Query to retrieve all columns for students based on the instructor's ID
            String query = "select * from student_enrollment se\n" +
                    "left join semesters s\n" +
                    "on s.semester_batch_id =se.semester_batch_id\n" +
                    "left join batch b\n" +
                    "on b.batch_id=s.batch_id\n" +
                    "left join courses c\n" +
                    "on c.course_id=b.course_id\n" +
                    "left join students st\n" +
                    "on st.student_id=se.student_id\n" +
                    "left join academic_grades ag\n" +
                    "on ag.enrollment_id=se.enrollment_id\n" +
                    "where s.instructor_id=?";
            List<Map<String, Object>> students = jdbcTemplate.queryForList(query, instructorId);

            return ResponseEntity.status(HttpStatus.OK).body(students);
        } catch (Exception e) {
            // Handle any exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }



    @GetMapping("/batches-instructor")
    public ResponseEntity<List<Map<String, Object>>> getBatchesForInstructor() {
        try {
            String sqlQuery ="select * from batch b\n" +
                    "    left join courses c\n" +
                    "    on c.course_id=b.course_id\n" +
                    "    left join major m\n" +
                    "    on c.major_id=m.major_id\n" +
                    "    left join programmes p\n" +
                    "    on m.programme_id=p.programmeid";
            List<Map<String, Object>> batches = jdbcTemplate.queryForList(sqlQuery);
            return ResponseEntity.ok().body(batches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/activating-batch")
    public ResponseEntity<Map<String, String>> activateBatch(@RequestBody Map<String, String> payload) {
        Map<String, String> response = new HashMap<>();
        try {
            // Extract data from the payload
            int batchId = Integer.parseInt(payload.get("batch_id"));
            int instructorId = Integer.parseInt(payload.get("instructor_id"));
            String semesterName = payload.get("semester_name");

            // Check if the batch is already activated
            String checkQuery = "SELECT COUNT(*) FROM Semesters WHERE batch_id = ? AND semester_name = ?";
            int count = jdbcTemplate.queryForObject(checkQuery, Integer.class, batchId, semesterName);
            if (count > 0) {
                // Batch is already activated, return error message
                response.put("message", "Batch is already activated");
                response.put("status", "false");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Activate the batch by inserting into the Semesters table
            String insertQuery = "INSERT INTO Semesters (batch_id, instructor_id, semester_name) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertQuery, batchId, instructorId, semesterName);

            // Populate response map with success message
            response.put("message", "Batch activated successfully");
            response.put("status", "true");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // Handle any exceptions
            response.put("error", e.getMessage());
            response.put("status", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/insert-academic-grade")
    public ResponseEntity<Map<String, String>> insertAcademicGrade(@RequestBody Map<String, Object> payload) {
        Map<String, String> response = new HashMap<>();
        try {
            // Extract data from the payload
            int enrollmentId = (int) payload.get("enrollment_id");
            String grade = (String) payload.get("grade");

            // Check if the entry exists
            String checkQuery = "SELECT COUNT(*) FROM academic_grades WHERE enrollment_id = ?";
            int count = jdbcTemplate.queryForObject(checkQuery, Integer.class, enrollmentId);

            if (count > 0) {
                // If the entry exists, update it
                String updateQuery = "UPDATE academic_grades SET grade = ? WHERE enrollment_id = ?";
                jdbcTemplate.update(updateQuery, grade, enrollmentId);
                response.put("message", "Academic grade updated successfully");
            } else {
                // If the entry doesn't exist, insert it
                String insertQuery = "INSERT INTO academic_grades (enrollment_id, grade) VALUES (?, ?)";
                jdbcTemplate.update(insertQuery, enrollmentId, grade);
                response.put("message", "Academic grade added successfully");
            }

            response.put("status", "success");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // Handle any exceptions
            response.put("message", "Error: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }







}

class LoginRequest{

    public String getUserid() {
        return userId;
    }

    public void setUser_id(String user_id) {
        this.userId = user_id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String userId;
    public String password;

    public String who;

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }
}