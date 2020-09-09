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
package org.xwiki.ranking.internal;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ranking.AverageRank;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Default implementation of {@link AverageRank}.
 * This implementation provides a builder API.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class DefaultAverageRank implements AverageRank
{
    private String identifier;
    private String managerId;
    private EntityReference rankedElement;
    private double averageRank;
    private long rankingNumber;
    private int scale;
    private Date updatedAt;

    /**
     * Default constructor with identifier.
     *
     * @param identifier unique identifier of the average.
     */
    public DefaultAverageRank(String identifier)
    {
        this.identifier = identifier;
        this.updatedAt = new Date();
    }

    /**
     * Constructor that allows cloning an existing average rank.
     *
     * @param averageRank the already existing object to clone.
     */
    public DefaultAverageRank(AverageRank averageRank)
    {
        this.identifier = averageRank.getId();
        this.managerId = averageRank.getManagerId();
        this.rankedElement = averageRank.getRankedElement();
        this.averageRank = averageRank.getAverage();
        this.rankingNumber = averageRank.getRankingNumber();
        this.scale = averageRank.getScale();
        this.updatedAt = averageRank.getUpdatedAt();
    }

    @Override
    public AverageRank updateVote(int oldVote, int newVote)
    {
        double newTotal = (this.averageRank * this.rankingNumber) - oldVote + newVote;
        this.averageRank = newTotal / this.rankingNumber;
        this.updatedAt = new Date();
        return this;
    }

    @Override
    public AverageRank removeVote(int vote)
    {
        double newTotal = (this.averageRank * this.rankingNumber) - vote;
        this.rankingNumber--;
        this.averageRank = newTotal / this.rankingNumber;
        this.updatedAt = new Date();
        return this;
    }

    @Override
    public AverageRank addVote(int vote)
    {
        double newTotal = (this.averageRank * this.rankingNumber) + vote;
        this.rankingNumber++;
        this.averageRank = newTotal / this.rankingNumber;
        this.updatedAt = new Date();
        return this;
    }

    /**
     * @param managerId the manager identifier to set.
     * @return the current instance.
     */
    public DefaultAverageRank setManagerId(String managerId)
    {
        this.managerId = managerId;
        return this;
    }

    /**
     * @param rankedElement the reference of the element being ranked.
     * @return the current instance.
     */
    public DefaultAverageRank setRankedElement(EntityReference rankedElement)
    {
        this.rankedElement = rankedElement;
        return this;
    }

    /**
     * @param averageRank the average value.
     * @return the current instance.
     */
    public DefaultAverageRank setAverageRank(double averageRank)
    {
        this.averageRank = averageRank;
        return this;
    }

    /**
     * @param rankingNumber the number of elements ranked.
     * @return the current instance.
     */
    public DefaultAverageRank setRankingNumber(long rankingNumber)
    {
        this.rankingNumber = rankingNumber;
        return this;
    }

    /**
     * @param scale the scale used for ranking elements.
     * @return the current instance.
     */
    public DefaultAverageRank setScale(int scale)
    {
        this.scale = scale;
        return this;
    }

    /**
     * @param updatedAt the date of last update.
     * @return the current instance.
     */
    public DefaultAverageRank setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
        return this;
    }

    @Override
    public String getId()
    {
        return this.identifier;
    }

    @Override
    public String getManagerId()
    {
        return this.managerId;
    }

    @Override
    public EntityReference getRankedElement()
    {
        return this.rankedElement;
    }

    @Override
    public EntityType getEntityType()
    {
        return this.rankedElement.getType();
    }

    @Override
    public double getAverage()
    {
        return this.averageRank;
    }

    @Override
    public long getRankingNumber()
    {
        return this.rankingNumber;
    }

    @Override
    public int getScale()
    {
        return this.scale;
    }

    @Override
    public Date getUpdatedAt()
    {
        return updatedAt;
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

        DefaultAverageRank that = (DefaultAverageRank) o;

        return new EqualsBuilder()
            .append(averageRank, that.averageRank)
            .append(rankingNumber, that.rankingNumber)
            .append(scale, that.scale)
            .append(identifier, that.identifier)
            .append(managerId, that.managerId)
            .append(rankedElement, that.rankedElement)
            .append(updatedAt, that.updatedAt)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 67)
            .append(identifier)
            .append(managerId)
            .append(rankedElement)
            .append(averageRank)
            .append(rankingNumber)
            .append(scale)
            .append(updatedAt)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("identifier", identifier)
            .append("managerId", managerId)
            .append("rankedElement", rankedElement)
            .append("averageRank", averageRank)
            .append("rankingNumber", rankingNumber)
            .append("scale", scale)
            .append("updatedAt", updatedAt)
            .toString();
    }
}
