package hk.hkucs.comp7506_project.model;

public class NoteRecord {
    private final int id;
    private final String sourceType;
    private final String title;
    private final String content;
    private final String createdAt;

    public NoteRecord(int id, String sourceType, String title, String content, String createdAt) {
        this.id = id;
        this.sourceType = sourceType != null ? sourceType : "note";
        this.title = title != null ? title : "Untitled";
        this.content = content != null ? content : "";
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public int getId() { return id; }
    public String getSourceType() { return sourceType; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }

    public String getSourceInitial() {
        switch (sourceType) {
            case "recording": return "R";
            case "ocr":       return "O";
            case "ppt":       return "P";
            default:          return "N";
        }
    }

    public String getSourceLabel() {
        switch (sourceType) {
            case "recording": return "Recording";
            case "ocr":       return "OCR";
            case "ppt":       return "PPT / PDF";
            default:          return "Note";
        }
    }
}
