package emu.grasscutter.commands;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GenshinData;
import emu.grasscutter.data.def.ItemData;
import emu.grasscutter.data.def.AvatarData;
import emu.grasscutter.data.def.AvatarSkillDepotData;
import emu.grasscutter.data.def.MonsterData;
import emu.grasscutter.game.GenshinPlayer;
import emu.grasscutter.game.GenshinScene;
import emu.grasscutter.game.World;
import emu.grasscutter.game.avatar.GenshinAvatar;
import emu.grasscutter.game.entity.EntityAvatar;
import emu.grasscutter.game.entity.EntityItem;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.inventory.GenshinItem;
import emu.grasscutter.game.inventory.Inventory;
import emu.grasscutter.game.inventory.ItemType;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.game.props.FightProperty;
import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.server.packet.send.PacketEntityFightPropUpdateNotify;
import emu.grasscutter.server.packet.send.PacketItemAddHintNotify;
import emu.grasscutter.utils.Position;

import java.util.LinkedList;
import java.util.List;

/**
 * A container for player-related commands.
 */
public final class PlayerCommands {
    @Command(label = "give", aliases = {"g", "item", "giveitem"}, 
            usage = "Usage: give [player] <itemId|itemName> [amount]")
    public static class GiveCommand implements CommandHandler {

        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            int target, item, amount = 1;

            switch(args.size()) {
                default:
                    CommandHandler.sendMessage(player, "Usage: give <player> <itemId|itemName> [amount]");
                    return;
                case 1:
                    try {
                        item = Integer.parseInt(args.get(0));
                        target = player.getAccount().getPlayerId();
                    } catch (NumberFormatException ignored) {
                        // TODO: Parse from item name using GM Handbook.
                        CommandHandler.sendMessage(player, "Invalid item id.");
                        return;
                    }
                    break;
                case 2:
                    try {
                        target = Integer.parseInt(args.get(0));

                        if(Grasscutter.getGameServer().getPlayerByUid(target) == null) {
                            target = player.getUid(); amount = Integer.parseInt(args.get(1));
                            item = Integer.parseInt(args.get(0));
                        } else {
                            item = Integer.parseInt(args.get(1));
                        }
                    } catch (NumberFormatException ignored) {
                        // TODO: Parse from item name using GM Handbook.
                        CommandHandler.sendMessage(player, "Invalid item or player ID.");
                        return;
                    }
                    break;
                case 3:
                    try {
                        target = Integer.parseInt(args.get(0));
                        
                        if(Grasscutter.getGameServer().getPlayerByUid(target) == null) {
                            CommandHandler.sendMessage(player, "Invalid player ID."); return;
                        }

                        item = Integer.parseInt(args.get(1));
                        amount = Integer.parseInt(args.get(2));
                    } catch (NumberFormatException ignored) {
                        // TODO: Parse from item name using GM Handbook.
                        CommandHandler.sendMessage(player, "Invalid item or player ID.");
                        return;
                    }
                    break;
            }

            GenshinPlayer targetPlayer = Grasscutter.getGameServer().getPlayerByUid(target);

            if(targetPlayer == null) {
                CommandHandler.sendMessage(player, "Player not found."); return;
            }

            ItemData itemData = GenshinData.getItemDataMap().get(item);
            if(itemData == null) {
                CommandHandler.sendMessage(player, "Invalid item id."); return;
            }
            
            this.item(targetPlayer, itemData, amount);
        }

        /**
         * give [player] [itemId|itemName] [amount]
         */
        @Override public void execute(List<String> args) {
            if(args.size() < 2) {
                CommandHandler.sendMessage(null, "Usage: give <player> <itemId|itemName> [amount]");
                return;
            }

            try {
                int target = Integer.parseInt(args.get(0));
                int item = Integer.parseInt(args.get(1));
                int amount = 1; if(args.size() > 2) amount = Integer.parseInt(args.get(2));
                
                GenshinPlayer targetPlayer = Grasscutter.getGameServer().getPlayerByUid(target);

                if(targetPlayer == null) {
                    CommandHandler.sendMessage(null, "Player not found."); return;
                }
                
                ItemData itemData = GenshinData.getItemDataMap().get(item);
                if(itemData == null) {
                    CommandHandler.sendMessage(null, "Invalid item id."); return;
                }
                
                this.item(targetPlayer, itemData, amount);
            } catch (NumberFormatException ignored) {
                CommandHandler.sendMessage(null, "Invalid item or player ID.");
            }
        }
        
