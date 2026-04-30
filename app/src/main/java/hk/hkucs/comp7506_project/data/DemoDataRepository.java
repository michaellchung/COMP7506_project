package hk.hkucs.comp7506_project.data;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import hk.hkucs.comp7506_project.model.AiResult;
import hk.hkucs.comp7506_project.model.FeatureModule;
import hk.hkucs.comp7506_project.model.ModuleType;

public class DemoDataRepository {
    private final Map<ModuleType, FeatureModule> modules = new EnumMap<>(ModuleType.class);

    public DemoDataRepository() {
        modules.put(
                ModuleType.RECORDING_SUMMARY,
                new FeatureModule(
                        ModuleType.RECORDING_SUMMARY,
                        "Recording Summary",
                        "Upload English lecture audio and generate transcript, key outline, terminology, and possible exam hotspots.",
                        Arrays.asList(
                                "Connect Android file picker or recorder",
                                "Send audio metadata/file to Flask backend",
                                "Display transcript and summary returned by AI"
                        ),
                        new AiResult(
                                "Sample Lecture Summary",
                                "Topic: Dynamic Programming\nKey idea: Break a problem into overlapping subproblems and store intermediate answers.\nExam hotspot: Explain optimal substructure with a small example."
                        )
                )
        );

        modules.put(
                ModuleType.PHOTO_OCR,
                new FeatureModule(
                        ModuleType.PHOTO_OCR,
                        "Photo-to-Text OCR",
                        "Capture slides, whiteboards, or textbook pages and convert them into structured notes.",
                        Arrays.asList(
                                "Request camera/gallery permissions",
                                "Upload image to backend OCR endpoint",
                                "Let users edit and save recognized text"
                        ),
                        new AiResult(
                                "Sample OCR Note",
                                "Recognized text: Greedy algorithms choose the locally optimal option at each step.\nStructured note: Definition, examples, limitations."
                        )
                )
        );

        modules.put(
                ModuleType.KNOWLEDGE_QA,
                new FeatureModule(
                        ModuleType.KNOWLEDGE_QA,
                        "Personal Knowledge Base Q&A",
                        "Vectorize recordings, OCR captures, and notes so answers are grounded in the student's own materials.",
                        Arrays.asList(
                                "Save every processed note into SQLite",
                                "Create embedding/vectorization pipeline on backend",
                                "Return answers with source snippets for demo trustworthiness"
                        ),
                        new AiResult(
                                "Sample Grounded Answer",
                                "Based on your Week 4 lecture notes, dynamic programming is useful when subproblems repeat and the optimal solution can be built from smaller optimal solutions."
                        )
                )
        );

        modules.put(
                ModuleType.NOTES_LIBRARY,
                new FeatureModule(
                        ModuleType.NOTES_LIBRARY,
                        "Notes Library",
                        "Keep generated transcripts, OCR notes, summaries, and Q&A history in one searchable study space.",
                        Arrays.asList(
                                "Design local list/detail screens",
                                "Sync note records from Flask SQLite API",
                                "Add tags such as course, week, and source type"
                        ),
                        new AiResult(
                                "Sample Library Items",
                                "COMP7506 Week 2 Recording Summary\nCOMP7506 Slide OCR: Android Activity Lifecycle\nExam Review Q&A: Dynamic Programming"
                        )
                )
        );
    }

    public FeatureModule getModule(ModuleType type) {
        return modules.get(type);
    }

    public String answerQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "Try asking a question about your lecture notes, slides, or recordings.";
        }

        return "Demo answer based on personal notes: \"" + question.trim()
                + "\" is related to the uploaded course materials. Once the backend is connected, this area will show a DeepSeek-generated answer grounded in retrieved note snippets.";
    }
}
