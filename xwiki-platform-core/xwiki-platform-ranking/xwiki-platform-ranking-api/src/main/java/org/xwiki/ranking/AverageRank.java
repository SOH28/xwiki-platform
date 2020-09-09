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

/**
 * General interface to provide information about average ranking notation.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Unstable
public interface AverageRank
{
    /**
     * @return the identifier of this average rank data.
     */
    String getId();

    /**
     * @return the identifier of the manager who handles this average rank data.
     */
    String getManagerId();

    /**
     * @return the reference of the element this average rank is for.
     */
    EntityReference getRankedElement();

    /**
     * @return the type of the ranked element reference.
     */
    EntityType getEntityType();

    /**
     * @return the actual average rank.
     */
    double getAverage();

    /**
     * @return the total number of ranking notation used to compute this average.
     */
    long getRankingNumber();

    /**
     * @return the upper bound scale of the rankings.
     */
    int getScale();

    /**
     * @return the date of the last modification of this average.
     */
    Date getUpdatedAt();

    /**
     * Update the average rank by performing a change in an existing vote.
     *
     * @param oldVote the old rank value to be modified.
     * @param newVote the new rank value to be applied.
     * @return the current instance modified.
     */
    AverageRank updateVote(int oldVote, int newVote);

    /**
     * Update the average rank by removing the given rank.
     *
     * @param vote the old rank to be removed.
     * @return the current instance modified.
     */
    AverageRank removeVote(int vote);

    /**
     * Update the average rank to add a new rank.
     *
     * @param vote the new rank to be taken into account.
     * @return the current instance modified.
     */
    AverageRank addVote(int vote);
}
