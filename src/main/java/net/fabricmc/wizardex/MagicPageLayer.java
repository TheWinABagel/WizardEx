package net.fabricmc.wizardex;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.Consumer;

import com.github.clevernucleus.dataattributes.api.DataAttributesAPI;
import com.github.clevernucleus.dataattributes.api.attribute.IEntityAttribute;
import com.github.clevernucleus.playerex.PlayerEx;
import com.github.clevernucleus.playerex.api.EntityAttributeSupplier;
import com.github.clevernucleus.playerex.api.ExAPI;
import com.github.clevernucleus.playerex.api.PacketType;
import com.github.clevernucleus.playerex.api.PlayerData;
import com.github.clevernucleus.playerex.api.client.ClientUtil;
import com.github.clevernucleus.playerex.api.client.PageLayer;
import com.github.clevernucleus.playerex.api.client.RenderComponent;
import com.github.clevernucleus.playerex.client.gui.widget.ScreenButtonWidget;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.spell_power.api.MagicSchool;
import net.spell_power.api.SpellPower;

@Environment(EnvType.CLIENT)
public class MagicPageLayer extends PageLayer {
    private static Supplier<Float> scaleX = () -> ExAPI.getConfig().textScaleX();
    private static Supplier<Float> scaleY = () -> ExAPI.getConfig().textScaleY();
    private static float scaleZ = 0.75F;

    private PlayerData playerData;

    private static final List<RenderComponent> COMPONENTS = new ArrayList<RenderComponent>();
    public static final Identifier GUI = new Identifier("wizardex", "textures/gui/schools.png");
    public static final Identifier schools = new Identifier("wizardex", "textures/gui/schools.png");

    private static final List<Identifier> BUTTON_KEYS = ImmutableList.of(WizardEx.spell_power_fire,
            WizardEx.spell_power_frost, WizardEx.spell_power_arcane);

    public MagicPageLayer(HandledScreen<?> parent, ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(parent, handler, inventory, title);
    }

    private boolean canRefund() {
		return this.playerData.refundPoints() > 0;
	}
	
	private void forEachScreenButton(Consumer<ScreenButtonWidget> consumer) {
		this.children().stream().filter(e -> e instanceof ScreenButtonWidget).forEach(e -> consumer.accept((ScreenButtonWidget)e));
	}

    private void buttonPressed(ButtonWidget buttonIn) {
        ScreenButtonWidget button = (ScreenButtonWidget)buttonIn;
        EntityAttributeSupplier attribute = EntityAttributeSupplier.of(button.key());
        DataAttributesAPI.ifPresent(this.client.player, attribute, (Object)null, amount -> {
            double value = this.canRefund() ? -1.0D : 1.0D;
            ClientUtil.modifyAttributes(this.canRefund() ? PacketType.REFUND : PacketType.SKILL, c -> c.accept(attribute, value));
            this.client.player.playSound(PlayerEx.SP_SPEND_SOUND, SoundCategory.NEUTRAL, ExAPI.getConfig().skillUpVolume(), 1.5F);
            return (Object)null;
        });
    }

