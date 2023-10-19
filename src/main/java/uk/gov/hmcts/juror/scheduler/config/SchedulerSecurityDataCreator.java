package uk.gov.hmcts.juror.scheduler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.juror.standard.datastore.entity.auth.Role;
import uk.gov.hmcts.juror.standard.service.contracts.auth.PermissionService;
import uk.gov.hmcts.juror.standard.service.contracts.auth.RoleService;

import java.util.Set;

@Slf4j
@Order(2)
@Component
public class SchedulerSecurityDataCreator implements ApplicationRunner {

    private final RoleService roleService;
    private final PermissionService permissionService;


    public SchedulerSecurityDataCreator(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role userRole = roleService.getOrCreateRole("USER");
        Role externalAPIRole = roleService.getOrCreateRole("EXTERNAL_API");

        Set<Role> userSet = Set.of(userRole);
        permissionService.getOrCreatePermission(PermissionConstants.Job.ENABLE, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.DISABLE, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.DELETE, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.RUN, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.API_CREATE, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.API_UPDATE, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.VIEW, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.SEARCH, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Job.VIEW_STATUS, Set.of(externalAPIRole));

        permissionService.getOrCreatePermission(PermissionConstants.Task.VIEW, userSet);
        permissionService.getOrCreatePermission(PermissionConstants.Task.STATUS_UPDATE,
            Set.of(userRole, externalAPIRole));
        permissionService.getOrCreatePermission(PermissionConstants.Task.SEARCH, userSet);
    }


}
