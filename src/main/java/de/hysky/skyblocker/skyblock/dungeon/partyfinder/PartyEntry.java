package de.hysky.skyblocker.skyblock.dungeon.partyfinder;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.mixin.accessor.SkullBlockEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Matrix4f;

public class PartyEntry extends ElementListWidget.Entry<PartyEntry> {

    private static final Identifier PARTY_CARD_TEXTURE = new Identifier(SkyblockerMod.NAMESPACE, "textures/gui/party_card.png");
    private static final Identifier PARTY_CARD_TEXTURE_HOVER = new Identifier(SkyblockerMod.NAMESPACE, "textures/gui/party_card_hover.png");
    protected final PartyFinderScreen screen;
    protected final int slotID;
    Player partyLeader;
    String floor = "???";
    String dungeon = "???";
    String note = "";
    NbtCompound floorSkullNBT = new NbtCompound();
    Identifier partyLeaderSkin = DefaultSkinHelper.getTexture();
    Player[] partyMembers = new Player[4];

    int minClassLevel = -1;
    int minCatacombsLevel = -1;

    public boolean isLocked() {
        return isLocked;
    }

    boolean isLocked = false;
    Text lockReason = Text.empty();


    public PartyEntry(List<Text> tooltips, PartyFinderScreen screen, int slotID) {
        this.screen = screen;
        this.slotID = slotID;

        Arrays.fill(partyMembers, null);
        if (tooltips.isEmpty()) return;
        //System.out.println(tooltips);

        MinecraftClient client = MinecraftClient.getInstance();
        Text title = tooltips.get(0);
        String partyHost = title.getString().split("'s")[0];

        int membersIndex = -1;
        for (int i = 1; i < tooltips.size(); i++) {
            Text text = tooltips.get(i);
            String tooltipText = Formatting.strip(text.getString());
            assert tooltipText != null;
            String lowerCase = tooltipText.toLowerCase();
            //System.out.println("TOOLTIP"+i);
            //System.out.println(text.getSiblings());
            if (lowerCase.contains("members:") && membersIndex == -1) {
                membersIndex = i + 1;
            } else if (lowerCase.contains("class level")) {
                Matcher matcher = Pattern.compile("\\d+$").matcher(lowerCase);
                if (matcher.find()) minClassLevel = Integer.parseInt(matcher.group());
            } else if (lowerCase.contains("dungeon level")) {
                Matcher matcher = Pattern.compile("\\d+$").matcher(lowerCase);
                if (matcher.find()) minCatacombsLevel = Integer.parseInt(matcher.group());
            } else if (lowerCase.contains("floor:")) {
                floor = tooltipText.split(":")[1].trim();
                if (dungeon.equals("???")) continue;
                if (PartyFinderScreen.floorIconsMaster == null || PartyFinderScreen.floorIconsNormal == null) continue;
                if (dungeon.contains("Master Mode")) {
                    try {
                        floorSkullNBT = StringNbtReader.parse(PartyEntryListWidget.BASE_SKULL_NBT.replace("%TEXTURE%", PartyFinderScreen.floorIconsMaster.getOrDefault(floor.toLowerCase(), "")));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        floorSkullNBT = StringNbtReader.parse(PartyEntryListWidget.BASE_SKULL_NBT.replace("%TEXTURE%", PartyFinderScreen.floorIconsNormal.getOrDefault(floor.toLowerCase(), "")));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }

            } else if (lowerCase.contains("dungeon:")) {
                dungeon = tooltipText.split(":")[1].trim();
            } else if (!text.getSiblings().isEmpty() && Objects.equals(text.getSiblings().get(0).getStyle().getColor(), TextColor.fromRgb(Formatting.RED.getColorValue())) && !lowerCase.startsWith(" ")) {
                isLocked = true;
                lockReason = text;
            } else if (lowerCase.contains("note:")) {
                String[] split = tooltipText.split(":");
                //Sometimes the note can not exist??? this is only for debug temporarily
                if (split.length == 1) System.out.println(Arrays.toString(split));
            	if (split.length == 1) System.out.println(tooltips.stream().map(Text::getString).map(s -> "\"" + s + "\"").toArray(String[]::new));
                note = split[1].trim();
            }
        }
        if (membersIndex != -1) {
            for (int i = membersIndex, j = 0; i < membersIndex + 5; i++, j++) {
                if (i >= tooltips.size()) continue;

                Text text = tooltips.get(i);
                String memberText = text.getString();
                if (!memberText.startsWith(" ")) continue; // Member thingamajigs start with a space

                String[] parts = memberText.split(":", 2);
                String playerNameTrim = parts[0].trim();

                if (playerNameTrim.equals("Empty")) continue; // Don't care about these idiots lol

                List<Text> siblings = text.getSiblings();
                Style nameStyle = !siblings.isEmpty() ? siblings.get(Math.min(1, siblings.size() - 1)).getStyle() : text.getStyle();
                Text playerName = Text.literal(playerNameTrim).setStyle(nameStyle);
                String className = parts[1].trim().split(" ")[0];
                int classLevel = -1;
                Matcher matcher = Pattern.compile("\\((\\d+)\\)").matcher(parts[1]);
                if (matcher.find()) classLevel = Integer.parseInt(matcher.group(1));
                Player player = new Player(playerName, className, classLevel);

                SkullBlockEntityAccessor.invokeFetchProfile(playerNameTrim).thenAccept(
                        gameProfile -> gameProfile.ifPresent(profile -> player.skinTexture = (client.getSkinProvider().getSkinTextures(profile).texture())));

                if (playerNameTrim.equals(partyHost)) {
                    partyLeader = player;
                    j--;
                } else if (j > 3) {
                    partyLeader = player;
                } else partyMembers[j] = player;
            }
        }

        if (partyLeader == null) {
            for (int i = partyMembers.length - 1; i >= 0; i--) {
                if (partyMembers[i] != null) {
                    partyLeader = partyMembers[i];
                    partyMembers[i] = null;
                    break;
                }
            }
        }
        if (partyLeader == null) {
            partyLeader = new Player(Text.literal("Error"), "Error", -1);
        }

        SkullBlockEntityAccessor.invokeFetchProfile(partyLeader.name.getString()).thenAccept(
                gameProfile -> gameProfile.ifPresent(profile -> partyLeaderSkin = client.getSkinProvider().getSkinTextures(profile).texture()));
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return List.of();
    }

    @Override
    public List<? extends Element> children() {
        return List.of();
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (hovered && !isLocked) {
            context.drawTexture(PARTY_CARD_TEXTURE_HOVER, 0, 0, 0, 0, 336, 64, 336, 64);
            if (!(this instanceof YourParty)) context.drawText(textRenderer, Text.translatable("skyblocker.partyFinder.join"), 148, 6, 0xFFFFFFFF, false);
        } else context.drawTexture(PARTY_CARD_TEXTURE, 0, 0, 0, 0, 336, 64, 336, 64);
        int mouseXLocal = mouseX - x;
        int mouseYLocal = mouseY - y;

        context.drawText(textRenderer, this.partyLeader.toText(), 18, 6, 0xFFFFFFFF, true);

        if (PartyFinderScreen.DEBUG) {
            context.drawText(textRenderer, String.valueOf(slotID), 166, 6, 0xFFFFFFFF, true);
            if (hovered) {
                context.drawText(textRenderer, "H", 160, 6, 0xFFFFFFFF, true);
            }
        }
        PlayerSkinDrawer.draw(context, partyLeaderSkin, 6, 6, 8, true, false);
        for (int i = 0; i < partyMembers.length; i++) {
            Player partyMember = partyMembers[i];
            if (partyMember == null) continue;
            context.drawText(textRenderer, partyMember.toText(), 17 + 136 * (i % 2), 24 + 14 * (i / 2), 0xFFFFFFFF, true);
            PlayerSkinDrawer.draw(context, partyMember.skinTexture, 6 + 136 * (i % 2), 24 + 14 * (i / 2), 8, true, false);
        }

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider.Immediate vertexConsumers = context.getVertexConsumers();

        if (minClassLevel > 0) {
            textRenderer.drawWithOutline(Text.of("Class " + minClassLevel).asOrderedText(), 278, 25, 0xFF288BB5, 0xFF103848, positionMatrix, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            if (!isLocked && hovered && mouseXLocal >= 276 && mouseXLocal <= 331 && mouseYLocal >= 22 && mouseYLocal <= 35) {
                context.drawTooltip(textRenderer, Text.translatable("skyblocker.partyFinder.partyCard.minClassLevel", minClassLevel), mouseXLocal, mouseYLocal);
            }
        }

        if (minCatacombsLevel > 0) {
            textRenderer.drawWithOutline(Text.of("Cata " + minCatacombsLevel).asOrderedText(), 278, 43, 0xFF288BB5, 0xFF103848, positionMatrix, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            if (!isLocked && hovered && mouseXLocal >= 276 && mouseXLocal <= 331 && mouseYLocal >= 40 && mouseYLocal <= 53) {
                context.drawTooltip(textRenderer, Text.translatable("skyblocker.partyFinder.partyCard.minDungeonLevel", minCatacombsLevel), mouseXLocal, mouseYLocal);
            }
        }
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.setNbt(floorSkullNBT);
        context.drawItem(stack, 317, 3);

        int textWidth = textRenderer.getWidth(floor);
        context.drawText(textRenderer, floor, 316 - textWidth, 7, 0x70000000, false);

        context.drawText(textRenderer, note, 5, 52, 0xFFFFFFFF, true);

        if (isLocked) {
            matrices.push();
            matrices.translate(0, 0, 100f);
            context.fill(0, 0, entryWidth, entryHeight, 0x90000000);
            context.drawText(textRenderer, lockReason, entryWidth/2 - textRenderer.getWidth(lockReason)/2, entryHeight/2 - textRenderer.fontHeight/2, 0xFFFFFF, true);
            matrices.pop();
        }

        matrices.pop();

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //System.out.println("To be clicked" + slotID);
        if (slotID == -1) {
            PartyFinderScreen.LOGGER.error("[Skyblocker] Slot ID is null for " + partyLeader.name.getString() + "'s party");
        }
        if (button == 0 && !screen.isWaitingForServer() && slotID != -1) {
            screen.clickAndWaitForServer(slotID);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static class Player {
        public final Text name;
        public final String dungeonClass;
        public final int classLevel;
        public Identifier skinTexture = DefaultSkinHelper.getTexture();

        Player(Text name, String dungeonClass, int classLevel) {
            this.name = name;
            this.dungeonClass = dungeonClass;
            this.classLevel = classLevel;
        }

        public Text toText() {
            char dClass = dungeonClass.isEmpty() ? '?' : dungeonClass.charAt(0);
            return name.copy().append(Text.literal(" " + dClass + " " + classLevel).formatted(Formatting.YELLOW));
        }
    }

    public static class NoParties extends PartyEntry {

        public NoParties() {
            super(List.of(), null, -1);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("skyblocker.partyFinder.noParties"), x+entryWidth/2, y+entryHeight/2-textRenderer.fontHeight/2, 0xFFFFFFFF);
        }
    }

    public static class YourParty extends PartyEntry {

        public YourParty(List<Text> tooltips, PartyFinderScreen screen, int deListSlotId) {
            super(tooltips, screen, deListSlotId);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(x, y, 0);

            hovered = hovered && slotID != -1;

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            Text text;
            if (hovered) {
                text = Text.translatable("skyblocker.partyFinder.deList");
            } else text = Text.translatable("skyblocker.partyFinder.yourParty");
            context.drawText(textRenderer, text, 48, 6, 0x70000000, false);

            matrices.pop();
            super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
        }
    }
}
