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
import org.xwiki.ranking.Ranking;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.user.UserReference;

/**
 * Default implementation of {@link Ranking}.
 * This class provides a builder API for setting the values.
 *
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class DefaultRanking implements Ranking
{
    private String identifier;
    private String managerId;
    private EntityReference rankedElement;
    private UserReference voter;
    private Date createdAt;
    private Date updatedAt;
    private int rank;
    private int scale;

    /**
     * Default constructor.
     *
     * @param identifier the unique identifier.
     */
    public DefaultRanking(String identifier)
    {
        this.identifier = identifier;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Constructor to clone an existing instance of ranking.
     *
     * @param ranking the instance to clone.
     */
    public DefaultRanking(Ranking ranking)
    {
        this.identifier = ranking.getId();
        this.managerId = ranking.getManagerId();
        this.rankedElement = ranking.getRankedElement();
        this.rank = ranking.getRank();
        this.updatedAt = ranking.getUpdatedAt();
        this.createdAt = ranking.getCreatedAt();
        this.scale = ranking.getScale();
        this.voter = ranking.getVoter();
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
    public UserReference getVoter()
    {
        return this.voter;
    }

    @Override
    public Date getCreatedAt()
    {
        return this.createdAt;
    }

    @Override
    public Date getUpdatedAt()
    {
        return this.updatedAt;
    }

    /**
     * @param date the date of latest update.
     * @return the current instance.
     */
    public DefaultRanking setUpdatedAt(Date date)
    {
        this.updatedAt = date;
        return this;
    }

    @Override
    public int getRank()
    {
        return this.rank;
    }

    /**
     * @param id the identifier of the ranking.
     * @return the current instance.
     */
    public DefaultRanking setId(String id)
    {
        this.identifier = id;
        return this;
    }

    /**
     * @param rank the rank value to set.
     * @return the current instance.
     */
    public DefaultRanking setRank(int rank)
    {
        this.rank = rank;
        return this;
    }

    /**
     * @param managerId the identifier of the manager.
     * @return the current instance.
     */
    public DefaultRanking setManagerId(String managerId)
    {
        this.managerId = managerId;
        return this;
    }

    /**
     * @param rankedElement the element that has been ranked.
     * @return the current instance.
     */
    public DefaultRanking setRankedElement(EntityReference rankedElement)
    {
        this.rankedElement = rankedElement;
        return this;
    }

    /**
     * @param voter the user who performs the vote.
     * @return the current instance.
     */
    public DefaultRanking setVoter(UserReference voter)
    {
        this.voter = voter;
        return this;
    }

    /**
     * @param createdAt the date of creation.
     * @return the current instance.
     */
    public DefaultRanking setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * @param scale the scale of the ranks.
     * @return the current instance.
     */
    public DefaultRanking setScale(int scale)
    {
        this.scale = scale;
        return this;
    }

    @Override
    public int getScale()
    {
        return this.scale;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("identifier", identifier)
            .append("managerId", managerId)
            .append("rankedElement", rankedElement)
            .append("voter", voter)
            .append("createdAt", createdAt)
            .append("updatedAt", updatedAt)
            .append("rank", rank)
            .append("scale", scale)
            .toString();
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

        DefaultRanking that = (DefaultRanking) o;

        return new EqualsBuilder()
            .append(rank, that.rank)
            .append(scale, that.scale)
            .append(identifier, that.identifier)
            .append(managerId, that.managerId)
            .append(rankedElement, that.rankedElement)
            .append(voter, that.voter)
            .append(createdAt, that.createdAt)
            .append(updatedAt, that.updatedAt)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(13, 97)
            .append(identifier)
            .append(managerId)
            .append(rankedElement)
            .append(voter)
            .append(createdAt)
            .append(updatedAt)
            .append(rank)
            .append(scale)
            .toHashCode();
    }
}
