package com.xavierdpt.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Runs a task through the Claude Code CLI (not the Anthropic API) and waits
 * for it to finish so its final reply can be stored as the task's summary.
 *
 * <p>Uses the {@code claude} binary from the PATH by default. Set the
 * {@code CLAUDE_CLI_PATH} environment variable to point at a specific
 * install instead. The Claude Code config directory (credentials, settings)
 * is whatever the current environment already provides (e.g. via
 * {@code CLAUDE_CONFIG_DIR}); this class does not override it.
 */
public class ClaudeCodeLauncher {

    private static final String DEFAULT_CLAUDE_COMMAND = "claude";

    public static final class Result {
        public final boolean success;
        public final String output;

        private Result(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }

    /**
     * Runs a non-interactive Claude Code session for the given task and blocks
     * until it finishes, returning its final reply as the task summary.
     */
    public Result send(Task task) {
        String claudeCommand = resolveClaudeCommand();
        String prompt = "Task #" + task.getId() + ": " + task.getTitle() + "\n\n" + task.getContent()
                + "\n\nWhen you are done, reply with a concise summary (a few sentences) of what you did. "
                + "That reply will be stored as the task's summary, so do not include anything else in it.";

        ProcessBuilder builder = new ProcessBuilder(
                claudeCommand,
                "--print",
                "--output-format", "text",
                "--permission-mode", "auto",
                "--model", "sonnet",
                "--effort", "medium",
                prompt
        );
        builder.redirectErrorStream(true);
        builder.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));

        try {
            Process process = builder.start();
            String output = readAll(process);
            int exitCode = process.waitFor();
            return new Result(exitCode == 0, output.strip());
        } catch (IOException e) {
            return new Result(false, "Failed to start Claude Code: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, "Interrupted while starting Claude Code.");
        }
    }

    private static String resolveClaudeCommand() {
        String override = System.getenv("CLAUDE_CLI_PATH");
        return (override != null && !override.isBlank()) ? override : DEFAULT_CLAUDE_COMMAND;
    }

    private static String readAll(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
