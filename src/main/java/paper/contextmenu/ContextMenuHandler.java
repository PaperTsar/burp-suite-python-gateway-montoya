package paper.contextmenu;

import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ContextMenuHandler implements ContextMenuItemsProvider {
    private final Map<String, BiConsumer<ContextMenuEvent, ActionEvent>> entries = new ConcurrentHashMap<>();
    private final TreeNode<ContextMenuTreeNode> treeEntries = new TreeNode<>(null);

    @Override
    public synchronized List<Component> provideMenuItems(ContextMenuEvent contextMenuEvent) {
        List<Component> result = new ArrayList<>();
        for (TreeNode<ContextMenuTreeNode> node : treeEntries) {
            result.add(createJMenuChildItem(node, contextMenuEvent));
        }
        return result;
    }

    private synchronized Component createJMenuChildItem(TreeNode<ContextMenuTreeNode> parentNode, ContextMenuEvent contextMenuEvent) {
        if (parentNode.value().isSubMenu()) {
            JMenu subMenu = new JMenu(parentNode.value().itemText());
            for (TreeNode<ContextMenuTreeNode> child : parentNode) {
                subMenu.add(createJMenuChildItem(child, contextMenuEvent));
            }
            return subMenu;
        } else {
            JMenuItem menuItem = new JMenuItem(parentNode.value().itemText());
            menuItem.addActionListener(actionEvent -> {
                parentNode.value().callback().accept(contextMenuEvent, actionEvent);
            });
            return menuItem;
        }
    }


    public synchronized void setEntry(List<String> itemTexts, BiConsumer<ContextMenuEvent, ActionEvent> callback) {
        var currentNode = treeEntries;

        for (var it = itemTexts.iterator(); it.hasNext(); ) {
            var itemText = it.next();
            var optionalFoundChild = currentNode.findChild(c -> c.value().itemText().equals(itemText));

            if (!it.hasNext()) {
                optionalFoundChild.ifPresent(TreeNode::removeFromParent);
                currentNode.addChild(new ContextMenuTreeNode(itemText, callback));
                currentNode.sortChildren(Comparator.comparing(u -> u.value().itemText()));
            } else if (optionalFoundChild.isEmpty()) {
                currentNode = currentNode.addChild(new ContextMenuTreeNode(itemText, null));
            } else {
                optionalFoundChild.get().setValue(new ContextMenuTreeNode(itemText, null));
                currentNode.sortChildren(Comparator.comparing(u -> u.value().itemText()));
                currentNode = optionalFoundChild.get();
            }
        }
    }

    public synchronized void removeEntry(List<String> itemTexts) {
        var currentNode = treeEntries;

        for (var itemText : itemTexts) {
            var optionalFoundChild = currentNode.findChild(c -> c.value().itemText().equals(itemText));
            if (optionalFoundChild.isEmpty()) {
                return; // Not it the list, nothing to remove
            }

            currentNode = optionalFoundChild.get();
        }

        currentNode.removeFromParent();
    }
}