    private void buttonTooltip(ButtonWidget buttonIn, MatrixStack matrices, int mouseX, int mouseY) {
        ScreenButtonWidget button = (ScreenButtonWidget) buttonIn;
        Identifier lvl = new Identifier("playerex:level");
        Identifier key = button.key();

        if (key.equals(lvl)) {
            int requiredXp = ExAPI.getConfig().requiredXp(this.client.player);
            int currentXp = this.client.player.experienceLevel;
            String progress = "(" + currentXp + "/" + requiredXp + ")";
            Text tooltip = (Text.translatable("playerex.gui.page.attributes.tooltip.button.level", progress)).formatted(Formatting.GRAY);

            this.renderTooltip(matrices, tooltip, mouseX, mouseY);
        } else {
            Supplier<EntityAttribute> attribute = DataAttributesAPI.getAttribute(key);
            DataAttributesAPI.ifPresent(this.client.player, attribute, (Object) null, value -> {
                Text text = Text.translatable(attribute.get().getTranslationKey());
                String type = "playerex.gui.page.attributes.tooltip.button." + (this.canRefund() ? "refund" : "skill");
                Text tooltip = (Text.translatable(type)).append(text).formatted(Formatting.GRAY);

                this.renderTooltip(matrices, tooltip, mouseX, mouseY);
                return (Object) null;
            });
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        matrices.push();
        matrices.scale(scaleX.get(), scaleY.get(), scaleZ);

        COMPONENTS.forEach(component -> component.renderText(this.client.player, matrices, this.textRenderer, this.x,
                this.y, scaleX.get(), scaleY.get()));

//         this.textRenderer.draw(matrices, Text.of("Put character level points"), (this.x + 15) /
//         scaleX.get(), (this.y + 20) / scaleY.get(), 4210752);
//        this.textRenderer.draw(matrices, Text.of("into specific schools of magic"), (this.x + 15) /
//                scaleX.get(), (this.y + 27) / scaleY.get(), 4210752);

        this.textRenderer.draw(matrices, Text.translatable("wizardex.gui.page.tooltip.fire_header"), (this.x + 25) / scaleX.get(), (this.y + 40) / scaleY.get(),
                4210752);
        this.textRenderer.draw(matrices, Text.translatable("wizardex.gui.page.tooltip.frost_header"), (this.x + 25) / scaleX.get(), (this.y + 60) / scaleY.get(),
                4210752);
//        this.textRenderer.draw(matrices, Text.translatable("wizardex.gui.page.tooltip.lightning"), (this.x + 30) / scaleX.get(),
//                (this.y + 85) / scaleY.get(), 4210752);
        this.textRenderer.draw(matrices, Text.translatable("wizardex.gui.page.tooltip.arcane_header"), (this.x + 25) / scaleX.get(),
                (this.y + 80) / scaleY.get(), 4210752);

        this.textRenderer.draw(matrices, Text.translatable("wizardex.gui.page.tooltip.crit_chance_header"), (this.x + 95) / scaleX.get(),
                (this.y + 50) / scaleY.get(), 4210752);

        this.textRenderer.draw(matrices, Text.translatable("wizardex.gui.page.tooltip.crit_damage_header"), (this.x + 95) / scaleX.get(),
                (this.y + 65) / scaleY.get(), 4210752);

        matrices.pop();

        COMPONENTS.forEach(component -> component.renderTooltip(this.client.player, this::renderTooltip, matrices,
                this.textRenderer, this.x, this.y, mouseX, mouseY, scaleX.get(), scaleY.get()));
    }

    @Override
    public void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        // Set texture to get icons from
        // MUST BE 256x256 !!!
        RenderSystem.setShaderTexture(0, schools);

        // Fire Icon
        this.drawTexture(matrices, this.x + 7, this.y + 38, 0, 0, 16, 16);
        // Frost Icon
        this.drawTexture(matrices, this.x + 8, this.y + 58, 16, 0, 16, 16);
        // Lightning Icon
//        this.drawTexture(matrices, this.x + 8, this.y + 85, 32, 0, 16, 16);
        // Arcane Icon
        this.drawTexture(matrices, this.x + 8, this.y + 78, 48, 0, 16, 16);

        this.forEachScreenButton(button -> {
			Identifier key = button.key();
			Identifier lvl = new Identifier("playerex:level");
			EntityAttributeSupplier attribute = EntityAttributeSupplier.of(key);
			PlayerEntity player = this.client.player;
			
			DataAttributesAPI.ifPresent(player, attribute, (Object)null, value -> {
				if(BUTTON_KEYS.contains(key)) {
					double max = ((IEntityAttribute)attribute.get()).maxValue();
					
					if(key.equals(lvl)) {
						button.active = value < max && player.experienceLevel >= ExAPI.getConfig().requiredXp(player);
					} else {
						double modifierValue = this.playerData.get(attribute);
						
						if(this.canRefund()) {
							button.active = modifierValue >= 1.0D;
						} else {
							button.active = modifierValue < max && this.playerData.skillPoints() >= 1;
						}
						
						button.alt = this.canRefund();
					}
				}
				
				return (Object)null;
			});
		});
    }

    @Override
    protected void init() {
        super.init();
        this.playerData = ExAPI.PLAYER_DATA.get(this.client.player);

        // Fire level button
        this.addDrawableChild(new ScreenButtonWidget(this.parent, 69, 40, 204, 0, 11, 10, BUTTON_KEYS.get(0), this::buttonPressed, this::buttonTooltip));
        this.addDrawableChild(new ScreenButtonWidget(this.parent, 69, 60, 204, 0, 11, 10, BUTTON_KEYS.get(1), this::buttonPressed, this::buttonTooltip));
//      this.addDrawableChild(new ScreenButtonWidget(this.parent, 70, 92, 204, 0, 11, 10, BUTTON_KEYS.get(2), this::buttonPressed, this::buttonTooltip));
        this.addDrawableChild(new ScreenButtonWidget(this.parent, 69, 80, 204, 0, 11, 10, BUTTON_KEYS.get(2), this::buttonPressed, this::buttonTooltip));
    }

