package com.nikonhacker.gui.component.breakTrigger;

import com.nikonhacker.Format;
import com.nikonhacker.emu.trigger.BreakTrigger;
import com.nikonhacker.emu.trigger.condition.MemoryValueBreakCondition;
import com.nikonhacker.gui.component.cpu.CPUStateComponent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class BreakTriggerEditDialog extends JDialog {
    private final CPUStateComponent cpuStateComponent;
    private final BreakTrigger trigger;
    private final JTextField nameField;
    private final JCheckBox enableLogCheckBox;
    private final JCheckBox enableBreakCheckBox;
    private final JTextField interruptToTriggerField;
    private final JTextField pcToSetField;

    public BreakTriggerEditDialog(JDialog owner, final BreakTrigger trigger, String title) {
        super(owner, title, true);
        this.trigger = trigger;

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Name : "));
        nameField = new JTextField(20);
        nameField.setText(trigger.getName());
        topPanel.add(nameField);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        // CPU condition
        cpuStateComponent = new CPUStateComponent(trigger.getCpuStateValues(), trigger.getCpuStateFlags(), true);
        cpuStateComponent.refresh();
        tabbedPane.addTab("CPU conditions", null, cpuStateComponent);

        // Memory conditions
        tabbedPane.addTab("Memory conditions", null, new MemoryConditionListComponent());

        // Resulting actions
        JPanel actionPanel = new JPanel(new MigLayout());

        actionPanel.add(new JLabel("Enable break"));
        enableBreakCheckBox = new JCheckBox();
        enableBreakCheckBox.setSelected(trigger.getMustBreak());
        actionPanel.add(enableBreakCheckBox, "wrap");

        actionPanel.add(new JLabel("Enable log"));
        enableLogCheckBox = new JCheckBox();
        enableLogCheckBox.setSelected(trigger.getMustBeLogged());
        actionPanel.add(enableLogCheckBox, "wrap");

        actionPanel.add(new JLabel("Trigger interrupt  0x"));
        interruptToTriggerField = new JTextField(10);
        if (trigger.getInterruptToRequest() != null) {
            interruptToTriggerField.setText(Format.asHex(trigger.getInterruptToRequest(), 2));
        }
        actionPanel.add(interruptToTriggerField, "wrap");

        actionPanel.add(new JLabel("Jump to address 0x"));
        pcToSetField = new JTextField(10);
        if (trigger.getPcToSet() != null) {
            pcToSetField.setText(Format.asHex(trigger.getPcToSet(), 8));
        }
        actionPanel.add(pcToSetField, "wrap");

        tabbedPane.addTab("Actions", null, actionPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        bottomPanel.add(okButton);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);


        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private void save() {
        trigger.setName(nameField.getText());
        cpuStateComponent.saveValuesAndFlags();
        trigger.setMustBeLogged(enableLogCheckBox.isSelected());
        trigger.setMustBreak(enableBreakCheckBox.isSelected());
        trigger.setInterruptToRequest(StringUtils.isBlank(interruptToTriggerField.getText())?null:Format.parseIntHexField(interruptToTriggerField));
        trigger.setPcToSet(StringUtils.isBlank(pcToSetField.getText())?null:Format.parseIntHexField(pcToSetField));
        dispose();
    }

    private class MemoryConditionListComponent extends JComponent {
        final JList memoryConditionJList;

        public MemoryConditionListComponent() {
            setLayout(new BorderLayout());
            final List<MemoryValueBreakCondition> conditions = trigger.getMemoryValueBreakConditions();

            memoryConditionJList = new JList(createListModel(conditions));
            memoryConditionJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            memoryConditionJList.setLayoutOrientation(JList.VERTICAL);
            memoryConditionJList.setVisibleRowCount(10);
            JScrollPane listScroller = new JScrollPane(memoryConditionJList);
            listScroller.setPreferredSize(new Dimension(250, 300));
            add(listScroller, BorderLayout.CENTER);

            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new GridLayout(3, 1));

            JButton addButton = new JButton("Add");
            addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addCondition();
                }
            });
            rightPanel.add(addButton);

            JButton editButton = new JButton("Edit");
            editButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            editButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editCondition(memoryConditionJList.getSelectedIndex());
                }
            });
            rightPanel.add(editButton);

            JButton deleteButton = new JButton("Delete");
            deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            deleteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteCondition(memoryConditionJList.getSelectedIndex());
                }
            });
            rightPanel.add(deleteButton);

            add(rightPanel, BorderLayout.EAST);
        }

        private void editCondition(int index) {
            if (index != -1) {
                editCondition(trigger.getMemoryValueBreakConditions().get(index));
                memoryConditionJList.setSelectedIndex(index);
            }
        }

        private DefaultListModel createListModel(List<MemoryValueBreakCondition> conditions) {
            DefaultListModel listModel = new DefaultListModel();
            for (MemoryValueBreakCondition condition : conditions) {
                listModel.addElement(condition);
            }
            return listModel;
        }

        private void deleteCondition(int index) {
            if (index != -1) {
                trigger.getMemoryValueBreakConditions().remove(index);
                updateConditions();
            }
        }

        void addCondition() {
            MemoryValueBreakCondition condition = new MemoryValueBreakCondition(trigger);
            trigger.getMemoryValueBreakConditions().add(condition);
            memoryConditionJList.setSelectedIndex(trigger.getMemoryValueBreakConditions().size() - 1);
            new MemoryConditionEditDialog(BreakTriggerEditDialog.this, condition, "Edit memory value condition").setVisible(true);
            editCondition(condition);
        }

        void editCondition(MemoryValueBreakCondition condition) {
            new MemoryConditionEditDialog(BreakTriggerEditDialog.this, condition, "Edit memory value condition").setVisible(true);
            updateConditions();
        }

        private void updateConditions() {
            memoryConditionJList.setModel(createListModel(trigger.getMemoryValueBreakConditions()));
        }

    }
}
