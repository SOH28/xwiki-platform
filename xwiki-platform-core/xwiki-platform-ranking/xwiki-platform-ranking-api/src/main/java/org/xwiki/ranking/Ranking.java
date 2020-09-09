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

import java.util.Date;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Generic interface of what should be available in a Ranking.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Unstable
public interface Ranking
{
    /**
     * @return the identifier of this ranking data.
     */
    String getId();

    /**
     * @return the identifier of the manager who handles this data.
     */
    String getManagerId();

    /**
     * @return the reference of the element being ranked.
     */
    EntityReference getRankedElement();

    /**
     * @return the type of the ranked element.
     */
    EntityType getEntityType();

    /**
     * @return the reference of the user who performs the rank.
     */
    UserReference getVoter();

    /**
     * @return the date of the creation of this ranking data.
     */
    Date getCreatedAt();

    /**
     * @return the date of the last update of this ranking data. It could be same as {@link #getCreatedAt()} if no
     *          update has been performed.
     */
    Date getUpdatedAt();

    /**
     * @return the actual rank.
     */
    int getRank();

    /**
     * @return the upper bound of the scale used to rank.
     */
    int getScale();
}
