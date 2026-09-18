# claude-swing

A small Java Swing desktop app for managing a task list backed by a local
SQLite database, with an optional button to hand a task off to the
[Claude Code](https://claude.com/claude-code) CLI and have it work on it
autonomously.

## Features

- Create, edit, and delete tasks (`id`, `title`, `content`, `summary`)
  stored in a SQLite file.
- **Print**: dump the selected task to stdout.
- **Send to Claude Code**: runs the task through the `claude` CLI
  (not the Anthropic API) in non-interactive, auto-approval mode, waits
  for it to finish, and stores its final reply as the task's `summary`.

## Requirements

- JDK 21+
- Maven 3.9+
- The [Claude Code CLI](https://claude.com/claude-code) installed and
  logged in, with the `claude` binary available on your `PATH`
  (only needed for the "Send to Claude Code" button).

## Build

```bash
mvn package
```

This produces a self-contained jar at `target/task-manager.jar`
(dependencies, including the SQLite JDBC driver, are bundled via the
Shade plugin).

## Run

```bash
java -jar target/task-manager.jar [path/to/database.db]
```

If no path is given, a `tasks.db` file is created in the current working
directory. The table (`id`, `title`, `content`, `summary`) is created
automatically on first run, and existing databases are migrated in place
if they predate the `summary` column.

Alternatively, run directly with Maven:

```bash
mvn exec:java
```

## Configuration

The "Send to Claude Code" button shells out to the `claude` binary. By
default it looks for `claude` on `PATH`; to point at a specific install
instead, set:

```bash
export CLAUDE_CLI_PATH=/path/to/claude
```

Claude Code's own configuration (login, config directory, etc.) is left
untouched — whatever the CLI would normally use in your environment
(e.g. `CLAUDE_CONFIG_DIR`) applies here too.

Each run sends the task's title and content as the prompt, with
`--permission-mode auto --model sonnet --effort medium`, and asks it to
conclude with a concise summary, which is then saved back to the task.

## License

MIT — see [LICENSE](LICENSE).
