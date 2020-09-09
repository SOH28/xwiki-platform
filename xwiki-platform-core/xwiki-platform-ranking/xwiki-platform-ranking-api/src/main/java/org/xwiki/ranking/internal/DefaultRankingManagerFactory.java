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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.ranking.RankingConfiguration;
import org.xwiki.ranking.RankingException;
import org.xwiki.ranking.RankingManager;
import org.xwiki.ranking.RankingManagerFactory;

/**
 * Default implementation of {@link RankingManagerFactory}.
 * This implementation performs the following for getting a {@link RankingManager}:
 *   1. it looks in the context component manager for a RankingManager with the given hint, and returns it immediately
 *      if there is one. If there is not, it performs the following steps.
 *   2. it retrieves the {@link RankingConfiguration} based on the given hint, and fallback on the default one if it
 *      cannot find it.
 *   3. it retrieves an instance of a {@link RankingManager} based on the {@link RankingConfiguration#getStorageHint()}
 *   4. it set in the newly instance of {@link RankingManager} some information such as its identifiers
 *      and configuration
 *   5. it creates a new {@link ComponentDescriptor} by copying the descriptor of the retrieved
 *      {@link RankingManager}, modifies it to specify the asked hint and by changing the instatiation strategy
 *      so that it's only instantiated once
 *   6. finally it registers the new component descriptor it in the current component manager so that in next request of
 *      a {@link RankingManager} with the same hint, the retrieved instance will be returned.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Component
@Singleton
public class DefaultRankingManagerFactory implements RankingManagerFactory
{
    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private ComponentManager currentComponentManager;

    private RankingConfiguration getRankingConfiguration(String hint) throws ComponentLookupException
    {
        RankingConfiguration result;
        if (this.contextComponentManager.hasComponent(RankingConfiguration.class, hint)) {
            result = this.contextComponentManager.getInstance(RankingConfiguration.class, hint);
        } else {
            result = this.contextComponentManager.getInstance(RankingConfiguration.class);
        }
        return result;
    }

    @Override
    public RankingManager getInstance(String hint) throws RankingException
    {
        try {
            RankingManager result;
            if (!this.contextComponentManager.hasComponent(RankingManager.class, hint)) {
                // step 2: retrieve the configuration
                RankingConfiguration rankingConfiguration = this.getRankingConfiguration(hint);

                // step 3: use the configuration information to retrieve the RankingManager and its descriptor.
                ComponentDescriptor<RankingManager> componentDescriptor = this.contextComponentManager
                    .getComponentDescriptor(RankingManager.class, rankingConfiguration.getStorageHint());
                result = this.contextComponentManager.getInstance(RankingManager.class,
                    rankingConfiguration.getStorageHint());

                // step 4: set the information of the RankingManager
                result.setRankingConfiguration(rankingConfiguration);
                result.setIdentifer(hint);

                // step 5: copy the descriptor and modifies the hint and the instantiation strategy
                DefaultComponentDescriptor<RankingManager> componentDescriptorCopy =
                    new DefaultComponentDescriptor<>(componentDescriptor);
                componentDescriptorCopy.setRoleHint(hint);
                componentDescriptorCopy.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);

                // step 6: register it in the current component manager for next request.
                this.currentComponentManager.registerComponent(componentDescriptorCopy, result);
            } else {
                // step 1, return directly the component if it can be found.
                result = this.contextComponentManager.getInstance(RankingManager.class, hint);
            }
            return result;
        } catch (ComponentLookupException | ComponentRepositoryException e) {
            throw new RankingException("Error when trying to get a RankingManager", e);
        }
    }
}
