package com.faforever.client.play;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.relay.event.GameFullEvent;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapPreviewService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.util.ProgrammingError;
import com.google.common.eventbus.EventBus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Thread.sleep;

/**
 * Starts flashing the Forged Alliance window whenever a {@link com.faforever.client.game.relay.event.GameFullEvent} is
 * triggered and stops as soon as the window is focused. Also shows a transient notification.
 */

@Component
public class OnGameFullNotifier implements InitializingBean {

  private final PlatformService platformService;
  private final Executor executor;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final MapPreviewService mapPreviewService;
  private final EventBus eventBus;
  private final GameService gameService;
  private final String faWindowTitle;


  public OnGameFullNotifier(PlatformService platformService, @Qualifier("taskExecutor") Executor executor, NotificationService notificationService,
                            I18n i18n, MapPreviewService mapPreviewService, EventBus eventBus, ClientProperties clientProperties,
                            GameService gameService) {
    this.platformService = platformService;
    this.executor = executor;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.mapPreviewService = mapPreviewService;
    this.eventBus = eventBus;
    this.faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
    this.gameService = gameService;
  }

  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @EventListener
  public void onGameFull(GameFullEvent event) {
    executor.execute(() -> {
      platformService.startFlashingWindow(faWindowTitle);
      while (gameService.isGameRunning() && !platformService.isWindowFocused(faWindowTitle)) {
        noCatch(() -> sleep(500));
      }
      platformService.stopFlashingWindow(faWindowTitle);
    });

    Game currentGame = gameService.getCurrentGame();
    if (currentGame == null) {
      throw new ProgrammingError("Got a GameFull notification but player is not in a game");
    }
    if (platformService.isWindowFocused(faWindowTitle)) {
      return;
    }

    notificationService.addNotification(new TransientNotification(i18n.get("game.full"), i18n.get("game.full.action"),
        mapPreviewService.loadPreview(currentGame.getMapName(), PreviewSize.SMALL),
        v -> platformService.focusWindow(faWindowTitle)));
  }
}
