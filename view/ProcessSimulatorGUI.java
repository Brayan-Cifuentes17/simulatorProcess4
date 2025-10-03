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

    // Campos de Procesos
    private JTextField txtProcessName;
    private JTextField txtProcessTime;
    private JTextField txtProcessSize;
    private JComboBox<String> cmbStatus;
    private JComboBox<Partition> cmbPartition;

    // Campos de Particiones
    private JTextField txtPartitionName;
    private JTextField txtPartitionSize;

    // Tablas
    private DefaultTableModel processTableModel;
    private JTable processTable;
    private DefaultTableModel partitionTableModel;
    private JTable partitionTable;

    // Panel de resultados
    private JPanel resultsPanel;
    private CardLayout cardLayout;

    private DefaultTableModel[] resultTableModels;
    private String[] tableNames = {
            "Inicial", "Listo", "Despachar", "En Ejecución",
            "Expiración de Tiempo", "Espera de E/S", "Bloqueado", 
            "Terminación de operación E/S", "Salidas",
            "Particiones", "Finalización de Particiones", "No Ejecutados"
    };

    private Filter[] filters = {
            Filter.INICIAL, Filter.LISTO, Filter.DESPACHAR, Filter.EN_EJECUCION,
            Filter.TIEMPO_EXPIRADO, Filter.TRANSICION_BLOQUEO, Filter.BLOQUEADO, 
            Filter.DESPERTAR, Filter.FINALIZADO,
            Filter.PARTICIONES, Filter.FINALIZACION_PARTICIONES, Filter.NO_EJECUTADO
    };

    private String currentAction;
    private NumberFormat numberFormatter;
    
    // ComboBox para filtro de particiones en Listo
    private JComboBox<String> cmbPartitionFilter;

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

        // Campos de Procesos
        txtProcessName = new JTextField(15);
        txtProcessTime = new JTextField(15);
        txtProcessSize = new JTextField(15);
        cmbStatus = new JComboBox<>(new String[] { "No bloqueado", "Bloqueado" });
        cmbPartition = new JComboBox<>();

        setupTimeField(txtProcessTime);
        setupTimeField(txtProcessSize);

        // Campos de Particiones
        txtPartitionName = new JTextField(15);
        txtPartitionSize = new JTextField(15);
        setupTimeField(txtPartitionSize);

        // Tabla de Procesos
        processTableModel = new DefaultTableModel(
                new String[] { "Nombre", "Tiempo", "Estado", "Tamaño", "Partición" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Tabla de Particiones
        partitionTableModel = new DefaultTableModel(
                new String[] { "Nombre", "Tamaño", "Procesos Asignados" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        partitionTable = new JTable(partitionTableModel);
        partitionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Panel de resultados
        cardLayout = new CardLayout();
        resultsPanel = new JPanel(cardLayout);

        resultTableModels = new DefaultTableModel[tableNames.length];
        for (int i = 0; i < tableNames.length; i++) {
            // Tabla especial para Finalización de Particiones
            if (i == 10) { // FINALIZACION_PARTICIONES
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Partición", "Tamaño", "Procesos Asignados", "Tiempo Total" },
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            } else {
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Proceso", "Tiempo Restante", "Estado", "Tamaño", "Partición", "Ciclos" },
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }

            JTable table = new JTable(resultTableModels[i]);
            table.setFont(new Font("Arial", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(table);
            resultsPanel.add(scrollPane, tableNames[i]);
        }
        
        // ComboBox para filtro de particiones
        cmbPartitionFilter = new JComboBox<>();
        cmbPartitionFilter.addItem("Todas las particiones");
    }

    private void setupTimeField(JTextField textField) {
        textField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }

                if (Character.isDigit(c)) {
                    SwingUtilities.invokeLater(() -> {
                        formatTimeFieldInRealTime(textField);
                    });
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                formatTimeFieldInRealTime(textField);
            }
        });
    }

    private void formatTimeFieldInRealTime(JTextField textField) {
        String text = textField.getText().replaceAll("[^0-9]", "");
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

                if (!textField.getText().equals(displayText)) {
                    int caretPos = textField.getCaretPosition();
                    textField.setText(displayText);
                    try {
                        int newCaretPos = Math.min(caretPos + (displayText.length() - text.length()),
                                displayText.length());
                        textField.setCaretPosition(newCaretPos);
                    } catch (IllegalArgumentException ex) {
                        textField.setCaretPosition(displayText.length());
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

                    if (!textField.getText().equals(formatted.toString())) {
                        int caretPos = textField.getCaretPosition();
                        textField.setText(formatted.toString());
                        try {
                            textField.setCaretPosition(Math.min(caretPos, formatted.length()));
                        } catch (IllegalArgumentException ex2) {
                            textField.setCaretPosition(formatted.length());
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

    private long parseTimeField(JTextField field) throws NumberFormatException {
        return parseTimeWithTrick(field.getText());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("SIMULADOR DE PROCESOS CON PARTICIONES");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Panel izquierdo con pestañas
        JTabbedPane leftTabbedPane = new JTabbedPane();
        leftTabbedPane.setPreferredSize(new Dimension(500, 0));

        // Pestaña de Procesos
        JPanel processesTab = createProcessesTab();
        leftTabbedPane.addTab("Procesos", processesTab);

        // Pestaña de Particiones
        JPanel partitionsTab = createPartitionsTab();
        leftTabbedPane.addTab("Particiones", partitionsTab);

        // Panel derecho (resultados)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Resultados de la Simulación"));

        JPanel buttonPanel = createResultButtonPanel();
        rightPanel.add(buttonPanel, BorderLayout.NORTH);

        rightPanel.add(resultsPanel, BorderLayout.CENTER);

        // Panel de acciones globales
        JPanel globalActionsPanel = createGlobalActionsPanel();

        add(titlePanel, BorderLayout.NORTH);
        add(leftTabbedPane, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        add(globalActionsPanel, BorderLayout.SOUTH);
    }

    private JPanel createProcessesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = createProcessFormPanel();
        panel.add(formPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(processTable);
        tableScrollPane.setPreferredSize(new Dimension(480, 200));
        panel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel actionPanel = createProcessActionPanel();
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPartitionsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = createPartitionFormPanel();
        panel.add(formPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(partitionTable);
        tableScrollPane.setPreferredSize(new Dimension(480, 200));
        panel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel actionPanel = createPartitionActionPanel();
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProcessFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Proceso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

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
        panel.add(new JLabel("Tamaño:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessSize, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbStatus, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Partición:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbPartition, gbc);

        return panel;
    }

    private JPanel createPartitionFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Crear Nueva Partición"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPartitionName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Tamaño:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPartitionSize, gbc);

        return panel;
    }

    private JPanel createProcessActionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JButton btnAdd = new JButton("Agregar");
        JButton btnEdit = new JButton("Modificar");
        JButton btnDelete = new JButton("Eliminar");

        Dimension buttonSize = new Dimension(140, 35);
        btnAdd.setPreferredSize(buttonSize);
        btnEdit.setPreferredSize(buttonSize);
        btnDelete.setPreferredSize(buttonSize);

        btnAdd.addActionListener(e -> addProcess());
        btnEdit.addActionListener(e -> editProcess());
        btnDelete.addActionListener(e -> deleteProcess());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(btnAdd, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(btnEdit, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(btnDelete, gbc);

        return panel;
    }

    private JPanel createPartitionActionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JButton btnAdd = new JButton("Agregar");
        JButton btnDelete = new JButton("Eliminar");

        Dimension buttonSize = new Dimension(140, 35);
        btnAdd.setPreferredSize(buttonSize);
        btnDelete.setPreferredSize(buttonSize);

        btnAdd.addActionListener(e -> addPartition());
        btnDelete.addActionListener(e -> deletePartition());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(btnAdd, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(btnDelete, gbc);

        return panel;
    }

    private JPanel createGlobalActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton btnSimulate = new JButton("Ejecutar Simulación");
        JButton btnReset = new JButton("Limpiar Todo");
        JButton btnManual = new JButton("Manual de usuario");
        JButton btnExit = new JButton("Salir");

        Dimension buttonSize = new Dimension(180, 40);
        btnSimulate.setPreferredSize(buttonSize);
        btnReset.setPreferredSize(buttonSize);
        btnManual.setPreferredSize(buttonSize);
        btnExit.setPreferredSize(buttonSize);

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

        btnSimulate.addActionListener(e -> runSimulation());
        btnReset.addActionListener(e -> clearAll());
        btnManual.addActionListener(e -> openUserManual());
        btnExit.addActionListener(e -> System.exit(0));

        panel.add(btnSimulate);
        panel.add(btnReset);
        panel.add(btnManual);
        panel.add(btnExit);

        return panel;
    }

    private JPanel createResultButtonPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new GridLayout(3, 4, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (int i = 0; i < tableNames.length; i++) {
            JButton btn = new JButton(tableNames[i]);
            btn.setPreferredSize(new Dimension(120, 30));
            final int index = i;
            btn.addActionListener(e -> {
                cardLayout.show(resultsPanel, tableNames[index]);
                updateResultTable(index);
            });
            buttonPanel.add(btn);
        }
        
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        
        // Panel de filtro para Listo
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filtrar por Partición:"));
        filterPanel.add(cmbPartitionFilter);
        
        JButton btnApplyFilter = new JButton("Aplicar Filtro");
        btnApplyFilter.addActionListener(e -> applyPartitionFilter());
        filterPanel.add(btnApplyFilter);
        
        mainPanel.add(filterPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void setupEventHandlers() {
        // Eventos ya manejados en los métodos create...
    }

    // ========== GESTIÓN DE PARTICIONES ==========

    private void addPartition() {
        String name = txtPartitionName.getText().trim();
        String sizeText = txtPartitionSize.getText().trim();

        if (name.isEmpty()) {
            showError("El nombre de la partición no puede estar vacío");
            return;
        }

        if (processManager.partitionExists(name)) {
            showError("Ya existe una partición con ese nombre");
            return;
        }

        try {
            long size = parseTimeField(txtPartitionSize);
            if (size <= 0) {
                showError("El tamaño debe ser mayor a 0");
                return;
            }

            processManager.addPartition(name, size);
            updatePartitionTable();
            updatePartitionComboBox();
            updatePartitionFilterComboBox();
            clearPartitionForm();
            showInfo("Partición agregada exitosamente");

        } catch (NumberFormatException ex) {
            showError("Ingrese un tamaño válido");
        }
    }

    private void deletePartition() {
        int selectedRow = partitionTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione una partición para eliminar");
            return;
        }

        String partitionName = (String) partitionTableModel.getValueAt(selectedRow, 0);
        
        if (processManager.hasPartitionAssignedProcesses(partitionName)) {
            showError("No se puede eliminar la partición '" + partitionName + 
                     "' porque tiene procesos asignados");
            return;
        }

        currentAction = "DELETE_PARTITION:" + partitionName;
        new CustomDialog(this, "¿Está seguro de que desea eliminar la partición '" + partitionName + "'?",
                CustomDialog.CONFIRM_TYPE);
    }

    private void updatePartitionTable() {
        partitionTableModel.setRowCount(0);

        for (Partition p : processManager.getPartitions()) {
            String formattedSize = numberFormatter.format(p.getSize());
            partitionTableModel.addRow(new Object[] {
                    p.getName(),
                    formattedSize,
                    p.getProcessCount()
            });
        }
    }

    private void updatePartitionComboBox() {
        cmbPartition.removeAllItems();
        for (Partition p : processManager.getPartitions()) {
            cmbPartition.addItem(p);
        }
    }
    
    private void updatePartitionFilterComboBox() {
        cmbPartitionFilter.removeAllItems();
        cmbPartitionFilter.addItem("Todas las particiones");
        for (Partition p : processManager.getPartitions()) {
            cmbPartitionFilter.addItem(p.getName());
        }
    }

    private void clearPartitionForm() {
        txtPartitionName.setText("");
        txtPartitionSize.setText("");
    }

    // ========== GESTIÓN DE PROCESOS ==========

    private void addProcess() {
        String name = txtProcessName.getText().trim();

        if (name.isEmpty()) {
            showError("El nombre del proceso no puede estar vacío");
            return;
        }

        if (processManager.processExists(name)) {
            showError("Ya existe un proceso con ese nombre");
            return;
        }

        try {
            long time = parseTimeField(txtProcessTime);
            if (time <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return;
            }

            long size = parseTimeField(txtProcessSize);
            if (size <= 0) {
                showError("El tamaño debe ser mayor a 0");
                return;
            }

            Status status = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;
            Partition partition = (Partition) cmbPartition.getSelectedItem();

            if (partition == null) {
                showError("Debe seleccionar una partición");
                return;
            }

            processManager.addProcess(name, time, status, size, partition);

            updateProcessTable();
            updatePartitionTable();
            clearProcessForm();
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
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);

        dialog.setLayout(new GridBagLayout());
        dialog.setSize(450, 450);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(44, 62, 80));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);

        JLabel titleLabel = new JLabel("Modificar Proceso");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 10, 8, 10);

        JTextField txtEditName = new JTextField(process.getName(), 20);
        txtEditName.setEditable(false);
        txtEditName.setBackground(Color.LIGHT_GRAY);
        txtEditName.setFont(new Font("Arial", Font.PLAIN, 14));

        JTextField txtEditTime = new JTextField(numberFormatter.format(process.getOriginalTime()), 20);
        txtEditTime.setFont(new Font("Arial", Font.PLAIN, 14));
        setupTimeField(txtEditTime);

        JTextField txtEditSize = new JTextField(numberFormatter.format(process.getSize()), 20);
        txtEditSize.setFont(new Font("Arial", Font.PLAIN, 14));
        setupTimeField(txtEditSize);

        JComboBox<String> cmbEditStatus = new JComboBox<>(new String[] { "No bloqueado", "Bloqueado" });
        cmbEditStatus.setSelectedIndex(process.isBlocked() ? 1 : 0);
        cmbEditStatus.setFont(new Font("Arial", Font.PLAIN, 14));

        JComboBox<Partition> cmbEditPartition = new JComboBox<>();
        for (Partition p : processManager.getPartitions()) {
            cmbEditPartition.addItem(p);
        }
        cmbEditPartition.setSelectedItem(process.getPartition());
        cmbEditPartition.setFont(new Font("Arial", Font.PLAIN, 14));

        int row = 1;
        addDialogComponentStyled(mainPanel, gbc, "Nombre:", txtEditName, row++);
        addDialogComponentStyled(mainPanel, gbc, "Tiempo:", txtEditTime, row++);
        addDialogComponentStyled(mainPanel, gbc, "Tamaño:", txtEditSize, row++);
        addDialogComponentStyled(mainPanel, gbc, "Estado:", cmbEditStatus, row++);
        addDialogComponentStyled(mainPanel, gbc, "Partición:", cmbEditPartition, row++);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(44, 62, 80));

        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");

        Dimension buttonSize = new Dimension(100, 35);
        btnSave.setPreferredSize(buttonSize);
        btnCancel.setPreferredSize(buttonSize);

        btnSave.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSave.setBackground(Color.WHITE);
        btnSave.setForeground(new Color(44, 62, 80));
        btnSave.setFocusPainted(false);

        btnCancel.setFont(new Font("Arial", Font.PLAIN, 14));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setForeground(new Color(44, 62, 80));
        btnCancel.setFocusPainted(false);

        btnSave.addActionListener(e -> {
            if (saveEditedProcess(dialog, process, selectedRow, txtEditTime, txtEditSize,
                    cmbEditStatus, cmbEditPartition)) {
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

        dialog.add(mainPanel);

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
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);
    }

    private boolean saveEditedProcess(JDialog dialog, model.Process originalProcess, int selectedRow,
            JTextField txtTime, JTextField txtSize, JComboBox<String> cmbStatus,
            JComboBox<Partition> cmbPartition) {
        try {
            long newTime = parseTimeField(txtTime);
            if (newTime <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return false;
            }

            long newSize = parseTimeField(txtSize);
            if (newSize <= 0) {
                showError("El tamaño debe ser mayor a 0");
                return false;
            }

            Status newStatus = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;
            Partition newPartition = (Partition) cmbPartition.getSelectedItem();

            if (newPartition == null) {
                showError("Debe seleccionar una partición");
                return false;
            }

            processManager.editProcess(selectedRow, originalProcess.getName(), newTime, newStatus,
                    newSize, newPartition);

            updateProcessTable();
            updatePartitionTable();
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

    private void updateProcessTable() {
        processTableModel.setRowCount(0);

        for (model.Process p : processManager.getInitialProcesses()) {
            String formattedTime = numberFormatter.format(p.getOriginalTime());
            String formattedSize = numberFormatter.format(p.getSize());
            String partitionName = p.getPartition() != null ? p.getPartition().getName() : "Sin partición";

            processTableModel.addRow(new Object[] {
                    p.getName(),
                    formattedTime,
                    p.getStatusString(),
                    formattedSize,
                    partitionName
            });
        }
    }

    private void clearProcessForm() {
        txtProcessName.setText("");
        txtProcessTime.setText("");
        txtProcessSize.setText("");
        cmbStatus.setSelectedIndex(0);
        if (cmbPartition.getItemCount() > 0) {
            cmbPartition.setSelectedIndex(0);
        }
    }

    // ========== SIMULACIÓN ==========

    private void runSimulation() {
        if (processManager.isEmpty()) {
            showError("No hay procesos para simular");
            return;
        }

        if (processManager.getPartitions().isEmpty()) {
            showError("No hay particiones creadas");
            return;
        }

        processManager.runSimulation();

        for (int i = 0; i < tableNames.length; i++) {
            updateResultTable(i);
        }

        cardLayout.show(resultsPanel, tableNames[0]);
        showInfo("Simulación ejecutada exitosamente");
    }

    private void updateResultTable(int tableIndex) {
        // Tabla especial para Particiones (índice 9)
        if (tableIndex == 9) {
            resultTableModels[9].setRowCount(0);
            for (Partition p : processManager.getPartitions()) {
                String formattedSize = numberFormatter.format(p.getSize());
                resultTableModels[9].addRow(new Object[] {
                        p.getName(),
                        formattedSize,
                        p.getProcessCount(),
                        ""
                });
            }
            return;
        }

        // Tabla especial para Finalización de Particiones (índice 10)
        if (tableIndex == 10) {
            resultTableModels[10].setRowCount(0);
            List<ProcessManager.PartitionFinalizationInfo> report = processManager.getPartitionFinalizationReport();
            
            for (ProcessManager.PartitionFinalizationInfo info : report) {
                String formattedSize = numberFormatter.format(info.getSize());
                String formattedTime = numberFormatter.format(info.getTotalTime());
                
                resultTableModels[10].addRow(new Object[] {
                        info.getName(),
                        formattedSize,
                        info.getProcessCount(),
                        formattedTime
                });
            }
            return;
        }

        // Tabla especial para Inicial (índice 0)
        if (tableIndex == 0) {
            resultTableModels[0].setRowCount(0);
            for (model.Process p : processManager.getInitialProcesses()) {
                String formattedTime = numberFormatter.format(p.getOriginalTime());
                String formattedSize = numberFormatter.format(p.getSize());
                String partitionName = p.getPartition() != null ? p.getPartition().getName() : "Sin partición";

                resultTableModels[0].addRow(new Object[] {
                        p.getName(),
                        formattedTime,
                        p.getStatusString(),
                        formattedSize,
                        partitionName,
                        0
                });
            }
            return;
        }

        // Resto de tablas (logs)
        List<Log> logs = processManager.getLogsByFilter(filters[tableIndex]);
        resultTableModels[tableIndex].setRowCount(0);

        for (Log log : logs) {
            String formattedTime = numberFormatter.format(log.getRemainingTime());
            String formattedSize = numberFormatter.format(log.getSize());
            String partitionName = log.getPartition() != null ? log.getPartition().getName() : "Sin partición";

            resultTableModels[tableIndex].addRow(new Object[] {
                    log.getProcessName(),
                    formattedTime,
                    log.getStatusString(),
                    formattedSize,
                    partitionName,
                    log.getCycleCount()
            });
        }
    }

    private void applyPartitionFilter() {
        String selectedPartition = (String) cmbPartitionFilter.getSelectedItem();
        
        if (selectedPartition == null || selectedPartition.equals("Todas las particiones")) {
            // Mostrar todos los logs de LISTO
            updateResultTable(1); // índice 1 es LISTO
        } else {
            // Filtrar por partición específica
            resultTableModels[1].setRowCount(0);
            List<Log> logs = processManager.getLogsByFilterAndPartition(Filter.LISTO, selectedPartition);
            
            for (Log log : logs) {
                String formattedTime = numberFormatter.format(log.getRemainingTime());
                String formattedSize = numberFormatter.format(log.getSize());

                resultTableModels[1].addRow(new Object[] {
                        log.getProcessName(),
                        formattedTime,
                        log.getStatusString(),
                        formattedSize,
                        log.getPartitionName(),
                        log.getCycleCount()
                });
            }
        }
        
        // Asegurarse de mostrar la tabla de Listo
        cardLayout.show(resultsPanel, tableNames[1]);
    }

    // ========== UTILIDADES ==========

    private void clearAll() {
        currentAction = "CLEAR_ALL";
        new CustomDialog(this, "¿Está seguro de que desea eliminar todos los datos?", CustomDialog.CONFIRM_TYPE);
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
                            "Manual_Usuario.pdf");
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
            case view.Constants.CLOSE_WARNING:
            case view.Constants.CLOSE_INFO:
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;

            case view.Constants.CONFIRM_YES:
                handleConfirmYes();
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;

            case view.Constants.CONFIRM_NO:
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
                updatePartitionTable();
                showInfo("Proceso eliminado");
            } else if (currentAction.startsWith("DELETE_PARTITION:")) {
                String partitionName = currentAction.substring("DELETE_PARTITION:".length());
                processManager.removePartition(partitionName);
                updatePartitionTable();
                updatePartitionComboBox();
                updatePartitionFilterComboBox();
                showInfo("Partición eliminada");
            } else if (currentAction.equals("CLEAR_ALL")) {
                processManager.clearAll();
                updateProcessTable();
                updatePartitionTable();
                updatePartitionComboBox();
                updatePartitionFilterComboBox();

                for (DefaultTableModel model : resultTableModels) {
                    model.setRowCount(0);
                }

                clearProcessForm();
                clearPartitionForm();
                showInfo("Todos los datos han sido eliminados");
            }
            currentAction = null;
        }
    }
}