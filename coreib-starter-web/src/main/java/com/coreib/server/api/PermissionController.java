package com.coreib.server.api;

import com.coreib.security.CurrentPermissionProvider;
import com.coreib.security.PermissionSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Stable permission contract consumed by the Vue admin shell. */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {
    private final CurrentPermissionProvider permissionProvider;

    public PermissionController(CurrentPermissionProvider permissionProvider) {
        this.permissionProvider = permissionProvider;
    }

    @GetMapping("/effective")
    public PermissionSnapshot effective() {
        return permissionProvider.current();
    }
}
