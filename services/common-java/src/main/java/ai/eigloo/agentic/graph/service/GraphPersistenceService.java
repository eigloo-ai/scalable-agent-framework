package ai.eigloo.agentic.graph.service;

import ai.eigloo.agentic.graph.exception.GraphPersistenceException;
import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.GraphEdge;
import ai.eigloo.agentic.graph.model.GraphNodeType;
import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.GraphEdgeEntity;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphEdgeRepository;
import ai.eigloo.agentic.graph.repository.PlanRepository;
import ai.eigloo.agentic.graph.repository.TaskRepository;
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
    private final GraphEdgeRepository graphEdgeRepository;

    public GraphPersistenceService(AgentGraphRepository agentGraphRepository,
                                   GraphEdgeRepository graphEdgeRepository,
                                   PlanRepository planRepository,
                                   TaskRepository taskRepository) {
        this.agentGraphRepository = agentGraphRepository;
        this.graphEdgeRepository = graphEdgeRepository;
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
                TaskEntity taskEntity = new TaskEntity(taskId, task.name(), task.label(), task.taskSource().toString(), graphEntity);
                taskRepository.save(taskEntity);
                taskEntityMap.put(task.name(), taskEntity);
            }

            persistGraphEdges(graph, graphEntity, planEntityMap, taskEntityMap);
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

    private void persistGraphEdges(
            AgentGraph graph,
            AgentGraphEntity graphEntity,
            Map<String, PlanEntity> planEntityMap,
            Map<String, TaskEntity> taskEntityMap
    ) {
        Set<String> dedupe = new HashSet<>();
        for (GraphEdge edge : graph.edges()) {
            String edgeKey = edge.fromType() + "|" + edge.from() + "->" + edge.toType() + "|" + edge.to();
            if (!dedupe.add(edgeKey)) {
                continue;
            }

            if (edge.fromType() == GraphNodeType.PLAN && edge.toType() == GraphNodeType.TASK) {
                if (!planEntityMap.containsKey(edge.from()) || !taskEntityMap.containsKey(edge.to())) {
                    throw new GraphPersistenceException(
                            "Invalid PLAN->TASK edge '" + edge.from() + "' -> '" + edge.to() + "' while persisting graph");
                }
            } else if (edge.fromType() == GraphNodeType.TASK && edge.toType() == GraphNodeType.PLAN) {
                if (!taskEntityMap.containsKey(edge.from()) || !planEntityMap.containsKey(edge.to())) {
                    throw new GraphPersistenceException(
                            "Invalid TASK->PLAN edge '" + edge.from() + "' -> '" + edge.to() + "' while persisting graph");
                }
            } else {
                throw new GraphPersistenceException(
                        "Invalid edge type combination '" + edge.fromType() + "' -> '" + edge.toType() + "'");
            }

            GraphEdgeEntity edgeEntity = new GraphEdgeEntity(
                    UUID.randomUUID().toString(),
                    graphEntity,
                    edge.from(),
                    edge.fromType(),
                    edge.to(),
                    edge.toType());
            graphEdgeRepository.save(edgeEntity);
        }
    }

    private void deleteGraphData(String graphId) {
        graphEdgeRepository.deleteByAgentGraphId(graphId);
        taskRepository.deleteByAgentGraphId(graphId);
        planRepository.deleteByAgentGraphId(graphId);
        agentGraphRepository.deleteById(graphId);
    }

    private AgentGraph convertToAgentGraph(AgentGraphEntity entity) {
        List<PlanEntity> planEntities = planRepository.findByAgentGraphId(entity.getId());
        List<TaskEntity> taskEntities = taskRepository.findByAgentGraphId(entity.getId());
        List<GraphEdgeEntity> edgeEntities = graphEdgeRepository.findByAgentGraphId(entity.getId());

        List<GraphEdge> edges = new ArrayList<>();
        for (GraphEdgeEntity edgeEntity : edgeEntities) {
            GraphEdge edge = new GraphEdge(
                    edgeEntity.getFromNodeName(),
                    edgeEntity.getFromNodeType(),
                    edgeEntity.getToNodeName(),
                    edgeEntity.getToNodeType());
            edges.add(edge);
        }

        Map<String, Plan> plans = planEntities.stream().collect(Collectors.toMap(
                PlanEntity::getName, this::convertToPlan));

        Map<String, Task> tasks = taskEntities.stream().collect(Collectors.toMap(
                TaskEntity::getName, this::convertToTask));

        return AgentGraph.of(entity.getTenantId(), entity.getName(), plans, tasks, edges);
    }

    private Plan convertToPlan(PlanEntity entity) {
        return new Plan(entity.getName(), entity.getLabel(), Path.of(entity.getPlanSourcePath()), java.util.List.of());
    }

    private Task convertToTask(TaskEntity entity) {
        return new Task(entity.getName(), entity.getLabel(), Path.of(entity.getTaskSourcePath()), java.util.List.of());
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
