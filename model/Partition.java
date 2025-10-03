package model;

import java.util.ArrayList;

public class Partition {
    private String name;
    private long size;
    private ArrayList<Process> assignedProcesses;

    public Partition(String name, long size) {
        this.name = name;
        this.size = size;
        this.assignedProcesses = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public ArrayList<Process> getAssignedProcesses() {
        return assignedProcesses;
    }

    public void addProcess(Process process) {
        if (!assignedProcesses.contains(process)) {
            assignedProcesses.add(process);
        }
    }

    public void removeProcess(Process process) {
        assignedProcesses.remove(process);
    }

    public void removeProcessByName(String processName) {
        assignedProcesses.removeIf(p -> p.getName().equalsIgnoreCase(processName));
    }

    public long getTotalExecutionTime() {
        long total = 0;
        for (Process p : assignedProcesses) {
            total += p.getOriginalTime();
        }
        return total;
    }

    public int getProcessCount() {
        return assignedProcesses.size();
    }

    public boolean hasAssignedProcesses() {
        return !assignedProcesses.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}