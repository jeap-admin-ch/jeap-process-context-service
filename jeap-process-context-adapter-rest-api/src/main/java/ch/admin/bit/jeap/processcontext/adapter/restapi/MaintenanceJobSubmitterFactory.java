package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobSubmitter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class MaintenanceJobSubmitterFactory {

    private MaintenanceJobSubmitterFactory() {
    }

    static MaintenanceJobSubmitter from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return null;
        }
        return new MaintenanceJobSubmitter(
                jwtAuthenticationToken.getToken().getClaimAsString("name"),
                jwtAuthenticationToken.getToken().getClaimAsString("ext_id"));
    }
}
