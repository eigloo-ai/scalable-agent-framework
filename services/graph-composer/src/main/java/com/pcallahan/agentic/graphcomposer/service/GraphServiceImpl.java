package com.pcallahan.agentic.graphcomposer.service;

import com.pcallahan.agentic.graph.entity.AgentGraphEntity;
import com.pcallahan.agentic.graph.entity.ExecutorFileEntity;
import com.pcallahan.agentic.graph.entity.PlanEntity;
import com.pcallahan.agentic.graph.entity.TaskEntity;
import com.pcallahan.agentic.graph.repository.AgentGraphRepository;
import com.pcallahan.agentic.graph.repository.PlanRepository;
import com.pcallahan.agentic.graph.repository.TaskRepository;
import com.pcallahan.agentic.graphcomposer.dto.*;
import com.pcallahan.agentic.graphcomposer.exception.GraphValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final FileService fileService;
    private final ValidationService validationService;

    @Autowired
    public GraphServiceImpl(AgentGraphRepository agentGraphRepository,
                           PlanRepository planRepository,
                           TaskRepository taskRepository,
                           FileService fileService,
                           ValidationService validationService) {
        this.agentGraphRepository = agentGraphRepository;
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.fileService = fileService;
        this.validationService = validationService;
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
        
        // Use optimized query with fetch joins to avoid N+1 problems
        AgentGraphEntity graph = agentGraphRepository.findByIdAndTenantIdWithAllRelations(graphId, tenantId)
                .orElseThrow(() -> new GraphNotFoundException("Graph not found: " + graphId));
        
        return convertToDtoOptimized(graph);
    }

    @Override
    public AgentGraphDto createGraph(CreateGraphRequest request) {
        logger.info("Creating new graph '{}' for tenant: {}", request.getName(), request.getTenantId());
        
        AgentGraphEntity graph = new AgentGraphEntity();
        graph.setId(UUID.randomUUID().toString());
        graph.setName(request.getName());
        graph.setTenantId(request.getTenantId());
        graph.setStatus(com.pcallahan.agentic.graph.entity.GraphStatus.NEW);
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
            existingGraph.setStatus(convertToEntityStatus(graphDto.getStatus()));
        }
        
        // Update plans and tasks using single-transaction cascading approach
        AgentGraphEntity savedGraph = updateGraphWithCascading(existingGraph, graphDto);
        
        logger.info("Updated graph: {}", savedGraph.getId());
        
        // Return the updated graph DTO
        AgentGraphDto updatedDto = new AgentGraphDto();
        updatedDto.setId(savedGraph.getId());
        updatedDto.setName(savedGraph.getName());
        updatedDto.setTenantId(savedGraph.getTenantId());
        updatedDto.setStatus(convertToDtoStatus(savedGraph.getStatus()));
        updatedDto.setPlans(graphDto.getPlans());
        updatedDto.setTasks(graphDto.getTasks());
        List<PlanDto> plans = graphDto.getPlans() != null ? graphDto.getPlans() : new ArrayList<>();
        List<TaskDto> tasks = graphDto.getTasks() != null ? graphDto.getTasks() : new ArrayList<>();
        updatedDto.setPlanToTasks(computePlanToTasks(plans, tasks));
        updatedDto.setTaskToPlan(computeTaskToPlan(tasks));
        updatedDto.setCreatedAt(savedGraph.getCreatedAt());
        updatedDto.setUpdatedAt(savedGraph.getUpdatedAt());
        
        return updatedDto;
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
        
        // Validate graph before execution
        AgentGraphDto graphDto = convertToDto(graph);
        ValidationResult validation = validationService.validateGraph(graphDto);
        if (!validation.isValid()) {
            throw new GraphValidationException("Cannot execute invalid graph", validation.getErrors(), validation.getWarnings());
        }
        
        // Update status to running
        graph.setStatus(com.pcallahan.agentic.graph.entity.GraphStatus.RUNNING);
        graph.setUpdatedAt(LocalDateTime.now());
        agentGraphRepository.save(graph);
        
        // Create execution response (placeholder implementation)
        ExecutionResponse response = new ExecutionResponse();
        response.setExecutionId(UUID.randomUUID().toString());
        response.setStatus("SUBMITTED");
        response.setMessage("Graph submitted for execution");
        
        logger.info("Graph {} submitted for execution with ID: {}", graphId, response.getExecutionId());
        return response;
    }

    @Override
    public void updateGraphStatus(String graphId, GraphStatusUpdate statusUpdate) {
        logger.info("Updating status for graph {} to: {}", graphId, statusUpdate.getStatus());
        
        AgentGraphEntity graph = agentGraphRepository.findById(graphId)
                .orElseThrow(() -> new GraphNotFoundException("Graph not found: " + graphId));
        
        graph.setStatus(convertToEntityStatus(statusUpdate.getStatus()));
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
        
        // Compute planToTasks and taskToPlan mappings
        Map<String, Set<String>> planToTasks = computePlanToTasks(planDtos, taskDtos);
        Map<String, String> taskToPlan = computeTaskToPlan(taskDtos);
        
        return new AgentGraphDto(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                convertToDtoStatus(entity.getStatus()),
                planDtos,
                taskDtos,
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
        
        // Compute planToTasks and taskToPlan mappings
        Map<String, Set<String>> planToTasks = computePlanToTasks(planDtos, taskDtos);
        Map<String, String> taskToPlan = computeTaskToPlan(taskDtos);
        
        return new AgentGraphDto(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                convertToDtoStatus(entity.getStatus()),
                planDtos,
                taskDtos,
                planToTasks,
                taskToPlan,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PlanDto convertPlanToDto(PlanEntity planEntity, List<TaskEntity> allTasks) {
        List<ExecutorFileDto> files = fileService.getPlanFiles(planEntity.getId());
        
        // Find upstream tasks for this plan
        Set<String> upstreamTaskIds = allTasks.stream()
                .filter(task -> task.getUpstreamPlan() != null && planEntity.getName().equals(task.getUpstreamPlan().getName()))
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
        
        // Find upstream tasks for this plan
        Set<String> upstreamTaskIds = allTasks.stream()
                .filter(task -> task.getUpstreamPlan() != null && planEntity.getName().equals(task.getUpstreamPlan().getName()))
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

    private GraphStatus convertToDtoStatus(com.pcallahan.agentic.graph.entity.GraphStatus entityStatus) {
        return switch (entityStatus) {
            case NEW -> GraphStatus.NEW;
            case RUNNING -> GraphStatus.RUNNING;
            case STOPPED -> GraphStatus.STOPPED;
            case ERROR -> GraphStatus.ERROR;
        };
    }

    private com.pcallahan.agentic.graph.entity.GraphStatus convertToEntityStatus(GraphStatus dtoStatus) {
        return switch (dtoStatus) {
            case NEW -> com.pcallahan.agentic.graph.entity.GraphStatus.NEW;
            case RUNNING -> com.pcallahan.agentic.graph.entity.GraphStatus.RUNNING;
            case STOPPED -> com.pcallahan.agentic.graph.entity.GraphStatus.STOPPED;
            case ERROR -> com.pcallahan.agentic.graph.entity.GraphStatus.ERROR;
        };
    }
    
    /**
     * Updates the graph with plans and tasks using JPA cascading for single-transaction operation.
     * This replaces the old multi-step approach with a single save operation.
     */
    private AgentGraphEntity updateGraphWithCascading(AgentGraphEntity existingGraph, AgentGraphDto graphDto) {
        // Create plan entities with files
        List<PlanEntity> planEntities = createPlanEntitiesFromDtos(existingGraph, graphDto.getPlans());
        
        // Create task entities with files and establish relationships
        List<TaskEntity> taskEntities = createTaskEntitiesFromDtos(existingGraph, graphDto.getTasks(), planEntities);
        
        // Replace plans and tasks using cascade operations
        existingGraph.replacePlans(planEntities);
        existingGraph.replaceTasks(taskEntities);
        
        // Establish task-plan relationships after all entities are created
        establishTaskPlanRelationships(taskEntities, planEntities, graphDto);
        
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
    private List<TaskEntity> createTaskEntitiesFromDtos(AgentGraphEntity graph, List<TaskDto> taskDtos, List<PlanEntity> planEntities) {
        List<TaskEntity> taskEntities = new ArrayList<>();
        
        if (taskDtos != null) {
            for (TaskDto taskDto : taskDtos) {
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setId(UUID.randomUUID().toString());
                taskEntity.setName(taskDto.getName());
                taskEntity.setLabel(taskDto.getLabel());
                taskEntity.setAgentGraph(graph);
                
                // Find upstream plan by name
                if (taskDto.getUpstreamPlanId() != null && !taskDto.getUpstreamPlanId().isEmpty()) {
                    PlanEntity upstreamPlan = planEntities.stream()
                            .filter(p -> p.getName().equals(taskDto.getUpstreamPlanId()))
                            .findFirst()
                            .orElse(null);
                    taskEntity.setUpstreamPlan(upstreamPlan);
                }
                
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
     * Establishes task-plan relationships based on the graph structure.
     */
    private void establishTaskPlanRelationships(List<TaskEntity> taskEntities, List<PlanEntity> planEntities, AgentGraphDto graphDto) {
        // Use the planToTasks and taskToPlan mappings to establish downstream relationships
        if (graphDto.getTaskToPlan() != null) {
            for (TaskEntity task : taskEntities) {
                String downstreamPlanName = graphDto.getTaskToPlan().get(task.getName());
                if (downstreamPlanName != null) {
                    PlanEntity downstreamPlan = planEntities.stream()
                            .filter(p -> p.getName().equals(downstreamPlanName))
                            .findFirst()
                            .orElse(null);
                    task.setDownstreamPlan(downstreamPlan);
                }
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
    
    /**
     * Compute planToTasks mapping from DTOs.
     */
    private Map<String, Set<String>> computePlanToTasks(List<PlanDto> planDtos, List<TaskDto> taskDtos) {
        Map<String, Set<String>> planToTasks = new HashMap<>();
        
        if (planDtos != null) {
            // Initialize all plans with empty sets
            for (PlanDto plan : planDtos) {
                planToTasks.put(plan.getName(), new HashSet<>());
            }
        }
        
        if (taskDtos != null) {
            // Add tasks to their upstream plans
            for (TaskDto task : taskDtos) {
                if (task.getUpstreamPlanId() != null) {
                    planToTasks.computeIfAbsent(task.getUpstreamPlanId(), k -> new HashSet<>())
                              .add(task.getName());
                }
            }
        }
        
        return planToTasks;
    }
    
    /**
     * Compute taskToPlan mapping from DTOs.
     */
    private Map<String, String> computeTaskToPlan(List<TaskDto> taskDtos) {
        Map<String, String> taskToPlan = new HashMap<>();
        
        if (taskDtos != null) {
            for (TaskDto task : taskDtos) {
                if (task.getUpstreamPlanId() != null) {
                    taskToPlan.put(task.getName(), task.getUpstreamPlanId());
                }
            }
        }
        
        return taskToPlan;
    }
}