package model;

public class Process {
    private String name;
    private long originalTime;
    private long remainingTime;
    private Status status; 
    private long size;
    private Partition partition;
    private int cycleCount;

    public Process(String name, long time, Status status, long size, Partition partition) {
        this.name = name;
        this.originalTime = time;
        this.remainingTime = time;
        this.status = status;
        this.size = size;
        this.partition = partition;
        this.cycleCount = 0;
    }

    public Process(String name, long originalTime, long remainingTime, Status status, 
                   long size, Partition partition, int cycleCount) {
        this.name = name;
        this.originalTime = originalTime;
        this.remainingTime = remainingTime;
        this.status = status;
        this.size = size;
        this.partition = partition;
        this.cycleCount = cycleCount;
    }

    public void subtractTime(long time) {
        this.remainingTime -= time;
        if (remainingTime < 0) {
            remainingTime = 0;
        }
    }

    public void incrementCycle() {
        this.cycleCount++;
    }

    public boolean isFinished() {
        return remainingTime <= 0;
    }

    public boolean isBlocked() {
        return status == Status.BLOQUEADO;
    }

    public boolean fitsInPartition() {
        return partition != null && size <= partition.getSize();
    }

    public void resetTime() {
        remainingTime = originalTime;
    }

    // Getters
    public String getName() {
        return name;
    }

    public long getOriginalTime() {
        return originalTime;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public Status getStatus() {
        return status;
    }

    public long getSize() {
        return size;
    }

    public Partition getPartition() {
        return partition;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public String getStatusString() {
        return status == Status.BLOQUEADO ? "Bloqueado" : "No bloqueado";
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setOriginalTime(long originalTime) {
        this.originalTime = originalTime;
        this.remainingTime = originalTime;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public void setCycleCount(int cycleCount) {
        this.cycleCount = cycleCount;
    }

    public Process clone() {
        return new Process(name, originalTime, remainingTime, status, size, partition, cycleCount);
    }

    @Override
    public String toString() {
        return "Process{" +
                "name='" + name + '\'' +
                ", originalTime=" + originalTime +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                ", size=" + size +
                ", partition=" + (partition != null ? partition.getName() : "null") +
                ", cycleCount=" + cycleCount +
                '}';
    }
}