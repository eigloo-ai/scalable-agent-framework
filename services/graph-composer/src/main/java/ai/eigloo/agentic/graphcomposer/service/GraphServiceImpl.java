package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graph.entity.GraphStatus;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.GraphEdgeEntity;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphEdgeRepository;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import ai.eigloo.agentic.graph.repository.PlanRepository;
import ai.eigloo.agentic.graph.repository.TaskRepository;
import ai.eigloo.agentic.graphcomposer.dto.AgentGraphDto;
import ai.eigloo.agentic.graphcomposer.dto.AgentGraphSummary;
import ai.eigloo.agentic.graphcomposer.dto.CreateGraphRequest;
import ai.eigloo.agentic.graphcomposer.dto.ExecutionResponse;
import ai.eigloo.agentic.graphcomposer.dto.ExecutorFileDto;
import ai.eigloo.agentic.graphcomposer.dto.GraphStatusUpdate;
import ai.eigloo.agentic.graphcomposer.dto.PlanDto;
import ai.eigloo.agentic.graphcomposer.dto.TaskDto;
import ai.eigloo.agentic.graphcomposer.dto.ValidationResult;
import ai.eigloo.agentic.graphcomposer.dto.*;
import ai.eigloo.agentic.graphcomposer.exception.GraphValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of GraphService for Agent Graph management operations.
 * Provides business logic for graph CRUD operations with tenant isolation.
 */
@Service
@Transactional
public class GraphServiceImpl implements GraphService {

    private static final Logger logger = LoggerFactory.getLogger(GraphServiceImpl.class);

    private final AgentGraphRepository agentGraphRepository;
    private final GraphEdgeRepository graphEdgeRepository;
    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final GraphRunRepository graphRunRepository;
    private final FileService fileService;
    private final ValidationService validationService;
    private final GraphExecutionBootstrapPublisher graphExecutionBootstrapPublisher;

