package com.newrelic.slack;

import com.slack.api.Slack;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.views.ViewsUpdateResponse;
import com.slack.api.model.Message;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.StaticSelectElement;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.view.Views.*;
import static com.slack.api.model.block.element.BlockElements.*;

import com.slack.api.bolt.util.JsonOps;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class FieldAssistant {
    private static List<SMETopic> TOPICS;
    private static Map<String, QuestionResources> RESOURCES;

    public static void main(String[] args) throws Exception {
        TOPICS = SMEData.getSMEs();
        RESOURCES = SMEData.getResources();

        var app = new App();

        app.messageShortcut("sme", (req, ctx) -> {
            String userId = req.getPayload().getUser().getId();
            Message message = req.getPayload().getMessage();

            ViewsOpenResponse viewsOpenResponse = ctx.client().viewsOpen(r -> r
                    .triggerId(ctx.getTriggerId())
                    .view(buildSmeView(message,
                            req.getPayload().getChannel().getId(),
                            req.getPayload().getMessageTs()))
            );

            if (!viewsOpenResponse.isOk()) {
                String errorCode = viewsOpenResponse.getError();
                ctx.logger.error("response = " + viewsOpenResponse.getResponseMetadata().toString());
                ctx.logger.error("Failed to open modal for user {} and error {}", userId, errorCode);
                ctx.respond(":x: Failed to open a modal view because of " + errorCode);
            }

            return ctx.ack();
        });

        app.messageShortcut("resources", (req, ctx) -> {
            String userId = req.getPayload().getUser().getId();
            Message message = req.getPayload().getMessage();

            ViewsOpenResponse viewsOpenResponse = ctx.client().viewsOpen(r -> r
                    .triggerId(ctx.getTriggerId())
                    .view(buildResourcesView(null,
                            req.getPayload().getChannel().getId(),
                            req.getPayload().getMessageTs()))
            );

            if (!viewsOpenResponse.isOk()) {
                String errorCode = viewsOpenResponse.getError();
                ctx.logger.error("response = " + viewsOpenResponse.getResponseMetadata().toString());
                ctx.logger.error("Failed to open modal for user {} and error {}", userId, errorCode);
                ctx.respond(":x: Failed to open a modal view because of " + errorCode);
            }

            return ctx.ack();
        });

        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            var appHomeView = view(view -> view
                    .type("home")
                    .blocks(asBlocks(
                            section(section -> section.text(markdownText(mt -> mt.text("*Welcome to your _App's Home_* :tada:")))),
                            divider(),
                            section(section -> section.text(markdownText(mt -> mt.text("Thiszz button won't do much for now but you can set up a listener for it using the `actions()` method and passing its unique `action_id`. See an example on <https://slack.dev/java-slack-sdk/guides/interactive-components|slack.dev/java-slack-sdk>.")))),
                            actions(actions -> actions
                                    .elements(asElements(
                                            button(b -> b.text(plainText(pt -> pt.text("Click me!"))).value("button1").actionId("button_1"))
                                    ))
                            )
                    ))
            );

            var res = ctx.client().viewsPublish(r -> r
                    .userId(payload.getEvent().getUser())
                    .view(appHomeView)
            );

            return ctx.ack();
        });

        app.blockAction("sme-select", (req, ctx) -> {
            String value = req.getPayload().getActions().get(0).getValue();
            Map<String, String> privateMetadata = JsonOps.fromJson(req.getPayload().getView().getPrivateMetadata(), Map.class);
            var client = Slack.getInstance().methods();

            client.chatPostMessage(r -> r
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .channel(privateMetadata.get("channelId"))
                    .threadTs(privateMetadata.get("messageTs"))
                    .linkNames(true)
                    .text("@" + req.getPayload().getUser().getUsername() + " please respond!")
            );

            return ctx.ack();
        });

        app.blockAction("resource-topic-select", (req, ctx) -> {
            ctx.logger.info("resource-topic-select..." + req.getPayload().getActions().get(0).getSelectedOption().getValue());

            Map<String, String> privateMetadata = JsonOps.fromJson(req.getPayload().getView().getPrivateMetadata(), Map.class);

            View view = buildResourcesView(req.getPayload().getActions().get(0).getSelectedOption().getValue(), privateMetadata.get("channelId"), privateMetadata.get("messageTs") );
            ViewsUpdateResponse viewsUpdateResponse = ctx.client().viewsUpdate(r -> r
                    .view(view)
                    .viewId(req.getPayload().getView().getId())
                    .hash(req.getPayload().getView().getHash())
            );

            return ctx.ack();
        });

        app.blockAction("resource-share", (req, ctx) -> {
            ctx.logger.info(req.getPayload().getActions().get(0).getValue());

            String value = req.getPayload().getActions().get(0).getValue();
            Map<String, String> privateMetadata = JsonOps.fromJson(req.getPayload().getView().getPrivateMetadata(), Map.class);

            ctx.logger.info("metdata " + privateMetadata.get("channelId") + " " + privateMetadata.get("messageTs"));

            var client = Slack.getInstance().methods();

            client.chatPostMessage(r -> r
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .channel(privateMetadata.get("channelId"))
                    .threadTs(privateMetadata.get("messageTs"))
                    .text(":white_check_mark: Check out the following resource for the answer to your question: " + value)
            );

            return ctx.ack();
        });

        var server = new SlackAppServer(app, 3001);
        server.start();
    }

    private static View buildResourcesView(String selection, String channelId, String messageTs) {
        List<LayoutBlock> blocks = new ArrayList<>();

        LayoutBlock selectLabel = section(section -> section
                .text(markdownText("Select a Topic"))
        );

        blocks.add(selectLabel);

        Set<String> keys = RESOURCES.keySet();
        List<OptionObject> optionObjects = new ArrayList<>();

        OptionObject initialOption = null;

        for (String key : keys) {
            OptionObject option = option(plainText(key), key.strip());
            optionObjects.add(option);

            if (selection != null && selection.equals(key)) {
                initialOption = option;
            }
        }

        LayoutBlock options = actions(asElements(staticSelect(select -> select
                .actionId("resource-topic-select")
                .placeholder(plainText("select a topic"))
                .options(optionObjects)
        )));

        ((StaticSelectElement)((ActionsBlock)options).getElements().get(0)).setInitialOption(initialOption);

        blocks.add(options);

        if (selection != null) {
            List<QuestionResource> questionResources = RESOURCES.get(selection).getResources();

            for (QuestionResource topic: questionResources) {
                String link = "<" + topic.getResource() + "|" + topic.getName() + ">";
                String text = link + "\n_" + topic.getDescription() + "_";

                LayoutBlock resources = section(section -> section
                        .text(markdownText(text))
                        .accessory(button(b -> b
                                .text(plainText("share"))
                                .value(link)
                                .actionId("resource-share")
                        ))
                );

                blocks.add(resources);
                blocks.add(divider());
            }

        }

        return view(view -> view
                .callbackId("resource-topic-select")
                .type("modal")
                .privateMetadata("{\"channelId\":\"" + channelId + "\",\"messageTs\":\"" + messageTs + "\"}")
                .title(viewTitle(title -> title.type("plain_text").text("Resources").emoji(true)))
                .blocks(blocks)
        );
    }

    private static View buildSmeView(Message message, String channelId, String messageTs) {
        var client = Slack.getInstance().methods(System.getenv("SLACK_BOT_TOKEN"));

        List<LayoutBlock> blocks = new ArrayList<>();
        int i = 0;

        for (SMETopic topic: TOPICS) {

            String blockId = "topic-" + i++;
            LayoutBlock block = section(section -> {
                        return section
                                .blockId("topic" + blockId)
                                .text(markdownText(getBlockText(client, topic)))
                                .accessory(button(b -> b
                                    .text(plainText(pt -> pt.text("tag")))
                                    .actionId("sme-select")
                                    .value("foo")));
                    }
            );

            blocks.add(block);
            blocks.add(divider());
        }

        return view(view -> {
                    return view
                            .callbackId("meeting-arrangement")
                            .type("modal")
                            .notifyOnClose(true)
                            .title(viewTitle(title -> title.type("plain_text").text("New Relic SMEs").emoji(true)))
                            .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                            .privateMetadata("{\"channelId\":\"" + channelId + "\",\"messageTs\":\"" + messageTs + "\"}")
                            .blocks(blocks);
                }
        );
    }

    @NotNull
    private static String getBlockText(com.slack.api.methods.MethodsClient client, SMETopic topic) {
        List<String> smes = topic.getSmes();
        String decoratedSmeList = "";
        UsersLookupByEmailResponse response = null;

        for (String sme : smes) {
            try {
                response = client.usersLookupByEmail(lookup -> lookup.email(sme));
                decoratedSmeList = decoratedSmeList + "@" + response.getUser().getName() + " ";
                System.out.println("is ok? " + response.isOk());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SlackApiException e) {
                e.printStackTrace();
            }
        }

        String wizardsChannel = "";
        if (topic.getWizardsChannel() != null && topic.getWizardsChannel().length() !=0) {
            wizardsChannel = "\n#" + topic.getWizardsChannel();
        }

        String blockText = topic.getTopic() + "\n" + decoratedSmeList + wizardsChannel;
        return blockText;
    }
}
