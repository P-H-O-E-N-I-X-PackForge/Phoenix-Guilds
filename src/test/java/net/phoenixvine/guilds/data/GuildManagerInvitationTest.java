package net.phoenixvine.guilds.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GuildManagerInvitationTest {

    private GuildManager mgr;
    private UUID ownerUuid = UUID.randomUUID();
    private UUID targetUuid = UUID.randomUUID();
    private Guild guild;

    @BeforeEach
    void setup() {
        mgr = new GuildManager();
        guild = mgr.createGuild("TestGuild", ownerUuid);
    }

    @Test
    void testSendInvite() {
        String result = mgr.sendInvite(guild.getId(), ownerUuid, targetUuid);
        assertEquals("sent", result);
    }

    @Test
    void testSendInviteNoPermission() {
        UUID nonOfficer = UUID.randomUUID();
        mgr.addMember(guild.getId(), nonOfficer);
        String result = mgr.sendInvite(guild.getId(), nonOfficer, targetUuid);
        assertEquals("no_permission", result);
    }

    @Test
    void testSendInviteAlreadyInGuild() {
        mgr.addMember(guild.getId(), targetUuid);
        String result = mgr.sendInvite(guild.getId(), ownerUuid, targetUuid);
        assertEquals("already_in_guild", result);
    }

    @Test
    void testAcceptInvite() {
        mgr.sendInvite(guild.getId(), ownerUuid, targetUuid);
        String result = mgr.acceptInvite(targetUuid, "TestGuild");
        assertEquals("accepted", result);
        assertTrue(mgr.isInGuild(targetUuid));
        assertEquals(guild.getId(), mgr.getGuildFor(targetUuid).get().getId());
    }

    @Test
    void testAcceptInviteNoInvite() {
        String result = mgr.acceptInvite(targetUuid, "TestGuild");
        assertEquals("no_invite", result);
        assertFalse(mgr.isInGuild(targetUuid));
    }

    @Test
    void testDeclineInvite() {
        mgr.sendInvite(guild.getId(), ownerUuid, targetUuid);
        String result = mgr.declineInvite(targetUuid, "TestGuild");
        assertEquals("declined", result);
        assertFalse(mgr.isInGuild(targetUuid));

        String acceptResult = mgr.acceptInvite(targetUuid, "TestGuild");
        assertEquals("no_invite", acceptResult);
    }
}
