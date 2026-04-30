package hk.hkucs.comp7506_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import hk.hkucs.comp7506_project.data.DemoDataRepository;
import hk.hkucs.comp7506_project.model.FeatureModule;
import hk.hkucs.comp7506_project.model.ModuleType;

public class MainActivity extends AppCompatActivity {

    private DemoDataRepository repository;
    private TextView selectedModuleTitle;
    private TextView selectedModuleDescription;
    private TextView selectedModuleTasks;
    private TextView mockResultTitle;
    private TextView mockResultBody;
    private TextView avatarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        repository = new DemoDataRepository();
        bindViews();
        bindActions();
        showModule(ModuleType.RECORDING_SUMMARY);
        refreshAvatar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAvatar();
    }

    private void bindViews() {
        selectedModuleTitle = findViewById(R.id.selectedModuleTitle);
        selectedModuleDescription = findViewById(R.id.selectedModuleDescription);
        selectedModuleTasks = findViewById(R.id.selectedModuleTasks);
        mockResultTitle = findViewById(R.id.mockResultTitle);
        mockResultBody = findViewById(R.id.mockResultBody);
        avatarButton = findViewById(R.id.avatarButton);
    }

    private void bindActions() {
        MaterialButton btnRecording = findViewById(R.id.btnRecording);
        MaterialButton btnOcr = findViewById(R.id.btnOcr);
        MaterialButton btnKnowledgeBase = findViewById(R.id.btnKnowledgeBase);
        MaterialButton btnLibrary = findViewById(R.id.btnLibrary);
        FloatingActionButton fabChat = findViewById(R.id.fabChat);

        btnRecording.setOnClickListener(v -> showModule(ModuleType.RECORDING_SUMMARY));
        btnOcr.setOnClickListener(v -> showModule(ModuleType.PHOTO_OCR));
        btnKnowledgeBase.setOnClickListener(v -> showModule(ModuleType.KNOWLEDGE_QA));
        btnLibrary.setOnClickListener(v -> showModule(ModuleType.NOTES_LIBRARY));

        fabChat.setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));

        avatarButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void showModule(ModuleType type) {
        FeatureModule module = repository.getModule(type);
        if (module == null) return;

        selectedModuleTitle.setText(module.getTitle());
        selectedModuleDescription.setText(module.getDescription());
        selectedModuleTasks.setText(formatTasks(module.getNextTasks()));
        mockResultTitle.setText(module.getDemoResult().getTitle());
        mockResultBody.setText(module.getDemoResult().getBody());
        updateModuleButtons(type);
    }

    private void updateModuleButtons(ModuleType selected) {
        styleModuleButton(findViewById(R.id.btnRecording), selected == ModuleType.RECORDING_SUMMARY);
        styleModuleButton(findViewById(R.id.btnOcr), selected == ModuleType.PHOTO_OCR);
        styleModuleButton(findViewById(R.id.btnKnowledgeBase), selected == ModuleType.KNOWLEDGE_QA);
        styleModuleButton(findViewById(R.id.btnLibrary), selected == ModuleType.NOTES_LIBRARY);
    }

    private void styleModuleButton(MaterialButton btn, boolean active) {
        int bg = ContextCompat.getColor(this, active ? R.color.notemind_text_primary : R.color.notemind_card);
        int fg = ContextCompat.getColor(this, active ? R.color.white : R.color.notemind_text_primary);
        int stroke = ContextCompat.getColor(this, active ? R.color.notemind_text_primary : R.color.notemind_hairline);
        btn.setBackgroundTintList(ColorStateList.valueOf(bg));
        btn.setTextColor(fg);
        btn.setStrokeColor(ColorStateList.valueOf(stroke));
    }

    private void refreshAvatar() {
        SharedPreferences prefs = getSharedPreferences("notemind_profile", MODE_PRIVATE);
        String name = prefs.getString("display_name", "");
        String initials = name.isEmpty() ? "U" : String.valueOf(name.charAt(0)).toUpperCase();
        avatarButton.setText(initials);
    }

    private String formatTasks(List<String> tasks) {
        StringBuilder sb = new StringBuilder("Next implementation steps:");
        for (String t : tasks) sb.append("\n- ").append(t);
        return sb.toString();
    }
}
