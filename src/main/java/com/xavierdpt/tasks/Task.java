package com.xavierdpt.tasks;

public class Task {
    private final int id;
    private String title;
    private String content;
    private String summary;

    public Task(int id, String title, String content, String summary) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.summary = summary;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
