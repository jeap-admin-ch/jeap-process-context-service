package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "pcs_maintenance_process_data")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class MaintenanceProcessDataEntity {

    @Id
    private UUID id;
    private UUID taskId;
    @Column(name = "key_")
    private String key;
    @Column(name = "value_")
    private String value;
    @Column(name = "role_")
    private String role;

    static MaintenanceProcessDataEntity fromDomain(UUID taskId, ProcessDataValue value) {
        return new MaintenanceProcessDataEntity(
                Generators.timeBasedEpochGenerator().generate(), taskId, value.key(), value.value(), value.role());
    }

    UUID getTaskId() {
        return taskId;
    }

    ProcessDataValue toDomain() {
        return new ProcessDataValue(key, value, role);
    }
}
