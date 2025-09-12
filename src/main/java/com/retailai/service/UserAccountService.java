package com.retailai.service;


import com.retailai.api.dto.UserProfileResponse;

public interface UserAccountService {
    void updatePassword(String username, String currentPassword, String newPassword, String confirmPassword);
    void updateQcallApiKey(String username, String apiKey);
    void revokeOtherSessions(String username);
    UserProfileResponse me(String username);
}
