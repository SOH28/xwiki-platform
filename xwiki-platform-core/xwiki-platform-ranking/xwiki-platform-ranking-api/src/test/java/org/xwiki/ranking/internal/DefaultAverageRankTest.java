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

import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ranking.AverageRank;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link org.xwiki.ranking.AverageRank}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class DefaultAverageRankTest
{
    @Test
    void simpleConstructor()
    {
        Date currentDate = new Date();
        DefaultAverageRank defaultAverageRank = new DefaultAverageRank("myId");
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(0, defaultAverageRank.getRankingNumber());
        assertEquals(0, defaultAverageRank.getAverage(), 0);
        assertTrue(currentDate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
    }

    @Test
    void cloneConstructor()
    {
        AverageRank customRank = new AverageRank()
        {
            @Override
            public String getId()
            {
                return "someId";
            }

            @Override
            public String getManagerId()
            {
                return "myManagerId";
            }

            @Override
            public EntityReference getRankedElement()
            {
                return new EntityReference("Foobar", EntityType.ATTACHMENT);
            }

            @Override
            public EntityType getEntityType()
            {
                return EntityType.ATTACHMENT;
            }

            @Override
            public double getAverage()
            {
                return 0.23;
            }

            @Override
            public long getRankingNumber()
            {
                return 1343;
            }

            @Override
            public int getScale()
            {
                return 3;
            }

            @Override
            public Date getUpdatedAt()
            {
                return new Date(24);
            }

            @Override
            public AverageRank updateVote(int oldVote, int newVote)
            {
                return this;
            }

            @Override
            public AverageRank removeVote(int vote)
            {
                return this;
            }

            @Override
            public AverageRank addVote(int vote)
            {
                return this;
            }
        };

        DefaultAverageRank defaultAverageRank = new DefaultAverageRank(customRank);
        assertNotEquals(defaultAverageRank, customRank);
        assertEquals("someId", defaultAverageRank.getId());
        assertEquals("myManagerId", defaultAverageRank.getManagerId());
        assertEquals(new EntityReference("Foobar", EntityType.ATTACHMENT), defaultAverageRank.getRankedElement());
        assertEquals(EntityType.ATTACHMENT, defaultAverageRank.getEntityType());
        assertEquals(0.23, defaultAverageRank.getAverage(), 0);
        assertEquals(1343, defaultAverageRank.getRankingNumber());
        assertEquals(3, defaultAverageRank.getScale());
        assertEquals(new Date(24), defaultAverageRank.getUpdatedAt());
        assertEquals(defaultAverageRank, new DefaultAverageRank(defaultAverageRank));
    }

    @Test
    void addVote()
    {
        DefaultAverageRank defaultAverageRank = new DefaultAverageRank("myId");
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(0, defaultAverageRank.getRankingNumber());
        assertEquals(0, defaultAverageRank.getAverage(), 0);

        Date beforeFirstUpdate = new Date();
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.addVote(4);
        assertEquals(4, defaultAverageRank.getAverage(), 0);
        assertEquals(1, defaultAverageRank.getRankingNumber());
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.addVote(1);
        defaultAverageRank.addVote(3);
        defaultAverageRank.addVote(0);
        defaultAverageRank.addVote(3);
        defaultAverageRank.addVote(2);
        assertEquals(6, defaultAverageRank.getRankingNumber());
        assertEquals(2.166667, defaultAverageRank.getAverage(), 0.000001);
    }

    @Test
    void removeVote()
    {
        DefaultAverageRank defaultAverageRank = new DefaultAverageRank("myId")
            .setAverageRank(13.0 / 6)
            .setRankingNumber(6);
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(6, defaultAverageRank.getRankingNumber());
        assertEquals(2.166667, defaultAverageRank.getAverage(), 0.000001);

        Date beforeFirstUpdate = new Date();
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.removeVote(0);
        assertEquals(2.6, defaultAverageRank.getAverage(), 0);
        assertEquals(5, defaultAverageRank.getRankingNumber());
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
    }

    @Test
    void updateVote()
    {
        DefaultAverageRank defaultAverageRank = new DefaultAverageRank("myId")
            .setAverageRank(13.0 / 6)
            .setRankingNumber(6);
        assertEquals("myId", defaultAverageRank.getId());
        assertEquals(6, defaultAverageRank.getRankingNumber());
        assertEquals(2.166667, defaultAverageRank.getAverage(), 0.000001);

        Date beforeFirstUpdate = new Date();
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));

        defaultAverageRank.updateVote(0, 5);
        assertEquals(3, defaultAverageRank.getAverage(), 0);
        assertEquals(6, defaultAverageRank.getRankingNumber());
        assertNotEquals(new Date(0), defaultAverageRank.getUpdatedAt());
        assertNotNull(defaultAverageRank.getUpdatedAt());
        assertTrue(beforeFirstUpdate.toInstant()
            .isAfter(defaultAverageRank.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
    }
}
