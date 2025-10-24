package paper.contextmenu;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.function.Predicate;

public class TreeNode<T> implements Iterable<TreeNode<T>> {
    private T value;
    private final List<TreeNode<T>> children = new ArrayList<>();
    private TreeNode<T> parent; // optional

    public TreeNode(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    public void setValue(T v) {
        value = v;
    }

    public List<TreeNode<T>> children() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public Optional<TreeNode<T>> parent() {
        return Optional.ofNullable(parent);
    }

    public TreeNode<T> addChild(T v) {
        TreeNode<T> child = new TreeNode<>(v);
        child.parent = this;
        children.add(child);
        return child;
    }

    public void removeChild(TreeNode<T> v) {
        children().remove(v);
    }

    public void removeFromParent() {
        if (parent().isPresent()) {
            parent().get().removeChild(this);
        }
    }

    public void sortChildren(Comparator<? super TreeNode<T>> comparator) {
        children().sort(comparator);
    }

    public Optional<TreeNode<T>> findChild(Predicate<? super TreeNode<T>> predicate) {
        return children.stream().filter(predicate).findFirst();
    }

    @Override
    public Iterator<TreeNode<T>> iterator() {
        return children.iterator();
    }
}
