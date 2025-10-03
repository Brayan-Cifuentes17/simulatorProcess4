package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessManager {
    private ArrayList<Process> initialProcesses;
    private ArrayList<Partition> partitions;
    private ArrayList<Log> executionLogs;

    public ProcessManager() {
        initialProcesses = new ArrayList<>();
        partitions = new ArrayList<>();
        executionLogs = new ArrayList<>();
    }

    // particiones
    
    public void addPartition(String name, long size) {
        Partition partition = new Partition(name, size);
        partitions.add(partition);
    }

    public boolean partitionExists(String name) {
        return partitions.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void removePartition(String name) {
        partitions.removeIf(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public Partition findPartitionByName(String name) {
        return partitions.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    public boolean hasPartitionAssignedProcesses(String partitionName) {
        return initialProcesses.stream()
                .anyMatch(p -> p.getPartition() != null && 
                         p.getPartition().getName().equalsIgnoreCase(partitionName));
    }

    public ArrayList<Partition> getPartitions() {
        return new ArrayList<>(partitions);
    }

    // procesos
    
    public void addProcess(String name, long time, Status status, long size, Partition partition) {
        Process process = new Process(name, time, status, size, partition);
        initialProcesses.add(process);
        if (partition != null) {
            partition.addProcess(process);
        }
    }

    public boolean processExists(String name) {
        return initialProcesses.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void removeProcess(String name) {
        Process process = findProcessByName(name);
        if (process != null && process.getPartition() != null) {
            process.getPartition().removeProcess(process);
        }
        initialProcesses.removeIf(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void editProcess(int position, String processName, long newTime, Status newStatus, 
                           long newSize, Partition newPartition) {
        if (position >= 0 && position < initialProcesses.size()) {
            Process existingProcess = initialProcesses.get(position);
            
            if (existingProcess.getName().equalsIgnoreCase(processName)) {
                // Remover de la partición anterior
                if (existingProcess.getPartition() != null) {
                    existingProcess.getPartition().removeProcess(existingProcess);
                }
                
                // Actualizar datos del proceso
                existingProcess.setOriginalTime(newTime);
                existingProcess.setStatus(newStatus);
                existingProcess.setSize(newSize);
                existingProcess.setPartition(newPartition);
                
                // Agregar a la nueva partición
                if (newPartition != null) {
                    newPartition.addProcess(existingProcess);
                }
            }
        }
    }

    private Process findProcessByName(String name) {
        return initialProcesses.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    public boolean isEmpty() {
        return initialProcesses.isEmpty();
    }

    public ArrayList<Process> getInitialProcesses() {
        return new ArrayList<>(initialProcesses);
    }

    // simulacion
    
    public void runSimulation() {
        executionLogs.clear();
        
        // Registrar procesos iniciales (en orden de entrada, sin ordenar)
        for (Process p : initialProcesses) {
            addLog(p, Filter.INICIAL);
        }
        
        // Registrar particiones
        for (Partition part : partitions) {
            if (!part.getAssignedProcesses().isEmpty()) {
                Process firstProcess = part.getAssignedProcesses().get(0);
                addLog(firstProcess, Filter.PARTICIONES);
            }
        }
        
        // Separar procesos ejecutables de no ejecutables
        ArrayList<Process> executableProcesses = new ArrayList<>();
        
        for (Process p : initialProcesses) {
            if (!p.fitsInPartition()) {
                // No cabe en la partición -> No ejecutado
                addLog(p, Filter.NO_EJECUTADO);
            } else {
                // Clonar el proceso para la simulación
                executableProcesses.add(p.clone());
            }
        }
        
        // ORDENAR GLOBALMENTE UNA SOLA VEZ por tiempo (menor a mayor)
        // Todos los procesos juntos, sin importar la partición
        executableProcesses.sort((p1, p2) -> Long.compare(p1.getOriginalTime(), p2.getOriginalTime()));
        
        // Crear cola global de procesos listos para Round Robin
        ArrayList<Process> readyQueue = new ArrayList<>(executableProcesses);
        
        // Ejecutar Round Robin global (todos los procesos juntos)
        executeRoundRobin(readyQueue);
    }

    private void executeRoundRobin(ArrayList<Process> readyQueue) {
        while (!readyQueue.isEmpty()) {
            // Tomar el primer proceso de la cola
            Process currentProcess = readyQueue.remove(0);
            
        
            addLog(currentProcess, Filter.LISTO);
            
            // Despachar
            addLog(currentProcess, Filter.DESPACHAR);
            
          
            addLog(currentProcess, Filter.EN_EJECUCION);
            
            
            currentProcess.subtractTime(Constants.QUANTUM_TIME);
            currentProcess.incrementCycle();
            
           
            if (currentProcess.isFinished()) {
                addLog(currentProcess, Filter.FINALIZADO);
                continue; 
            }
            
           
            if (currentProcess.isBlocked()) {
                addLog(currentProcess, Filter.TRANSICION_BLOQUEO);
                addLog(currentProcess, Filter.BLOQUEADO);
                addLog(currentProcess, Filter.DESPERTAR);
               
                readyQueue.add(currentProcess);
            } else {
                
                addLog(currentProcess, Filter.TIEMPO_EXPIRADO);
               
                readyQueue.add(currentProcess);
            }
        }
    }

    private void addLog(Process process, Filter filter) {
        Log log = new Log(process, filter);
        executionLogs.add(log);
    }

    // logs
    
    public List<Log> getLogsByFilter(Filter filter) {
        return executionLogs.stream()
                .filter(log -> log.getFilter() == filter)
                .collect(Collectors.toList());
    }

    public List<Log> getLogsByFilterAndPartition(Filter filter, String partitionName) {
        return executionLogs.stream()
                .filter(log -> log.getFilter() == filter && 
                       log.getPartition() != null &&
                       log.getPartition().getName().equalsIgnoreCase(partitionName))
                .collect(Collectors.toList());
    }

    public ArrayList<Log> getAllLogs() {
        return new ArrayList<>(executionLogs);
    }

    // Informe de finalización de particiones
    
    public List<PartitionFinalizationInfo> getPartitionFinalizationReport() {
        List<PartitionFinalizationInfo> report = new ArrayList<>();
        
        for (Partition partition : partitions) {
            // Obtener solo procesos ejecutables (que caben en la partición)
            List<Process> executableProcesses = partition.getAssignedProcesses().stream()
                    .filter(p -> p.getSize() <= partition.getSize())
                    .collect(Collectors.toList());
            
            // Calcular tiempo total solo de procesos ejecutables
            long totalTime = 0;
            StringBuilder processNames = new StringBuilder();
            
            for (int i = 0; i < executableProcesses.size(); i++) {
                Process p = executableProcesses.get(i);
                totalTime += p.getOriginalTime();
                processNames.append(p.getName());
                if (i < executableProcesses.size() - 1) {
                    processNames.append(", ");
                }
            }
            
            PartitionFinalizationInfo info = new PartitionFinalizationInfo(
                partition.getName(),
                partition.getSize(),
                processNames.toString().isEmpty() ? "Ninguno" : processNames.toString(),
                totalTime
            );
            report.add(info);
        }
        
        // Ordenar por tiempo total (menor a mayor)
        report.sort((p1, p2) -> Long.compare(p1.getTotalTime(), p2.getTotalTime()));
        
        return report;
    }

    // Clase interna para el informe de finalización
    public static class PartitionFinalizationInfo {
        private String name;
        private long size;
        private String processNames;
        private long totalTime;

        public PartitionFinalizationInfo(String name, long size, String processNames, long totalTime) {
            this.name = name;
            this.size = size;
            this.processNames = processNames;
            this.totalTime = totalTime;
        }

        public String getName() { return name; }
        public long getSize() { return size; }
        public String getProcessNames() { return processNames; }
        public long getTotalTime() { return totalTime; }
    }

    // ========== LIMPIEZA ==========
    
    public void clearAll() {
        initialProcesses.clear();
        partitions.clear();
        executionLogs.clear();
    }

    public void clearLogs() {
        executionLogs.clear();
    }
}