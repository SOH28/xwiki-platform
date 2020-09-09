/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.ranking;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration of {@link RankingManager}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Unstable
@Role
public interface RankingConfiguration
{
    /**
     * @return {@code false} if a vote set to 0 shoud lead to the deletion of a previously made vote. {@code true} means
     * that all votes noted to 0 are stored. Note that this option will impact the average rank.
     */
    boolean storeZero();

    /**
     * @return the upper bound of the scale. The lower bound is always 0.
     */
    int getScale();

    /**
     * @return {@code true} to create - if possible, depending on the storage implementation - a dedicated store.
     *         {@code false} means that all ranks from any applications will be stored on the same storage.
     */
    boolean hasDedicatedCore();

    /**
     * @return {@code true} to store the average rank. {@code false} means that the average rank is never stored nor
     * computed.
     */
    boolean storeAverage();

    /**
     * @return the storage hint to be used (e.g. solr).
     */
    String getStorageHint();
}
