package hk.hkucs.comp7506_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID          = "id";
    public static final String EXTRA_TITLE       = "title";
    public static final String EXTRA_CONTENT     = "content";
    public static final String EXTRA_SOURCE_TYPE = "source_type";
    public static final String EXTRA_CREATED_AT  = "created_at";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        Intent intent = getIntent();
        String title      = intent.getStringExtra(EXTRA_TITLE);
        String content    = intent.getStringExtra(EXTRA_CONTENT);
        String sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE);
        String createdAt  = intent.getStringExtra(EXTRA_CREATED_AT);

        if (title == null)      title      = "Note";
        if (content == null)    content    = "";
        if (sourceType == null) sourceType = "note";
        if (createdAt == null)  createdAt  = "";

        // Toolbar
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        TextView sourceChip   = findViewById(R.id.noteSourceChip);

        btnBack.setOnClickListener(v -> finish());
        toolbarTitle.setText(title);
        sourceChip.setText(sourceLabel(sourceType));

        // Body
        TextView detailTitle = findViewById(R.id.noteDetailTitle);
        TextView metaInfo    = findViewById(R.id.noteMetaInfo);
        TextView noteContent = findViewById(R.id.noteContent);

        detailTitle.setText(title);
        String dateStr = createdAt.length() > 10 ? createdAt.substring(0, 10) : createdAt;
        metaInfo.setText(sourceLabel(sourceType) + "  ·  " + dateStr);
        noteContent.setText(content);
    }

    private String sourceLabel(String type) {
        switch (type) {
            case "recording": return "Recording";
            case "ocr":       return "OCR";
            case "ppt":       return "PPT / PDF";
            default:          return "Note";
        }
    }
}
