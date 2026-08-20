package ch.admin.bit.jeap.processcontext.adapter.jpa.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface MaintenanceJobJpaRepository extends JpaRepository<MaintenanceJobEntity, UUID> {
}
