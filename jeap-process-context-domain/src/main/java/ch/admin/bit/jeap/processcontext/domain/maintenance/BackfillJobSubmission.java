package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record BackfillJobSubmission(
        UUID jobId,
        String processTemplateName,
        List<BackfillJobEntry> entries,
        MaintenanceJobSubmitter submitter) {

    NormalizedBackfillJobSubmission normalized(MaintenanceProperties.Limits limits) {
        if (jobId == null) {
            throw MaintenanceJobException.invalidRequest("jobId must not be null");
        }
        String normalizedTemplateName = normalizeRequired(processTemplateName, "processTemplateName", limits);
        if (entries == null || entries.isEmpty()) {
            throw MaintenanceJobException.invalidRequest("entries must not be empty");
        }
        if (entries.size() > limits.getMaxTasksPerJob()) {
            throw MaintenanceJobException.invalidRequest(
                    "entries exceeds the configured maximum of " + limits.getMaxTasksPerJob());
        }

        List<NormalizedBackfillJobEntry> normalizedEntries = new ArrayList<>(entries.size());
        Set<String> processIds = new HashSet<>();
        long processDataValueCount = 0;
        for (BackfillJobEntry entry : entries) {
            if (entry == null) {
                throw MaintenanceJobException.invalidRequest("entries must not contain null values");
            }
            String processId = normalizeRequired(entry.originProcessId(), "originProcessId", limits);
            if (!processIds.add(processId)) {
                throw MaintenanceJobException.invalidRequest("entries contains duplicate originProcessId values");
            }
            List<ProcessDataValue> normalizedProcessData = normalizeProcessData(processId, entry.processData(), limits);
            processDataValueCount += normalizedProcessData.size();
            if (processDataValueCount > limits.getMaxProcessDataValuesPerJob()) {
                throw MaintenanceJobException.invalidRequest("processData exceeds the configured per-job maximum of "
                        + limits.getMaxProcessDataValuesPerJob());
            }
            normalizedEntries.add(new NormalizedBackfillJobEntry(processId, normalizedProcessData));
        }
        normalizedEntries.sort(Comparator.comparing(NormalizedBackfillJobEntry::originProcessId));

        String requestHash = requestHash(normalizedTemplateName, normalizedEntries);
        return new NormalizedBackfillJobSubmission(
                jobId, normalizedTemplateName, normalizedEntries, submitter, requestHash);
    }

    private static List<ProcessDataValue> normalizeProcessData(
            String processId, List<ProcessDataValue> processData, MaintenanceProperties.Limits limits) {
        if (processData == null || processData.isEmpty()) {
            throw MaintenanceJobException.invalidRequest(
                    "processData must not be empty for originProcessId '" + processId + "'");
        }
        if (processData.size() > limits.getMaxProcessDataValuesPerTask()) {
            throw MaintenanceJobException.invalidRequest("processData exceeds the configured maximum of "
                    + limits.getMaxProcessDataValuesPerTask() + " for originProcessId '" + processId + "'");
        }

        List<ProcessDataValue> normalizedValues = ProcessDataValue.canonicalize(processData.stream()
                .map(value -> normalizeValue(value, limits))
                .toList());
        if (new HashSet<>(normalizedValues).size() != normalizedValues.size()) {
            throw MaintenanceJobException.invalidRequest(
                    "processData contains duplicate values for originProcessId '" + processId + "'");
        }
        return normalizedValues;
    }

    private static ProcessDataValue normalizeValue(ProcessDataValue value, MaintenanceProperties.Limits limits) {
        if (value == null) {
            throw MaintenanceJobException.invalidRequest("processData must not contain null values");
        }
        return new ProcessDataValue(
                normalizeRequired(value.key(), "key", limits),
                normalizeRequired(value.value(), "value", limits),
                normalizeOptional(value.role(), "role", limits));
    }

    private static String normalizeRequired(String value, String fieldName, MaintenanceProperties.Limits limits) {
        if (value == null || value.isBlank()) {
            throw MaintenanceJobException.invalidRequest(fieldName + " must not be blank");
        }
        return validateLength(value.trim(), fieldName, limits);
    }

    private static String normalizeOptional(String value, String fieldName, MaintenanceProperties.Limits limits) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return validateLength(value.trim(), fieldName, limits);
    }

    private static String validateLength(String value, String fieldName, MaintenanceProperties.Limits limits) {
        if (value.getBytes(StandardCharsets.UTF_8).length > limits.getMaxFieldLength()) {
            throw MaintenanceJobException.invalidRequest(
                    fieldName + " exceeds the configured maximum length of " + limits.getMaxFieldLength());
        }
        return value;
    }

    private static String requestHash(String processTemplateName, List<NormalizedBackfillJobEntry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, MaintenanceJobType.PROCESS_DATA_BACKFILL.name());
            update(digest, processTemplateName);
            update(digest, entries.size());
            entries.forEach(entry -> {
                update(digest, entry.originProcessId());
                update(digest, entry.processData().size());
                entry.processData().forEach(value -> {
                    update(digest, value.key());
                    update(digest, value.value());
                    updateNullable(digest, value.role());
                });
            });
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

    private static void update(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    private static void updateNullable(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
        } else {
            update(digest, value);
        }
    }
}

record NormalizedBackfillJobSubmission(
        UUID jobId,
        String processTemplateName,
        List<NormalizedBackfillJobEntry> entries,
        MaintenanceJobSubmitter submitter,
        String requestHash) {

    NormalizedBackfillJobSubmission {
        entries = List.copyOf(entries);
    }
}

record NormalizedBackfillJobEntry(String originProcessId, List<ProcessDataValue> processData) {

    NormalizedBackfillJobEntry {
        processData = List.copyOf(processData);
    }
}
