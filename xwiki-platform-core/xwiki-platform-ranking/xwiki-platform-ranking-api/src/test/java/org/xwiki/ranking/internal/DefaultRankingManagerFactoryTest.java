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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ranking.RankingConfiguration;
import org.xwiki.ranking.RankingException;
import org.xwiki.ranking.RankingManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultRankingManagerFactory}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@ComponentTest
public class DefaultRankingManagerFactoryTest
{
    @InjectMockComponents
    private DefaultRankingManagerFactory factory;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    private ComponentManager currentComponentManager;

    @MockComponent
    private RankingManager rankingManager;

    @MockComponent
    private RankingConfiguration rankingConfiguration;

    @Test
    void getExistingInstance() throws Exception
    {
        String hint = "existingInstance";
        when(this.contextComponentManager.hasComponent(RankingManager.class, hint)).thenReturn(true);
        when(this.contextComponentManager.getInstance(RankingManager.class, hint)).thenReturn(rankingManager);

        assertSame(rankingManager, this.factory.getInstance(hint));
        verify(this.currentComponentManager, never()).registerComponent(any(), any());
    }

    @Test
    void getNewInstanceCustomConfiguration() throws Exception
    {
        String hint = "newInstance";
        when(this.contextComponentManager.hasComponent(RankingManager.class, hint)).thenReturn(false);
        when(this.contextComponentManager.hasComponent(RankingConfiguration.class, hint)).thenReturn(true);
        when(this.contextComponentManager.getInstance(RankingConfiguration.class, hint))
            .thenReturn(this.rankingConfiguration);
        when(this.rankingConfiguration.getStorageHint()).thenReturn("someStorage");
        when(this.contextComponentManager.getInstance(RankingManager.class, "someStorage"))
            .thenReturn(this.rankingManager);
        ComponentDescriptor componentDescriptor = mock(ComponentDescriptor.class);
        when(componentDescriptor.getImplementation()).thenReturn(DefaultRankingManager.class);
        when(this.contextComponentManager.getComponentDescriptor(RankingManager.class, "someStorage"))
            .thenReturn(componentDescriptor);
        DefaultComponentDescriptor<RankingManager> expectedComponentDescriptor = new DefaultComponentDescriptor<>();
        expectedComponentDescriptor.setImplementation(DefaultRankingManager.class);
        expectedComponentDescriptor.setRoleHint(hint);
        expectedComponentDescriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);

        assertSame(this.rankingManager, this.factory.getInstance(hint));
        verify(this.currentComponentManager).registerComponent(expectedComponentDescriptor, this.rankingManager);
    }

    @Test
    void getNewInstanceDefaultConfiguration() throws Exception
    {
        String hint = "newInstance";
        when(this.contextComponentManager.hasComponent(RankingManager.class, hint)).thenReturn(false);
        when(this.contextComponentManager.hasComponent(RankingConfiguration.class, hint)).thenReturn(false);
        when(this.contextComponentManager.getInstance(RankingConfiguration.class))
            .thenReturn(this.rankingConfiguration);
        when(this.rankingConfiguration.getStorageHint()).thenReturn("someStorage");
        when(this.contextComponentManager.getInstance(RankingManager.class, "someStorage"))
            .thenReturn(this.rankingManager);
        ComponentDescriptor componentDescriptor = mock(ComponentDescriptor.class);
        when(componentDescriptor.getImplementation()).thenReturn(DefaultRankingManager.class);
        when(this.contextComponentManager.getComponentDescriptor(RankingManager.class, "someStorage"))
            .thenReturn(componentDescriptor);
        DefaultComponentDescriptor<RankingManager> expectedComponentDescriptor = new DefaultComponentDescriptor<>();
        expectedComponentDescriptor.setImplementation(DefaultRankingManager.class);
        expectedComponentDescriptor.setRoleHint(hint);
        expectedComponentDescriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);

        assertSame(this.rankingManager, this.factory.getInstance(hint));
        verify(this.currentComponentManager).registerComponent(expectedComponentDescriptor, this.rankingManager);
    }
}
