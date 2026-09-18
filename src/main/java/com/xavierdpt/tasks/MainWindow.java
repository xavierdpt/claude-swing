package com.xavierdpt.tasks;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

public class MainWindow extends JFrame {

    private final TaskRepository repository;
    private final ClaudeCodeLauncher claudeCodeLauncher = new ClaudeCodeLauncher();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField titleField;
    private final JTextArea contentArea;
    private final JTextArea summaryArea;
    private JButton sendToClaudeButton;

    private Integer selectedTaskId;

    public MainWindow(TaskRepository repository) {
        super("Task Manager");
        this.repository = repository;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel(new Object[]{"Id", "Title"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTableSelectionChanged();
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);

        titleField = new JTextField();
        contentArea = new JTextArea();
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        summaryArea = new JTextArea();
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setEditable(false);
        summaryArea.setRows(4);
        summaryArea.setBackground(getContentPane().getBackground());

        JPanel editPanel = new JPanel(new BorderLayout(5, 5));
        JPanel titlePanel = new JPanel(new BorderLayout(5, 5));
        titlePanel.add(new javax.swing.JLabel("Title:"), BorderLayout.WEST);
        titlePanel.add(titleField, BorderLayout.CENTER);
        editPanel.add(titlePanel, BorderLayout.NORTH);
        editPanel.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        JPanel summaryPanel = new JPanel(new BorderLayout(5, 5));
        summaryPanel.add(new javax.swing.JLabel("Summary:"), BorderLayout.NORTH);
        summaryPanel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(summaryPanel, BorderLayout.CENTER);
        editPanel.add(bottomPanel, BorderLayout.SOUTH);

        JButton newButton = new JButton("New");
        JButton saveButton = new JButton("Save");
        JButton deleteButton = new JButton("Delete");
        JButton printButton = new JButton("Print");
        sendToClaudeButton = new JButton("Send to Claude Code");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(newButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(printButton);
        buttonPanel.add(sendToClaudeButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        newButton.addActionListener(this::onNew);
        saveButton.addActionListener(this::onSave);
        deleteButton.addActionListener(this::onDelete);
        printButton.addActionListener(this::onPrint);
        sendToClaudeButton.addActionListener(this::onSendToClaude);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, editPanel);
        splitPane.setDividerLocation(300);

        getContentPane().add(splitPane, BorderLayout.CENTER);

        reloadTasks();
    }

    private void reloadTasks() {
        tableModel.setRowCount(0);
        List<Task> tasks = repository.findAll();
        for (Task task : tasks) {
            tableModel.addRow(new Object[]{task.getId(), task.getTitle()});
        }
    }

    private void onTableSelectionChanged() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Task task = repository.findAll().stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
        if (task == null) {
            return;
        }
        selectedTaskId = task.getId();
        titleField.setText(task.getTitle());
        contentArea.setText(task.getContent());
        summaryArea.setText(task.getSummary());
    }

    private void onNew(ActionEvent e) {
        selectedTaskId = null;
        titleField.setText("");
        contentArea.setText("");
        summaryArea.setText("");
        table.clearSelection();
        titleField.requestFocusInWindow();
    }

    private void onSave(ActionEvent e) {
        String title = titleField.getText().trim();
        String content = contentArea.getText();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedTaskId == null) {
            Task created = repository.insert(title, content);
            selectedTaskId = created.getId();
        } else {
            repository.update(selectedTaskId, title, content);
        }
        reloadTasks();
        selectRow(selectedTaskId);
    }

    private void onDelete(ActionEvent e) {
        if (selectedTaskId == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected task?", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        repository.delete(selectedTaskId);
        onNew(null);
        reloadTasks();
    }

    private void onPrint(ActionEvent e) {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this, "No task selected.", "Nothing to print", JOptionPane.WARNING_MESSAGE);
            return;
        }
        System.out.println("Id: " + selectedTaskId);
        System.out.println("Title: " + titleField.getText());
        System.out.println("Content: " + contentArea.getText());
        System.out.println("Summary: " + summaryArea.getText());
        System.out.println("---");
    }

    private void onSendToClaude(ActionEvent e) {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this, "No task selected.", "Nothing to send", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int taskId = selectedTaskId;
        Task task = new Task(taskId, titleField.getText(), contentArea.getText(), null);

        sendToClaudeButton.setEnabled(false);
        sendToClaudeButton.setText("Working...");
        summaryArea.setText("Claude is working on this task...");

        new SwingWorker<ClaudeCodeLauncher.Result, Void>() {
            @Override
            protected ClaudeCodeLauncher.Result doInBackground() {
                return claudeCodeLauncher.send(task);
            }

            @Override
            protected void done() {
                sendToClaudeButton.setEnabled(true);
                sendToClaudeButton.setText("Send to Claude Code");
                try {
                    ClaudeCodeLauncher.Result result = get();
                    if (result.success) {
                        repository.updateSummary(taskId, result.output);
                        if (selectedTaskId != null && selectedTaskId == taskId) {
                            summaryArea.setText(result.output);
                        }
                        JOptionPane.showMessageDialog(MainWindow.this, result.output,
                                "Claude Code finished", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        summaryArea.setText("");
                        JOptionPane.showMessageDialog(MainWindow.this, result.output,
                                "Failed to send to Claude Code", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    summaryArea.setText("");
                    JOptionPane.showMessageDialog(MainWindow.this, ex.getMessage(),
                            "Failed to send to Claude Code", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void selectRow(int taskId) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if ((int) tableModel.getValueAt(row, 0) == taskId) {
                table.setRowSelectionInterval(row, row);
                break;
            }
        }
    }

    public static void launch(TaskRepository repository) {
        SwingUtilities.invokeLater(() -> new MainWindow(repository).setVisible(true));
    }
}
