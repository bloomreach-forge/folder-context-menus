package org.onehippo.forge.folderctxmenus.common;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Traversing node visitor abstraction.
 */
public interface NodeTraverser {

    /**
     * Returns true if the {@code node} is acceptable.
     * @param node node
     * @return true if the {@code node} is acceptable
     * @throws RepositoryException if repository exception occurs
     */
    boolean isAcceptable(Node node) throws RepositoryException;

    /**
     * Returns true if the {@code node} can be traversed further to its descendants.
     * @param node node
     * @return true if the {@code node} can be traversed further to its descendants
     * @throws RepositoryException if repository exception occurs
     */
    boolean isTraversable(Node node) throws RepositoryException;

    /**
     * Accept the {@code node}.
     * @param node node
     * @throws RepositoryException if repository exception occurs
     */
    void accept(Node node) throws RepositoryException;

}
