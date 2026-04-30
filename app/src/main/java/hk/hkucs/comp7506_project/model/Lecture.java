package hk.hkucs.comp7506_project.model;

public class Lecture {
    private final int id;
    private final int courseId;
    private final int lectureNumber;
    private final String title;
    private final String lectureDate;
    private final int noteCount;

    public Lecture(int id, int courseId, int lectureNumber, String title, String lectureDate, int noteCount) {
        this.id = id;
        this.courseId = courseId;
        this.lectureNumber = lectureNumber;
        this.title = title != null ? title : "Lecture " + lectureNumber;
        this.lectureDate = lectureDate != null ? lectureDate : "";
        this.noteCount = noteCount;
    }

    public int getId() { return id; }
    public int getCourseId() { return courseId; }
    public int getLectureNumber() { return lectureNumber; }
    public String getTitle() { return title; }
    public String getLectureDate() { return lectureDate; }
    public int getNoteCount() { return noteCount; }
}
