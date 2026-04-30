package hk.hkucs.comp7506_project.model;

public class Course {
    private final int id;
    private final String title;
    private final String description;
    private final String schedule;
    private final int lectureCount;

    public Course(int id, String title, String description, String schedule, int lectureCount) {
        this.id = id;
        this.title = title != null ? title : "Untitled Course";
        this.description = description != null ? description : "";
        this.schedule = schedule != null ? schedule : "";
        this.lectureCount = lectureCount;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSchedule() { return schedule; }
    public int getLectureCount() { return lectureCount; }
}