        private void item(GenshinPlayer player, ItemData itemData, int amount) {
            GenshinItem genshinItem = new GenshinItem(itemData);
            if(itemData.isEquip()) {
                List<GenshinItem> items = new LinkedList<>();
                for(int i = 0; i < amount; i++) {
                    items.add(genshinItem);
                } player.getInventory().addItems(items);
                player.sendPacket(new PacketItemAddHintNotify(items, ActionReason.SubfieldDrop));
            } else {
                genshinItem.setCount(amount);
                player.getInventory().addItem(genshinItem);
                player.sendPacket(new PacketItemAddHintNotify(genshinItem, ActionReason.SubfieldDrop));
            }
        }
    }
    
    @Command(label = "drop", aliases = {"d", "dropitem"}, 
            usage = "Usage: drop <itemId|itemName> [amount]", 
            execution = Command.Execution.PLAYER)
    public static class DropCommand implements CommandHandler {

        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            if(args.size() < 1) {
                CommandHandler.sendMessage(player, "Usage: drop <itemId|itemName> [amount]");
                return;
            }

            try {
                int item = Integer.parseInt(args.get(0));
                int amount = 1; if(args.size() > 1) amount = Integer.parseInt(args.get(1));

                ItemData itemData = GenshinData.getItemDataMap().get(item);
                if(itemData == null) {
                    CommandHandler.sendMessage(player, "Invalid item id."); return;
                }

                if (itemData.isEquip()) {
                    float range = (5f + (.1f * amount));
                    for (int i = 0; i < amount; i++) {
                        Position pos = player.getPos().clone().addX((float) (Math.random() * range) - (range / 2)).addY(3f).addZ((float) (Math.random() * range) - (range / 2));
                        EntityItem entity = new EntityItem(player.getScene(), player, itemData, pos, 1);
                        player.getScene().addEntity(entity);
                    }
                } else {
                    EntityItem entity = new EntityItem(player.getScene(), player, itemData, player.getPos().clone().addY(3f), amount);
                    player.getScene().addEntity(entity);
                }
            } catch (NumberFormatException ignored) {
                CommandHandler.sendMessage(player, "Invalid item or player ID.");
            }
        }
    }

    @Command(label = "givechar", aliases = {"givec"}, 
            usage = "Usage: givechar <player|avatarId> [level|avatarId] [level]")
    public static class GiveCharCommand implements CommandHandler {
        @Override public void execute(GenshinPlayer player, List<String> args) {
            int target, avatarId, level = 1, ascension = 1;

            if(args.size() < 1) {
                CommandHandler.sendMessage(player, "Usage: givechar <player> <avatarId> [level]");
                return;
            }
            
            switch(args.size()) {
                default:
                CommandHandler.sendMessage(player, "Usage: givechar <player> <avatarId> [level]");
                    return;
                case 2:
                    try {
                        target = Integer.parseInt(args.get(0));
                        if(Grasscutter.getGameServer().getPlayerByUid(target) == null) {
                            target = player.getUid(); 
                            level = Integer.parseInt(args.get(1));
                            avatarId = Integer.parseInt(args.get(0));
                        } else {
                            avatarId = Integer.parseInt(args.get(1));
                        }
                    } catch (NumberFormatException ignored) {
                        // TODO: Parse from avatar name using GM Handbook.
                        CommandHandler.sendMessage(player, "Invalid avatar or player ID.");
                        return;
                    }
                    break;
                case 3:
                    try {
                        target = Integer.parseInt(args.get(0));
                        if(Grasscutter.getGameServer().getPlayerByUid(target) == null) {
                            CommandHandler.sendMessage(player, "Invalid player ID."); return;
                        }

                        avatarId = Integer.parseInt(args.get(1));
                        level = Integer.parseInt(args.get(2));
                    } catch (NumberFormatException ignored) {
                        // TODO: Parse from avatar name using GM Handbook.
                        CommandHandler.sendMessage(player, "Invalid avatar or player ID.");
                        return;
                    }
                    break;
            }

            GenshinPlayer targetPlayer = Grasscutter.getGameServer().getPlayerByUid(target);
            if(targetPlayer == null) {
                CommandHandler.sendMessage(player, "Player not found."); return;
            }
                
            AvatarData avatarData = GenshinData.getAvatarDataMap().get(avatarId);
            if(avatarData == null) {
                CommandHandler.sendMessage(player, "Invalid avatar id."); return;
            }

            // Calculate ascension level.
            if (level <= 40) {
                ascension = (int) Math.ceil(level / 20f);
            } else {
                ascension = (int) Math.ceil(level / 10f) - 3;
            }

            GenshinAvatar avatar = new GenshinAvatar(avatarId);
            avatar.setLevel(level);
            avatar.setPromoteLevel(ascension);
            
            // This will handle stats and talents
            avatar.recalcStats();
    
            targetPlayer.addAvatar(avatar);
        }

        @Override
        public void execute(List<String> args) {
            if(args.size() < 2) {
                CommandHandler.sendMessage(null, "Usage: givechar <player> <itemId|itemName> [amount]");
                return;
            }

            try {
                int target = Integer.parseInt(args.get(0));
                int avatarID = Integer.parseInt(args.get(1));
                int level = 1; if(args.size() > 2) level = Integer.parseInt(args.get(2));
                int ascension;
                
                GenshinPlayer targetPlayer = Grasscutter.getGameServer().getPlayerByUid(target);
                if(targetPlayer == null) {
                    CommandHandler.sendMessage(null, "Player not found."); return;
                }
                    
                AvatarData avatarData = GenshinData.getAvatarDataMap().get(avatarID);
                if(avatarData == null) {
                    CommandHandler.sendMessage(null, "Invalid avatar id."); return;
                }
                
                // Calculate ascension level.
                if (level <= 40) {
                    ascension = (int) Math.ceil(level / 20f);
                } else {
                    ascension = (int) Math.ceil(level / 10f) - 3;
                }
                
                GenshinAvatar avatar = new GenshinAvatar(avatarID);
                avatar.setLevel(level);
                avatar.setPromoteLevel(ascension);

                // This will handle stats and talents
                avatar.recalcStats();
        
                targetPlayer.addAvatar(avatar);
            } catch (NumberFormatException ignored) {
                CommandHandler.sendMessage(null, "Invalid item or player ID.");
            }
        }
    }

    @Command(label = "spawn", execution = Command.Execution.PLAYER, 
            usage = "Usage: spawn <entityId|entityName> [level] [amount]")
    public static class SpawnCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            if(args.size() < 1) {
                CommandHandler.sendMessage(null, "Usage: spawn <entityId|entityName> [amount]");
                return;
            }

            try {
                int entity = Integer.parseInt(args.get(0));
                int level = 1; if(args.size() > 1) level = Integer.parseInt(args.get(1));
                int amount = 1; if(args.size() > 2) amount = Integer.parseInt(args.get(2));

                MonsterData entityData = GenshinData.getMonsterDataMap().get(entity);
                if(entityData == null) {
                    CommandHandler.sendMessage(null, "Invalid entity id."); return;
                }

                float range = (5f + (.1f * amount));
                for (int i = 0; i < amount; i++) {
                    Position pos = player.getPos().clone().addX((float) (Math.random() * range) - (range / 2)).addY(3f).addZ((float) (Math.random() * range) - (range / 2));
                    EntityMonster monster = new EntityMonster(player.getScene(), entityData, pos, level);
                    player.getScene().addEntity(monster);
                }
            } catch (NumberFormatException ignored) {
                CommandHandler.sendMessage(null, "Invalid item or player ID.");
            }
        }
    }
    
    @Command(label = "killall", 
            usage = "Usage: killall [playerUid] [sceneId]")
    public static class KillAllCommand implements CommandHandler {

        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            GenshinScene scene = player.getScene();
            scene.getEntities().values().stream()
                    .filter(entity -> entity instanceof EntityMonster)
                    .forEach(entity -> scene.killEntity(entity, 0));
            CommandHandler.sendMessage(null, "Killing all monsters in scene " + scene.getId());
        }
        
        @Override
        public void execute(List<String> args) {
            if(args.size() < 2) {
                CommandHandler.sendMessage(null, "Usage: killall [playerUid] [sceneId]"); return;
            }

            try {
                int playerUid = Integer.parseInt(args.get(0));
                int sceneId = Integer.parseInt(args.get(1));
                
                GenshinPlayer player = Grasscutter.getGameServer().getPlayerByUid(playerUid);
                if (player == null) {
                    CommandHandler.sendMessage(null, "Player not found or offline.");
                    return;
                }
                
                GenshinScene scene = player.getWorld().getSceneById(sceneId);
                if (scene == null) {
                    CommandHandler.sendMessage(null, "Scene not found in player world");
                    return;
                }
                
                scene.getEntities().values().stream()
                        .filter(entity -> entity instanceof EntityMonster)
                        .forEach(entity -> scene.killEntity(entity, 0));
                CommandHandler.sendMessage(null, "Killing all monsters in scene " + scene.getId());
            } catch (NumberFormatException ignored) {
                CommandHandler.sendMessage(null, "Invalid arguments.");
            }
        }
    }
    
    @Command(label = "resetconst", aliases = {"resetconstellation"}, 
            usage = "Usage: resetconst [all]", execution = Command.Execution.PLAYER)
    public static class ResetConstellationCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            if(args.size() > 0 && args.get(0).equalsIgnoreCase("all")) {
                player.getAvatars().forEach(this::resetConstellation);
                player.dropMessage("Reset all avatars' constellations.");
            } else {
                EntityAvatar entity = player.getTeamManager().getCurrentAvatarEntity(); 
                if(entity == null)
                    return;
                
                GenshinAvatar avatar = entity.getAvatar();
                this.resetConstellation(avatar);

                player.dropMessage("Constellations for " + avatar.getAvatarData().getName() + " have been reset. Please relog to see changes.");
            }
        }
        
        private void resetConstellation(GenshinAvatar avatar) {
            avatar.getTalentIdList().clear();
            avatar.setCoreProudSkillLevel(0);
            avatar.recalcStats();
            avatar.save();
        }
    }
    
    @Command(label = "setstatatk", aliases = {"statattack", "statatk"}, 
            usage = "Usage: setstatatk [new base attack]", execution = Command.Execution.PLAYER)
    public static class SetAttackStatCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
                EntityAvatar entity = player.getTeamManager().getCurrentAvatarEntity(); 
                GenshinAvatar avatar = entity.getAvatar();
                avatar.avatarSetBaseAtk(Float.parseFloat(args.get(0)));
                this.resetConstellation(avatar);
                player.dropMessage("Base attack damage for " + avatar.getAvatarData().getName() + " has been set.");
            }
        private void resetConstellation(GenshinAvatar avatar) {
            avatar.recalcStats();
            avatar.save();
        }
    }
    
    @Command(label = "setstathp", aliases = {"stathp"}, 
            usage = "Usage: setstathp [new base HP]", execution = Command.Execution.PLAYER)
    public static class SetHealthStatCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
                EntityAvatar entity = player.getTeamManager().getCurrentAvatarEntity(); 
                GenshinAvatar avatar = entity.getAvatar();
                avatar.avatarSetBaseHp(Float.parseFloat(args.get(0)));
                this.resetConstellation(avatar);
                player.dropMessage("Base HP for " + avatar.getAvatarData().getName() + " has been set.");
            }
        private void resetConstellation(GenshinAvatar avatar) {
            avatar.recalcStats();
            avatar.save();
        }
    }
    
    @Command(label = "setstatdef", aliases = {"statdef"}, 
            usage = "Usage: setstatdef [new base defense]", execution = Command.Execution.PLAYER)
    public static class SetDefenseStatCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
                EntityAvatar entity = player.getTeamManager().getCurrentAvatarEntity(); 
                GenshinAvatar avatar = entity.getAvatar();
                avatar.avatarSetBaseDef(Float.parseFloat(args.get(0)));
                this.resetConstellation(avatar);
                player.dropMessage("Defense for " + avatar.getAvatarData().getName() + " has been set.");
            }
        private void resetConstellation(GenshinAvatar avatar) {
            avatar.recalcStats();
            avatar.save();
        }
    }
    
    @Command(label = "setstatcrit", aliases = {"statcrit"}, 
            usage = "Usage: setstatcrit [new base crit chance]", execution = Command.Execution.PLAYER)
    public static class SetCritStatCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
                EntityAvatar entity = player.getTeamManager().getCurrentAvatarEntity(); 
                GenshinAvatar avatar = entity.getAvatar();
                avatar.avatarSetBaseCrit(Float.parseFloat(args.get(0)));
                this.resetConstellation(avatar);
                player.dropMessage("Crit change for " + avatar.getAvatarData().getName() + " has been set.");
            }
        private void resetConstellation(GenshinAvatar avatar) {
            avatar.recalcStats();
            avatar.save();
        }
    }
    
    @Command(label = "setstatcritdmg", aliases = {"statcritdmg"}, 
            usage = "Usage: setstatcritdmg [new base crit dmg]", execution = Command.Execution.PLAYER)
    public static class SetCritDmgStatCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
                EntityAvatar entity = player.getTeamManager().getCurrentAvatarEntity(); 
                GenshinAvatar avatar = entity.getAvatar();
                avatar.avatarSetBaseCritDmg(Float.parseFloat(args.get(0)));
                this.resetConstellation(avatar);
                player.dropMessage("Crit DMG for " + avatar.getAvatarData().getName() + " has been set.");
            }
        private void resetConstellation(GenshinAvatar avatar) {
            avatar.recalcStats();
            avatar.save();
        }
    }
    
    @Command(label = "godmode",
            usage = "Usage: godmode", execution = Command.Execution.PLAYER)
    public static class GodModeCommand implements CommandHandler {
        
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            player.setGodmode(!player.inGodmode());
            player.dropMessage("Godmode is now " + (player.inGodmode() ? "enabled" : "disabled") + ".");
        }
    }
    
    @Command(label = "sethealth", aliases = {"sethp"}, 
            usage = "Usage: sethealth <hp>", execution = Command.Execution.PLAYER)
    public static class SetHealthCommand implements CommandHandler {

        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            if(args.size() < 1) {
                CommandHandler.sendMessage(null, "Usage: sethealth <hp>"); return;
            }
            
            try {
                int health = Integer.parseInt(args.get(0));
                EntityAvatar entity = player.getTeamManager().getCurrentAvatarEntity();
                if(entity == null)
                    return;
                
                entity.setFightProperty(FightProperty.FIGHT_PROP_CUR_HP, health);
                entity.getWorld().broadcastPacket(new PacketEntityFightPropUpdateNotify(entity, FightProperty.FIGHT_PROP_CUR_HP));
                player.dropMessage("Health set to " + health + ".");
            } catch (NumberFormatException ignored) {
                CommandHandler.sendMessage(null, "Invalid health value.");
            }
        }
    }

    @Command(label = "setworldlevel", aliases = {"setworldlvl"}, 
            usage = "Usage: setworldlevel <level>", execution = Command.Execution.PLAYER)
    public static class SetWorldLevelCommand implements CommandHandler {
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            if(args.size() < 1) {
                CommandHandler.sendMessage(player, "Usage: setworldlevel <level>"); return;
            }
            
            try {
                int level = Integer.parseInt(args.get(0));

                // Set in both world and player props
                player.getWorld().setWorldLevel(level);
                player.setProperty(PlayerProperty.PROP_PLAYER_WORLD_LEVEL, level);

                player.dropMessage("World level set to " + level + ".");
            } catch (NumberFormatException ignored) {
                CommandHandler.sendMessage(null, "Invalid world level.");
            }
        }
    }
    
    @Command(label = "clearartifacts", aliases = {"clearart"}, 
            usage = "Usage: clearartifacts", execution = Command.Execution.PLAYER)
    public static class ClearArtifactsCommand implements CommandHandler {
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            Inventory playerInventory = player.getInventory();
            playerInventory.getItems().values().stream()
                    .filter(item -> item.getItemType() == ItemType.ITEM_RELIQUARY)
                    .filter(item -> item.getLevel() == 1 && item.getExp() == 0)
                    .filter(item -> !item.isLocked() && !item.isEquipped())
                    .forEach(item -> playerInventory.removeItem(item, item.getCount()));
        }
    }

    @Command(label = "changescene", aliases = {"scene"}, 
            usage = "Usage: changescene <scene id>", execution = Command.Execution.PLAYER)
    public static class ChangeSceneCommand implements CommandHandler {
        @Override
        public void execute(GenshinPlayer player, List<String> args) {
            if(args.size() < 1) {
                CommandHandler.sendMessage(player, "Usage: changescene <scene id>"); return;
            }
        
            try {
                int sceneId = Integer.parseInt(args.get(0));
                boolean result = player.getWorld().transferPlayerToScene(player, sceneId, player.getPos());

                if (!result) {
                    CommandHandler.sendMessage(null, "Scene does not exist or you are already in it");
                }
            } catch (Exception e) {
                CommandHandler.sendMessage(player, "Usage: changescene <scene id>"); return;
            }
        }
    }
}
