package com.kingdom.Service;

import com.kingdom.API.ApiException;
import com.kingdom.Config.AuthUtil;
import com.kingdom.DTO.OUT.LobbyMemberOut;
import com.kingdom.Enums.InviteStatus;
import com.kingdom.Enums.LobbyStatus;
import com.kingdom.Enums.LobbyVisibility;
import com.kingdom.Enums.MemberRole;
import com.kingdom.Model.*;
import com.kingdom.Repository.KingdomMembershipRepository;
import com.kingdom.Repository.LobbyInviteRepository;
import com.kingdom.Repository.LobbyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LobbyMemberService {

   private final ModelMapper modelMapper;
   private final LobbyMemberRepository lobbyMemberRepository;
   private final LobbyInviteRepository lobbyInviteRepository;
   private final KingdomMembershipRepository kingdomMembershipRepository;
   private final LobbyService lobbyService;
   private final PlayerService playerService;
   private final WhatsAppService whatsAppService;

   public List<LobbyMemberOut> getAllLobbyMembers() {
      List<LobbyMemberOut> members = new ArrayList<>();
      for (LobbyMember member : lobbyMemberRepository.findAll()) {
         members.add(toMemberOut(member));
      }
      return members;
   }

   public LobbyMemberOut getLobbyMemberById(Integer memberId) {
      LobbyMember member = lobbyMemberRepository.findLobbyMemberById(memberId);
      if (member == null) {
         throw new ApiException("عضو اللوبي غير موجود");
      }
      return toMemberOut(member);
   }

   public void updateLobbyMemberRole(Integer memberId, MemberRole role) {
      LobbyMember member = lobbyMemberRepository.findLobbyMemberById(memberId);
      if (member == null) {
         throw new ApiException("عضو اللوبي غير موجود");
      }
      member.setRole(role);
      lobbyMemberRepository.save(member);
   }

   public void deleteLobbyMember(Integer memberId) {
      LobbyMember member = lobbyMemberRepository.findLobbyMemberById(memberId);
      if (member == null) {
         throw new ApiException("عضو اللوبي غير موجود");
      }
      lobbyMemberRepository.delete(member);
   }

   public void joinPrivateLobbyByInviteCode(String inviteCode, Integer playerId) {
      Player player = playerService.checkPlayer(playerId);

      LobbyInvite invite = lobbyInviteRepository.findByInviteCode(inviteCode);

      if (invite == null) {
         throw new ApiException("رمز الدعوة غير صالح");
      }

      if (!invite.getInvitedPlayer().getId().equals(playerId)) {
         throw new ApiException("رمز الدعوة هذا لا يخص هذا اللاعب");
      }

      if (invite.getStatus() != InviteStatus.PENDING) {
         throw new ApiException("تم استخدام هذه الدعوة أو الرد عليها مسبقًا");
      }

      Lobby lobby = invite.getLobby();

      if (lobby.getVisibility() != LobbyVisibility.PRIVATE) {
         throw new ApiException("رمز الدعوة هذا مخصص للوبيات الخاصة فقط");
      }

      if (lobby.getStatus() != LobbyStatus.OPEN) {
         throw new ApiException("اللوبي ليس مفتوحًا");
      }

      if (lobbyMemberRepository.existsByLobbyIdAndPlayerId(lobby.getId(), playerId)) {
         throw new ApiException("اللاعب منضم بالفعل إلى هذا اللوبي");
      }

      if (lobbyService.isLobbyFull(lobby.getId())) {
         throw new ApiException("هذا اللوبي ممتلئ");
      }

      LobbyMember member = new LobbyMember();
      member.setLobby(lobby);
      member.setPlayer(player);
      member.setRole(MemberRole.MEMBER);
      member.setJoinedAt(LocalDateTime.now());

      lobbyMemberRepository.save(member);

      invite.setStatus(InviteStatus.ACCEPTED);
      invite.setRespondedAt(LocalDateTime.now());
      lobbyInviteRepository.save(invite);
   }

   public void joinLobby(Integer lobbyId, Integer playerId) {
      Lobby lobby = lobbyService.checkLobby(lobbyId);
      Player player = playerService.checkPlayer(playerId);

      if (lobbyMemberRepository.existsByLobbyIdAndPlayerId(lobbyId, playerId)) {
         throw new ApiException("اللاعب منضم بالفعل إلى هذا اللوبي");
      }
      //make sure 10 members for private and public lobby
      if (lobbyService.isLobbyFull(lobbyId)) {
         throw new ApiException("هذا اللوبي ممتلئ، تم الوصول إلى الحد الأقصى وهو 10 أعضاء");
      }
      if (lobby.getVisibility() == LobbyVisibility.PUBLIC) {
         KingdomMembership membership = kingdomMembershipRepository.findByPlayerIdAndKingdomId(playerId, lobby.getKingdom().getId());
         if (membership == null) {
            throw new ApiException("يجب أن تنضم إلى هذه المملكة أولًا للدخول إلى لوبياتها العامة");
         }

         if (!Objects.equals(membership.getDivision(), lobby.getDivision())) {
            throw new ApiException("هذا اللوبي العام مقفل على درجة مختلفة عن درجتك");
         }
      } else {
         LobbyInvite acceptedInvite = lobbyInviteRepository.findByLobbyIdAndInvitedPlayerIdAndStatus(lobbyId, playerId, InviteStatus.ACCEPTED);
         if (acceptedInvite == null){
            throw new ApiException("تحتاج إلى دعوة مقبولة للانضمام إلى هذا اللوبي الخاص");
         }}
      if (lobby.getVisibility() != LobbyVisibility.PUBLIC) {
         throw new ApiException("يجب الانضمام إلى اللوبيات الخاصة باستخدام رمز الدعوة");
      }

      if (lobbyMemberRepository.existsByLobbyIdAndPlayerId(lobbyId, playerId)) {
         throw new ApiException("اللاعب منضم بالفعل إلى هذا اللوبي");
      }

      if (lobbyService.isLobbyFull(lobbyId)) {
         throw new ApiException("هذا اللوبي ممتلئ، تم الوصول إلى الحد الأقصى وهو 10 أعضاء");
      }

      KingdomMembership membership =
              kingdomMembershipRepository.findByPlayerIdAndKingdomId(playerId, lobby.getKingdom().getId());

      if (membership == null) {
         throw new ApiException("يجب أن تنضم إلى هذه المملكة أولًا للدخول إلى لوبياتها العامة");
      }

      if (!Objects.equals(membership.getDivision(), lobby.getDivision())) {
         throw new ApiException("هذا اللوبي العام مقفل على درجة مختلفة عن درجتك");
      }

      LobbyMember member = new LobbyMember();
      member.setLobby(lobby);
      member.setPlayer(player);
      member.setRole(MemberRole.MEMBER);
      member.setJoinedAt(LocalDateTime.now());

      lobbyMemberRepository.save(member);
      notifyJoinedOnWhatsapp(player, lobby);
   }

   // WhatsApp the player the moment they join a (public) lobby, including the lobby's challenge so they know what to do.
   private void notifyJoinedOnWhatsapp(Player player, Lobby lobby) {
      try {
         if (player.getUser() == null || player.getUser().getPhoneNumber() == null
                 || player.getUser().getPhoneNumber().isBlank()) {
            return;
         }
         Challenge challenge = lobby.getChallenge();
         String title = challenge != null ? challenge.getTitle() : "التحدي";
         String desc = (challenge != null && challenge.getDescription() != null) ? challenge.getDescription() : "";
         String msg = "🎉 انضممت إلى اللوبي «" + lobby.getName() + "»!\n"
                 + "تحدّيك: " + title + (desc.isBlank() ? "" : "\n" + desc) + "\n"
                 + "بالتوفيق يا بطل 👑🔥";
         whatsAppService.sendMessage(player.getUser().getPhoneNumber(), msg);
      } catch (Exception ignored) {
      }
   }

   public void leaveLobby(Integer lobbyId, Integer playerId) {
      Lobby lobby = lobbyService.checkLobby(lobbyId);
      LobbyMember member = checkMembership(lobbyId, playerId);
      if (member.getRole() == MemberRole.HOST) {
         throw new ApiException("لا يمكن للمضيف مغادرة اللوبي");
      }
      if (lobby.getStatus() != LobbyStatus.OPEN) {
         throw new ApiException("يمكن للأعضاء المغادرة فقط قبل بدء اللوبي");
      }
      if (lobbyService.isLockedForExit(lobby)) {
         throw new ApiException("لا يمكنك المغادرة، يتبقى أقل من 8 ساعات على البدء، يجب أن تُكمل التحدي");
      }

      if (member.getRole() == MemberRole.HOST) {
         throw new ApiException("لا يمكن للمضيف مغادرة اللوبي");
      }

      if (lobby.getStatus() != LobbyStatus.OPEN) {
         throw new ApiException("يمكن للأعضاء المغادرة فقط قبل بدء اللوبي");
      }

      if (lobbyService.isLockedForExit(lobby)) {
         throw new ApiException("لا يمكنك المغادرة، يتبقى أقل من 8 ساعات على البدء، يجب أن تُكمل التحدي");
      }

      lobbyMemberRepository.delete(member);
   }

   public List<LobbyMemberOut> getMembers(Integer lobbyId) {
      lobbyService.checkLobby(lobbyId);

      List<LobbyMemberOut> members = new ArrayList<>();
      for (LobbyMember m : lobbyMemberRepository.findAllByLobbyId(lobbyId)) {
         members.add(toMemberOut(m));
      }
      return members;
   }

   public void kickMember(Integer lobbyId, Integer hostPlayerId, Integer targetPlayerId) {
      AuthUtil.requireSelfOrAdmin(hostPlayerId);
      Lobby lobby = lobbyService.checkLobby(lobbyId);

      if (!lobby.getHostPlayerId().equals(hostPlayerId)) {
         throw new ApiException("يمكن للمضيف فقط طرد الأعضاء");
      }
      if (hostPlayerId.equals(targetPlayerId)) {
         throw new ApiException("لا يمكن للمضيف طرد نفسه");
      }


      if (hostPlayerId.equals(targetPlayerId)) {
         throw new ApiException("لا يمكن للمضيف طرد نفسه");
      }

      LobbyMember member = checkMembership(lobbyId, targetPlayerId);

      if (member.getRole() == MemberRole.HOST) {
         throw new ApiException("لا يمكن طرد المضيف");
      }

      if (lobby.getStatus() != LobbyStatus.OPEN) {
         throw new ApiException("يمكن طرد الأعضاء فقط قبل بدء اللوبي");
      }

      lobbyMemberRepository.delete(member);
   }

   //helper method
   private LobbyMember checkMembership(Integer lobbyId, Integer playerId) {
      LobbyMember member = lobbyMemberRepository.findByLobbyIdAndPlayerId(lobbyId, playerId);
      if (member == null) {
         throw new ApiException("اللاعب ليس عضوًا في هذا اللوبي");
      }
      return member;
   }

   private LobbyMemberOut toMemberOut(LobbyMember member) {
      LobbyMemberOut out = modelMapper.map(member, LobbyMemberOut.class);
      if (member.getPlayer() != null) {
         out.setDisplayName(member.getPlayer().getDisplayName());
         if (member.getPlayer().getUser() != null) {
            out.setUsername(member.getPlayer().getUser().getUsername());
         }
      }
      return out;
   }
}
