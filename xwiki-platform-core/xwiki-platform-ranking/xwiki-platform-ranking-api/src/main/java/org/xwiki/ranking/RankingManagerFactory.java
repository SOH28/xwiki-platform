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
 * Allow to create dedicated instances of {@link RankingManager} for any ranking usage.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Role
@Unstable
public interface RankingManagerFactory
{
    /**
     * Create or retrieve an instance of {@link RankingManager} for the given hint.
     * If the instance needs to be created, the {@link RankingConfiguration} based on this hint will be used to create
     * it. If there is no instance of {@link RankingConfiguration} matching the given hint, the default implementation
     * will be used.
     *
     * @param hint an hint of an instance to create or retrieve.
     * @return a {@link RankingManager} identified with the given hint.
     * @throws RankingException in case of problem when creating or retrieving the component.
     */
    RankingManager getInstance(String hint) throws RankingException;
}
