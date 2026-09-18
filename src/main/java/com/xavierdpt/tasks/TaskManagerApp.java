package com.xavierdpt.tasks;

import java.nio.file.Path;

public class TaskManagerApp {

    public static void main(String[] args) {
        Path dbFile = Path.of(args.length > 0 ? args[0] : "tasks.db");
        TaskRepository repository = new TaskRepository(dbFile);
        MainWindow.launch(repository);
    }
}