    public static Supplier<EntityAttribute> GetSpellPowerAll() {
        Supplier<EntityAttribute> spellPowerSupplier = EntityAttributeSupplier.of(WizardEx.spell_power_all);
        return spellPowerSupplier;
    }

    public static Supplier<EntityAttribute> GetSpellPowerFire() {
        Supplier<EntityAttribute> spellPowerSupplier = EntityAttributeSupplier.of(WizardEx.spell_power_fire);
        return spellPowerSupplier;
    }

    public static Supplier<EntityAttribute> GetSpellPowerFrost() {
        Supplier<EntityAttribute> spellPowerSupplier = EntityAttributeSupplier.of(WizardEx.spell_power_frost);
        return spellPowerSupplier;
    }

    public static Supplier<EntityAttribute> GetSpellPowerLightning() {
        Supplier<EntityAttribute> spellPowerSupplier = EntityAttributeSupplier.of(WizardEx.spell_power_lightning);
        return spellPowerSupplier;
    }

    public static Supplier<EntityAttribute> GetSpellPowerArcane() {
        Supplier<EntityAttribute> spellPowerSupplier = EntityAttributeSupplier.of(WizardEx.spell_power_arcane);
        return spellPowerSupplier;
    }

    public static Supplier<EntityAttribute> GetSpellPowerCritChance() {
        Supplier<EntityAttribute> spellPowerSupplier = EntityAttributeSupplier.of(WizardEx.spell_power_crit_chance);
        return spellPowerSupplier;
    }

    public static Supplier<EntityAttribute> GetSpellPowerCritDamage() {
        Supplier<EntityAttribute> spellPowerSupplier = EntityAttributeSupplier.of(WizardEx.spell_power_crit_damage);
        return spellPowerSupplier;
    }

    static {
        // SECTION: Show Skill Points/Level
//        COMPONENTS.add(RenderComponent.of(ExAPI.LEVEL, value -> {
//            return Text.translatable("playerex.gui.page.attributes.text.level", Math.round(value)).formatted(Formatting.DARK_GRAY);
//        }, value -> {
//            List<Text> tooltip = new ArrayList<Text>();
//            tooltip.add((Text.translatable("playerex.gui.page.attributes.tooltip.level[0]")).formatted(Formatting.GRAY));
//            tooltip.add((Text.translatable("playerex.gui.page.attributes.tooltip.level[1]")).formatted(Formatting.GRAY));
//            tooltip.add(Text.empty());
//            tooltip.add((Text.translatable("playerex.gui.page.attributes.tooltip.level[2]", ExAPI.getConfig().skillPointsPerLevelUp())).formatted(Formatting.GRAY));
//            tooltip.add(Text.translatable("playerex.gui.page.attributes.tooltip.level[0]").formatted(Formatting.GRAY));
//            tooltip.add(Text.translatable("playerex.gui.page.attributes.tooltip.level[1]").formatted(Formatting.GRAY));
//            tooltip.add(Text.empty());
//            tooltip.add(Text.translatable("playerex.gui.page.attributes.tooltip.level[2]", ExAPI.getConfig().skillPointsPerLevelUp()).formatted(Formatting.GRAY));
//            return tooltip;
//        }, 80, 130));
        COMPONENTS.add(RenderComponent.of(entity -> {
            return Text.translatable("playerex.gui.page.attributes.text.skill_points", ExAPI.PLAYER_DATA.get(entity).skillPoints()).formatted(Formatting.DARK_GRAY);
        }, entity -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("playerex.gui.page.attributes.tooltip.skill_points[0]")).formatted(Formatting.GRAY));
            tooltip.add((Text.translatable("playerex.gui.page.attributes.tooltip.skill_points[1]")).formatted(Formatting.GRAY));
           // tooltip.add(Text.translatable("playerex.gui.page.attributes.tooltip.skill_points[0]").formatted(Formatting.GRAY));
           // tooltip.add(Text.translatable("playerex.gui.page.attributes.tooltip.skill_points[1]").formatted(Formatting.GRAY));
            return tooltip;
        }, 8, 25));

        // SECTION: WizardEx Stats
        // add Indiviual magic school stats
        // ----------------------------------
        COMPONENTS.add(RenderComponent.of(GetSpellPowerFire(), value -> {
            var intValue = value.intValue();
            return Text.of(String.valueOf(intValue));
        }, value -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.fire_specialization")));

            ClientUtil.appendChildrenToTooltip(tooltip, GetSpellPowerFire());
            return tooltip;
        }, 56, 40));

        COMPONENTS.add(RenderComponent.of(GetSpellPowerFrost(), value -> {
            var intValue = value.intValue();
            return Text.of(String.valueOf(intValue));
        }, value -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.frost_specialization")));

            ClientUtil.appendChildrenToTooltip(tooltip, GetSpellPowerFrost());
            return tooltip;
        }, 56, 60));

