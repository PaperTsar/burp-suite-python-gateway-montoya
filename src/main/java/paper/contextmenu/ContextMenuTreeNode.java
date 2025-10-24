package paper.contextmenu;

import burp.api.montoya.ui.contextmenu.ContextMenuEvent;

import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.BiConsumer;

public record ContextMenuTreeNode(String itemText, BiConsumer<ContextMenuEvent, ActionEvent> callback) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ContextMenuTreeNode that = (ContextMenuTreeNode) o;
        // Ignore callback field
        return Objects.equals(itemText, that.itemText);
    }

    @Override
    public int hashCode() {
        // Ignore callback field
        return Objects.hash(itemText);
    }

    public boolean isSubMenu() {
        return callback() == null;
    }
}
