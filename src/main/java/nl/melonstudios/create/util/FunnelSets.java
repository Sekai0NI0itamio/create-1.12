package nl.melonstudios.create.util;

import nl.melonstudios.create.block.funnel.BlockFunnelDown;
import nl.melonstudios.create.block.funnel.BlockFunnelWall;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.item.ItemBlockFunnel;

import java.util.HashMap;
import java.util.Map;

public class FunnelSets {
    private static final Map<String, FunnelSet> map = new HashMap<>();
    public static void put(String name, FunnelSet set) {
        map.put(name, set);
    }
    public static FunnelSet get(String name) {
        return map.get(name);
    }

    public static final FunnelSet ANDESITE = new FunnelSet() {
        @Override
        public BlockFunnelWall getWall() {
            return BlockInit.FUNNEL_ANDESITE_WALL;
        }

        @Override
        public BlockFunnelDown getDown() {
            return BlockInit.FUNNEL_ANDESITE;
        }

        @Override
        public ItemBlockFunnel getItem() {
            return ItemInit.FUNNEL_ANDESITE;
        }
    };

    public static final FunnelSet BRASS = new FunnelSet() {
        @Override
        public BlockFunnelWall getWall() {
            return BlockInit.FUNNEL_BRASS_WALL;
        }

        @Override
        public BlockFunnelDown getDown() {
            return BlockInit.FUNNEL_BRASS;
        }

        @Override
        public ItemBlockFunnel getItem() {
            return ItemInit.FUNNEL_BRASS;
        }
    };

    static {
        put("andesite", ANDESITE);
        put("brass", BRASS);
    }
}