//        COMPONENTS.add(RenderComponent.of(GetSpellPowerLightning(), value -> {
//            return Text.of(value.toString());
//        }, value -> {
//            List<Text> tooltip = new ArrayList<Text>();
//            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.lightning_specialization")));
//
//            // ClientUtil.appendChildrenToTooltip(tooltip, ExAPI.CONSTITUTION);
//            return tooltip;
//        }, 65, 85));

        COMPONENTS.add(RenderComponent.of(GetSpellPowerArcane(), value -> {
            var intValue = value.intValue();
            return Text.of(String.valueOf(intValue));
        }, value -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.arcane_specialization")));

             ClientUtil.appendChildrenToTooltip(tooltip, GetSpellPowerArcane());
            return tooltip;
        }, 56, 80));


        COMPONENTS.add(RenderComponent.of(entity -> {
            var critChance = SpellPower.getCriticalChance(entity) * 100;
            var statFormatted = new DecimalFormat("###");
            return Text.of( statFormatted.format(critChance) + "%");
        }, entity -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.crit_chance")));
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.item_bonus")));
            return tooltip;
        }, 143, 50));
        COMPONENTS.add(RenderComponent.of(entity -> {
            var critDmg = SpellPower.getCriticalMultiplier(entity) * 100;
            var statFormatted = new DecimalFormat("###");
            return Text.of(statFormatted.format(critDmg) + "%");
        }, entity -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.crit_damage")));
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.item_bonus")));
            return tooltip;
        }, 143, 65));



        // SECTION: Spellpower Stats
        // show resulting stats from SpellPower API
        // -----------------------------------------
        COMPONENTS.add(RenderComponent.of(entity -> {
            var school = SpellPower.getSpellPower(MagicSchool.FIRE, entity);
            var value = school.baseValue();
            var statFormatted = new DecimalFormat("#.##");
            return Text.of(statFormatted.format(value));
        }, entity -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.fire_bonus")));
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.item_bonus")));
            return tooltip;
        }, 32, 47));
        COMPONENTS.add(RenderComponent.of(entity -> {
            var school = SpellPower.getSpellPower(MagicSchool.FROST, entity);
            var value = school.baseValue();
            var statFormatted = new DecimalFormat("###.##");
            return Text.of(statFormatted.format(value));
        }, entity -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.frost_bonus")));
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.item_bonus")));
            return tooltip;
        }, 32, 67));
//        COMPONENTS.add(RenderComponent.of(entity -> {
//            var school = SpellPower.getSpellPower(MagicSchool.LIGHTNING, entity);
//            var value = school.baseValue();
//            return Text.of(String.valueOf(value));
//        }, entity -> {
//            List<Text> tooltip = new ArrayList<Text>();
//            tooltip.add((Text.of("Lightning")));
//            return tooltip;
//        }, 32, 95));
        COMPONENTS.add(RenderComponent.of(entity -> {
            var school = SpellPower.getSpellPower(MagicSchool.ARCANE, entity);
            var value = school.baseValue();
            var statFormatted = new DecimalFormat("###.##");
            return Text.of(statFormatted.format(value));
        }, entity -> {
            List<Text> tooltip = new ArrayList<Text>();
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.arcane_bonus")));
            tooltip.add((Text.translatable("wizardex.gui.page.tooltip.item_bonus")));
            return tooltip;
        }, 32, 87));
    }
}
