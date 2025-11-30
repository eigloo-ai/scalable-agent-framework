package ai.eigloo.agentic.common;

import com.google.protobuf.InvalidProtocolBufferException;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.TaskInput;
import ai.eigloo.proto.model.Common.TaskResult;
import ai.eigloo.proto.model.Common.PlanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for protobuf serialization and deserialization for Kafka messaging.
 * Provides methods to convert protobuf messages to/from byte arrays.
 */
public class ProtobufUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ProtobufUtils.class);
    
    private ProtobufUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Serialize a TaskExecution protobuf message to byte array.
     * 
     * @param taskExecution the TaskExecution message to serialize
     * @return byte array representation, or null if serialization fails
     */
    public static byte[] serializeTaskExecution(TaskExecution taskExecution) {
        try {
            if (taskExecution == null) {
                logger.warn("Cannot serialize null TaskExecution");
                return null;
            }
            return taskExecution.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to serialize TaskExecution: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Deserialize a byte array to TaskExecution protobuf message.
     * 
     * @param data the byte array to deserialize
     * @return TaskExecution message, or null if deserialization fails
     */
    public static TaskExecution deserializeTaskExecution(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                logger.warn("Cannot deserialize null or empty byte array to TaskExecution");
                return null;
            }
            return TaskExecution.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize TaskExecution from byte array: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Serialize a PlanInput protobuf message to byte array.
     * 
     * @param planInput the PlanInput message to serialize
     * @return byte array representation, or null if serialization fails
     */
    public static byte[] serializePlanInput(PlanInput planInput) {
        try {
            if (planInput == null) {
                logger.warn("Cannot serialize null PlanInput");
                return null;
            }
            return planInput.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to serialize PlanInput: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Deserialize a byte array to PlanInput protobuf message.
     * 
     * @param data the byte array to deserialize
     * @return PlanInput message, or null if deserialization fails
     */
    public static PlanInput deserializePlanInput(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                logger.warn("Cannot deserialize null or empty byte array to PlanInput");
                return null;
            }
            return PlanInput.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize PlanInput from byte array: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Serialize a PlanExecution protobuf message to byte array.
     * 
     * @param planExecution the PlanExecution message to serialize
     * @return byte array representation, or null if serialization fails
     */
    public static byte[] serializePlanExecution(PlanExecution planExecution) {
        try {
            if (planExecution == null) {
                logger.warn("Cannot serialize null PlanExecution");
                return null;
            }
            return planExecution.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to serialize PlanExecution: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Deserialize a byte array to PlanExecution protobuf message.
     * 
     * @param data the byte array to deserialize
     * @return PlanExecution message, or null if deserialization fails
     */
    public static PlanExecution deserializePlanExecution(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                logger.warn("Cannot deserialize null or empty byte array to PlanExecution");
                return null;
            }
            return PlanExecution.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize PlanExecution from byte array: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Serialize a TaskInput protobuf message to byte array.
     * 
     * @param taskInput the TaskInput message to serialize
     * @return byte array representation, or null if serialization fails
     */
    public static byte[] serializeTaskInput(TaskInput taskInput) {
        try {
            if (taskInput == null) {
                logger.warn("Cannot serialize null TaskInput");
                return null;
            }
            return taskInput.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to serialize TaskInput: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Deserialize a byte array to TaskInput protobuf message.
     * 
     * @param data the byte array to deserialize
     * @return TaskInput message, or null if deserialization fails
     */
    public static TaskInput deserializeTaskInput(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                logger.warn("Cannot deserialize null or empty byte array to TaskInput");
                return null;
            }
            return TaskInput.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize TaskInput from byte array: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Serialize a TaskResult protobuf message to byte array.
     * 
     * @param taskResult the TaskResult message to serialize
     * @return byte array representation, or null if serialization fails
     */
    public static byte[] serializeTaskResult(TaskResult taskResult) {
        try {
            if (taskResult == null) {
                logger.warn("Cannot serialize null TaskResult");
                return null;
            }
            return taskResult.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to serialize TaskResult: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Deserialize a byte array to TaskResult protobuf message.
     * 
     * @param data the byte array to deserialize
     * @return TaskResult message, or null if deserialization fails
     */
    public static TaskResult deserializeTaskResult(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                logger.warn("Cannot deserialize null or empty byte array to TaskResult");
                return null;
            }
            return TaskResult.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize TaskResult from byte array: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Serialize a PlanResult protobuf message to byte array.
     * 
     * @param planResult the PlanResult message to serialize
     * @return byte array representation, or null if serialization fails
     */
    public static byte[] serializePlanResult(PlanResult planResult) {
        try {
            if (planResult == null) {
                logger.warn("Cannot serialize null PlanResult");
                return null;
            }
            return planResult.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to serialize PlanResult: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Deserialize a byte array to PlanResult protobuf message.
     * 
     * @param data the byte array to deserialize
     * @return PlanResult message, or null if deserialization fails
     */
    public static PlanResult deserializePlanResult(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                logger.warn("Cannot deserialize null or empty byte array to PlanResult");
                return null;
            }
            return PlanResult.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to deserialize PlanResult from byte array: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Validate if a protobuf message is valid.
     * 
     * @param message the protobuf message to validate
     * @return true if the message is valid, false otherwise
     */
    public static boolean isValidMessage(com.google.protobuf.Message message) {
        if (message == null) {
            return false;
        }
        
        try {
            return message.isInitialized();
        } catch (Exception e) {
            logger.error("Error validating protobuf message: {}", e.getMessage(), e);
            return false;
        }
    }
} 