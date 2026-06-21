package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import javafx.scene.image.Image;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Centralised access to GUI image assets.
 *
 * <p>Provides a process-wide {@link Image} cache (so the same PNG is decoded
 * once, as mandated by the GUI rules) and the mapping from a player's
 * {@link TotemColor} to the matching totem artwork bundled under
 * {@code /cards/totem/}.
 *
 * <p>Two totem styles ship with the game: an angled 3-D render (files
 * {@code Livello 17-21}) used for prominent spots such as the players strip,
 * and a flat front view ({@code Livello 12-16}) used for small markers on the
 * board tiles and the turn-order rail.
 */
public final class GuiAssets {

    private static final Logger LOGGER = Logger.getLogger(GuiAssets.class.getName());
    public static final String APP_ICON_PATH = "/cards/totem/Livello 17.png";

    /** Decoded-image cache keyed by classpath resource path. */
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private GuiAssets() {
    }

    /**
     * Returns the cached {@link Image} for the given absolute classpath
     * resource path, decoding it on first access.
     *
     * @param resourcePath absolute classpath path (e.g. {@code /cards/...})
     * @return the cached image, or {@code null} if the resource is missing
     */
    public static Image image(String resourcePath) {
        if (resourcePath == null) return null;
        Image cached = CACHE.get(resourcePath);
        if (cached != null) return cached;
        var stream = GuiAssets.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            LOGGER.warning(() -> "Image resource not found: " + resourcePath);
            return null;
        }
        Image img = new Image(stream);
        CACHE.put(resourcePath, img);
        return img;
    }

    /** Application/window icon used for JavaFX stage and desktop taskbar. */
    public static Image appIcon() {
        return image(APP_ICON_PATH);
    }

    /** Angled 3-D totem render for the given color (players strip, podium). */
    public static Image totem3D(TotemColor color) {
        return image(totem3DPath(color));
    }

    /** Flat front-view totem for the given color (board markers, turn rail). */
    public static Image totemFlat(TotemColor color) {
        return image(totemFlatPath(color));
    }

    /** Classpath path of the angled 3-D totem for the given color. */
    public static String totem3DPath(TotemColor color) {
        return "/cards/totem/" + switch (color) {
            case RED -> "Livello 17.png";
            case YELLOW -> "Livello 18.png";
            case BLUE -> "Livello 19.png";
            case BLACK -> "Livello 20.png";
            case WHITE -> "Livello 21.png";
        };
    }

    /** Classpath path of the flat front-view totem for the given color. */
    public static String totemFlatPath(TotemColor color) {
        return "/cards/totem/" + switch (color) {
            case RED -> "Livello 12.png";
            case YELLOW -> "Livello 13.png";
            case BLUE -> "Livello 14.png";
            case BLACK -> "Livello 15.png";
            case WHITE -> "Livello 16.png";
        };
    }
}
