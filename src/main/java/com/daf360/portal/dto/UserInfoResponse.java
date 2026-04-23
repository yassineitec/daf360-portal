package com.daf360.portal.dto;

import lombok.Builder;
import lombok.Data;

/**
 * JSON response sent to the Angular frontend via GET /api/me.
 */
@Data
@Builder
public class UserInfoResponse {
    private String azureOid;
    private String displayName;
    private String email;
    private String username;
}
