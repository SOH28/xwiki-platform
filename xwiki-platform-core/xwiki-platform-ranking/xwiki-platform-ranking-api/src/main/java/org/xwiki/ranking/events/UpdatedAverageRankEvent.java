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
package org.xwiki.ranking.events;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

/**
 * Event sent whenever an {@link org.xwiki.ranking.AverageRank} is updated.
 * The event is sent with the following informations:
 *   - source: the identifier of the {@link org.xwiki.ranking.RankingManager}
 *   - data: the {@link org.xwiki.ranking.AverageRank} updated.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Unstable
public class UpdatedAverageRankEvent implements Event
{
    private double oldAverage;
    private long oldRatingCount;

    /**
     * Default constructor.
     *
     * @param oldAverage the old average value, before the update.
     * @param oldRatingCount the old number of ratings, before the update.
     */
    public UpdatedAverageRankEvent(double oldAverage, long oldRatingCount)
    {
        this.oldAverage = oldAverage;
        this.oldRatingCount = oldRatingCount;
    }

    /**
     * @return the old average value, before the update.
     */
    public double getOldAverage()
    {
        return oldAverage;
    }

    /**
     * @return the old number of rating, before the update.
     */
    public long getOldRatingCount()
    {
        return oldRatingCount;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof UpdatedAverageRankEvent;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpdatedAverageRankEvent that = (UpdatedAverageRankEvent) o;

        return new EqualsBuilder()
            .append(oldAverage, that.oldAverage)
            .append(oldRatingCount, that.oldRatingCount)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(oldAverage)
            .append(oldRatingCount)
            .toHashCode();
    }
}
