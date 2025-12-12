package com.example.honestgaze;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentMonitorAdapter extends RecyclerView.Adapter<StudentMonitorAdapter.ViewHolder> {

    private List<StudentMonitorModel> students;

    public StudentMonitorAdapter(List<StudentMonitorModel> students) {
        this.students = students;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.student_monitor_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentMonitorModel student = students.get(position);
        holder.nameText.setText(student.getName());
        holder.warningsText.setText("Warnings: " + student.getTotalWarnings());
        holder.eventsText.setText("Events: " + student.getEventsCount());
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public void setStudents(List<StudentMonitorModel> students) {
        this.students = students;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, warningsText, eventsText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.studentName);
            warningsText = itemView.findViewById(R.id.studentWarnings);
            eventsText = itemView.findViewById(R.id.studentEvents);
        }
    }
}
