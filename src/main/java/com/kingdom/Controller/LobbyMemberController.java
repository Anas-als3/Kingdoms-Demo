package com.kingdom.Controller;

import com.kingdom.API.ApiResponse;
import com.kingdom.Config.CustomUserDetails;
import com.kingdom.Enums.MemberRole;
import com.kingdom.Service.LobbyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lobby-member")
@RequiredArgsConstructor
public class LobbyMemberController {

    private final LobbyMemberService lobbyMemberService;
    @GetMapping("/get")
    public ResponseEntity<?> getAllLobbyMembers() {
        return ResponseEntity.status(200).body(lobbyMemberService.getAllLobbyMembers());
    }

    @GetMapping("/get/{memberId}")
    public ResponseEntity<?> getLobbyMemberById(@PathVariable Integer memberId) {
        return ResponseEntity.status(200).body(lobbyMemberService.getLobbyMemberById(memberId));
    }

    @PostMapping("/join/{lobbyId}")
    public ResponseEntity<?> joinLobby(@PathVariable Integer lobbyId, @AuthenticationPrincipal CustomUserDetails me) {
        lobbyMemberService.joinLobby(lobbyId, me.getId());
        return ResponseEntity.status(200).body(new ApiResponse("تم الانضمام إلى اللوبي بنجاح"));
    }

    @PutMapping("/update-role/{memberId}/{role}")
    public ResponseEntity<?> updateLobbyMemberRole(@PathVariable Integer memberId, @PathVariable MemberRole role) {
        lobbyMemberService.updateLobbyMemberRole(memberId, role);
        return ResponseEntity.status(200).body(new ApiResponse("تم تحديث دور عضو اللوبي بنجاح"));
    }

    @DeleteMapping("/delete/{memberId}")
    public ResponseEntity<?> deleteLobbyMember(@PathVariable Integer memberId) {
        lobbyMemberService.deleteLobbyMember(memberId);
        return ResponseEntity.status(200).body(new ApiResponse("تم حذف عضو اللوبي بنجاح"));
    }

    @GetMapping("/members/{lobbyId}")
    public ResponseEntity<?> getMembers(@PathVariable Integer lobbyId) {
        return ResponseEntity.status(200).body(lobbyMemberService.getMembers(lobbyId));
    }

    @DeleteMapping("/leave/{lobbyId}")
    public ResponseEntity<?> leaveLobby(@PathVariable Integer lobbyId, @AuthenticationPrincipal CustomUserDetails me) {
        lobbyMemberService.leaveLobby(lobbyId, me.getId());
        return ResponseEntity.status(200).body(new ApiResponse("تم مغادرة اللوبي بنجاح"));
    }
    @DeleteMapping("/kick/{lobbyId}/{targetPlayerId}")
    public ResponseEntity<?> kickMember(@PathVariable Integer lobbyId, @AuthenticationPrincipal CustomUserDetails me, @PathVariable Integer targetPlayerId) {
        lobbyMemberService.kickMember(lobbyId, me.getId(), targetPlayerId);
        return ResponseEntity.status(200).body(new ApiResponse("تم طرد العضو بنجاح"));
    }

    @PostMapping("/join-private/{inviteCode}")
    public ResponseEntity<?> joinPrivateLobbyByInviteCode(@PathVariable String inviteCode, @AuthenticationPrincipal CustomUserDetails me) {
        lobbyMemberService.joinPrivateLobbyByInviteCode(inviteCode, me.getId());
        return ResponseEntity.ok(new ApiResponse("تم الانضمام إلى اللوبي الخاص بنجاح"));
    }
}
