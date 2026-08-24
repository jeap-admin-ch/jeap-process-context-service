package ch.admin.bit.jeap.processcontext.domain.maintenance;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public record RelationPublicationJobSubmission(
        UUID jobId,
        List<UUID> relationIds,
        MaintenanceJobSubmitter submitter) {

    NormalizedRelationPublicationJobSubmission normalized(MaintenanceProperties.Limits limits) {
        if (jobId == null) {
            throw MaintenanceJobException.invalidRequest("jobId must not be null");
        }
        if (relationIds == null || relationIds.isEmpty()) {
            throw MaintenanceJobException.invalidRequest("relations must not be empty");
        }
        if (relationIds.size() > limits.getMaxTasksPerJob()) {
            throw MaintenanceJobException.invalidRequest(
                    "relations exceeds the configured maximum of " + limits.getMaxTasksPerJob());
        }
        if (relationIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw MaintenanceJobException.invalidRequest("relationId must not be null");
        }

        List<UUID> normalizedRelationIds = relationIds.stream().sorted().toList();
        if (new HashSet<>(normalizedRelationIds).size() != normalizedRelationIds.size()) {
            throw MaintenanceJobException.invalidRequest("relations contains duplicate relationId values");
        }
        return new NormalizedRelationPublicationJobSubmission(
                jobId, normalizedRelationIds, submitter, requestHash(normalizedRelationIds));
    }

    private static String requestHash(List<UUID> relationIds) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, MaintenanceJobType.RELATION_REPUBLICATION.name());
            relationIds.forEach(relationId -> update(digest, relationId.toString()));
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

record NormalizedRelationPublicationJobSubmission(
        UUID jobId,
        List<UUID> relationIds,
        MaintenanceJobSubmitter submitter,
        String requestHash) {
}
