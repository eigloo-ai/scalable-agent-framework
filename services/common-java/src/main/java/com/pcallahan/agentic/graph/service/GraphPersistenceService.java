package com.pcallahan.agentic.graph.service;

import com.pcallahan.agentic.graph.exception.GraphPersistenceException;
import com.pcallahan.agentic.graph.model.AgentGraph;
import com.pcallahan.agentic.graph.model.Plan;
import com.pcallahan.agentic.graph.model.Task;
import com.pcallahan.agentic.graph.entity.AgentGraphEntity;
import com.pcallahan.agentic.graph.entity.PlanEntity;
import com.pcallahan.agentic.graph.entity.TaskEntity;
import com.pcallahan.agentic.graph.repository.AgentGraphRepository;
import com.pcallahan.agentic.graph.repository.PlanRepository;
import com.pcallahan.agentic.graph.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(GraphPersistenceService.class);

    private final AgentGraphRepository agentGraphRepository;
    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;

    public GraphPersistenceService(AgentGraphRepository agentGraphRepository,
                                   PlanRepository planRepository,
                                   TaskRepository taskRepository) {
        this.agentGraphRepository = agentGraphRepository;
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public String persistGraph(AgentGraph graph, String tenantId) {
        String correlationId = getOrCreateCorrelationId();
        try {
            logger.info("Persisting agent graph '{}' for tenant {} [correlationId={}]", graph.name(), tenantId, correlationId);
            if (graph == null) {
                throw new GraphPersistenceException("Graph cannot be null", correlationId, null);
            }
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new GraphPersistenceException("Tenant ID cannot be null or empty", correlationId, graph.name());
            }

            Optional<AgentGraphEntity> existingGraph = agentGraphRepository.findByTenantIdAndName(tenantId, graph.name());
            existingGraph.ifPresent(entity -> deleteGraphData(entity.getId()));

            String graphId = UUID.randomUUID().toString();
            AgentGraphEntity graphEntity = new AgentGraphEntity(graphId, tenantId, graph.name());
            agentGraphRepository.save(graphEntity);

            Map<String, PlanEntity> planEntityMap = new HashMap<>();
            for (Plan plan : graph.plans().values()) {
                String planId = UUID.randomUUID().toString();
                PlanEntity planEntity = new PlanEntity(planId, plan.name(), plan.label(), plan.planSource().toString(), graphEntity);
                planRepository.save(planEntity);
                planEntityMap.put(plan.name(), planEntity);
            }

            Map<String, TaskEntity> taskEntityMap = new HashMap<>();
            for (Task task : graph.tasks().values()) {
                String taskId = UUID.randomUUID().toString();
                PlanEntity upstreamPlan = planEntityMap.get(task.upstreamPlanId());
                TaskEntity taskEntity = new TaskEntity(taskId, task.name(), task.label(), task.taskSource().toString(), graphEntity, upstreamPlan);
                taskRepository.save(taskEntity);
                taskEntityMap.put(task.name(), taskEntity);
            }

            updatePlanTaskRelationships(graph, planEntityMap, taskEntityMap);
            return graphId;
        } catch (GraphPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new GraphPersistenceException("Failed to persist graph: " + e.getMessage(), e, getOrCreateCorrelationId(), graph != null ? graph.name() : null);
        }
    }

    @Transactional(readOnly = true)
    public AgentGraph getPersistedGraph(String graphId) {
        String correlationId = getOrCreateCorrelationId();
        try {
            if (graphId == null || graphId.trim().isEmpty()) {
                throw new GraphPersistenceException("Graph ID cannot be null or empty", correlationId, null);
            }
            AgentGraphEntity graphEntity = agentGraphRepository.findById(graphId)
                .orElseThrow(() -> new GraphPersistenceException("Graph not found with ID: " + graphId, correlationId, null));
            return convertToAgentGraph(graphEntity);
        } catch (GraphPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new GraphPersistenceException("Failed to retrieve graph: " + e.getMessage(), e, correlationId, null);
        }
    }

    @Transactional(readOnly = true)
    public List<GraphInfo> listPersistedGraphs(String tenantId) {
        String correlationId = getOrCreateCorrelationId();
        try {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new GraphPersistenceException("Tenant ID cannot be null or empty", correlationId, null);
            }
            List<AgentGraphEntity> graphEntities = agentGraphRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
            return graphEntities.stream()
                .map(entity -> new GraphInfo(entity.getId(), entity.getName(), entity.getTenantId(), entity.getCreatedAt(), entity.getUpdatedAt()))
                .collect(Collectors.toList());
        } catch (GraphPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new GraphPersistenceException("Failed to list graphs: " + e.getMessage(), e, correlationId, null);
        }
    }

    private void updatePlanTaskRelationships(AgentGraph graph, Map<String, PlanEntity> planEntityMap, Map<String, TaskEntity> taskEntityMap) {
        for (Plan plan : graph.plans().values()) {
            PlanEntity planEntity = planEntityMap.get(plan.name());
            
            // Set upstreamPlan on each task that belongs to this plan
            for (String taskId : plan.upstreamTaskIds()) {
                TaskEntity taskEntity = taskEntityMap.get(taskId);
                if (taskEntity != null) {
                    taskEntity.setUpstreamPlan(planEntity);
                    taskRepository.save(taskEntity);
                }
            }
        }
    }

    private void deleteGraphData(String graphId) {
        taskRepository.deleteByAgentGraphId(graphId);
        planRepository.deleteByAgentGraphId(graphId);
        agentGraphRepository.deleteById(graphId);
    }

    private AgentGraph convertToAgentGraph(AgentGraphEntity entity) {
        List<PlanEntity> planEntities = planRepository.findByAgentGraphId(entity.getId());
        List<TaskEntity> taskEntities = taskRepository.findByAgentGraphId(entity.getId());
        
        // Build planToTasks mapping from foreign key relationships
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        for (TaskEntity taskEntity : taskEntities) {
            if (taskEntity.getUpstreamPlan() != null) {
                String planName = taskEntity.getUpstreamPlan().getName();
                String taskName = taskEntity.getName();
                planToTasks.computeIfAbsent(planName, k -> new HashSet<>()).add(taskName);
                taskToPlan.put(taskName, planName);
            }
        }
        
        // Convert entities to domain objects
        Map<String, Plan> plans = planEntities.stream().collect(Collectors.toMap(
                PlanEntity::getName, planEntity -> convertToPlan(planEntity, planToTasks.getOrDefault(planEntity.getName(), new HashSet<>()))));

        Map<String, Task> tasks = taskEntities.stream().collect(Collectors.toMap(
                TaskEntity::getName, this::convertToTask));

        return AgentGraph.of(entity.getTenantId(), entity.getName(), plans, tasks, planToTasks, taskToPlan);
    }

    private Plan convertToPlan(PlanEntity entity, Set<String> upstreamTaskIds) {
        return new Plan(entity.getName(), entity.getLabel(), Path.of(entity.getPlanSourcePath()), upstreamTaskIds, java.util.List.of());
    }

    private Task convertToTask(TaskEntity entity) {
        String upstreamPlanId = entity.getUpstreamPlan() != null ? entity.getUpstreamPlan().getName() : null;
        return new Task(entity.getName(), entity.getLabel(), Path.of(entity.getTaskSourcePath()), upstreamPlanId, java.util.List.of());
    }

    public record GraphInfo(String id, String name, String tenantId, java.time.LocalDateTime createdAt,
                            java.time.LocalDateTime updatedAt) {}

    private String getOrCreateCorrelationId() {
        String mdcId = MDC.get("correlationId");
        if (mdcId != null && !mdcId.trim().isEmpty()) {
            return mdcId;
        }
        String newId = UUID.randomUUID().toString();
        MDC.put("correlationId", newId);
        return newId;
    }
}


