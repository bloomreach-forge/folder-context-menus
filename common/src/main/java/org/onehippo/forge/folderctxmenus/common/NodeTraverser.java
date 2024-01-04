/*
 * Copyright 2024 Bloomreach (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
