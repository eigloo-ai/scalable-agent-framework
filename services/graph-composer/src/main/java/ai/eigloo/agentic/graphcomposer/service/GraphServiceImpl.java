package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graph.entity.GraphStatus;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
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
    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final GraphRunRepository graphRunRepository;
    private final FileService fileService;
    private final ValidationService validationService;
    private final GraphExecutionBootstrapPublisher graphExecutionBootstrapPublisher;

    @Autowired
    public GraphServiceImpl(AgentGraphRepository agentGraphRepository,
                           PlanRepository planRepository,
                           TaskRepository taskRepository,
                           GraphRunRepository graphRunRepository,
                           FileService fileService,
                           ValidationService validationService,
                           GraphExecutionBootstrapPublisher graphExecutionBootstrapPublisher) {
        this.agentGraphRepository = agentGraphRepository;
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
        // Get plans and tasks for this graph using optimized queries
        List<PlanEntity> plans = planRepository.findByAgentGraphIdWithFiles(entity.getId());
        List<TaskEntity> tasks = taskRepository.findByAgentGraphIdWithFiles(entity.getId());
        
        List<PlanDto> planDtos = plans.stream()
                .map(planEntity -> convertPlanToDto(planEntity, tasks))
                .collect(Collectors.toList());
        
        List<TaskDto> taskDtos = tasks.stream()
                .map(taskEntity -> convertTaskToDto(taskEntity, plans))
                .collect(Collectors.toList());
        
        List<GraphEdgeDto> edges = deriveEdgesFromDtos(planDtos, taskDtos);
        Map<String, Set<String>> planToTasks = computePlanToTasks(edges, planDtos);
        Map<String, String> taskToPlan = computeTaskToUpstreamPlan(edges, taskDtos);
        
        return new AgentGraphDto(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                convertToDtoStatus(entity.getStatus()),
                planDtos,
                taskDtos,
                edges,
                planToTasks,
                taskToPlan,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Optimized conversion when entity already has all relationships loaded.
     */
    private AgentGraphDto convertToDtoOptimized(AgentGraphEntity entity) {
        List<PlanDto> planDtos = entity.getPlans().stream()
                .map(planEntity -> convertPlanToDtoOptimized(planEntity, entity.getTasks()))
                .collect(Collectors.toList());
        
        List<TaskDto> taskDtos = entity.getTasks().stream()
                .map(taskEntity -> convertTaskToDtoOptimized(taskEntity, entity.getPlans()))
                .collect(Collectors.toList());
        
        List<GraphEdgeDto> edges = deriveEdgesFromDtos(planDtos, taskDtos);
        Map<String, Set<String>> planToTasks = computePlanToTasks(edges, planDtos);
        Map<String, String> taskToPlan = computeTaskToUpstreamPlan(edges, taskDtos);
        
        return new AgentGraphDto(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                convertToDtoStatus(entity.getStatus()),
                planDtos,
                taskDtos,
                edges,
                planToTasks,
                taskToPlan,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PlanDto convertPlanToDto(PlanEntity planEntity, List<TaskEntity> allTasks) {
        List<ExecutorFileDto> files = fileService.getPlanFiles(planEntity.getId());
        
        // Find tasks that feed into this plan (Task -> Plan relationships)
        Set<String> upstreamTaskIds = allTasks.stream()
                .filter(task -> task.getDownstreamPlan() != null && planEntity.getName().equals(task.getDownstreamPlan().getName()))
                .map(TaskEntity::getName)
                .collect(Collectors.toSet());
        
        return new PlanDto(
                planEntity.getName(),
                planEntity.getLabel(),
                upstreamTaskIds,
                files
        );
    }

    private TaskDto convertTaskToDto(TaskEntity taskEntity, List<PlanEntity> allPlans) {
        List<ExecutorFileDto> files = fileService.getTaskFiles(taskEntity.getId());
        
        String upstreamPlanId = taskEntity.getUpstreamPlan() != null ? taskEntity.getUpstreamPlan().getName() : null;
        
        return new TaskDto(
                taskEntity.getName(),
                taskEntity.getLabel(),
                upstreamPlanId,
                files
        );
    }

    /**
     * Optimized conversion when files are already loaded in the entity.
     */
    private PlanDto convertPlanToDtoOptimized(PlanEntity planEntity, List<TaskEntity> allTasks) {
        List<ExecutorFileDto> files = planEntity.getFiles().stream()
                .map(fileService::convertToDto)
                .collect(Collectors.toList());
        
        // Find tasks that feed into this plan (Task -> Plan relationships)
        Set<String> upstreamTaskIds = allTasks.stream()
                .filter(task -> task.getDownstreamPlan() != null && planEntity.getName().equals(task.getDownstreamPlan().getName()))
                .map(TaskEntity::getName)
                .collect(Collectors.toSet());
        
        return new PlanDto(
                planEntity.getName(),
                planEntity.getLabel(),
                upstreamTaskIds,
                files
        );
    }

    /**
     * Optimized conversion when files are already loaded in the entity.
     */
    private TaskDto convertTaskToDtoOptimized(TaskEntity taskEntity, List<PlanEntity> allPlans) {
        List<ExecutorFileDto> files = taskEntity.getFiles().stream()
                .map(fileService::convertToDto)
                .collect(Collectors.toList());
        
        String upstreamPlanId = taskEntity.getUpstreamPlan() != null ? taskEntity.getUpstreamPlan().getName() : null;
        
        return new TaskDto(
                taskEntity.getName(),
                taskEntity.getLabel(),
                upstreamPlanId,
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
        // Create plan entities with files
        List<PlanEntity> planEntities = createPlanEntitiesFromDtos(existingGraph, graphDto.getPlans());
        
        // Create task entities with files and establish relationships
        List<TaskEntity> taskEntities = createTaskEntitiesFromDtos(existingGraph, graphDto.getTasks());
        
        // Replace plans and tasks using cascade operations
        existingGraph.replacePlans(planEntities);
        existingGraph.replaceTasks(taskEntities);
        
        // Establish plan/task relationships from canonical directed edges.
        establishTaskPlanRelationships(taskEntities, planEntities, deriveCanonicalEdges(graphDto));
        
        // Single save operation - JPA will cascade to save all plans, tasks, and files
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
    
    /**
     * Establishes task relationships from canonical directed edges.
     */
    private void establishTaskPlanRelationships(
            List<TaskEntity> taskEntities,
            List<PlanEntity> planEntities,
            List<GraphEdgeDto> canonicalEdges) {
        Map<String, TaskEntity> tasksByName = taskEntities.stream()
                .collect(Collectors.toMap(TaskEntity::getName, task -> task));
        Map<String, PlanEntity> plansByName = planEntities.stream()
                .collect(Collectors.toMap(PlanEntity::getName, plan -> plan));

        for (GraphEdgeDto edge : canonicalEdges) {
            if (edge.getFrom() == null || edge.getTo() == null) {
                continue;
            }

            if (edge.getFromType() == GraphNodeType.PLAN && edge.getToType() == GraphNodeType.TASK) {
                PlanEntity upstreamPlan = plansByName.get(edge.getFrom());
                TaskEntity task = tasksByName.get(edge.getTo());
                if (upstreamPlan == null || task == null) {
                    continue;
                }
                if (task.getUpstreamPlan() != null && !upstreamPlan.getName().equals(task.getUpstreamPlan().getName())) {
                    throw new GraphValidationException(
                            "Task has multiple upstream plans",
                            List.of("Task '" + task.getName() + "' has multiple upstream plans in edges."),
                            List.of());
                }
                task.setUpstreamPlan(upstreamPlan);
                continue;
            }

            if (edge.getFromType() == GraphNodeType.TASK && edge.getToType() == GraphNodeType.PLAN) {
                TaskEntity task = tasksByName.get(edge.getFrom());
                PlanEntity downstreamPlan = plansByName.get(edge.getTo());
                if (task == null || downstreamPlan == null) {
                    continue;
                }
                if (task.getDownstreamPlan() != null && !downstreamPlan.getName().equals(task.getDownstreamPlan().getName())) {
                    throw new GraphValidationException(
                            "Task has multiple downstream plans",
                            List.of("Task '" + task.getName()
                                    + "' has multiple downstream plans in edges. "
                                    + "Current persistence supports at most one downstream plan per task."),
                            List.of());
                }
                task.setDownstreamPlan(downstreamPlan);
            }
        }
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
    
    private List<GraphEdgeDto> deriveCanonicalEdges(AgentGraphDto graphDto) {
        LinkedHashMap<String, GraphEdgeDto> deduped = new LinkedHashMap<>();

        if (graphDto.getEdges() != null && !graphDto.getEdges().isEmpty()) {
            for (GraphEdgeDto edge : graphDto.getEdges()) {
                addEdge(deduped, edge.getFrom(), edge.getFromType(), edge.getTo(), edge.getToType());
            }
            return new ArrayList<>(deduped.values());
        }

        for (GraphEdgeDto edge : deriveEdgesFromDtos(graphDto.getPlans(), graphDto.getTasks())) {
            addEdge(deduped, edge.getFrom(), edge.getFromType(), edge.getTo(), edge.getToType());
        }

        if (graphDto.getPlanToTasks() != null) {
            for (Map.Entry<String, Set<String>> entry : graphDto.getPlanToTasks().entrySet()) {
                for (String taskName : entry.getValue()) {
                    addEdge(deduped, entry.getKey(), GraphNodeType.PLAN, taskName, GraphNodeType.TASK);
                }
            }
        }

        // Legacy semantics for taskToPlan map in existing clients: Task -> upstream Plan.
        if (graphDto.getTaskToPlan() != null) {
            for (Map.Entry<String, String> entry : graphDto.getTaskToPlan().entrySet()) {
                addEdge(deduped, entry.getValue(), GraphNodeType.PLAN, entry.getKey(), GraphNodeType.TASK);
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private List<GraphEdgeDto> deriveEdgesFromDtos(List<PlanDto> planDtos, List<TaskDto> taskDtos) {
        LinkedHashMap<String, GraphEdgeDto> deduped = new LinkedHashMap<>();

        if (taskDtos != null) {
            for (TaskDto task : taskDtos) {
                if (task.getUpstreamPlanId() != null && !task.getUpstreamPlanId().isBlank()) {
                    addEdge(deduped, task.getUpstreamPlanId(), GraphNodeType.PLAN, task.getName(), GraphNodeType.TASK);
                }
            }
        }

        if (planDtos != null) {
            for (PlanDto plan : planDtos) {
                if (plan.getUpstreamTaskIds() == null) {
                    continue;
                }
                for (String upstreamTaskName : plan.getUpstreamTaskIds()) {
                    addEdge(deduped, upstreamTaskName, GraphNodeType.TASK, plan.getName(), GraphNodeType.PLAN);
                }
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private Map<String, Set<String>> computePlanToTasks(List<GraphEdgeDto> edges, List<PlanDto> plans) {
        Map<String, Set<String>> planToTasks = new HashMap<>();
        if (plans != null) {
            for (PlanDto plan : plans) {
                if (plan.getName() != null && !plan.getName().isBlank()) {
                    planToTasks.put(plan.getName(), new LinkedHashSet<>());
                }
            }
        }

        for (GraphEdgeDto edge : edges) {
            if (edge.getFromType() == GraphNodeType.PLAN && edge.getToType() == GraphNodeType.TASK) {
                planToTasks.computeIfAbsent(edge.getFrom(), ignored -> new LinkedHashSet<>()).add(edge.getTo());
            }
        }
        return planToTasks;
    }

    /**
     * Compatibility map preserving legacy Task -> upstream Plan semantics.
     */
    private Map<String, String> computeTaskToUpstreamPlan(List<GraphEdgeDto> edges, List<TaskDto> tasks) {
        Map<String, String> taskToPlan = new HashMap<>();
        Set<String> knownTasks = new HashSet<>();
        if (tasks != null) {
            knownTasks.addAll(tasks.stream()
                    .map(TaskDto::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toSet()));
        }

        for (GraphEdgeDto edge : edges) {
            if (edge.getFromType() != GraphNodeType.PLAN || edge.getToType() != GraphNodeType.TASK) {
                continue;
            }
            if (!knownTasks.isEmpty() && !knownTasks.contains(edge.getTo())) {
                continue;
            }
            taskToPlan.putIfAbsent(edge.getTo(), edge.getFrom());
        }
        return taskToPlan;
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
