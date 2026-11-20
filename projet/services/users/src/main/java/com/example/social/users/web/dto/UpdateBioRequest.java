package com.example.social.users.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateBioRequest(@Size(max = 500) String bio) {}
