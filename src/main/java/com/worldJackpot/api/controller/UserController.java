package com.worldJackpot.api.controller;

import com.worldJackpot.api.dto.user.UserProfileDto;
import com.worldJackpot.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.repository.UserRepository;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user management and profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/{id}")
    @Operation(summary = "Get a user's public profile and their bets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved active user profile"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PutMapping("/me/avatar")
    @Operation(summary = "Update current user's avatar", description = "Updates the avatarId for the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized request")
    })
    public ResponseEntity<Void> updateAvatar(
            @AuthenticationPrincipal UserDetails userDetails, 
            @RequestBody UpdateAvatarRequest request) {
        if (userDetails instanceof User user) {
            user.setAvatarId(request.avatarId());
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).build();
    }

    public record UpdateAvatarRequest(String avatarId) {}
}
