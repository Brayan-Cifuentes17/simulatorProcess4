package view;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProcessSimulatorGUI extends JFrame implements ActionListener {
    private ProcessManager processManager;

    private JTextField txtProcessName;
    private JTextField txtProcessTime;
    private JComboBox<String> cmbStatus;
    private JComboBox<String> cmbSuspendedReady;
    private JComboBox<String> cmbSuspendedBlocked;
    private JComboBox<String> cmbResumed;

    private DefaultTableModel processTableModel;
    private JTable processTable;

    private JPanel resultsPanel;
    private CardLayout cardLayout;

    private DefaultTableModel[] resultTableModels;
    private String[] tableNames = {
            "Inicial", "Listo", "Despachar", "En Ejecución",
            "Expiracion de Tiempo","Espera de E/S", "Bloqueado", "<html>Terminacion de operacion<br>E/S o evento-De Bloqueo a Listo</html>", "Salidas",
            "<html>Suspender de Ejecucion <br> a Suspendido Listo</html>", "Suspendido Listo", "Reanudar a listo", "De Listo a Suspendido Listo",
            "<html>Suspender de bloqueado <br> a suspendido bloqueado</html>", "Suspendido Bloqueado", "Reanudar a Bloqueado",
            "<html>Term.de operacion E/S o evento <br> de Susp.bloqueado a Susp.Listo</html>"
    };

    private Filter[] filters = {
            Filter.INICIAL, Filter.LISTO, Filter.DESPACHADO, Filter.EN_EJECUCION,
            Filter.TIEMPO_EXPIRADO,Filter.BLOQUEAR, Filter.BLOQUEADO, Filter.DESPERTAR, Filter.FINALIZADO,
            Filter.SUSPENDER_LISTOS, Filter.SUSPENDIDO_LISTO, Filter.REANUDAR_LISTOS,Filter.DE_LISTO_A_SUSPENDIDO,
            Filter.SUSPENDER_BLOQUEADOS, Filter.SUSPENDIDO_BLOQUEADO, Filter.REANUDAR_BLOQUEADOS,
            Filter.TRANSICION_BLOQUEADO_A_LISTO
    };

    private String currentAction;
    private NumberFormat numberFormatter;

    public ProcessSimulatorGUI() {
        processManager = new ProcessManager();
        numberFormatter = NumberFormat.getNumberInstance(new Locale("es", "ES"));
        initializeComponents();
        setupLayout();
        setupEventHandlers();

        setUndecorated(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void initializeComponents() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        txtProcessName = new JTextField(15);
        txtProcessTime = new JTextField(15);
        cmbStatus = new JComboBox<>(new String[] { "No Bloqueado", "Bloqueado" });
        cmbSuspendedReady = new JComboBox<>(new String[] { "No", "Si" });
        cmbSuspendedBlocked = new JComboBox<>(new String[] { "No", "Si" });
        cmbResumed = new JComboBox<>(new String[] { "No", "Si" });

        setupTimeField();

        processTableModel = new DefaultTableModel(
                new String[] { "Nombre", "Tiempo", "Estado", "Suspendido Listo", "Suspendido Bloqueado", "Reanudado" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cardLayout = new CardLayout();
        resultsPanel = new JPanel(cardLayout);

        resultTableModels = new DefaultTableModel[tableNames.length];
        for (int i = 0; i < tableNames.length; i++) {
            resultTableModels[i] = new DefaultTableModel(
                    new String[] { "Proceso", "Tiempo Restante", "Estado", "Suspendido Listo", "Suspendido Bloqueado",
                            "Reanudado", "Ciclos" },
                    0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable table = new JTable(resultTableModels[i]);
            table.setFont(new Font("Arial", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(table);
            resultsPanel.add(scrollPane, tableNames[i]);
        }
    }

    private void setupTimeField() {
        txtProcessTime.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }

                if (Character.isDigit(c)) {
                    SwingUtilities.invokeLater(() -> {
                        formatTimeFieldInRealTime();
                    });
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                formatTimeFieldInRealTime();
            }
        });
    }

    private void formatTimeFieldInRealTime() {
        String text = txtProcessTime.getText().replaceAll("[^0-9]", "");
        if (!text.isEmpty()) {
            try {
                String displayText = text;

                if (text.length() > 18) {
                    StringBuilder formatted = new StringBuilder();
                    int count = 0;
                    for (int i = displayText.length() - 1; i >= 0; i--) {
                        if (count > 0 && count % 3 == 0) {
                            formatted.insert(0, ".");
                        }
                        formatted.insert(0, displayText.charAt(i));
                        count++;
                    }
                    displayText = formatted.toString();
                } else {
                    long number = Long.parseLong(text);
                    displayText = numberFormatter.format(number);
                }

                if (!txtProcessTime.getText().equals(displayText)) {
                    int caretPos = txtProcessTime.getCaretPosition();
                    txtProcessTime.setText(displayText);
                    try {
                        int newCaretPos = Math.min(caretPos + (displayText.length() - text.length()),
                                displayText.length());
                        txtProcessTime.setCaretPosition(newCaretPos);
                    } catch (IllegalArgumentException ex) {
                        txtProcessTime.setCaretPosition(displayText.length());
                    }
                }
            } catch (NumberFormatException ex) {
                if (text.length() > 0) {
                    StringBuilder formatted = new StringBuilder();
                    int count = 0;
                    for (int i = text.length() - 1; i >= 0; i--) {
                        if (count > 0 && count % 3 == 0) {
                            formatted.insert(0, ".");
                        }
                        formatted.insert(0, text.charAt(i));
                        count++;
                    }

                    if (!txtProcessTime.getText().equals(formatted.toString())) {
                        int caretPos = txtProcessTime.getCaretPosition();
                        txtProcessTime.setText(formatted.toString());
                        try {
                            txtProcessTime.setCaretPosition(Math.min(caretPos, formatted.length()));
                        } catch (IllegalArgumentException ex2) {
                            txtProcessTime.setCaretPosition(formatted.length());
                        }
                    }
                }
            }
        }
    }

    private long parseTimeWithTrick(String timeText) throws NumberFormatException {
        String numbersOnly = timeText.replaceAll("[^0-9]", "");
        if (numbersOnly.isEmpty()) {
            throw new NumberFormatException("Campo vacío");
        }

        if (numbersOnly.length() > 18) {
            numbersOnly = numbersOnly.substring(0, 18);
        }

        while (numbersOnly.length() > 1) {
            try {
                long result = Long.parseLong(numbersOnly);
                return result;
            } catch (NumberFormatException ex) {
                numbersOnly = numbersOnly.substring(1);
            }
        }

        return Long.parseLong(numbersOnly);
    }

    private long parseTimeField() throws NumberFormatException {
        return parseTimeWithTrick(txtProcessTime.getText());
    }

    private long parseTimeFieldForDialog(JTextField timeField) throws NumberFormatException {
        return parseTimeWithTrick(timeField.getText());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("SIMULADOR DE PROCESOS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(450, 0));

        JPanel formPanel = createFormPanel();
        leftPanel.add(formPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(processTable);
        tableScrollPane.setPreferredSize(new Dimension(430, 250));
        leftPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel actionPanel = createActionPanel();
        leftPanel.add(actionPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Resultados de la Simulación"));

        JPanel buttonPanel = createResultButtonPanel();
        rightPanel.add(buttonPanel, BorderLayout.NORTH);

        rightPanel.add(resultsPanel, BorderLayout.CENTER);

        add(titlePanel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Proceso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessName, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Tiempo:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessTime, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbStatus, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Suspendido Listo:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbSuspendedReady, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Suspendido Bloqueado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbSuspendedBlocked, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Reanudado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbResumed, gbc);

        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JButton btnAdd = new JButton("Agregar");
        JButton btnEdit = new JButton("Modificar");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnSimulate = new JButton("Ejecutar Simulación");
        JButton btnReset = new JButton("Limpiar Todo");
        JButton btnExit = new JButton("Salir");
        JButton btnManual = new JButton("Manual de usuario");

        Dimension buttonSize = new Dimension(140, 35);
        btnAdd.setPreferredSize(buttonSize);
        btnEdit.setPreferredSize(buttonSize);
        btnDelete.setPreferredSize(buttonSize);
        btnSimulate.setPreferredSize(buttonSize);
        btnReset.setPreferredSize(buttonSize);
        btnExit.setPreferredSize(buttonSize);
        btnManual.setPreferredSize(buttonSize);

        btnSimulate.setBackground(new Color(46, 125, 50));
        btnSimulate.setForeground(Color.WHITE);
        btnSimulate.setOpaque(true);
        btnSimulate.setBorderPainted(false);
        btnSimulate.setFocusPainted(false);

        btnExit.setBackground(new Color(198, 40, 40));
        btnExit.setForeground(Color.WHITE);
        btnExit.setOpaque(true);
        btnExit.setBorderPainted(false);
        btnExit.setFocusPainted(false);

        btnAdd.addActionListener(e -> addProcess());
        btnEdit.addActionListener(e -> editProcess());
        btnDelete.addActionListener(e -> deleteProcess());
        btnSimulate.addActionListener(e -> runSimulation());
        btnExit.addActionListener(e -> System.exit(0));
        btnManual.addActionListener(e -> openUserManual());
        btnReset.addActionListener(e -> clearAll());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(btnAdd, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(btnEdit, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(btnDelete, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(btnSimulate, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(btnManual, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(btnReset, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(btnExit, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(Box.createRigidArea(new Dimension(0, 0)), gbc);

        return panel;
    }

    private JPanel createResultButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 4, 5, 5));

        for (int i = 0; i < tableNames.length; i++) {
            JButton btn = new JButton(tableNames[i]);
            btn.setPreferredSize(new Dimension(120, 30));
            final int index = i;
            btn.addActionListener(e -> {
                cardLayout.show(resultsPanel, tableNames[index]);
                updateResultTable(index);
            });
            panel.add(btn);
        }

        return panel;
    }

    private void setupEventHandlers() {

    }

    private void addProcess() {
        String name = txtProcessName.getText().trim();
        String timeText = txtProcessTime.getText().trim();

        if (name.isEmpty()) {
            showError("El nombre del proceso no puede estar vacío");
            return;
        }

        if (processManager.processExists(name)) {
            showError("Ya existe un proceso con ese nombre");
            return;
        }

        try {
            long time = parseTimeField();
            if (time <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return;
            }

            Status status = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;

            Status suspendedReady = cmbSuspendedReady.getSelectedIndex() == 0 ? Status.NO_SUSPENDIDO_LISTO
                    : Status.SUSPENDIDO_LISTO;

            Status suspendedBlocked = cmbSuspendedBlocked.getSelectedIndex() == 0 ? Status.NO_SUSPENDIDO_BLOQUEADO
                    : Status.SUSPENDIDO_BLOQUEADO;

            Status resumed = cmbResumed.getSelectedIndex() == 0 ? Status.NO_REANUDADO : Status.REANUDADO;

            if (resumed == Status.REANUDADO &&
                    suspendedReady == Status.NO_SUSPENDIDO_LISTO &&
                    suspendedBlocked == Status.NO_SUSPENDIDO_BLOQUEADO) {
                showError("Un proceso no puede ser reanudado sin estar suspendido");
                return;
            }
            if (suspendedBlocked == Status.SUSPENDIDO_BLOQUEADO && status == Status.NO_BLOQUEADO) {
                showError("Un proceso no puede marcarse como Suspendido Bloqueado si su estado es No Bloqueado");
                return;
            }

            processManager.addProcess(name, time, status, suspendedReady, suspendedBlocked, resumed);

            updateProcessTable();
            clearForm();
            showInfo("Proceso agregado exitosamente");

        } catch (NumberFormatException ex) {
            showError("Ingrese valores numéricos válidos");
        }
    }

    private void editProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para modificar");
            return;
        }

        String oldName = (String) processTableModel.getValueAt(selectedRow, 0);
        model.Process selectedProcess = null;

        for (model.Process p : processManager.getInitialProcesses()) {
            if (p.getName().equals(oldName)) {
                selectedProcess = p;
                break;
            }
        }

        if (selectedProcess == null)
            return;

        JDialog editDialog = createEditDialog(selectedProcess, selectedRow);
        editDialog.setVisible(true);
    }

    private JDialog createEditDialog(model.Process process, int selectedRow) {
        JDialog dialog = new JDialog(this, "Modificar Proceso", true);

        // Aplicar el mismo estilo que CustomDialog
        dialog.setUndecorated(true); // Quitar la decoración del marco
        dialog.setAlwaysOnTop(true); // Mantener arriba como los otros diálogos

        dialog.setLayout(new GridBagLayout());
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);

        // Crear panel principal con el mismo color de fondo que CustomDialog
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(44, 62, 80)); // Mismo color que CustomDialog
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);

        // Título del diálogo
        JLabel titleLabel = new JLabel("Modificar Proceso");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(titleLabel, gbc);

        // Resetear configuración para los campos
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 10, 8, 10);

        // Campos del formulario
        JTextField txtEditName = new JTextField(process.getName(), 20);
        txtEditName.setEditable(false);
        txtEditName.setBackground(Color.LIGHT_GRAY);
        txtEditName.setFont(new Font("Arial", Font.PLAIN, 14));

        JTextField txtEditTime = new JTextField(String.valueOf(process.getOriginalTime()), 20);
        txtEditTime.setFont(new Font("Arial", Font.PLAIN, 14));

        JComboBox<String> cmbEditStatus = new JComboBox<>(new String[] { "No Bloqueado", "Bloqueado" });
        cmbEditStatus.setSelectedIndex(process.isBlocked() ? 1 : 0);
        cmbEditStatus.setFont(new Font("Arial", Font.PLAIN, 14));

        JComboBox<String> cmbEditSuspendedReady = new JComboBox<>(new String[] { "No", "Si" });
        cmbEditSuspendedReady.setSelectedIndex(process.isSuspendedReady() ? 1 : 0);
        cmbEditSuspendedReady.setFont(new Font("Arial", Font.PLAIN, 14));

        JComboBox<String> cmbEditSuspendedBlocked = new JComboBox<>(new String[] { "No", "Si" });
        cmbEditSuspendedBlocked.setSelectedIndex(process.isSuspendedBlocked() ? 1 : 0);
        cmbEditSuspendedBlocked.setFont(new Font("Arial", Font.PLAIN, 14));

        JComboBox<String> cmbEditResumed = new JComboBox<>(new String[] { "No", "Si" });
        cmbEditResumed.setSelectedIndex(process.isResumed() ? 1 : 0);
        cmbEditResumed.setFont(new Font("Arial", Font.PLAIN, 14));

        // Agregar componentes al panel principal
        int row = 1;
        addDialogComponentStyled(mainPanel, gbc, "Nombre:", txtEditName, row++);
        addDialogComponentStyled(mainPanel, gbc, "Tiempo:", txtEditTime, row++);
        addDialogComponentStyled(mainPanel, gbc, "Estado:", cmbEditStatus, row++);
        addDialogComponentStyled(mainPanel, gbc, "Suspendido Listo:", cmbEditSuspendedReady, row++);
        addDialogComponentStyled(mainPanel, gbc, "Suspendido Bloqueado:", cmbEditSuspendedBlocked, row++);
        addDialogComponentStyled(mainPanel, gbc, "Reanudado:", cmbEditResumed, row++);

        // Panel de botones con el mismo estilo
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(44, 62, 80)); // Mismo fondo

        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");

        Dimension buttonSize = new Dimension(100, 35);
        btnSave.setPreferredSize(buttonSize);
        btnCancel.setPreferredSize(buttonSize);

        // Estilo de botones similar al CustomDialog
        btnSave.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSave.setBackground(Color.WHITE);
        btnSave.setForeground(new Color(44, 62, 80));
        btnSave.setFocusPainted(false);

        btnCancel.setFont(new Font("Arial", Font.PLAIN, 14));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setForeground(new Color(44, 62, 80));
        btnCancel.setFocusPainted(false);

        btnSave.addActionListener(e -> {
            if (saveEditedProcess(dialog, process, selectedRow, txtEditTime,
                    cmbEditStatus, cmbEditSuspendedReady,
                    cmbEditSuspendedBlocked, cmbEditResumed)) {
                dialog.dispose();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        mainPanel.add(buttonPanel, gbc);

        // Agregar el panel principal al diálogo
        dialog.add(mainPanel);

        // Manejador de teclas para Enter y Escape
        dialog.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnSave.doClick();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        dialog.setFocusable(true);
        dialog.requestFocus();

        return dialog;
    }

    private void addDialogComponentStyled(JPanel panel, GridBagConstraints gbc, String labelText, JComponent component,
            int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(140, 25));
        label.setForeground(Color.WHITE); // Texto blanco para contraste
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);
    }

    private void addDialogComponent(JDialog dialog, GridBagConstraints gbc, String label, JComponent component,
            int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;

        JLabel lblComponent = new JLabel(label);
        lblComponent.setPreferredSize(new Dimension(140, 25));
        dialog.add(lblComponent, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(component, gbc);
    }

    private boolean saveEditedProcess(JDialog dialog, model.Process originalProcess, int selectedRow,
            JTextField txtTime, JComboBox<String> cmbStatus,
            JComboBox<String> cmbSuspendedReady, JComboBox<String> cmbSuspendedBlocked,
            JComboBox<String> cmbResumed) {
        try {
            long newTime = parseTimeFieldForDialog(txtTime);
            if (newTime <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return false;
            }

            Status newStatus = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;
            Status newSuspendedReady = cmbSuspendedReady.getSelectedIndex() == 0 ? Status.NO_SUSPENDIDO_LISTO
                    : Status.SUSPENDIDO_LISTO;
            Status newSuspendedBlocked = cmbSuspendedBlocked.getSelectedIndex() == 0 ? Status.NO_SUSPENDIDO_BLOQUEADO
                    : Status.SUSPENDIDO_BLOQUEADO;
            Status newResumed = cmbResumed.getSelectedIndex() == 0 ? Status.NO_REANUDADO : Status.REANUDADO;

            if (newResumed == Status.REANUDADO &&
                    newSuspendedReady == Status.NO_SUSPENDIDO_LISTO &&
                    newSuspendedBlocked == Status.NO_SUSPENDIDO_BLOQUEADO) {
                showError("Un proceso no puede ser reanudado sin estar suspendido");
                return false;
            }
            if (newSuspendedBlocked == Status.SUSPENDIDO_BLOQUEADO && newStatus == Status.NO_BLOQUEADO) {
                showError("Un proceso no puede marcarse como Suspendido Bloqueado si su estado es No Bloqueado");
                return false;
            }

            processManager.editProcess(selectedRow, originalProcess.getName(), newTime, newStatus,
                    newSuspendedReady, newSuspendedBlocked, newResumed);

            updateProcessTable();
            showInfo("Proceso editado exitosamente");
            return true;

        } catch (NumberFormatException ex) {
            showError("Ingrese valores numéricos válidos");
            return false;
        }
    }

    private void deleteProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para eliminar");
            return;
        }

        String processName = (String) processTableModel.getValueAt(selectedRow, 0);
        currentAction = "DELETE_PROCESS:" + processName;
        new CustomDialog(this, "¿Está seguro de que desea eliminar el proceso '" + processName + "'?",
                CustomDialog.CONFIRM_TYPE);
    }

    private void runSimulation() {
        if (processManager.isEmpty()) {
            showError("No hay procesos para simular");
            return;
        }

        processManager.runSimulation();

        for (int i = 0; i < tableNames.length; i++) {
            updateResultTable(i);
        }

        cardLayout.show(resultsPanel, tableNames[0]);
        showInfo("Simulación ejecutada exitosamente");
    }

    private void updateProcessTable() {
        processTableModel.setRowCount(0);

        for (model.Process p : processManager.getInitialProcesses()) {
            String formattedTime = numberFormatter.format(p.getOriginalTime());

            processTableModel.addRow(new Object[] {
                    p.getName(),
                    formattedTime,
                    p.getStatusString(),
                    p.getSuspendedReadyString(),
                    p.getSuspendedBlockedString(),
                    p.getResumedString()
            });
        }
    }

    private void updateResultTable(int tableIndex) {
        if (tableIndex == 0) {

            resultTableModels[0].setRowCount(0);
            for (model.Process p : processManager.getInitialProcesses()) {
                String formattedTime = numberFormatter.format(p.getOriginalTime());

                resultTableModels[0].addRow(new Object[] {
                        p.getName(),
                        formattedTime,
                        p.getStatusString(),
                        p.getSuspendedReadyString(),
                        p.getSuspendedBlockedString(),
                        p.getResumedString(),
                        0
                });
            }
        } else {

            List<Log> logs = processManager.getLogsByFilter(filters[tableIndex]);
            resultTableModels[tableIndex].setRowCount(0);

            for (Log log : logs) {
                String formattedTime = numberFormatter.format(log.getRemainingTime());

                resultTableModels[tableIndex].addRow(new Object[] {
                        log.getProcessName(),
                        formattedTime,
                        log.getStatusString(),
                        log.getSuspendedReadyString(),
                        log.getSuspendedBlockedString(),
                        log.getResumedString(),
                        log.getCycleCount()
                });
            }
        }
    }

    private void clearAll() {
        currentAction = "CLEAR_ALL";
        new CustomDialog(this, "¿Está seguro de que desea eliminar todos los procesos?", CustomDialog.CONFIRM_TYPE);
    }

    private void clearForm() {
        txtProcessName.setText("");
        txtProcessTime.setText("");
        cmbStatus.setSelectedIndex(0);
        cmbSuspendedReady.setSelectedIndex(0);
        cmbSuspendedBlocked.setSelectedIndex(0);
        cmbResumed.setSelectedIndex(0);
    }

    private void openUserManual() {
        try {
            File manualFile = new File("Manual_Usuario.pdf");

            if (!manualFile.exists()) {
                showError("No se encontró el archivo del manual de usuario.<br>" +
                        "Asegúrese de que el archivo 'Manual_Usuario.pdf'<br>" +
                        "esté en la misma carpeta que el programa.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(manualFile);
                } else {
                    showError("Su sistema no permite abrir archivos PDF automáticamente.<br>" +
                            "Por favor, abra manualmente el archivo:<br>" +
                            "Manual_Usuario_Simulador_Procesos.pdf");
                }
            } else {
                showError("Su sistema no permite abrir archivos automáticamente.<br>" +
                        "Por favor, abra manualmente el archivo:<br>" +
                        manualFile.getAbsolutePath());
            }

        } catch (IOException ex) {
            showError("Error al abrir el manual de usuario:<br>" + ex.getMessage());
        } catch (Exception ex) {
            showError("Error inesperado al abrir el manual:<br>" + ex.getMessage());
        }
    }

    private void showError(String message) {
        new CustomDialog(this, message, CustomDialog.WARNING_TYPE);
    }

    private void showInfo(String message) {
        new CustomDialog(this, message, CustomDialog.INFO_TYPE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case Constants.CLOSE_WARNING:
            case Constants.CLOSE_INFO:
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;

            case Constants.CONFIRM_YES:
                handleConfirmYes();
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;

            case Constants.CONFIRM_NO:
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;
        }
    }

    private void handleConfirmYes() {
        if (currentAction != null) {
            if (currentAction.startsWith("DELETE_PROCESS:")) {
                String processName = currentAction.substring("DELETE_PROCESS:".length());
                processManager.removeProcess(processName);
                updateProcessTable();
                showInfo("Proceso eliminado");
            } else if (currentAction.equals("CLEAR_ALL")) {
                processManager.clearAll();
                updateProcessTable();

                for (DefaultTableModel model : resultTableModels) {
                    model.setRowCount(0);
                }

                clearForm();
                showInfo("Todos los datos han sido eliminados");
            }
            currentAction = null;
        }
    }
}