    @Autowired
    public GraphServiceImpl(AgentGraphRepository agentGraphRepository,
                           GraphEdgeRepository graphEdgeRepository,
                           PlanRepository planRepository,
                           TaskRepository taskRepository,
                           GraphRunRepository graphRunRepository,
                           FileService fileService,
                           ValidationService validationService,
                           GraphExecutionBootstrapPublisher graphExecutionBootstrapPublisher) {
        this.agentGraphRepository = agentGraphRepository;
        this.graphEdgeRepository = graphEdgeRepository;
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.graphRunRepository = graphRunRepository;
        this.fileService = fileService;
        this.validationService = validationService;
        this.graphExecutionBootstrapPublisher = graphExecutionBootstrapPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentGraphSummary> listGraphs(String tenantId) {
        logger.info("Listing graphs for tenant: {}", tenantId);
        
        // Use optimized query that doesn't fetch relationships for listing
        List<AgentGraphEntity> graphs = agentGraphRepository.findByTenantIdOptimized(tenantId);
        
        return graphs.stream()
                .map(this::convertToSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AgentGraphDto getGraph(String graphId, String tenantId) {
        logger.info("Getting graph {} for tenant: {}", graphId, tenantId);

        // Avoid loading all nested eager collections in one go to prevent Hibernate multiple-bag fetch errors.
        AgentGraphEntity graph = agentGraphRepository.findByIdAndTenantId(graphId, tenantId)
                .orElseThrow(() -> new GraphNotFoundException("Graph not found: " + graphId));

        return convertToDto(graph);
    }

    @Override
    public AgentGraphDto createGraph(CreateGraphRequest request) {
        logger.info("Creating new graph '{}' for tenant: {}", request.getName(), request.getTenantId());
        
        AgentGraphEntity graph = new AgentGraphEntity();
        graph.setId(UUID.randomUUID().toString());
        graph.setName(request.getName());
        graph.setTenantId(request.getTenantId());
        graph.setStatus(GraphStatus.NEW);
        graph.setCreatedAt(LocalDateTime.now());
        graph.setUpdatedAt(LocalDateTime.now());
        
        AgentGraphEntity savedGraph = agentGraphRepository.save(graph);
        
        logger.info("Created graph with ID: {}", savedGraph.getId());
        return convertToDto(savedGraph);
    }

    @Override
    public AgentGraphDto updateGraph(String graphId, AgentGraphDto graphDto) {
        logger.info("Updating graph {} for tenant: {}", graphId, graphDto.getTenantId());
        
        AgentGraphEntity existingGraph = agentGraphRepository.findByIdAndTenantId(graphId, graphDto.getTenantId())
                .orElseThrow(() -> new GraphNotFoundException("Graph not found: " + graphId));
        
        // Validate the graph before updating
        ValidationResult validation = validationService.validateGraph(graphDto);
        if (!validation.isValid()) {
            throw new GraphValidationException("Graph validation failed", validation.getErrors(), validation.getWarnings());
        }
        
        // Update basic properties
        existingGraph.setName(graphDto.getName());
        existingGraph.setUpdatedAt(LocalDateTime.now());
        
        // Update status if provided
        if (graphDto.getStatus() != null) {
            transitionGraphStatus(existingGraph, convertToEntityStatus(graphDto.getStatus()), "updateGraph");
        }
        
        // Update plans and tasks using single-transaction cascading approach
        AgentGraphEntity savedGraph = updateGraphWithCascading(existingGraph, graphDto);
        
        logger.info("Updated graph: {}", savedGraph.getId());

        return convertToDto(savedGraph);
    }

    @Override
    public void deleteGraph(String graphId, String tenantId) {
        logger.info("Deleting graph {} for tenant: {}", graphId, tenantId);
        
        AgentGraphEntity graph = agentGraphRepository.findByIdAndTenantId(graphId, tenantId)
                .orElseThrow(() -> new GraphNotFoundException("Graph not found: " + graphId));
        
        agentGraphRepository.delete(graph);
        
        logger.info("Deleted graph: {}", graphId);
    }



    @Override
    public ExecutionResponse submitForExecution(String graphId, String tenantId) {
        logger.info("Submitting graph {} for execution for tenant: {}", graphId, tenantId);
        
        AgentGraphEntity graph = agentGraphRepository.findByIdAndTenantId(graphId, tenantId)
                .orElseThrow(() -> new GraphNotFoundException("Graph not found: " + graphId));

        if (graph.getStatus() == GraphStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived graph cannot be executed");
        }
        
        // Validate graph before execution
        AgentGraphDto graphDto = convertToDto(graph);
        ValidationResult validation = validationService.validateGraph(graphDto);
        if (!validation.isValid()) {
            throw new GraphValidationException("Cannot execute invalid graph", validation.getErrors(), validation.getWarnings());
        }

        List<String> entryPlanNames = resolveEntryPlanNames(graphDto);
        if (entryPlanNames.isEmpty()) {
            throw new GraphValidationException(
                    "Cannot execute graph without entry plans",
                    List.of("No entry plans found. At least one plan must have no upstream tasks."),
                    List.of());
        }

        String lifetimeId = UUID.randomUUID().toString();
        if (graph.getStatus() != GraphStatus.ACTIVE) {
            transitionGraphStatus(graph, GraphStatus.ACTIVE, "submitForExecution");
            graph.setUpdatedAt(LocalDateTime.now());
            agentGraphRepository.save(graph);
        }

        GraphRunEntity graphRun = createQueuedGraphRun(tenantId, graphId, lifetimeId, entryPlanNames);
        try {
            for (String planName : entryPlanNames) {
                graphExecutionBootstrapPublisher.publishStartPlanInput(tenantId, graphId, lifetimeId, planName);
            }
        } catch (Exception e) {
            graphRun.setStatus(GraphRunStatus.FAILED);
            graphRun.setCompletedAt(Instant.now());
            graphRun.setErrorMessage(compactError(e.getMessage()));
            graphRunRepository.save(graphRun);
            throw new IllegalStateException("Failed to enqueue graph execution bootstrap messages", e);
        }

        ExecutionResponse response = new ExecutionResponse();
        response.setExecutionId(lifetimeId);
        response.setStatus("RUNNING");
        response.setMessage("Graph execution started with " + entryPlanNames.size() + " entry plan(s)");
        
        logger.info(
                "Graph {} execution started for tenant {} lifetime {} entryPlans={}",
                graphId, tenantId, lifetimeId, entryPlanNames);
        return response;
    }

    private GraphRunEntity createQueuedGraphRun(
            String tenantId,
            String graphId,
            String lifetimeId,
            List<String> entryPlanNames) {
        GraphRunEntity graphRun = new GraphRunEntity();
        graphRun.setLifetimeId(lifetimeId);
        graphRun.setTenantId(tenantId);
        graphRun.setGraphId(graphId);
        graphRun.setStatus(GraphRunStatus.QUEUED);
        graphRun.setEntryPlanNames(String.join(",", entryPlanNames));
        graphRun.setErrorMessage(null);
        graphRun.setCreatedAt(Instant.now());
        graphRun.setStartedAt(null);
        graphRun.setCompletedAt(null);
        return graphRunRepository.save(graphRun);
    }

    private static String compactError(String message) {
        if (message == null || message.isBlank()) {
            return "Execution bootstrap failed";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    @Override
    public void updateGraphStatus(String graphId, GraphStatusUpdate statusUpdate) {
        logger.info("Updating status for graph {} to: {}", graphId, statusUpdate.getStatus());
        
        AgentGraphEntity graph = agentGraphRepository.findById(graphId)
                .orElseThrow(() -> new GraphNotFoundException("Graph not found: " + graphId));

        transitionGraphStatus(graph, convertToEntityStatus(statusUpdate.getStatus()), "updateGraphStatus");
        graph.setUpdatedAt(LocalDateTime.now());
        agentGraphRepository.save(graph);
        
        logger.info("Updated graph {} status to: {}", graphId, statusUpdate.getStatus());
    }

    private AgentGraphSummary convertToSummary(AgentGraphEntity entity) {
        return new AgentGraphSummary(
                entity.getId(),
                entity.getName(),
                convertToDtoStatus(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AgentGraphDto convertToDto(AgentGraphEntity entity) {
        List<PlanEntity> plans = planRepository.findByAgentGraphIdWithFiles(entity.getId());
        List<TaskEntity> tasks = taskRepository.findByAgentGraphIdWithFiles(entity.getId());
        List<GraphEdgeDto> edges = toEdgeDtos(graphEdgeRepository.findByAgentGraphId(entity.getId()));

        List<PlanDto> planDtos = plans.stream()
                .map(this::convertPlanToDto)
                .collect(Collectors.toList());

        List<TaskDto> taskDtos = tasks.stream()
                .map(this::convertTaskToDto)
                .collect(Collectors.toList());

        return new AgentGraphDto(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                convertToDtoStatus(entity.getStatus()),
                planDtos,
                taskDtos,
                edges,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Optimized conversion when entity already has all relationships loaded.
     */
    private AgentGraphDto convertToDtoOptimized(AgentGraphEntity entity) {
        List<GraphEdgeDto> edges = toEdgeDtos(entity.getEdges());

        List<PlanDto> planDtos = entity.getPlans().stream()
                .map(this::convertPlanToDtoOptimized)
                .collect(Collectors.toList());

        List<TaskDto> taskDtos = entity.getTasks().stream()
                .map(this::convertTaskToDtoOptimized)
                .collect(Collectors.toList());

        return new AgentGraphDto(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                convertToDtoStatus(entity.getStatus()),
                planDtos,
                taskDtos,
                edges,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PlanDto convertPlanToDto(PlanEntity planEntity) {
        List<ExecutorFileDto> files = fileService.getPlanFiles(planEntity.getId());

        return new PlanDto(
                planEntity.getName(),
                planEntity.getLabel(),
                files
        );
    }

    private TaskDto convertTaskToDto(TaskEntity taskEntity) {
        List<ExecutorFileDto> files = fileService.getTaskFiles(taskEntity.getId());

        return new TaskDto(
                taskEntity.getName(),
                taskEntity.getLabel(),
                files
        );
    }

    /**
     * Optimized conversion when files are already loaded in the entity.
     */
    private PlanDto convertPlanToDtoOptimized(PlanEntity planEntity) {
        List<ExecutorFileDto> files = planEntity.getFiles().stream()
                .map(fileService::convertToDto)
                .collect(Collectors.toList());

        return new PlanDto(
                planEntity.getName(),
                planEntity.getLabel(),
                files
        );
    }

    /**
     * Optimized conversion when files are already loaded in the entity.
     */
    private TaskDto convertTaskToDtoOptimized(TaskEntity taskEntity) {
        List<ExecutorFileDto> files = taskEntity.getFiles().stream()
                .map(fileService::convertToDto)
                .collect(Collectors.toList());

        return new TaskDto(
                taskEntity.getName(),
                taskEntity.getLabel(),
                files
        );
    }

    private ai.eigloo.agentic.graphcomposer.dto.GraphStatus convertToDtoStatus(GraphStatus entityStatus) {
        return switch (entityStatus) {
            case NEW -> ai.eigloo.agentic.graphcomposer.dto.GraphStatus.NEW;
            case ACTIVE -> ai.eigloo.agentic.graphcomposer.dto.GraphStatus.ACTIVE;
            case ARCHIVED -> ai.eigloo.agentic.graphcomposer.dto.GraphStatus.ARCHIVED;
        };
    }

    private GraphStatus convertToEntityStatus(ai.eigloo.agentic.graphcomposer.dto.GraphStatus dtoStatus) {
        return switch (dtoStatus) {
            case NEW -> GraphStatus.NEW;
            case ACTIVE -> GraphStatus.ACTIVE;
            case ARCHIVED -> GraphStatus.ARCHIVED;
        };
    }

    private static void transitionGraphStatus(AgentGraphEntity graph, GraphStatus targetStatus, String operation) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph is required for status transition");
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target graph status is required");
        }

        GraphStatus currentStatus = graph.getStatus() != null ? graph.getStatus() : GraphStatus.NEW;
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalArgumentException(String.format(
                    "Illegal graph status transition during %s: %s -> %s",
                    operation,
                    currentStatus,
                    targetStatus));
        }
        graph.setStatus(targetStatus);
    }

    private List<String> resolveEntryPlanNames(AgentGraphDto graphDto) {
        if (graphDto.getPlans() == null) {
            return List.of();
        }
        List<GraphEdgeDto> edges = deriveCanonicalEdges(graphDto);
        Set<String> plansWithIncomingTaskEdges = edges.stream()
                .filter(edge -> edge.getFromType() == GraphNodeType.TASK && edge.getToType() == GraphNodeType.PLAN)
                .map(GraphEdgeDto::getTo)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());

        return graphDto.getPlans().stream()
                .map(PlanDto::getName)
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !plansWithIncomingTaskEdges.contains(name))
                .distinct()
                .toList();
    }
    
    /**
     * Updates the graph with plans and tasks using JPA cascading for single-transaction operation.
     * This replaces the old multi-step approach with a single save operation.
     */
    private AgentGraphEntity updateGraphWithCascading(AgentGraphEntity existingGraph, AgentGraphDto graphDto) {
        List<PlanEntity> planEntities = createPlanEntitiesFromDtos(existingGraph, graphDto.getPlans());
        List<TaskEntity> taskEntities = createTaskEntitiesFromDtos(existingGraph, graphDto.getTasks());
        List<GraphEdgeDto> canonicalEdges = deriveCanonicalEdges(graphDto);
        List<GraphEdgeEntity> edgeEntities = createGraphEdgeEntities(existingGraph, taskEntities, planEntities, canonicalEdges);

        existingGraph.replacePlans(planEntities);
        existingGraph.replaceTasks(taskEntities);
        existingGraph.replaceEdges(edgeEntities);

        return agentGraphRepository.save(existingGraph);
    }
    
    /**
     * Creates plan entities from DTOs with their associated files.
     */
    private List<PlanEntity> createPlanEntitiesFromDtos(AgentGraphEntity graph, List<PlanDto> planDtos) {
        List<PlanEntity> planEntities = new ArrayList<>();
        
        if (planDtos != null) {
            for (PlanDto planDto : planDtos) {
                PlanEntity planEntity = new PlanEntity();
                planEntity.setId(UUID.randomUUID().toString());
                planEntity.setName(planDto.getName());
                planEntity.setLabel(planDto.getLabel());
                planEntity.setAgentGraph(graph);
                
                // Create files for this plan
                if (planDto.getFiles() != null) {
                    for (ExecutorFileDto fileDto : planDto.getFiles()) {
                        ExecutorFileEntity fileEntity = createFileEntity(fileDto, planEntity, null);
                        planEntity.addFile(fileEntity);
                    }
                }
                
                planEntities.add(planEntity);
            }
        }
        
        return planEntities;
    }
    
    /**
     * Creates task entities from DTOs with their associated files.
     */
    private List<TaskEntity> createTaskEntitiesFromDtos(AgentGraphEntity graph, List<TaskDto> taskDtos) {
        List<TaskEntity> taskEntities = new ArrayList<>();
        
        if (taskDtos != null) {
            for (TaskDto taskDto : taskDtos) {
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setId(UUID.randomUUID().toString());
                taskEntity.setName(taskDto.getName());
                taskEntity.setLabel(taskDto.getLabel());
                taskEntity.setAgentGraph(graph);
                
                // Create files for this task
                if (taskDto.getFiles() != null) {
                    for (ExecutorFileDto fileDto : taskDto.getFiles()) {
                        ExecutorFileEntity fileEntity = createFileEntity(fileDto, null, taskEntity);
                        taskEntity.addFile(fileEntity);
                    }
                }
                
                taskEntities.add(taskEntity);
            }
        }
        
        return taskEntities;
    }
    
    private List<GraphEdgeEntity> createGraphEdgeEntities(
            AgentGraphEntity graph,
            List<TaskEntity> taskEntities,
            List<PlanEntity> planEntities,
            List<GraphEdgeDto> canonicalEdges) {
        List<GraphEdgeEntity> edgeEntities = new ArrayList<>();
        Map<String, TaskEntity> tasksByName = taskEntities.stream()
                .collect(Collectors.toMap(TaskEntity::getName, task -> task));
        Map<String, PlanEntity> plansByName = planEntities.stream()
                .collect(Collectors.toMap(PlanEntity::getName, plan -> plan));
        Map<String, String> upstreamPlanByTask = new HashMap<>();
        Map<String, String> downstreamPlanByTask = new HashMap<>();
        LinkedHashSet<String> dedupe = new LinkedHashSet<>();

        for (GraphEdgeDto edge : canonicalEdges) {
            if (edge.getFrom() == null || edge.getTo() == null) {
                continue;
            }

            String edgeKey = edge.getFromType() + "|" + edge.getFrom() + "->" + edge.getToType() + "|" + edge.getTo();
            if (!dedupe.add(edgeKey)) {
                continue;
            }

            if (edge.getFromType() == GraphNodeType.PLAN && edge.getToType() == GraphNodeType.TASK) {
                if (!plansByName.containsKey(edge.getFrom()) || !tasksByName.containsKey(edge.getTo())) {
                    throw new GraphValidationException(
                            "Invalid PLAN->TASK edge",
                            List.of("Edge '" + edge.getFrom() + "' -> '" + edge.getTo() + "' references unknown nodes."),
                            List.of());
                }
                String existingUpstreamPlan = upstreamPlanByTask.putIfAbsent(edge.getTo(), edge.getFrom());
                if (existingUpstreamPlan != null && !existingUpstreamPlan.equals(edge.getFrom())) {
                    throw new GraphValidationException(
                            "Task has multiple upstream plans",
                            List.of("Task '" + edge.getTo() + "' has multiple upstream plans in edges."),
                            List.of());
                }
            } else if (edge.getFromType() == GraphNodeType.TASK && edge.getToType() == GraphNodeType.PLAN) {
                if (!tasksByName.containsKey(edge.getFrom()) || !plansByName.containsKey(edge.getTo())) {
                    throw new GraphValidationException(
                            "Invalid TASK->PLAN edge",
                            List.of("Edge '" + edge.getFrom() + "' -> '" + edge.getTo() + "' references unknown nodes."),
                            List.of());
                }
                String existingDownstreamPlan = downstreamPlanByTask.putIfAbsent(edge.getFrom(), edge.getTo());
                if (existingDownstreamPlan != null && !existingDownstreamPlan.equals(edge.getTo())) {
                    throw new GraphValidationException(
                            "Task has multiple downstream plans",
                            List.of("Task '" + edge.getFrom()
                                    + "' has multiple downstream plans in edges. "
                                    + "Current execution routing supports at most one downstream plan per task."),
                            List.of());
                }
            } else {
                throw new GraphValidationException(
                        "Invalid edge type combination",
                        List.of("Edge '" + edge.getFrom() + "' -> '" + edge.getTo()
                                + "' must be PLAN->TASK or TASK->PLAN."),
                        List.of());
            }

            edgeEntities.add(new GraphEdgeEntity(
                    UUID.randomUUID().toString(),
                    graph,
                    edge.getFrom(),
                    toModelNodeType(edge.getFromType()),
                    edge.getTo(),
                    toModelNodeType(edge.getToType())));
        }

        return edgeEntities;
    }
    

    
    /**
     * Creates an ExecutorFileEntity from a DTO for use in cascading operations.
     */
    private ExecutorFileEntity createFileEntity(ExecutorFileDto fileDto, PlanEntity plan, TaskEntity task) {
        ExecutorFileEntity fileEntity = new ExecutorFileEntity();
        fileEntity.setId(UUID.randomUUID().toString());
        fileEntity.setName(fileDto.getName());
        fileEntity.setContents(fileDto.getContents());
        fileEntity.setCreationDate(LocalDateTime.now());
        fileEntity.setVersion("1.0");
        
        if (plan != null) {
            fileEntity.setPlan(plan);
        }
        if (task != null) {
            fileEntity.setTask(task);
        }
        
        return fileEntity;
    }

    private List<GraphEdgeDto> toEdgeDtos(List<GraphEdgeEntity> edgeEntities) {
        if (edgeEntities == null) {
            return List.of();
        }
        return edgeEntities.stream()
                .map(edge -> new GraphEdgeDto(
                        edge.getFromNodeName(),
                        toDtoNodeType(edge.getFromNodeType()),
                        edge.getToNodeName(),
                        toDtoNodeType(edge.getToNodeType())))
                .toList();
    }

    private static ai.eigloo.agentic.graph.model.GraphNodeType toModelNodeType(GraphNodeType type) {
        if (type == null) {
            throw new IllegalArgumentException("Edge node type cannot be null");
        }
        return switch (type) {
            case PLAN -> ai.eigloo.agentic.graph.model.GraphNodeType.PLAN;
            case TASK -> ai.eigloo.agentic.graph.model.GraphNodeType.TASK;
        };
    }

    private static GraphNodeType toDtoNodeType(ai.eigloo.agentic.graph.model.GraphNodeType type) {
        if (type == null) {
            throw new IllegalArgumentException("Edge node type cannot be null");
        }
        return switch (type) {
            case PLAN -> GraphNodeType.PLAN;
            case TASK -> GraphNodeType.TASK;
        };
    }
    
    private List<GraphEdgeDto> deriveCanonicalEdges(AgentGraphDto graphDto) {
        LinkedHashMap<String, GraphEdgeDto> deduped = new LinkedHashMap<>();
        if (graphDto.getEdges() != null) {
            for (GraphEdgeDto edge : graphDto.getEdges()) {
                addEdge(deduped, edge.getFrom(), edge.getFromType(), edge.getTo(), edge.getToType());
            }
        }

        return new ArrayList<>(deduped.values());
    }
    private void addEdge(
            LinkedHashMap<String, GraphEdgeDto> deduped,
            String from,
            GraphNodeType fromType,
            String to,
            GraphNodeType toType) {
        if (from == null || from.isBlank() || to == null || to.isBlank() || fromType == null || toType == null) {
            return;
        }
        String key = fromType + "|" + from + "->" + toType + "|" + to;
        deduped.putIfAbsent(key, new GraphEdgeDto(from, fromType, to, toType));
    }
}
