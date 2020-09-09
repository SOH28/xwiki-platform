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
import org.xwiki.ranking.Ranking;
import org.xwiki.user.UserReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultRanking}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class DefaultRankingTest
{
    @Test
    void simpleConstructor()
    {
        Date currentDate = new Date();
        DefaultRanking defaultRanking = new DefaultRanking("myId");
        assertEquals("myId", defaultRanking.getId());
        assertTrue(currentDate.toInstant()
            .isAfter(defaultRanking.getUpdatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
        assertTrue(currentDate.toInstant()
            .isAfter(defaultRanking.getCreatedAt().toInstant().minus(1, ChronoUnit.MINUTES)));
    }

    @Test
    void cloneConstructor()
    {
        UserReference userReference = mock(UserReference.class);
        Ranking customRanking = new Ranking()
        {
            @Override
            public String getId()
            {
                return "SOmething";
            }

            @Override
            public String getManagerId()
            {
                return "Foobarbar";
            }

            @Override
            public EntityReference getRankedElement()
            {
                return new EntityReference("Aahahha", EntityType.CLASS_PROPERTY);
            }

            @Override
            public EntityType getEntityType()
            {
                return EntityType.CLASS_PROPERTY;
            }

            @Override
            public UserReference getVoter()
            {
                return userReference;
            }

            @Override
            public Date getCreatedAt()
            {
                return new Date(12);
            }

            @Override
            public Date getUpdatedAt()
            {
                return new Date(243);
            }

            @Override
            public int getRank()
            {
                return 42;
            }

            @Override
            public int getScale()
            {
                return 43;
            }
        };

        DefaultRanking defaultRanking = new DefaultRanking(customRanking);
        assertNotEquals(defaultRanking, customRanking);
        assertEquals("SOmething", defaultRanking.getId());
        assertEquals("Foobarbar", defaultRanking.getManagerId());
        assertEquals(new EntityReference("Aahahha", EntityType.CLASS_PROPERTY), defaultRanking.getRankedElement());
        assertEquals(EntityType.CLASS_PROPERTY, defaultRanking.getEntityType());
        assertSame(userReference, defaultRanking.getVoter());
        assertEquals(new Date(12), defaultRanking.getCreatedAt());
        assertEquals(new Date(243), defaultRanking.getUpdatedAt());
        assertEquals(42, defaultRanking.getRank());
        assertEquals(43, defaultRanking.getScale());

        assertEquals(defaultRanking, new DefaultRanking(defaultRanking));
    }
}
