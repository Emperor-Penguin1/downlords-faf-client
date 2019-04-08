package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.game.Game;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.play.JoinGameHelper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.IdenticonUtil;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


/**
 * Displays a notification whenever a friend joins a preferences (if enabled in settings).
 */
@Component
public class FriendJoinedGameNotifier {

  private final NotificationService notificationService;
  private final I18n i18n;
  private final JoinGameHelper joinGameHelper;
  private final PreferencesService preferencesService;
  private final AudioService audioService;


  public FriendJoinedGameNotifier(NotificationService notificationService, I18n i18n,
                                  JoinGameHelper joinGameHelper, PreferencesService preferencesService,
                                  AudioService audioService) {
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.joinGameHelper = joinGameHelper;
    this.preferencesService = preferencesService;
    this.audioService = audioService;
  }

  @EventListener
  public void onFriendJoinedGame(FriendJoinedGameEvent event) {
    Player player = event.getPlayer();
    Game game = event.getGame();

    audioService.playFriendJoinsGameSound();

    if (preferencesService.getPreferences().getNotification().isFriendJoinsGameToastEnabled()) {
      notificationService.addNotification(new TransientNotification(
        i18n.get("friend.joinedGameNotification.title", player.getDisplayName(), game.getTitle()),
        i18n.get("friend.joinedGameNotification.action"),
        IdenticonUtil.createIdenticon(player.getId()),
        event1 -> joinGameHelper.join(player.getGame())
      ));
    }
  }
}
