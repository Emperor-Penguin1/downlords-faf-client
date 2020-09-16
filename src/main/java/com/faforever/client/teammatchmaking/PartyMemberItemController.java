package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ChatUserContextMenuController;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Strings;
import com.jfoenix.controls.JFXButton;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.ref.WeakReference;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PartyMemberItemController implements Controller<Node> {

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final PlayerService playerService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final UiService uiService;
  private final ChatService chatService;
  private final I18n i18n;

  @FXML
  public Node playerItemRoot;
  @FXML
  public ImageView avatarImageView;
  @FXML
  public ImageView countryImageView;
  @FXML
  public Label clanLabel;
  @FXML
  public Label usernameLabel;
  @FXML
  public Label ratingLabel;
  @FXML
  public Label gameCountLabel;
  @FXML
  public JFXButton kickPlayerButton;
  public Label uefLabel;
  public Label cybranLabel;
  public Label aeonLabel;
  public Label seraphimLabel;

  private Player player;
  //TODO: this is a bit hacky
  private WeakReference<ChatUserContextMenuController> contextMenuController = null;

  @Override
  public void initialize() {
    clanLabel.managedProperty().bind(clanLabel.visibleProperty());
  }

  @Override
  public Node getRoot() {
    return playerItemRoot;
  }

  private ChatChannelUser chatUser;

  private boolean isFactionSelectedInParty(int faction) {
    return teamMatchmakingService.getParty().getMembers().stream()
        .anyMatch(m -> m.getPlayer().getId() == player.getId() && m.getFactions().get(faction));
  }

  public void onKickPlayerButtonClicked(ActionEvent actionEvent) {
    teamMatchmakingService.kickPlayerFromParty(this.player);
  }

  public PartyMemberItemController(CountryFlagService countryFlagService, AvatarService avatarService, PlayerService playerService, TeamMatchmakingService teamMatchmakingService, UiService uiService, ChatService chatService, I18n i18n) {
    this.countryFlagService = countryFlagService;
    this.avatarService = avatarService;
    this.playerService = playerService;
    this.teamMatchmakingService = teamMatchmakingService;
    this.uiService = uiService;
    this.chatService = chatService;
    this.i18n = i18n;
  }

  void setMember(PartyMember member) {
    this.player = member.getPlayer();
    //TODO: this is a bit hacky, a chat channel user is required to create a context menu as in the chat tab (for foeing/befriending/messaging people...)
    chatUser = new ChatChannelUser(player.getUsername(), chatService.getChatUserColor(player.getUsername()), false, player);

//    userImageView.setImage(IdenticonUtil.createIdenticon(player.getId()));

    countryImageView.imageProperty().bind(createObjectBinding(() -> countryFlagService.loadCountryFlag(
        StringUtils.isEmpty(player.getCountry()) ? "" : player.getCountry()).orElse(null), player.countryProperty()));

//    leagueImageView.visibleProperty().bind(player.avatarUrlProperty().isNotNull().and(player.avatarUrlProperty().isNotEmpty()));
//    leagueImageView.imageProperty().bind(createObjectBinding(() -> Strings.isNullOrEmpty(player.getAvatarUrl()) ? null : avatarService.loadAvatar(player.getAvatarUrl()), player.avatarUrlProperty()));
    avatarImageView.setImage(avatarService.loadAvatar("https://content.faforever.com/faf/avatars/ICE_Test.png"));

    clanLabel.visibleProperty().bind(player.clanProperty().isNotEmpty().and(player.clanProperty().isNotNull()));
    clanLabel.textProperty().bind(createStringBinding(() -> Strings.isNullOrEmpty(player.getClan()) ? "" : String.format("[%s]", player.getClan()), player.clanProperty()));

    usernameLabel.textProperty().bind(player.usernameProperty());
//    usernameLabel.setText("GGGGGGGGGGGGGGGG"); // TODO: REMOVE

    ratingLabel.textProperty().bind(createStringBinding(
        () -> ratingLabel.getStyleClass().contains("uppercase") ?
            i18n.get("teammatchmaking.rating", RatingUtil.getRoundedGlobalRating(player)).toUpperCase() :
            i18n.get("teammatchmaking.rating", RatingUtil.getRoundedGlobalRating(player)),
        player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()));
    gameCountLabel.textProperty().bind(createStringBinding(
        () -> gameCountLabel.getStyleClass().contains("uppercase") ?
            i18n.get("teammatchmaking.gameCount", player.getNumberOfGames()).toUpperCase() :
            i18n.get("teammatchmaking.gameCount", player.getNumberOfGames()),
        player.numberOfGamesProperty()));

    BooleanBinding isDifferentPlayerBinding = playerService.currentPlayerProperty().isNotEqualTo(player);
    kickPlayerButton.visibleProperty().bind(teamMatchmakingService.getParty().ownerProperty().isEqualTo(playerService.currentPlayerProperty()).and(isDifferentPlayerBinding));
    kickPlayerButton.managedProperty().bind(kickPlayerButton.visibleProperty());
//    kickPlayerButton.visibleProperty().set(true); // TODO: remove

      aeonLabel.setDisable(!isFactionSelectedInParty(0));
      cybranLabel.setDisable(!isFactionSelectedInParty(1));
      uefLabel.setDisable(!isFactionSelectedInParty(2));
      seraphimLabel.setDisable(!isFactionSelectedInParty(3));

      boolean ready = teamMatchmakingService.getParty().getMembers().stream()
          .anyMatch(m -> m.getPlayer().getId() == player.getId() && m.isReady());
      ObservableList<String> classes = playerItemRoot.getStyleClass();
      if (ready && !classes.contains("card-playerReady")) {
        classes.add("card-playerReady");
      }
      if (!ready) {
        classes.remove("card-playerReady");
      }
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    if (contextMenuController != null) {
      ChatUserContextMenuController controller = contextMenuController.get();
      if (controller != null) {
        controller.getContextMenu().show(playerItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        return;
      }
    }

    ChatUserContextMenuController controller = uiService.loadFxml("theme/chat/chat_user_context_menu.fxml");
    controller.setChatUser(chatUser);
    controller.getContextMenu().show(playerItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());

    contextMenuController = new WeakReference<>(controller);
  }
}