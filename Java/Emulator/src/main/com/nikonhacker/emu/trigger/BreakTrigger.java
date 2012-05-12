package com.nikonhacker.emu.trigger;
import com.nikonhacker.Format;
import com.nikonhacker.dfr.CPUState;
import com.nikonhacker.dfr.CodeStructure;
import com.nikonhacker.dfr.Function;
import com.nikonhacker.dfr.Symbol;
import com.nikonhacker.emu.CallStackItem;
import com.nikonhacker.emu.memory.Memory;
import com.nikonhacker.emu.trigger.condition.*;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A trigger represents a CPU state accompanied by memory conditions which,
 * when matched together, will trigger a break or a log
 */
public class BreakTrigger {
    private String name;
    private CPUState cpuStateValues;
    private CPUState cpuStateFlags;
    private List<MemoryValueBreakCondition> memoryValueBreakConditions;
    private boolean mustBeLogged = false;
    private boolean mustBreak = true;
    private Function function;

    public BreakTrigger(String name, CPUState cpuStateValues, CPUState cpuStateFlags, List<MemoryValueBreakCondition> memoryValueBreakConditions) {
        this.name = name;
        this.cpuStateValues = cpuStateValues;
        this.cpuStateFlags = cpuStateFlags;
        this.memoryValueBreakConditions = memoryValueBreakConditions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CPUState getCpuStateValues() {
        return cpuStateValues;
    }

    public boolean mustBeLogged() {
        return mustBeLogged;
    }

    public boolean getMustBeLogged() {
        return mustBeLogged;
    }

    public void setMustBeLogged(boolean mustBeLogged) {
        this.mustBeLogged = mustBeLogged;
    }

    public boolean mustBreak() {
        return mustBreak;
    }

    public boolean getMustBreak() {
        return mustBreak;
    }

    public void setMustBreak(boolean mustBreak) {
        this.mustBreak = mustBreak;
    }

    public void setCpuStateValues(CPUState cpuStateValues) {
        this.cpuStateValues = cpuStateValues;
    }

    public CPUState getCpuStateFlags() {
        return cpuStateFlags;
    }

    public void setCpuStateFlags(CPUState cpuStateFlags) {
        this.cpuStateFlags = cpuStateFlags;
    }

    public List<MemoryValueBreakCondition> getMemoryValueBreakConditions() {
        if (memoryValueBreakConditions == null) {
            memoryValueBreakConditions = new ArrayList<MemoryValueBreakCondition>();
        }
        return memoryValueBreakConditions;
    }

    public List<BreakCondition> getBreakConditions(CodeStructure codeStructure) {
        List<BreakCondition> conditions = new ArrayList<BreakCondition>();
        if (cpuStateFlags.pc != 0) {
            if (codeStructure != null && codeStructure.getFunctions().containsKey(cpuStateValues.pc)) {
                // this is a break on a function. Store it for later
                function = codeStructure.getFunctions().get(cpuStateValues.pc);
            }
            conditions.add(new BreakPointCondition(cpuStateValues.pc, this));
        }
        for (int i = 0; i <= CPUState.MDL; i++) {
            if (cpuStateFlags.getReg(i) != 0) {
                conditions.add(new RegisterEqualityBreakCondition(i, cpuStateValues.getReg(i), this));
            }
        }
        if (cpuStateFlags.getCCR() != 0) {
            conditions.add(new CCRBreakCondition(cpuStateValues.getCCR(), cpuStateFlags.getCCR(), this));
        }
        if (cpuStateFlags.getSCR() != 0) {
            conditions.add(new SCRBreakCondition(cpuStateValues.getSCR(), cpuStateFlags.getSCR(), this));
        }
        if (cpuStateFlags.getILM() != 0) {
            conditions.add(new ILMBreakCondition(cpuStateValues.getILM(), cpuStateFlags.getILM(), this));
        }

        conditions.addAll(memoryValueBreakConditions);

        return conditions;
    }

    @Override
    public String toString() {
        return name + (getMustBreak()?"break":"") + (getMustBeLogged()?" log":"");
    }

    /**
     * This is the default logging behaviour
     * @param printWriter printWriter to which the log must be output
     * @param cpuState optional cpu state at the time the condition matches
     * @param callStack optional call stack at the time the condition matches
     * @param memory
     */
    public void log(PrintWriter printWriter, CPUState cpuState, Deque<CallStackItem> callStack, Memory memory) {
        String msg;
        if (function != null) {
            // This is a function call. Parse its arguments and log them
            msg = function.getName() + "(";
            if (function.getParameterList() != null) {
                for (Symbol.Parameter parameter : function.getParameterList()) {
                    if (parameter.getInVariable() != null) {
                        String paramString = parameter.getInVariable() + "=";
                        int value = cpuState.getReg(parameter.getRegister());
                        if (parameter.getInVariable().startsWith("sz")) {
                            paramString+="\"";
                            // Dump as String
                            int character = memory.loadUnsigned8(value++);
                            while (character > 0) {
                                paramString += (char)character;
                                character = memory.loadUnsigned8(value++);
                            }
                            paramString+="\"";
                        }
                        else {
                            // Dump as Int
                            paramString += "0x" + Format.asHex(value,8);
                        }
                        if (!msg.endsWith("(")) {
                            msg+=", ";
                        }
                        msg += paramString;
                    }
                }
            }
            msg +=") ";
        }
        else {
            msg = name + " triggered at 0x" + Format.asHex(cpuState.pc, 8);
        }

        if (callStack != null) {
            for (CallStackItem callStackItem : callStack) {
                msg += " << " + StringUtils.strip(callStackItem.toString()).replaceAll("\\s+", " ");
            }
        }
        printWriter.print(msg + "\n");
    }


}

