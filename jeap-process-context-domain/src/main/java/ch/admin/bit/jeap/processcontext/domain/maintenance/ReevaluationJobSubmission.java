package ch.admin.bit.jeap.processcontext.domain.maintenance;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public record ReevaluationJobSubmission(
        UUID jobId,
        String processTemplateName,
        List<String> originProcessIds,
        MaintenanceJobSubmitter submitter) {

    NormalizedReevaluationJobSubmission normalized(MaintenanceProperties.Limits limits) {
        if (jobId == null) {
            throw MaintenanceJobException.invalidRequest("jobId must not be null");
        }
        String normalizedTemplateName = normalizeRequired(processTemplateName, "processTemplateName", limits);
        if (originProcessIds == null || originProcessIds.isEmpty()) {
            throw MaintenanceJobException.invalidRequest("processes must not be empty");
        }
        if (originProcessIds.size() > limits.getMaxTasksPerJob()) {
            throw MaintenanceJobException.invalidRequest("processes exceeds the configured maximum of " + limits.getMaxTasksPerJob());
        }

        List<String> normalizedProcessIds = originProcessIds.stream()
                .map(processId -> normalizeRequired(processId, "originProcessId", limits))
                .sorted()
                .toList();
        if (new HashSet<>(normalizedProcessIds).size() != normalizedProcessIds.size()) {
            throw MaintenanceJobException.invalidRequest("processes contains duplicate originProcessId values");
        }

        String requestHash = requestHash(normalizedTemplateName, normalizedProcessIds);
        return new NormalizedReevaluationJobSubmission(jobId, normalizedTemplateName, normalizedProcessIds, submitter, requestHash);
    }

    private static String normalizeRequired(String value, String fieldName, MaintenanceProperties.Limits limits) {
        if (value == null || value.isBlank()) {
            throw MaintenanceJobException.invalidRequest(fieldName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.getBytes(StandardCharsets.UTF_8).length > limits.getMaxFieldLength()) {
            throw MaintenanceJobException.invalidRequest(fieldName + " exceeds the configured maximum length of " + limits.getMaxFieldLength());
        }
        return normalized;
    }

    private static String requestHash(String processTemplateName, List<String> originProcessIds) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, MaintenanceJobType.RELATION_REEVALUATION.name());
            update(digest, processTemplateName);
            originProcessIds.forEach(processId -> update(digest, processId));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}

record NormalizedReevaluationJobSubmission(
        UUID jobId,
        String processTemplateName,
        List<String> originProcessIds,
        MaintenanceJobSubmitter submitter,
        String requestHash) {
}
