package hk.hkucs.comp7506_project.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hk.hkucs.comp7506_project.R;
import hk.hkucs.comp7506_project.model.Course;

public class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
        void onAddCourseClick();
    }

    private static final int TYPE_ADD    = 0;
    private static final int TYPE_COURSE = 1;

    private final List<Course> items;
    private final OnCourseClickListener listener;

    public CourseAdapter(List<Course> items, OnCourseClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemCount() { return items.size() + 1; }

    @Override
    public int getItemViewType(int position) {
        return position == items.size() ? TYPE_ADD : TYPE_COURSE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View v = inflater.inflate(R.layout.item_course_add, parent, false);
            return new AddVH(v);
        }
        View v = inflater.inflate(R.layout.item_course, parent, false);
        return new CourseVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddVH) {
            holder.itemView.setOnClickListener(v -> listener.onAddCourseClick());
            return;
        }

        CourseVH vh = (CourseVH) holder;
        Course c = items.get(position);
        vh.title.setText(c.getTitle());

        int n = c.getLectureCount();
        String label = n == 0 ? "No lectures"
                : (n == 1 ? "1 lecture" : n + " lectures");
        vh.lectureCount.setText(label);

        if (c.getDescription().isEmpty()) {
            vh.description.setVisibility(View.GONE);
        } else {
            vh.description.setVisibility(View.VISIBLE);
            vh.description.setText(c.getDescription());
        }

        if (c.getSchedule().isEmpty()) {
            vh.scheduleRow.setVisibility(View.GONE);
        } else {
            vh.scheduleRow.setVisibility(View.VISIBLE);
            vh.schedule.setText(c.getSchedule());
        }

        vh.itemView.setOnClickListener(v -> listener.onCourseClick(c));
    }

    static class CourseVH extends RecyclerView.ViewHolder {
        final TextView title, description, schedule, lectureCount;
        final LinearLayout scheduleRow;

        CourseVH(View v) {
            super(v);
            title         = v.findViewById(R.id.courseTitle);
            description   = v.findViewById(R.id.courseDescription);
            schedule      = v.findViewById(R.id.courseSchedule);
            lectureCount  = v.findViewById(R.id.courseLectureCount);
            scheduleRow   = v.findViewById(R.id.courseScheduleRow);
        }
    }

    static class AddVH extends RecyclerView.ViewHolder {
        AddVH(View v) { super(v); }
    }
}
