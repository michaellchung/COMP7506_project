package hk.hkucs.comp7506_project.model;

public class AiResult {
    private final String title;
    private final String body;

    public AiResult(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}
