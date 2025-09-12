package com.retailai.web;

import com.retailai.model.AppUser;
import com.retailai.security.CurrentUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalUserModel {
    private final CurrentUser currentUser;
    public GlobalUserModel(CurrentUser currentUser) { this.currentUser = currentUser; }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        return currentUser.get().orElse(null); // available in Thymeleaf as ${currentUser}
    }
}
