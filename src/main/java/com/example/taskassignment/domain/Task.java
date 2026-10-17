package com.example.taskassignment.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "effort_hours", nullable = false)
    private int effortHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.NOT_DONE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_skills", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "skill", nullable = false, length = 100)
    private Set<String> skills = new HashSet<>();

    public Task() {
    }

    public Task(String name, int effortHours, TaskStatus status, Set<String> skills) {
        this.name = name;
        this.effortHours = effortHours;
        this.status = status != null ? status : TaskStatus.NOT_DONE;
        if (skills != null) {
            this.skills = new HashSet<>(skills);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEffortHours() {
        return effortHours;
    }

    public void setEffortHours(int effortHours) {
        this.effortHours = effortHours;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills != null ? new HashSet<>(skills) : new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

